package dev.factoryascension.gun;

import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Weapons Workbench's slots: the gun being worked on, the Ammo Press's case and powder inputs, and your inventory.
 * Whatever is left in the bench comes back to you when you close it.
 */
public class WeaponsBenchMenu extends AbstractContainerMenu {
    public static final int GUN = 0, CASE = 1, POWDER = 2;
    public static final int W = 400, H = 232;
    public final SimpleContainer bench = new SimpleContainer(3);
    public final BlockPos pos;

    public WeaponsBenchMenu(int id, Inventory inv, BlockPos pos) {
        super(FactoryAscension.WEAPONS_BENCH_MENU.get(), id);
        this.pos = pos;
        addSlot(new Slot(bench, GUN, 12, 24) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof GunItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(bench, CASE, 12, 70));
        addSlot(new Slot(bench, POWDER, 12, 94));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 154 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 212));
    }

    public ItemStack gun() {
        return bench.getItem(GUN);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), copy = s.copy();
        if (index < 3) {
            if (!moveItemStackTo(s, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (s.getItem() instanceof GunItem) {
            if (!moveItemStackTo(s, GUN, GUN + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, CASE, POWDER + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64
            && player.level().getBlockState(pos).getBlock() instanceof WeaponsBenchBlock;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, bench);
    }

    public Container container() {
        return bench;
    }
}
