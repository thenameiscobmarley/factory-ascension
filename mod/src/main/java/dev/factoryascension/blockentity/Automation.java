package dev.factoryascension.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Item handler views for hoppers and pipes: inputs they can only fill, outputs they can only empty. */
public final class Automation {
    private Automation() {
    }

    public record InsertOnly(IItemHandlerModifiable inner) implements IItemHandlerModifiable {
        public int getSlots() { return inner.getSlots(); }
        public ItemStack getStackInSlot(int s) { return inner.getStackInSlot(s); }
        public ItemStack insertItem(int s, ItemStack st, boolean sim) { return inner.insertItem(s, st, sim); }
        public ItemStack extractItem(int s, int n, boolean sim) { return ItemStack.EMPTY; }
        public int getSlotLimit(int s) { return inner.getSlotLimit(s); }
        public boolean isItemValid(int s, ItemStack st) { return inner.isItemValid(s, st); }
        public void setStackInSlot(int s, ItemStack st) { inner.setStackInSlot(s, st); }
    }

    public record ExtractOnly(IItemHandlerModifiable inner) implements IItemHandlerModifiable {
        public int getSlots() { return inner.getSlots(); }
        public ItemStack getStackInSlot(int s) { return inner.getStackInSlot(s); }
        public ItemStack insertItem(int s, ItemStack st, boolean sim) { return st; }
        public ItemStack extractItem(int s, int n, boolean sim) { return inner.extractItem(s, n, sim); }
        public int getSlotLimit(int s) { return inner.getSlotLimit(s); }
        public boolean isItemValid(int s, ItemStack st) { return false; }
        public void setStackInSlot(int s, ItemStack st) { inner.setStackInSlot(s, st); }
    }
}
