package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.WorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Ascension Workbench screen: gear slot, modifier slot, Apply (button 0) and Star Up (button 1). */
public class WorkbenchMenu extends AbstractContainerMenu {
    public static final int GEAR_X = 26, GEAR_Y = 34, MOD_X = 26, MOD_Y = 74, INV_X = 35, INV_Y = 150;
    public static final int BTN_APPLY = 0, BTN_STAR = 1;
    private final WorkbenchBlockEntity bench;

    public WorkbenchMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof WorkbenchBlockEntity b ? b : null);
    }

    public WorkbenchMenu(int id, Inventory inv, WorkbenchBlockEntity bench) {
        super(FactoryAscension.WORKBENCH_MENU.get(), id);
        this.bench = bench;
        ItemStackHandler items = bench != null ? bench.items() : new ItemStackHandler(2);
        addSlot(new SlotItemHandler(items, WorkbenchBlockEntity.GEAR, GEAR_X, GEAR_Y));
        addSlot(new SlotItemHandler(items, WorkbenchBlockEntity.MOD, MOD_X, MOD_Y));
        MenuUtil.addPlayerSlots(this::addSlot, inv, INV_X, INV_Y);
    }

    public WorkbenchBlockEntity bench() {
        return bench;
    }

    public ItemStack gear() {
        return slots.get(0).getItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null) return false;
        if (id == BTN_APPLY) bench.request("bench_apply", player);
        else if (id == BTN_STAR) bench.request("bench_star", player);
        else return false;
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getMaxStackSize() == 1 && !slots.get(0).hasItem() && !isModifier(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Tomes and the like go to the modifier slot even though they don't stack. */
    private static boolean isModifier(ItemStack stack) {
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !"kubejs".equals(key.getNamespace())) return false;
        String p = key.getPath();
        return p.startsWith("tome_") || p.startsWith("gem_") || p.startsWith("attachment_") || p.equals("chaos_tome") || p.equals("polishing_cloth");
    }

    @Override
    public boolean stillValid(Player player) {
        return bench != null && Container.stillValidBlockEntity(bench, player);
    }
}
