package dev.factoryascension.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * Ascension Quarry: turn its shaft and it mines out the square below it (FAConfig.quarryRadius around it), one layer at a
 * time, down to the bottom of the world. Faster the faster it spins. It mines like a diamond pickaxe and skips bedrock,
 * fluids and anything with an inventory or machine inside. What it digs goes into the inventory on top of it (or waits
 * inside, for hoppers and funnels, until there's room).
 */
public class QuarryBlockEntity extends KineticBlockEntity implements KineticMachineBlock.Status, KineticMachineBlock.Buffered {
    private final Output out = new Output(18, this::setChanged);
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new dev.factoryascension.blockentity.Automation.ExtractOnly(out.items));
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);
    private int layer = Integer.MIN_VALUE;   // the y being mined (MIN_VALUE: not started)
    private int cursor = 0;                  // index into the current layer's square
    private float work = 0;
    private long mined = 0;
    private boolean done = false;

    public QuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static QuarryBlockEntity create(BlockPos pos, BlockState state) {
        return new QuarryBlockEntity(FactoryAscension.QUARRY_BE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || done) return;
        if (level.getGameTime() % 10 == 0) out.push(level, worldPosition, Direction.UP);
        float rpm = Math.abs(getSpeed());
        if (rpm < FAConfig.quarryMinRpm || isOverStressed() || !out.hasRoom()) return;
        if (layer == Integer.MIN_VALUE) layer = worldPosition.getY() - 1;
        work += rpm;
        int r = FAConfig.quarryRadius, side = 2 * r + 1, steps = 0;
        // Each block costs quarryWork; empty spots are skipped for free (up to a whole layer per tick)
        while (work >= FAConfig.quarryWork && steps < FAConfig.quarryMaxPerTick && out.hasRoom()) {
            int scanned = 0;
            boolean minedOne = false;
            while (!minedOne && scanned++ < side * side) {
                if (cursor >= side * side) {
                    cursor = 0;
                    layer--;
                    if (layer < level.getMinBuildHeight()) { done = true; setChanged(); return; }
                }
                BlockPos p = new BlockPos(worldPosition.getX() - r + cursor % side, layer, worldPosition.getZ() - r + cursor / side);
                cursor++;
                minedOne = dig(p);
            }
            if (!minedOne) return;
            work -= FAConfig.quarryWork;
            steps++;
        }
        setChanged();
    }

    /** Mine one block if it's minable. */
    private boolean dig(BlockPos p) {
        BlockState state = level.getBlockState(p);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, p) < 0 || level.getBlockEntity(p) != null
            || state.is(Blocks.BEDROCK)) return false;
        for (ItemStack drop : Block.getDrops(state, (ServerLevel) level, p, null, null, TOOL)) out.add(drop);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        if (mined % 8 == 0) level.levelEvent(2001, p, Block.getId(state));
        mined++;
        return true;
    }

    @Override
    public Component status() {
        if (done) return Component.literal("Quarry: finished, " + mined + " blocks mined");
        float rpm = Math.abs(getSpeed());
        String why = rpm < FAConfig.quarryMinRpm ? " (needs at least " + (int) FAConfig.quarryMinRpm + " RPM)" : isOverStressed() ? " (overstressed)"
            : !out.hasRoom() ? " (full: put a chest on top)" : "";
        return Component.literal("Quarry: layer y=" + (layer == Integer.MIN_VALUE ? worldPosition.getY() - 1 : layer) + ", " + mined + " blocks mined" + why);
    }

    @Override
    public java.util.List<String[]> info() {
        float rpm = Math.abs(getSpeed());
        boolean slow = rpm < FAConfig.quarryMinRpm;
        return java.util.List.of(
            new String[] {"State", done ? "Finished" : slow ? "Stopped: too slow" : isOverStressed() ? "Stopped: overstressed" : !out.hasRoom() ? "Stopped: output full" : "Mining",
                done || slow || isOverStressed() || !out.hasRoom() ? "bad" : "ok"},
            new String[] {"Speed", Math.round(rpm) + " RPM (needs " + (int) FAConfig.quarryMinRpm + ")", slow ? "bad" : "ok"},
            new String[] {"Stress", (int) FAConfig.quarryStress + " SU" + (isOverStressed() ? ", the network can't carry it" : ""), isOverStressed() ? "bad" : ""},
            new String[] {"Layer", "y = " + (layer == Integer.MIN_VALUE ? worldPosition.getY() - 1 : layer), ""},
            new String[] {"Area", (FAConfig.quarryRadius * 2 + 1) + " x " + (FAConfig.quarryRadius * 2 + 1) + " blocks", ""},
            new String[] {"Mined", String.valueOf(mined), ""},
            new String[] {"Output", out.hasRoom() ? "Into the chest on top" : "Full: put a chest on top", out.hasRoom() ? "ok" : "bad"});
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = FAConfig.quarryStress;
        return FAConfig.quarryStress;
    }

    @Override
    public void dropBuffer(Level level, BlockPos pos) {
        out.drop(level, pos);
    }

    @Override
    public <C> LazyOptional<C> getCapability(Capability<C> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Layer", layer);
        tag.putInt("Cursor", cursor);
        tag.putFloat("Work", work);
        tag.putLong("Mined", mined);
        tag.putBoolean("Done", done);
        if (!clientPacket) tag.put("Out", out.items.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        layer = tag.contains("Layer") ? tag.getInt("Layer") : Integer.MIN_VALUE;
        cursor = tag.getInt("Cursor");
        work = tag.getFloat("Work");
        mined = tag.getLong("Mined");
        done = tag.getBoolean("Done");
        if (!clientPacket && tag.contains("Out")) out.items.deserializeNBT(tag.getCompound("Out"));
    }

    // For the self-test
    public long faMined() { return mined; }
    public int faLayer() { return layer; }
    public IItemHandler faOutput() { return out.items; }
}
