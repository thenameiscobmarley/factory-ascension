package dev.factoryascension.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** A machine's output buffer: results wait here, and go into the inventory on the given side whenever it has room. */
public final class Output {
    public final ItemStackHandler items;

    public Output(int slots, Runnable changed) {
        items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                changed.run();
            }
        };
    }

    /** Room for more? (Machines stop when it's full.) */
    public boolean hasRoom() {
        for (int i = 0; i < items.getSlots(); i++) if (items.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    public void add(ItemStack stack) {
        ItemStack left = ItemHandlerHelper.insertItemStacked(items, stack, false);
        if (!left.isEmpty()) items.setStackInSlot(items.getSlots() - 1, left);   // never lose anything: the last slot overflows
    }

    /** Move what fits into the inventory on side `to` of pos. */
    public void push(Level level, BlockPos pos, Direction to) {
        BlockEntity be = level.getBlockEntity(pos.relative(to));
        if (be == null) return;
        IItemHandler target = be.getCapability(ForgeCapabilities.ITEM_HANDLER, to.getOpposite()).orElse(null);
        if (target == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack left = ItemHandlerHelper.insertItemStacked(target, s.copy(), false);
            items.setStackInSlot(i, left);
        }
    }

    public void drop(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        for (int i = 0; i < items.getSlots(); i++) items.setStackInSlot(i, ItemStack.EMPTY);
    }
}
