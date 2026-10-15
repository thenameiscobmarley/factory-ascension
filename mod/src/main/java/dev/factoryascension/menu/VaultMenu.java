package dev.factoryascension.menu;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.VaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Vault screen's menu: a 9 x 6 window onto the vault. On the server each window slot points at the stored slot that
 * the current scroll row and search filter put there; on the client the window slots are plain display slots that the
 * normal slot syncing fills, so client and server can never disagree.
 */
public class VaultMenu extends AbstractContainerMenu {
    public static final int COLS = 9, ROWS = 6, VIEW = COLS * ROWS;
    public static final int BTN_SORT = 0, BTN_UP = 1, BTN_DOWN = 2, BTN_ROW = 100;

    private final VaultBlockEntity vault;   // null on the client if the block isn't loaded
    private final ContainerData data;       // 0 capacity, 1 used, 2 rows of matches, 3 first visible row
    private final List<Integer> matches = new ArrayList<>();
    private String filter = "";
    private int row = 0;

    // Client
    public VaultMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, inv.player.level().getBlockEntity(pos) instanceof VaultBlockEntity v ? v : null, true);
    }

    // Server
    public VaultMenu(int id, Inventory inv, VaultBlockEntity vault) {
        this(id, inv, vault, false);
    }

    private VaultMenu(int id, Inventory inv, VaultBlockEntity vault, boolean client) {
        super(FactoryAscension.VAULT_MENU.get(), id);
        this.vault = vault;
        Container display = new SimpleContainer(VIEW);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int view = r * COLS + c;
                int x = 8 + c * 18, y = 32 + r * 18;
                addSlot(client || vault == null ? new Slot(display, view, x, y) : new WindowSlot(display, view, x, y));
            }
        }
        MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 150);
        if (client || vault == null) {
            data = new SimpleContainerData(4);
        } else {
            data = new ContainerData() {
                public int get(int i) {
                    return switch (i) {
                        case 0 -> vault.capacity();
                        case 1 -> vault.usedSlots();
                        case 2 -> (matches.size() + COLS - 1) / COLS;
                        case 3 -> row;
                        default -> 0;
                    };
                }

                public void set(int i, int v) {
                }

                public int getCount() {
                    return 4;
                }
            };
            rebuild();
        }
        addDataSlots(data);
    }

    public int capacity() { return data.get(0); }
    public int used() { return data.get(1); }
    public int totalRows() { return data.get(2); }
    public int firstRow() { return data.get(3); }

    /** Which stored slots match the filter (all unlocked slots when there's no filter). */
    private void rebuild() {
        matches.clear();
        int cap = vault.capacity();
        for (int i = 0; i < cap; i++) {
            if (filter.isEmpty()) {
                matches.add(i);
                continue;
            }
            ItemStack s = vault.items().getStackInSlot(i);
            if (s.isEmpty()) continue;
            String name = s.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
            if (name.contains(filter) || id.contains(filter)) matches.add(i);
        }
        int maxRow = Math.max(0, (matches.size() + COLS - 1) / COLS - ROWS);
        row = Math.max(0, Math.min(row, maxRow));
    }

    /** Server: the stored slot behind a window slot, or -1. */
    int storedIndex(int view) {
        int i = row * COLS + view;
        return i < matches.size() ? matches.get(i) : -1;
    }

    /** Server: the search box changed (sent by the client). */
    public void setFilter(String text) {
        if (vault == null) return;
        filter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (filter.length() > 50) filter = filter.substring(0, 50);
        row = 0;
        rebuild();
        broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        if (vault != null && !filter.isEmpty()) rebuild();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (vault == null) return false;
        if (id == BTN_SORT) {
            vault.sortContents();
            rebuild();
        } else if (id == BTN_UP) {
            row--;
            rebuild();
        } else if (id == BTN_DOWN) {
            row++;
            rebuild();
        } else if (id >= BTN_ROW) {
            row = id - BTN_ROW;
            rebuild();
        } else {
            return false;
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index < VIEW) {
            // Vault -> player
            ItemStack copy = stack.copy();
            if (!moveItemStackTo(stack, VIEW, slots.size(), true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
        // Player -> vault (server only: the client waits for the sync)
        if (vault != null && !player.level().isClientSide) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(new RangedWrapper(vault.items(), 0, vault.capacity()), stack.copy(), false);
            slot.set(left);
            rebuild();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return vault != null && stillValid(ContainerLevelAccess.create(vault.getLevel(), vault.getBlockPos()), player, FactoryAscension.VAULT.get());
    }

    /** A window slot (server): reads and writes whichever stored slot it currently shows. */
    private class WindowSlot extends Slot {
        private final int view;

        WindowSlot(Container display, int view, int x, int y) {
            super(display, view, x, y);
            this.view = view;
        }

        private int stored() {
            return storedIndex(view);
        }

        @Override
        public ItemStack getItem() {
            int i = stored();
            return i < 0 ? ItemStack.EMPTY : vault.items().getStackInSlot(i);
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            int i = stored();
            if (i >= 0) vault.items().setStackInSlot(i, stack);
            setChanged();
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            set(stack);
        }

        @Override
        public ItemStack remove(int amount) {
            int i = stored();
            return i < 0 ? ItemStack.EMPTY : vault.items().extractItem(i, amount, false);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stored() >= 0;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setChanged() {
            vault.setChanged();
        }
    }
}
