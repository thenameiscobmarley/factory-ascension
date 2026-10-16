package dev.factoryascension.client.tarkov;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A client-only mirror of the player's inventory menu with the Tarkov screen's own slot positions. Same slots, same
 * numbers, same items (it reads the same containers), so every click still goes through the game's real inventory menu
 * (the click code always acts on the player's open menu, which is that one); only where the slots are drawn differs.
 * The 27 main slots are hidden: the grid draws and handles them.
 */
public final class TarkovView extends AbstractContainerMenu {
    public final InventoryMenu real;
    /** The screen hides the held item from the game's own drawing while the grid draws it at its size. */
    public java.util.function.BooleanSupplier hide = () -> false;

    public interface Placer {
        int[] at(int index);   // {x, y} for a slot of InventoryMenu, or null to hide it
    }

    public TarkovView(Player player, Placer placer) {
        super(null, 0);
        this.real = player.inventoryMenu;
        for (Slot orig : real.slots) {
            int[] p = placer.at(orig.index);
            boolean shown = p != null;
            addSlot(new Mirror(orig, shown ? p[0] : -10000, shown ? p[1] : -10000, shown));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return real.quickMoveStack(player, index);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // The item on the cursor lives in the real menu
    @Override
    public ItemStack getCarried() {
        return hide.getAsBoolean() ? ItemStack.EMPTY : real.getCarried();
    }

    @Override
    public void setCarried(ItemStack stack) {
        real.setCarried(stack);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return real.canTakeItemForPickAll(stack, real.getSlot(slot.index));
    }

    @Override
    public void removed(Player player) {
        // nothing: closing is handled by the real menu
    }

    static final class Mirror extends Slot {
        final Slot orig;
        final boolean shown;

        Mirror(Slot orig, int x, int y, boolean shown) {
            super(orig.container, orig.getContainerSlot(), x, y);
            this.orig = orig;
            this.shown = shown;
        }

        @Override public boolean isActive() { return shown && orig.isActive(); }
        @Override public boolean mayPlace(ItemStack stack) { return orig.mayPlace(stack); }
        @Override public boolean mayPickup(Player player) { return orig.mayPickup(player); }
        @Override public int getMaxStackSize() { return orig.getMaxStackSize(); }
        @Override public int getMaxStackSize(ItemStack stack) { return orig.getMaxStackSize(stack); }
        @Override public boolean allowModification(Player player) { return orig.allowModification(player); }
        @Nullable @Override public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() { return orig.getNoItemIcon(); }
    }
}
