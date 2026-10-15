package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.ForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Essence Forge screen: the gear slot, the tank level and what the forge is doing. */
public class ForgeMenu extends AbstractContainerMenu {
    private final ForgeBlockEntity forge;
    private final ContainerData data;   // 0 tank mB, 1 tank capacity, 2 levels last second, 3 total levels (capped)

    public ForgeMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof ForgeBlockEntity f ? f : null);
    }

    public ForgeMenu(int id, Inventory inv, ForgeBlockEntity forge) {
        super(FactoryAscension.FORGE_MENU.get(), id);
        this.forge = forge;
        ItemStackHandler items = forge != null ? forge.items() : new ItemStackHandler(1);
        addSlot(new SlotItemHandler(items, 0, 80, 52));
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 104);
        data = forge != null && !inv.player.level().isClientSide ? new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> forge.tank().getFluidAmount();
                    case 1 -> forge.tank().getCapacity();
                    case 2 -> forge.levelsLastSecond();
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, forge.totalLevels());
                    default -> 0;
                };
            }
            public void set(int i, int v) { }
            public int getCount() { return 4; }
        } : new SimpleContainerData(4);
        addDataSlots(data);
    }

    public int fluid() { return data.get(0); }
    public int capacity() { return Math.max(1, data.get(1)); }
    public int levelsLastSecond() { return data.get(2); }
    public int totalLevels() { return data.get(3); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return forge != null && stillValid(ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos()), player, FactoryAscension.FORGE.get());
    }
}
