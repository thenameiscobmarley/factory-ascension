package dev.factoryascension.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Many inventories (the chests linked to a terminal) seen as one long list of slots. A chest that has been broken
 * since the list was built reads as empty and takes nothing, so items can never be written into a removed block.
 */
public class LinkedInventory implements IItemHandlerModifiable {
    private record Part(BlockEntity be, IItemHandler handler, int start, int size) {
    }

    private final List<Part> parts = new ArrayList<>();
    private int size = 0;

    public void clear() {
        parts.clear();
        size = 0;
    }

    public void add(BlockEntity be, IItemHandler handler) {
        int n = handler.getSlots();
        parts.add(new Part(be, handler, size, n));
        size += n;
    }

    public int parts() {
        return parts.size();
    }

    private Part part(int slot) {
        if (slot < 0 || slot >= size) return null;
        int lo = 0, hi = parts.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (parts.get(mid).start <= slot) lo = mid;
            else hi = mid - 1;
        }
        Part p = parts.get(lo);
        // Gone, or shrunk since the list was built (a vault downgraded, a modded chest resized)
        if (p.be.isRemoved() || slot - p.start >= p.handler.getSlots()) return null;
        return p;
    }

    @Override
    public int getSlots() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Part p = part(slot);
        return p == null ? ItemStack.EMPTY : p.handler.getStackInSlot(slot - p.start);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        Part p = part(slot);
        return p == null ? stack : p.handler.insertItem(slot - p.start, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        Part p = part(slot);
        return p == null ? ItemStack.EMPTY : p.handler.extractItem(slot - p.start, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        Part p = part(slot);
        return p == null ? 0 : p.handler.getSlotLimit(slot - p.start);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        Part p = part(slot);
        return p != null && p.handler.isItemValid(slot - p.start, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        Part p = part(slot);
        if (p == null) return;
        int i = slot - p.start;
        if (p.handler instanceof IItemHandlerModifiable m) {
            m.setStackInSlot(i, stack);
        } else {
            // A read-only view: take out what's there, then put the new stack in
            ItemStack had = p.handler.getStackInSlot(i);
            if (!had.isEmpty()) p.handler.extractItem(i, had.getCount(), false);
            if (!stack.isEmpty()) p.handler.insertItem(i, stack, false);
        }
        p.be.setChanged();
    }

    /**
     * Merge and sort everything across the chests that allow it (their slots in link order). If anything wouldn't fit
     * back (a slot that refuses an item, like a shulker box refusing another shulker box), nothing changes.
     */
    public void sort() {
        List<int[]> slots = new ArrayList<>();          // [part, local slot]
        for (int k = 0; k < parts.size(); k++) {
            Part p = parts.get(k);
            if (p.be.isRemoved() || !(p.handler instanceof IItemHandlerModifiable)) continue;
            for (int i = 0; i < p.handler.getSlots(); i++) slots.add(new int[]{k, i});
        }
        List<ItemStack> before = new ArrayList<>(), all = new ArrayList<>();
        for (int[] s : slots) {
            ItemStack st = parts.get(s[0]).handler.getStackInSlot(s[1]);
            before.add(st.copy());
            if (st.isEmpty()) continue;
            ItemStack left = st.copy();
            for (ItemStack have : all) {
                if (left.isEmpty()) break;
                if (ItemStack.isSameItemSameTags(have, left) && have.getCount() < have.getMaxStackSize()) {
                    int move = Math.min(left.getCount(), have.getMaxStackSize() - have.getCount());
                    have.grow(move);
                    left.shrink(move);
                }
            }
            if (!left.isEmpty()) all.add(left);
        }
        all.sort(Comparator.comparing((ItemStack s) -> String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()))).thenComparing(s -> -s.getCount()));
        // Plan first: each slot takes the first remaining stack it accepts
        ItemStack[] plan = new ItemStack[slots.size()];
        for (int n = 0; n < slots.size(); n++) {
            int[] s = slots.get(n);
            IItemHandler h = parts.get(s[0]).handler;
            plan[n] = ItemStack.EMPTY;
            for (int j = 0; j < all.size(); j++) {
                ItemStack st = all.get(j);
                if (h.isItemValid(s[1], st) && st.getCount() <= h.getSlotLimit(s[1])) {
                    plan[n] = st;
                    all.remove(j);
                    break;
                }
            }
        }
        if (!all.isEmpty()) return;
        for (int n = 0; n < slots.size(); n++) {
            int[] s = slots.get(n);
            Part p = parts.get(s[0]);
            if (!ItemStack.matches(before.get(n), plan[n])) ((IItemHandlerModifiable) p.handler).setStackInSlot(s[1], plan[n]);
        }
        parts.forEach(p -> p.be.setChanged());
    }
}
