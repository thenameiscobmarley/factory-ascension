package dev.factoryascension.client.tarkov;

import com.mojang.math.Axis;
import dev.factoryascension.FAConfig;
import dev.factoryascension.network.FANetwork;
import dev.factoryascension.tarkov.TarkovLayout;
import dev.factoryascension.tarkov.TarkovServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tarkov inventory screen: your gear (armor, off hand, the player), the 2x2 crafting grid, the backpack grid where
 * items take their real size, the hotbar, and a strip for anything that didn't fit. Pick an item up with a click, R turns
 * it while you hold it, click a free spot to drop it there (or onto the same item to stack). Shift-click, number keys,
 * Q and double-click work like the normal inventory: every click goes through the game's own inventory code.
 */
public class TarkovScreen extends EffectRenderingInventoryScreen<TarkovView> {
    static final int C = 18;                    // a grid cell
    static final int GX = 112, GY = 30;         // the grid's corner in the panel
    // Midnight blue, the pack's gold and teal
    static final int BG = 0xF0101828, BG_EDGE = 0xFF2C3A58, CELL = 0xFF162034, CELL_LINE = 0xFF22304A, HEAD = 0xFF0B111E;
    static final int GOLD = 0xFFE8B03A, TEAL = 0xFF40E8D4, TEXT = 0xFFE8ECF4, DIM = 0xFF8E9AB5, BAD = 0xFFE0503A, OK = 0xFF50E07A;

    private final LocalPlayer player;
    private boolean carriedTurned = false;
    private int carriedFrom = TarkovLayout.NONE;   // the slot the held item came from (it goes back to that slot number)
    private boolean opened = false;
    private final List<Integer> unsorted = new ArrayList<>();

    public TarkovScreen(LocalPlayer player) {
        super(new TarkovView(player, TarkovScreen::place), player.getInventory(), Component.literal("Inventory"));
        this.player = player;
        menu.hide = () -> hideCarried;
        imageWidth = GX + TarkovLayout.width() * C + 8;
        imageHeight = Math.max(GY + TarkovLayout.height() * C + 4 + 26 + 18 + 8, 172);
        titleLabelX = 8;
        titleLabelY = 6;
    }

    static int gridW() { return TarkovLayout.width() * C; }
    static int gridH() { return TarkovLayout.height() * C; }
    static int stripY() { return GY + gridH() + 14; }
    static int hotbarY() { return stripY() + 26; }

    /** Where each of the real inventory menu's slots is drawn (null = hidden: the 27 main slots live in the grid). */
    static int[] place(int index) {
        if (index == 0) return new int[] {84, 129};                               // crafting result
        if (index >= 1 && index <= 4) return new int[] {8 + (index - 1) % 2 * 18, 120 + (index - 1) / 2 * 18};
        if (index >= 5 && index <= 8) return new int[] {8, 24 + (index - 5) * 18};   // helmet .. boots
        if (index >= 9 && index <= 35) return null;
        if (index >= 36 && index <= 44) return new int[] {GX + 1 + (index - 36) * 18, hotbarY() + 1};
        if (index == 45) return new int[] {84, 78};                               // off hand
        return null;
    }

    @Override
    protected void init() {
        super.init();
        if (!opened) {
            opened = true;
            TarkovClient.opened();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER, 0.8f, 0.7f));
        }
    }

    @Override
    public void removed() {
        super.removed();
        TarkovClient.closed();
    }

    private boolean ready() {
        return TarkovClient.grown() > 0.6f;
    }

    // ---- Drawing ----
    @Override
    public void renderBackground(GuiGraphics g) {
        int a = (int) (0x90 * TarkovClient.grown());
        g.fillGradient(0, 0, width, height, a << 24 | 0x060A14, a << 24 | 0x0A1020);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        float grow = TarkovClient.grown();
        if (grow <= 0) return;   // still coming out of the bag
        boolean overGrid = inGrid(mx, my) && ready();
        g.pose().pushPose();
        if (grow < 1) {
            // Grows out of the bag at the bottom middle of the view
            float s = 0.08f + 0.92f * grow, ox = width / 2f, oy = height * 0.82f;
            g.pose().translate(ox, oy, 0);
            g.pose().scale(s, s, 1);
            g.pose().translate(-ox, -oy, 0);
            mx = my = -10000;
        }
        TarkovView view = menu;
        hideCarried = overGrid;
        try {
            super.render(g, mx, my, partial);
        } finally {
            hideCarried = false;
        }
        if (ready()) {
            int slot = slotUnder(mx, my);
            if (slot != TarkovLayout.NONE) hoveredSlot = view.getSlot(TarkovLayout.FIRST + slot);
            if (overGrid && !menu.getCarried().isEmpty()) drawPlacing(g, mx, my);
            else if (slot != TarkovLayout.NONE) {
                int p = TarkovClient.LAYOUT.pos[slot];
                ItemStack st = items().get(TarkovLayout.FIRST + slot);
                if (p != TarkovLayout.NONE && !unsorted.contains(slot)) {
                    int x = leftPos + GX + TarkovLayout.x(p) * C, y = topPos + GY + TarkovLayout.y(p) * C;
                    g.fill(x, y, x + TarkovLayout.w(st, TarkovLayout.turned(p)) * C, y + TarkovLayout.h(st, TarkovLayout.turned(p)) * C, 0x40FFFFFF);
                }
            }
            renderTooltip(g, mx, my);
        }
        g.pose().popPose();
    }

    private boolean hideCarried = false;

    List<ItemStack> items() {
        return player.getInventory().items;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x0 = leftPos, y0 = topPos;
        // Panel: midnight blue with a thin steel edge and a dark header
        g.fill(x0 - 1, y0 - 1, x0 + imageWidth + 1, y0 + imageHeight + 1, BG_EDGE);
        g.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, BG);
        g.fill(x0, y0, x0 + imageWidth, y0 + 17, HEAD);
        g.fill(x0, y0 + 17, x0 + imageWidth, y0 + 18, GOLD);
        // Gear: armor column, the player, off hand
        for (int i = 0; i < 4; i++) cellSlot(g, x0 + 8, y0 + 24 + i * 18);
        g.fill(x0 + 27, y0 + 22, x0 + 80, y0 + 96, 0xFF0A101C);
        g.fill(x0 + 27, y0 + 95, x0 + 80, y0 + 96, BG_EDGE);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x0 + 53, y0 + 90, 30, x0 + 53 - mx, y0 + 40 - my, player);
        cellSlot(g, x0 + 84, y0 + 78);
        // Crafting
        g.drawString(font, "CRAFT", x0 + 8, y0 + 109, DIM, false);
        for (int i = 0; i < 4; i++) cellSlot(g, x0 + 8 + i % 2 * 18, y0 + 120 + i / 2 * 18);
        g.drawString(font, "→", x0 + 50, y0 + 133, DIM, false);
        cellSlot(g, x0 + 84, y0 + 129);
        // The grid
        int gx = x0 + GX, gy = y0 + GY;
        g.drawString(font, "BACKPACK", gx, gy - 10, DIM, false);
        g.fill(gx - 1, gy - 1, gx + gridW() + 1, gy + gridH() + 1, BG_EDGE);
        for (int cx = 0; cx < TarkovLayout.width(); cx++) for (int cy = 0; cy < TarkovLayout.height(); cy++) {
            int x = gx + cx * C, y = gy + cy * C;
            g.fill(x, y, x + C, y + C, CELL_LINE);
            g.fill(x + 1, y + 1, x + C, y + C, CELL);
        }
        // Items, at their size
        unsorted.clear();
        List<ItemStack> main = items();
        int[][] occ = TarkovClient.LAYOUT.occupancy(main);
        for (int s = 0; s < TarkovLayout.SLOTS; s++) {
            ItemStack st = main.get(TarkovLayout.FIRST + s);
            if (st.isEmpty()) continue;
            int p = TarkovClient.LAYOUT.pos[s];
            if (p == TarkovLayout.NONE || !owns(occ, s, st, p)) { unsorted.add(s); continue; }
            boolean t = TarkovLayout.turned(p);
            drawItem(g, st, gx + TarkovLayout.x(p) * C, gy + TarkovLayout.y(p) * C, TarkovLayout.w(st, t), TarkovLayout.h(st, t), t, 1);
        }
        // Unsorted strip (or the weight, when there's nothing in it)
        int sy = y0 + stripY();
        if (!unsorted.isEmpty()) {
            g.drawString(font, "NO ROOM", gx, sy - 9, BAD, false);
            for (int i = 0; i < unsorted.size() && i < 10; i++) {
                int x = gx + i * C;
                cellSlot(g, x, sy);
                g.renderItem(main.get(TarkovLayout.FIRST + unsorted.get(i)), x + 1, sy + 1);
                g.renderItemDecorations(font, main.get(TarkovLayout.FIRST + unsorted.get(i)), x + 1, sy + 1);
            }
        } else {
            drawWeight(g, gx, sy);
        }
        // Hotbar
        g.drawString(font, "HOTBAR", gx, y0 + hotbarY() - 9, DIM, false);
        for (int i = 0; i < 9; i++) cellSlot(g, gx + i * 18, y0 + hotbarY());
        int sel = player.getInventory().selected;
        g.fill(gx + sel * 18, y0 + hotbarY() + 17, gx + sel * 18 + 18, y0 + hotbarY() + 18, GOLD);
    }

    /** Is the slot's item really where its position says (and not overlapped by an earlier slot)? */
    private static boolean owns(int[][] occ, int s, ItemStack st, int p) {
        int x0 = TarkovLayout.x(p), y0 = TarkovLayout.y(p);
        boolean t = TarkovLayout.turned(p);
        int w = TarkovLayout.w(st, t), h = TarkovLayout.h(st, t);
        if (x0 + w > occ.length || y0 + h > occ[0].length) return false;
        for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + h; y++) if (occ[x][y] != s) return false;
        return true;
    }

    private void drawWeight(GuiGraphics g, int x, int y) {
        float kg = TarkovServer.weight(player);
        float slow = TarkovServer.slowdown(kg);
        String text = FAConfig.weightEnabled ? String.format("WEIGHT %.1f / %.0f kg", kg, FAConfig.weightCarry) : String.format("WEIGHT %.1f kg", kg);
        g.drawString(font, text, x, y + 1, slow > 0 ? BAD : DIM, false);
        if (FAConfig.weightEnabled) {
            int w = gridW(), bx = x, by = y + 11;
            g.fill(bx, by, bx + w, by + 3, 0xFF0A101C);
            g.fill(bx, by, bx + Math.round(w * Math.min(1, kg / Math.max(1, FAConfig.weightCarry))), by + 3, slow > 0 ? BAD : TEAL);
            if (slow > 0) g.drawString(font, String.format("-%d%% speed", Math.round(slow * 100)), x + w - font.width(String.format("-%d%% speed", Math.round(slow * 100))), y + 1, BAD, false);
        }
    }

    private void cellSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, CELL_LINE);
        g.fill(x + 1, y + 1, x + 18, y + 18, CELL);
    }

    /** Tarkov-style tints: weapons red, armor blue, tools amber, blocks grey, everything else teal. */
    private static int tint(ItemStack st) {
        if (st.getItem() instanceof dev.factoryascension.item.ShotgunItem || st.getItem() instanceof SwordItem) return 0x5A2A20;
        if (st.getItem() instanceof ArmorItem) return 0x24406A;
        if (st.getItem() instanceof TieredItem) return 0x5A4420;
        if (st.getItem() instanceof BlockItem) return 0x2E3646;
        return 0x1E4A4A;
    }

    /** An item filling its cells: tinted backing, the icon scaled up (long items laid along their length), count and bar. */
    void drawItem(GuiGraphics g, ItemStack st, int x, int y, int w, int h, boolean turned, float alpha) {
        int pw = w * C, ph = h * C;
        int a = (int) (0xB0 * alpha);
        g.fill(x + 1, y + 1, x + pw, y + ph, a << 24 | tint(st));
        g.fill(x + 1, y + 1, x + pw, y + 2, (int) (0x50 * alpha) << 24 | 0xFFFFFF);
        g.renderOutline(x + 1, y + 1, pw - 1, ph - 1, (int) (0xA0 * alpha) << 24 | 0x6A7AA0);
        int[] up = TarkovLayout.size(st);
        float angle = turned ? 90 : 0;
        float scale;
        // Tools, weapons, bows and rods are drawn on the diagonal; everything else (armor, shields, blocks) upright
        net.minecraft.world.item.Item it = st.getItem();
        boolean diagonal = it instanceof TieredItem || it instanceof net.minecraft.world.item.ProjectileWeaponItem || it instanceof net.minecraft.world.item.FishingRodItem
            || it instanceof net.minecraft.world.item.TridentItem || it instanceof dev.factoryascension.item.ShotgunItem && !(it instanceof dev.factoryascension.item.FlareGunItem);
        if (up[0] == up[1] || !diagonal) {
            scale = Math.min(pw, ph) / 16f * 0.86f;
        } else {
            // Long items are drawn on the diagonal: lay that along the long side
            angle += up[0] > up[1] ? 45 : -45;
            float lng = Math.max(up[0], up[1]) * C, shrt = Math.min(up[0], up[1]) * C;
            scale = Math.min(lng * 0.95f / 22.6f, shrt * 1.45f / 16f);
        }
        g.pose().pushPose();
        g.pose().translate(x + pw / 2f + 0.5f, y + ph / 2f + 0.5f, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        g.pose().scale(scale, scale, 1);
        g.renderItem(st, -8, -8);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 10);
        g.renderItemDecorations(font, st, x + pw - 17, y + ph - 17);
        g.pose().popPose();
    }

    /** Where the held item would go: its outline green (fits / stacks) or red, with the item drawn at its size. */
    private void drawPlacing(GuiGraphics g, int mx, int my) {
        ItemStack held = menu.getCarried();
        int[] spot = spotFor(held, mx, my);
        int w = TarkovLayout.w(held, carriedTurned), h = TarkovLayout.h(held, carriedTurned);
        int x = leftPos + GX + spot[0] * C, y = topPos + GY + spot[1] * C;
        boolean ok = spot[2] != 0;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x, y, x + w * C, y + h * C, (ok ? OK : BAD) & 0x40FFFFFF);
        g.renderOutline(x, y, w * C, h * C, ok ? OK : BAD);
        drawItem(g, held, x, y, w, h, carriedTurned, 0.6f);
        g.pose().popPose();
    }

    // ---- The grid under the mouse ----
    private boolean inGrid(double mx, double my) {
        double x = mx - leftPos - GX, y = my - topPos - GY;
        return x >= 0 && y >= 0 && x < gridW() && y < gridH();
    }

    /** The main slot under the mouse: in the grid, or in the no-room strip. */
    private int slotUnder(double mx, double my) {
        if (inGrid(mx, my)) {
            int cx = (int) ((mx - leftPos - GX) / C), cy = (int) ((my - topPos - GY) / C);
            int s = TarkovClient.LAYOUT.slotAt(items(), cx, cy);
            return s != TarkovLayout.NONE && !unsorted.contains(s) ? s : TarkovLayout.NONE;
        }
        double sx = mx - leftPos - GX, sy = my - topPos - stripY();
        if (sy >= 0 && sy < 18 && sx >= 0) {
            int i = (int) (sx / C);
            if (i < unsorted.size() && i < 10) return unsorted.get(i);
        }
        return TarkovLayout.NONE;
    }

    /** {x, y, fits (1/0), merge slot or NONE} for the held item centred on the mouse. */
    private int[] spotFor(ItemStack held, double mx, double my) {
        int w = TarkovLayout.w(held, carriedTurned), h = TarkovLayout.h(held, carriedTurned);
        int x = Math.round((float) ((mx - leftPos - GX) / C - w / 2.0)), y = Math.round((float) ((my - topPos - GY) / C - h / 2.0));
        x = Math.max(0, Math.min(TarkovLayout.width() - w, x));
        y = Math.max(0, Math.min(TarkovLayout.height() - h, y));
        int under = slotUnder(mx, my);
        if (under != TarkovLayout.NONE && ItemStack.isSameItemSameTags(items().get(TarkovLayout.FIRST + under), held)) return new int[] {x, y, 1, under};
        boolean free = TarkovLayout.free(TarkovClient.LAYOUT.occupancy(items()), x, y, w, h, TarkovLayout.NONE) && emptySlot() != TarkovLayout.NONE;
        return new int[] {x, y, free ? 1 : 0, TarkovLayout.NONE};
    }

    /** An empty main slot to put the held item in: the one it came from if that's still empty. */
    private int emptySlot() {
        if (carriedFrom != TarkovLayout.NONE && items().get(TarkovLayout.FIRST + carriedFrom).isEmpty()) return carriedFrom;
        for (int s = 0; s < TarkovLayout.SLOTS; s++) if (items().get(TarkovLayout.FIRST + s).isEmpty()) return s;
        return TarkovLayout.NONE;
    }

    // For the dev screenshots: screen positions of a grid cell, and of a main slot's item
    public double[] devCell(int cx, int cy) {
        return new double[] {leftPos + GX + cx * C + C / 2.0, topPos + GY + cy * C + C / 2.0};
    }

    public double[] devCellCenter(int slot) {
        int p = TarkovClient.LAYOUT.pos[slot];
        return devCell(TarkovLayout.x(p), TarkovLayout.y(p));
    }

    // ---- Clicks ----
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!ready()) return true;
        int under = slotUnder(mx, my);
        boolean grid = inGrid(mx, my);
        if (!grid && under == TarkovLayout.NONE) return super.mouseClicked(mx, my, button);
        if (button != 0 && button != 1) return true;
        ItemStack held = menu.getCarried();
        if (held.isEmpty()) {
            if (under == TarkovLayout.NONE) return true;
            Slot slot = menu.getSlot(TarkovLayout.FIRST + under);
            if (hasShiftDown()) {
                slotClicked(slot, slot.index, button, ClickType.QUICK_MOVE);
            } else {
                int p = TarkovClient.LAYOUT.pos[under];
                carriedTurned = p != TarkovLayout.NONE && TarkovLayout.turned(p);
                carriedFrom = under;
                slotClicked(slot, slot.index, button, ClickType.PICKUP);
            }
            return true;
        }
        if (!grid) return true;
        int[] spot = spotFor(held, mx, my);
        if (spot[3] != TarkovLayout.NONE) {
            Slot slot = menu.getSlot(TarkovLayout.FIRST + spot[3]);
            slotClicked(slot, slot.index, button, ClickType.PICKUP);   // stack onto the same item
            return true;
        }
        if (spot[2] == 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 0.6f, 1.4f));
            return true;
        }
        int s = emptySlot();
        TarkovClient.LAYOUT.pos[s] = TarkovLayout.pack(spot[0], spot[1], carriedTurned);   // shown right away; the server agrees a moment later
        FANetwork.CHANNEL.sendToServer(new FANetwork.TarkovPlan(s, spot[0], spot[1], carriedTurned));
        Slot slot = menu.getSlot(TarkovLayout.FIRST + s);
        slotClicked(slot, slot.index, button, ClickType.PICKUP);
        if (menu.getCarried().isEmpty()) carriedFrom = TarkovLayout.NONE;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (inGrid(mx, my) || slotUnder(mx, my) != TarkovLayout.NONE) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (inGrid(mx, my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_R && !menu.getCarried().isEmpty()) {
            carriedTurned = !carriedTurned;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.6f, 0.25f));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        return mx < left || my < top || mx >= left + imageWidth || my >= top + imageHeight;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "INVENTORY", 8, 5, TEXT, false);
        String hint = menu.getCarried().isEmpty() ? "" : "R: turn";
        g.drawString(font, hint, imageWidth - 8 - font.width(hint), 5, GOLD, false);
    }
}
