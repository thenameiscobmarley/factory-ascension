package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.SalvagerBlockEntity;
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

/** Salvager screen: 9 loot slots, 9 output slots, the Essence bank and totals. */
public class SalvagerMenu extends AbstractContainerMenu {
    private final SalvagerBlockEntity salvager;
    private final ContainerData data;   // 0 bank x100, 1 items salvaged, 2 Essence earned

    public SalvagerMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof SalvagerBlockEntity s ? s : null);
    }

    public SalvagerMenu(int id, Inventory inv, SalvagerBlockEntity salvager) {
        super(FactoryAscension.SALVAGER_MENU.get(), id);
        this.salvager = salvager;
        ItemStackHandler in = salvager != null ? salvager.input() : new ItemStackHandler(SalvagerBlockEntity.SLOTS);
        ItemStackHandler out = salvager != null ? salvager.output() : new ItemStackHandler(SalvagerBlockEntity.SLOTS);
        for (int c = 0; c < 9; c++) addSlot(new SlotItemHandler(in, c, 8 + c * 18, 24));
        for (int c = 0; c < 9; c++)
            addSlot(new SlotItemHandler(out, c, 8 + c * 18, 66) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 104);
        data = salvager != null && !inv.player.level().isClientSide ? new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> (int) Math.min(Integer.MAX_VALUE, Math.round(salvager.bank() * 100));
                    case 1 -> (int) Math.min(Integer.MAX_VALUE, salvager.salvaged());
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, Math.round(salvager.earned()));
                    default -> 0;
                };
            }
            public void set(int i, int v) { }
            public int getCount() { return 3; }
        } : new SimpleContainerData(3);
        addDataSlots(data);
    }

    public double bank() { return data.get(0) / 100.0; }
    public int salvaged() { return data.get(1); }
    public int earned() { return data.get(2); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = SalvagerBlockEntity.SLOTS * 2;
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SalvagerBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return salvager != null && stillValid(ContainerLevelAccess.create(salvager.getLevel(), salvager.getBlockPos()), player, FactoryAscension.SALVAGER.get());
    }
}
