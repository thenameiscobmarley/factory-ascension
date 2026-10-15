package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.MiniCrusherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Mini Crusher screen: motor and battery on the left, input, progress, four outputs. */
public class MiniCrusherMenu extends AbstractContainerMenu {
    public static final int[][] POS = {{62, 34}, {116, 25}, {134, 25}, {116, 43}, {134, 43}, {17, 25}, {17, 50}};
    private final MiniCrusherBlockEntity crusher;
    private final ContainerData data;

    public MiniCrusherMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof MiniCrusherBlockEntity c ? c : null);
    }

    public MiniCrusherMenu(int id, Inventory inv, MiniCrusherBlockEntity crusher) {
        super(FactoryAscension.MINI_CRUSHER_MENU.get(), id);
        this.crusher = crusher;
        boolean server = crusher != null && !inv.player.level().isClientSide;
        ItemStackHandler items = crusher != null ? crusher.items() : new ItemStackHandler(MiniCrusherBlockEntity.SLOTS);
        for (int i = 0; i < MiniCrusherBlockEntity.SLOTS; i++) {
            boolean out = i >= MiniCrusherBlockEntity.OUT0 && i < MiniCrusherBlockEntity.OUT0 + MiniCrusherBlockEntity.OUTS;
            addSlot(new SlotItemHandler(items, i, POS[i][0], POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !out && super.mayPlace(stack);
                }
            });
        }
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 84);
        data = server ? crusher.data() : new SimpleContainerData(5);
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public int needed() { return data.get(1); }
    public int charge() { return data.get(2); }
    public int chargeMax() { return data.get(3); }
    public float speed() { return data.get(4) / 10f; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int n = MiniCrusherBlockEntity.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, slots.size(), true)) return ItemStack.EMPTY;
        } else if (MiniCrusherBlockEntity.isMotor(stack)) {
            if (!moveItemStackTo(stack, MiniCrusherBlockEntity.MOTOR, MiniCrusherBlockEntity.MOTOR + 1, false)) return ItemStack.EMPTY;
        } else if (MiniCrusherBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, MiniCrusherBlockEntity.BATTERY, MiniCrusherBlockEntity.BATTERY + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, MiniCrusherBlockEntity.INPUT, MiniCrusherBlockEntity.INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return crusher != null && Container.stillValidBlockEntity(crusher, player);
    }
}
