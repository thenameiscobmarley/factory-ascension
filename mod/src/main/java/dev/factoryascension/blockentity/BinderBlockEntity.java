package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.menu.BinderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * Card Binder: 54 slots that only take enchant tomes ("cards"). Counts the different cards inside against every
 * tome the pack has, and shows the best one (highest foil, then highest level) floating on top of the block.
 */
public class BinderBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents {
    public static final int SLOTS = 54;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isCard(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            refreshShowcase();
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private ItemStack showcase = ItemStack.EMPTY;

    public BinderBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.BINDER_BE.get(), pos, state);
    }

    public static boolean isCard(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "kubejs".equals(key.getNamespace()) && key.getPath().startsWith("tome_");
    }

    /** How many different tomes exist (all kubejs:tome_* items). */
    public static int totalCards() {
        int n = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            var key = ForgeRegistries.ITEMS.getKey(item);
            if (key != null && "kubejs".equals(key.getNamespace()) && key.getPath().startsWith("tome_")) n++;
        }
        return n;
    }

    public ItemStackHandler items() {
        return items;
    }

    public int uniqueCards() {
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) seen.add(s.getItem());
        }
        return seen.size();
    }

    public ItemStack showcase() {
        return showcase;
    }

    private static long score(ItemStack s) {
        CompoundTag tag = s.getTag();
        if (tag == null) return 1;
        long foil = tag.getInt("CustomModelData");
        CompoundTag fa = tag.getCompound("FactoryAscension");
        long level = 1;
        for (String k : fa.getAllKeys()) if (fa.get(k) instanceof net.minecraft.nbt.IntTag) level = Math.max(level, fa.getInt(k));
        return foil * 10_000_000_000L + level;
    }

    private void refreshShowcase() {
        ItemStack best = ItemStack.EMPTY;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty() && (best.isEmpty() || score(s) > score(best))) best = s;
        }
        showcase = best.isEmpty() ? ItemStack.EMPTY : best.copyWithCount(1);
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.card_binder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BinderMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        if (!showcase.isEmpty()) tag.put("Showcase", showcase.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        showcase = tag.contains("Showcase") ? ItemStack.of(tag.getCompound("Showcase")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        // Clients only need the showcase card for the renderer
        CompoundTag tag = new CompoundTag();
        if (!showcase.isEmpty()) tag.put("Showcase", showcase.save(new CompoundTag()));
        return tag;
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
