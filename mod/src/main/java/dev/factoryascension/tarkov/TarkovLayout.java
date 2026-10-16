package dev.factoryascension.tarkov;

import dev.factoryascension.FAConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;

/**
 * The Tarkov-style grid over the main inventory. The items never leave the game's own 27 main slots (so quests, charms,
 * soulbound, death drops and every other mod still see them where they expect); this only remembers where each slot's
 * item sits in the grid and whether it's turned on its side, and makes sure everything physically fits. A slot whose item
 * can't fit anywhere is "unsorted" (still in your inventory, shown in its own strip until you make room).
 * The same code runs on the server (which decides) and the client (which shows it).
 */
public final class TarkovLayout {
    public static final int SLOTS = 27;           // the main inventory: Inventory.items 9..35
    public static final int FIRST = 9;
    public static final int NONE = -1;

    /** Per slot: x | y << 8 | turned << 16, or NONE. Kept for empty slots too (a spot planned for an item on its way). */
    public final int[] pos = new int[SLOTS];
    // What each slot held at the last check: items that were already sitting there keep their place first
    private final Item[] last = new Item[SLOTS];

    public TarkovLayout() {
        java.util.Arrays.fill(pos, NONE);
    }

    public static int x(int p) { return p & 0xFF; }
    public static int y(int p) { return (p >> 8) & 0xFF; }
    public static boolean turned(int p) { return (p >> 16 & 1) == 1; }
    public static int pack(int x, int y, boolean turned) { return x | y << 8 | (turned ? 1 << 16 : 0); }

    public static int width() { return FAConfig.tarkovWidth; }
    public static int height() { return FAConfig.tarkovHeight; }

    // ---- How big things are (width x height in cells, upright) ----
    public static int[] size(ItemStack stack) {
        if (stack.isEmpty()) return new int[] {1, 1};
        Item item = stack.getItem();
        int[] custom = FAConfig.tarkovSizes.get(String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
        if (custom != null) return custom;
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (path.endsWith("backpack")) return new int[] {3, 3};
        if (item instanceof dev.factoryascension.item.FlareGunItem) return new int[] {2, 2};
        if (item instanceof dev.factoryascension.item.ShotgunItem) return new int[] {5, 2};
        if (item instanceof ArmorItem armor) return switch (armor.getType()) {
            case HELMET, BOOTS -> new int[] {2, 2};
            case CHESTPLATE -> new int[] {3, 3};
            case LEGGINGS -> new int[] {2, 3};
        };
        if (item instanceof ElytraItem || item instanceof ShieldItem) return new int[] {2, 3};
        if (item instanceof SwordItem || item instanceof TridentItem) return new int[] {1, 3};
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof FishingRodItem) return new int[] {1, 3};
        if (item instanceof AxeItem) return new int[] {2, 2};
        if (item instanceof DiggerItem) return new int[] {1, 2};
        if (item instanceof BucketItem) return new int[] {1, 2};
        if (item instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock) return new int[] {2, 2};
        return new int[] {1, 1};
    }

    public static int w(ItemStack s, boolean turned) { return size(s)[turned ? 1 : 0]; }
    public static int h(ItemStack s, boolean turned) { return size(s)[turned ? 0 : 1]; }

    /** Weight in kg: cells x kg per cell, times how full the stack is (a lone block weighs 1/64 of a full stack). */
    public static float weight(ItemStack s) {
        if (s.isEmpty()) return 0;
        int[] sz = size(s);
        float fill = s.getMaxStackSize() > 1 ? s.getCount() / (float) s.getMaxStackSize() : 1;
        return sz[0] * sz[1] * FAConfig.weightPerCell * fill;
    }

    // ---- Space ----
    /** Which cells are taken, by slot number (NONE = free), counting only slots that hold something and are placed. */
    public int[][] occupancy(List<ItemStack> main) {
        int[][] occ = new int[width()][height()];
        for (int[] col : occ) java.util.Arrays.fill(col, NONE);
        for (int s = 0; s < SLOTS; s++) {
            ItemStack st = main.get(FIRST + s);
            if (st.isEmpty() || pos[s] == NONE) continue;
            fill(occ, s, st);
        }
        return occ;
    }

    private boolean fill(int[][] occ, int s, ItemStack st) {
        int p = pos[s], x0 = x(p), y0 = y(p), w = w(st, turned(p)), h = h(st, turned(p));
        if (x0 + w > width() || y0 + h > height()) return false;
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) if (occ[x][y] != NONE && occ[x][y] != s) return false;
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) occ[x][y] = s;
        return true;
    }

    public static boolean free(int[][] occ, int x0, int y0, int w, int h, int ignoreSlot) {
        if (x0 < 0 || y0 < 0 || x0 + w > occ.length || y0 + h > occ[0].length) return false;
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) if (occ[x][y] != NONE && occ[x][y] != ignoreSlot) return false;
        return true;
    }

    /** The first free spot for this stack, upright first and then turned: a packed position, or NONE. */
    public static int findSpot(int[][] occ, ItemStack stack) {
        int[] s = size(stack);
        for (int t = 0; t < 2; t++) {
            int w = s[t], h = s[1 - t];
            for (int y = 0; y + h <= occ[0].length; y++) for (int x = 0; x + w <= occ.length; x++)
                if (free(occ, x, y, w, h, NONE)) return pack(x, y, t == 1);
            if (s[0] == s[1]) break;
        }
        return NONE;
    }

    /** The slot whose item covers this cell, or NONE. */
    public int slotAt(List<ItemStack> main, int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= width() || cy >= height()) return NONE;
        return occupancy(main)[cx][cy];
    }

    /**
     * Make everything fit: items that were already sitting in their slot keep their place first, then everything else
     * (new arrivals, items whose spot is taken or off a smaller grid) goes in its planned spot if it's free, or the
     * first free one, or becomes unsorted. Returns true if anything moved.
     */
    public boolean validate(List<ItemStack> main) {
        int[] before = pos.clone();
        int[][] occ = new int[width()][height()];
        for (int[] col : occ) java.util.Arrays.fill(col, NONE);
        boolean[] done = new boolean[SLOTS];
        for (int pass = 0; pass < 2; pass++) {
            for (int s = 0; s < SLOTS; s++) {
                ItemStack st = main.get(FIRST + s);
                if (done[s] || st.isEmpty()) continue;
                boolean settled = last[s] == st.getItem();
                if (pass == 0 && !settled) continue;
                done[s] = true;
                if (pos[s] != NONE && fill(occ, s, st)) continue;
                pos[s] = findSpot(occ, st);
                if (pos[s] != NONE) fill(occ, s, st);
            }
        }
        for (int s = 0; s < SLOTS; s++) {
            ItemStack st = main.get(FIRST + s);
            last[s] = st.isEmpty() ? null : st.getItem();
        }
        return !java.util.Arrays.equals(before, pos);
    }

    /** Could a new stack of this (that doesn't merge into anything) go into the grid now? */
    public boolean hasRoomFor(List<ItemStack> main, ItemStack stack) {
        boolean emptySlot = false;
        for (int s = 0; s < SLOTS; s++) if (main.get(FIRST + s).isEmpty()) { emptySlot = true; break; }
        return emptySlot && findSpot(occupancy(main), stack) != NONE;
    }

    public void copyFrom(int[] packed) {
        if (packed.length != SLOTS) return;
        System.arraycopy(packed, 0, pos, 0, SLOTS);
    }
}
