package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.OpenerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Pack Opener screen: the pack slot, 27 output slots, and (drawn by the screen) the latest cards flipping over. */
public class OpenerMenu extends AbstractContainerMenu {
    private final OpenerBlockEntity opener;

    public OpenerMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof OpenerBlockEntity o ? o : null);
    }

    public OpenerMenu(int id, Inventory inv, OpenerBlockEntity opener) {
        super(FactoryAscension.OPENER_MENU.get(), id);
        this.opener = opener;
        ItemStackHandler in = opener != null ? opener.input() : new ItemStackHandler(1);
        ItemStackHandler out = opener != null ? opener.output() : new ItemStackHandler(OpenerBlockEntity.OUTPUTS);
        addSlot(new SlotItemHandler(in, 0, 8, 54));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new SlotItemHandler(out, r * 9 + c, 8 + c * 18, 80 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 144);
    }

    public OpenerBlockEntity opener() {
        return opener;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = 1 + OpenerBlockEntity.OUTPUTS;
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!OpenerBlockEntity.isPack(stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return opener != null && stillValid(ContainerLevelAccess.create(opener.getLevel(), opener.getBlockPos()), player, FactoryAscension.OPENER.get());
    }
}
