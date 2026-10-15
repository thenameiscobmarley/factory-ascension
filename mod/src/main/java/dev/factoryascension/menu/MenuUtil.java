package dev.factoryascension.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

final class MenuUtil {
    private MenuUtil() {
    }

    /** The player's inventory (3 rows) and hotbar, with the top-left slot at (x, y). Returns how many slots were added. */
    static void addPlayerSlots(java.util.function.Consumer<Slot> add, Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                add.accept(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++) add.accept(new Slot(inv, col, x + col * 18, y + 58));
    }
}
