package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.FusionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Tome Fusion Table screen: three tomes, a core slot, the result, and a Fuse button (menu button 0). */
public class FusionMenu extends AbstractContainerMenu {
    public static final int[][] POS = {{26, 26}, {44, 26}, {62, 26}, {44, 54}, {134, 40}};
    private final FusionBlockEntity table;

    public FusionMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof FusionBlockEntity f ? f : null);
    }

    public FusionMenu(int id, Inventory inv, FusionBlockEntity table) {
        super(FactoryAscension.FUSION_MENU.get(), id);
        this.table = table;
        ItemStackHandler items = table != null ? table.items() : new ItemStackHandler(5);
        for (int i = 0; i < 4; i++) addSlot(new SlotItemHandler(items, i, POS[i][0], POS[i][1]));
        addSlot(new SlotItemHandler(items, FusionBlockEntity.RESULT, POS[4][0], POS[4][1]) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 104);
    }

    public FusionBlockEntity table() {
        return table;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && table != null) {
            table.requestFuse(player);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 5) {
            if (!moveItemStackTo(stack, 5, slots.size(), true)) return ItemStack.EMPTY;
        } else if (FusionBlockEntity.isTome(stack)) {
            if (!moveItemStackTo(stack, 0, FusionBlockEntity.TOMES, false)) return ItemStack.EMPTY;
        } else if (FusionBlockEntity.isCore(stack)) {
            if (!moveItemStackTo(stack, FusionBlockEntity.CORE, FusionBlockEntity.CORE + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return table != null && stillValid(ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()), player, FactoryAscension.FUSION.get());
    }
}
