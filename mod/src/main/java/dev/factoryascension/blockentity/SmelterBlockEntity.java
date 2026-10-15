package dev.factoryascension.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.SmelterBlock;
import dev.factoryascension.menu.SmelterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Super Smelter: a Create machine. Turn its shaft and it smelts a whole batch at once (lanes, by tier), faster the faster
 * it spins. Upgrades (Voidsteel, Celestium, Ascendium) add lanes and stress; the pack's scripts apply them.
 */
public class SmelterBlockEntity extends KineticBlockEntity implements MenuProvider {
    public static final int SLOTS = 9;
    private final ItemStackHandler input = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !result(stack).isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler automation = new CombinedInvWrapper(new Automation.InsertOnly(input), new Automation.ExtractOnly(output));
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> automation);
    private final Map<Item, ItemStack> recipeCache = new HashMap<>();
    private int tier = 0;
    private float progress = 0;
    private long smelted = 0;
    private boolean working = false;

    public SmelterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStackHandler input() { return input; }
    public ItemStackHandler output() { return output; }
    public int tier() { return tier; }
    public int lanes() { return FAConfig.smelterLanes[Math.min(tier, FAConfig.smelterLanes.length - 1)]; }
    public long smelted() { return smelted; }
    public boolean working() { return working; }

    /** Progress of the current batch, 0..1. */
    public float progress() {
        return Math.min(1, progress / cost());
    }

    private float cost() {
        return FAConfig.smelterCookTicks * 64f;
    }

    /** What one of this item smelts into (empty if nothing). Cached per item: smelting results don't depend on NBT. */
    public ItemStack result(ItemStack stack) {
        if (stack.isEmpty() || level == null) return ItemStack.EMPTY;
        return recipeCache.computeIfAbsent(stack.getItem(), item -> level.getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(new ItemStack(item)), level)
            .map(r -> r.getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        float rpm = Math.abs(getSpeed());
        boolean running = rpm >= FAConfig.smelterMinRpm && !isOverStressed() && hasInput();
        if (running) {
            progress += rpm;
            if (progress >= cost()) {
                progress = 0;
                int done = smeltBatch();
                if (done == 0) running = false;
                smelted += done;
            }
        } else if (!hasInput()) {
            progress = 0;
        }
        if (running != working) {
            working = running;
            BlockState state = getBlockState();
            if (state.hasProperty(SmelterBlock.LIT) && state.getValue(SmelterBlock.LIT) != running)
                level.setBlock(worldPosition, state.setValue(SmelterBlock.LIT, running), 3);
        }
    }

    private boolean hasInput() {
        for (int i = 0; i < SLOTS; i++) if (!input.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    /** Smelt up to `lanes` items, one at a time, only when the result fits. Returns how many. */
    private int smeltBatch() {
        int lanes = lanes(), done = 0;
        for (int slot = 0; slot < SLOTS && done < lanes; slot++) {
            while (done < lanes) {
                ItemStack in = input.getStackInSlot(slot);
                if (in.isEmpty()) break;
                ItemStack out = result(in);
                if (out.isEmpty() || !ItemHandlerHelper.insertItemStacked(output, out.copy(), true).isEmpty()) break;
                ItemHandlerHelper.insertItemStacked(output, out.copy(), false);
                input.extractItem(slot, 1, false);
                done++;
            }
        }
        return done;
    }

    // ---- Stress: depends on the tier ----
    @Override
    public float calculateStressApplied() {
        float impact = FAConfig.smelterStress[Math.min(tier, FAConfig.smelterStress.length - 1)];
        lastStressApplied = impact;
        return impact;
    }

    // ---- For the pack's KubeJS scripts (upgrades) ----
    public int faGetTier() { return tier; }
    public int faMaxTier() { return FAConfig.smelterLanes.length - 1; }

    public void faSetTier(int t) {
        tier = Math.max(0, Math.min(faMaxTier(), t));
        if (hasNetwork()) getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        notifyUpdate();
    }

    public long faSmelted() { return smelted; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.super_smelter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SmelterMenu(id, inv, this);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null) return;
        for (ItemStackHandler h : new ItemStackHandler[]{input, output})
            for (int i = 0; i < SLOTS; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
            }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Tier", tier);
        if (clientPacket) return;
        tag.put("Input", input.serializeNBT());
        tag.put("Output", output.serializeNBT());
        tag.putFloat("Progress", progress);
        tag.putLong("Smelted", smelted);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        tier = tag.getInt("Tier");
        if (clientPacket) return;
        if (tag.contains("Input")) input.deserializeNBT(tag.getCompound("Input"));
        if (tag.contains("Output")) output.deserializeNBT(tag.getCompound("Output"));
        progress = tag.getFloat("Progress");
        smelted = tag.getLong("Smelted");
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

    public static SmelterBlockEntity create(BlockPos pos, BlockState state) {
        return new SmelterBlockEntity(FactoryAscension.SMELTER_BE.get(), pos, state);
    }
}
