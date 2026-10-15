package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.SmelterBlockEntity;
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

/** Super Smelter screen: 9 inputs, 9 outputs, the batch progress, speed, lanes and tier. */
public class SmelterMenu extends AbstractContainerMenu {
    private final SmelterBlockEntity smelter;
    private final ContainerData data;   // 0 progress x1000, 1 |rpm|, 2 lanes, 3 tier, 4 smelted, 5 overstressed

    public SmelterMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof SmelterBlockEntity s ? s : null);
    }

    public SmelterMenu(int id, Inventory inv, SmelterBlockEntity smelter) {
        super(FactoryAscension.SMELTER_MENU.get(), id);
        this.smelter = smelter;
        ItemStackHandler in = smelter != null ? smelter.input() : new ItemStackHandler(SmelterBlockEntity.SLOTS);
        ItemStackHandler out = smelter != null ? smelter.output() : new ItemStackHandler(SmelterBlockEntity.SLOTS);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) addSlot(new SlotItemHandler(in, r * 3 + c, 8 + c * 18, 22 + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                addSlot(new SlotItemHandler(out, r * 3 + c, 116 + c * 18, 22 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 104);
        data = smelter != null && !inv.player.level().isClientSide ? new ContainerData() {
            public int get(int i) {
                return switch (i) {
                    case 0 -> Math.round(smelter.progress() * 1000);
                    case 1 -> Math.round(Math.abs(smelter.getSpeed()));
                    case 2 -> smelter.lanes();
                    case 3 -> smelter.tier();
                    case 4 -> (int) Math.min(Integer.MAX_VALUE, smelter.smelted());
                    case 5 -> smelter.isOverStressed() ? 1 : 0;
                    default -> 0;
                };
            }
            public void set(int i, int v) { }
            public int getCount() { return 6; }
        } : new SimpleContainerData(6);
        addDataSlots(data);
    }

    public float progress() { return data.get(0) / 1000f; }
    public int rpm() { return data.get(1); }
    public int lanes() { return data.get(2); }
    public int tier() { return data.get(3); }
    public int smelted() { return data.get(4); }
    public boolean overstressed() { return data.get(5) != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = SmelterBlockEntity.SLOTS * 2;
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (smelter == null || smelter.result(stack).isEmpty() || !moveItemStackTo(stack, 0, SmelterBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return smelter != null && stillValid(ContainerLevelAccess.create(smelter.getLevel(), smelter.getBlockPos()), player, FactoryAscension.SMELTER.get());
    }
}
