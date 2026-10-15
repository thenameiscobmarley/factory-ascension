package dev.factoryascension.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Warp Hopper: items put into it (by a hopper, funnel or chute) go straight into the inventory UNDER another Warp Hopper
 * with the same key, at any distance and in any dimension (as long as that one is loaded). It takes the turn in order,
 * so several receivers share the load. If no receiver has room, it accepts nothing (the hopper feeding it just waits).
 */
public class WarpHopperBlockEntity extends BlockEntity {
    private static final Set<WarpHopperBlockEntity> LOADED = ConcurrentHashMap.newKeySet();
    private String key = "";
    private int turn = 0;
    private long sent = 0;
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new Sender());

    public WarpHopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setKey(Item item) {
        key = String.valueOf(BuiltInRegistries.ITEM.getKey(item));
        setChanged();
    }

    public String key() { return key; }

    /** How many Warp Hoppers are registered as loaded (the leak check: it must drop again when they're removed). */
    public static int faLoadedCount() { return LOADED.size(); }
    public long sent() { return sent; }

    /** Other loaded Warp Hoppers with this one's key. */
    private List<WarpHopperBlockEntity> others() {
        List<WarpHopperBlockEntity> list = new ArrayList<>();
        if (key.isEmpty()) return list;
        for (WarpHopperBlockEntity w : LOADED) if (w != this && !w.isRemoved() && w.level != null && key.equals(w.key)) list.add(w);
        list.sort((a, b) -> a.worldPosition.compareTo(b.worldPosition));
        return list;
    }

    public int peers() { return others().size(); }

    /** The inventory under a receiver. */
    private IItemHandler below() {
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(worldPosition.below());
        return be == null || be instanceof WarpHopperBlockEntity ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
    }

    public Component status() {
        if (key.isEmpty()) return Component.literal("Warp Hopper: not keyed. Right-click it with any item to key it.");
        ResourceLocation id = ResourceLocation.tryParse(key);
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return Component.literal("Warp Hopper keyed to ").append(item == null ? Component.literal(key) : new ItemStack(item).getHoverName())
            .append(": " + peers() + " linked, " + sent + " items sent");
    }

    /** Insert: forward to the next receiver with room. */
    private class Sender implements IItemHandler {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return 64; }
        public boolean isItemValid(int slot, ItemStack stack) { return true; }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            List<WarpHopperBlockEntity> to = others();
            if (to.isEmpty() || stack.isEmpty()) return stack;
            for (int i = 0; i < to.size(); i++) {
                WarpHopperBlockEntity w = to.get((turn + i) % to.size());
                IItemHandler target = w.below();
                if (target == null) continue;
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack, simulate);
                if (left.getCount() < stack.getCount()) {
                    if (!simulate) {
                        turn = (turn + i + 1) % to.size();
                        sent += stack.getCount() - left.getCount();
                        setChanged();
                    }
                    return left;
                }
            }
            return stack;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    @Override
    public <C> LazyOptional<C> getCapability(Capability<C> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != Direction.DOWN) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Key", key);
        tag.putLong("Sent", sent);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        key = tag.getString("Key");
        sent = tag.getLong("Sent");
    }

    // For the self-test
    public IItemHandler faInput() { return itemCap.orElse(null); }
}
