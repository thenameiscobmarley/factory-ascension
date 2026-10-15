package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.BinderBlockEntity;
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

/** Card Binder screen: 6 rows of card slots, plus the collection count (different cards inside). */
public class BinderMenu extends AbstractContainerMenu {
    private final BinderBlockEntity binder;
    private final ContainerData data;   // 0 different cards inside

    public BinderMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof BinderBlockEntity b ? b : null);
    }

    public BinderMenu(int id, Inventory inv, BinderBlockEntity binder) {
        super(FactoryAscension.BINDER_MENU.get(), id);
        this.binder = binder;
        ItemStackHandler items = binder != null ? binder.items() : new ItemStackHandler(BinderBlockEntity.SLOTS);
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new SlotItemHandler(items, r * 9 + c, 8 + c * 18, 32 + r * 18));
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 150);
        data = binder != null && !inv.player.level().isClientSide ? new ContainerData() {
            public int get(int i) { return binder.uniqueCards(); }
            public void set(int i, int v) { }
            public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(data);
    }

    public int uniqueCards() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int cards = BinderBlockEntity.SLOTS;
        if (index < cards) {
            if (!moveItemStackTo(stack, cards, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!BinderBlockEntity.isCard(stack) || !moveItemStackTo(stack, 0, cards, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return binder != null && stillValid(ContainerLevelAccess.create(binder.getLevel(), binder.getBlockPos()), player, FactoryAscension.BINDER.get());
    }
}
