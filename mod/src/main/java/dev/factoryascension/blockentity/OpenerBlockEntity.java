package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.menu.OpenerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Pack Opener: an input slot for Enchantment Packs (and Booster Boxes) and 27 output slots. The pack's KubeJS scripts open
 * one pack a second into the outputs and show each card with faShowCard; the screen flips the latest cards over.
 * Hoppers and pipes put packs into the input and take cards out of the outputs.
 */
public class OpenerBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents {
    public static final int OUTPUTS = 27;
    public static final int SHOWN = 5;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isPack(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(OUTPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    // Automation sees the input as insert-only and the outputs as extract-only
    private final IItemHandler automation = new CombinedInvWrapper(new InsertOnly(input), new ExtractOnly(output));
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> automation);
    private final List<ItemStack> recent = new ArrayList<>();
    private int opened = 0;
    /** Client only: when the latest cards arrived, for the flip animation. */
    public long revealedAt = 0;

    public OpenerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.OPENER_BE.get(), pos, state);
    }

    public static boolean isPack(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "kubejs".equals(key.getNamespace()) && (key.getPath().equals("enchantment_pack") || key.getPath().equals("booster_box"));
    }

    public ItemStackHandler input() {
        return input;
    }

    public ItemStackHandler output() {
        return output;
    }

    public List<ItemStack> recent() {
        return recent;
    }

    public int opened() {
        return opened;
    }

    // ---- For the pack's KubeJS scripts ----
    public ItemStackHandler faInput() {
        return input;
    }

    public ItemStackHandler faOutput() {
        return output;
    }

    /** Start a new reveal (call before showing a pack's cards). */
    public void faNewPack() {
        recent.clear();
        opened++;
    }

    /** Show one card from the pack being opened. */
    public void faShowCard(ItemStack card) {
        if (recent.size() < SHOWN) recent.add(card.copyWithCount(1));
    }

    /** Send the revealed cards to players looking at the screen. */
    public void faFlush() {
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.pack_opener");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OpenerMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        List<ItemStack> all = new ArrayList<>();
        all.add(input.getStackInSlot(0));
        for (int i = 0; i < OUTPUTS; i++) all.add(output.getStackInSlot(i));
        for (ItemStack s : all) if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
    }

    private CompoundTag recentTag(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ItemStack s : recent) list.add(s.save(new CompoundTag()));
        tag.put("Recent", list);
        tag.putInt("Opened", opened);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Input", input.serializeNBT());
        tag.put("Output", output.serializeNBT());
        recentTag(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Input")) input.deserializeNBT(tag.getCompound("Input"));
        if (tag.contains("Output")) output.deserializeNBT(tag.getCompound("Output"));
        recent.clear();
        for (Tag t : tag.getList("Recent", Tag.TAG_COMPOUND)) recent.add(ItemStack.of((CompoundTag) t));
        int was = opened;
        opened = tag.getInt("Opened");
        if (level != null && level.isClientSide && opened != was) revealedAt = System.currentTimeMillis();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return recentTag(new CompoundTag());
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

    private record InsertOnly(IItemHandlerModifiable inner) implements IItemHandlerModifiable {
        public int getSlots() { return inner.getSlots(); }
        public ItemStack getStackInSlot(int s) { return inner.getStackInSlot(s); }
        public ItemStack insertItem(int s, ItemStack st, boolean sim) { return inner.insertItem(s, st, sim); }
        public ItemStack extractItem(int s, int n, boolean sim) { return ItemStack.EMPTY; }
        public int getSlotLimit(int s) { return inner.getSlotLimit(s); }
        public boolean isItemValid(int s, ItemStack st) { return inner.isItemValid(s, st); }
        public void setStackInSlot(int s, ItemStack st) { inner.setStackInSlot(s, st); }
    }

    private record ExtractOnly(IItemHandlerModifiable inner) implements IItemHandlerModifiable {
        public int getSlots() { return inner.getSlots(); }
        public ItemStack getStackInSlot(int s) { return inner.getStackInSlot(s); }
        public ItemStack insertItem(int s, ItemStack st, boolean sim) { return st; }
        public ItemStack extractItem(int s, int n, boolean sim) { return inner.extractItem(s, n, sim); }
        public int getSlotLimit(int s) { return inner.getSlotLimit(s); }
        public boolean isItemValid(int s, ItemStack st) { return false; }
        public void setStackInSlot(int s, ItemStack st) { inner.setStackInSlot(s, st); }
    }
}
