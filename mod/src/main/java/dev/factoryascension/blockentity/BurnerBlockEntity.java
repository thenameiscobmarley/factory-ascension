package dev.factoryascension.blockentity;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.BurnerBlock;
import dev.factoryascension.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Burner Module: holds fuel (any furnace fuel; hoppers and chutes can fill it). The Engine Core burns it, one Burner per few Coils. */
public class BurnerBlockEntity extends SyncedBlockEntity implements MachineBlock.DropsContents {
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return burnTime(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> fuel);
    private int burn = 0, burnTotal = 0;

    public BurnerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.BURNER_BE.get(), pos, state);
    }

    public static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    public ItemStackHandler fuel() { return fuel; }
    public boolean burning() { return burn > 0; }
    public int burnTicks() { return burn; }
    public int burnTotal() { return burnTotal; }
    /** Seconds of burning left, counting the fuel waiting in the slot. */
    public int secondsLeft() {
        ItemStack s = fuel.getStackInSlot(0);
        return (int) ((burn + burnTime(s) * s.getCount() * FAConfig.engineFuelMultiplier) / 20);
    }

    /** Called by the Engine Core every tick it needs this Burner: burn, lighting the next fuel when it runs out. Returns lit. */
    public boolean burnTick() {
        if (burn <= 0) {
            ItemStack s = fuel.getStackInSlot(0);
            int t = burnTime(s);
            if (t <= 0) {
                setLit(false);
                return false;
            }
            ItemStack container = s.getCraftingRemainingItem();
            fuel.extractItem(0, 1, false);
            if (!container.isEmpty() && fuel.getStackInSlot(0).isEmpty()) fuel.setStackInSlot(0, container);   // lava bucket -> bucket
            burn = burnTotal = Math.max(1, Math.round(t * FAConfig.engineFuelMultiplier));
        }
        burn--;
        setLit(true);
        return true;
    }

    /** The core stopped needing this Burner (no coils left for it): let it go out. */
    public void idle() {
        if (burn <= 0) setLit(false);
    }

    private void setLit(boolean lit) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(BurnerBlock.LIT) && state.getValue(BurnerBlock.LIT) != lit)
            level.setBlock(worldPosition, state.setValue(BurnerBlock.LIT, lit), 3);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        ItemStack s = fuel.getStackInSlot(0);
        if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fuel", fuel.serializeNBT());
        tag.putInt("Burn", burn);
        tag.putInt("BurnTotal", burnTotal);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Fuel")) fuel.deserializeNBT(tag.getCompound("Fuel"));
        burn = tag.getInt("Burn");
        burnTotal = tag.getInt("BurnTotal");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return new CompoundTag();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}
