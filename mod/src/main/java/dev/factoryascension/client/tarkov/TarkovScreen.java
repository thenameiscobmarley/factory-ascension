package dev.factoryascension.client.tarkov;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.factoryascension.FAConfig;
import dev.factoryascension.network.FANetwork;
import dev.factoryascension.tarkov.TarkovLayout;
import dev.factoryascension.tarkov.TarkovServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Tarkov inventory screen, laid out like Escape from Tarkov's gear screen and filling the whole window:
 *  - a tab bar along the top, a menu bar along the bottom, a stats row (weight, health, food, armor, level) and the
 *    numbered QUICK USE row (the hotbar) above it;
 *  - left: your gear in labelled frames around a dim silhouette of you (headwear, body armor, leg armor, footwear,
 *    holster = off hand, on sling = what's in your hand);
 *  - middle: the 2x2 crafting grid and the overflow (items that have no spot in the backpack yet);
 *  - right: the backpack, a big grid where items take their real size, with a filter column down its side.
 * Pick an item up with a click, R turns it while you hold it, click a free spot to drop it there (or onto the same item
 * to stack). Shift-click, number keys, Q and double-click work like the normal inventory: every click goes through the
 * game's own inventory code.
 */
public class TarkovScreen extends AbstractContainerScreen<TarkovView> {
    // Tarkov's palette: near-black panels, thin grey edges, warm light-grey text
    static final int BG = 0xEC0C0C0C, PANEL = 0xC0141414, EDGE = 0xFF3A3A38, EDGE_LIGHT = 0xFF5A5A56, CELL = 0xFF121212, CELL_LINE = 0xFF262626;
    public static final int ACCENT = 0xFFB4BEC8, SHADOW = 0x70000000, HILITE = 0x22FFFFFF;
    /** The UI font: the game's own (the mod ships no fonts). */
    static final net.minecraft.resources.ResourceLocation UI_FONT = new net.minecraft.resources.ResourceLocation(dev.factoryascension.FactoryAscension.MODID, "ui"),
        UI_BOLD = new net.minecraft.resources.ResourceLocation(dev.factoryascension.FactoryAscension.MODID, "ui_bold");
    static final int TEXT = 0xFFD6D4CC, DIM = 0xFF8C8A84, FAINT = 0xFF56544F, BAD = 0xFFC8452E, OK = 0xFF6FB04A, BAR = 0xFF0A0A0A;
    static final String[] TABS = {"OVERALL", "GEAR", "HEALTH", "SKILLS", "MAP", "TASKS"};
    static final String[] FILTERS = {"All", "Weapons", "Armor", "Tools", "Blocks", "Food", "Other"};
    static final ItemStack[] FILTER_ICONS = {new ItemStack(Items.CHEST), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_CHESTPLATE),
        new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.BREAD), new ItemStack(Items.PAPER)};

    /** Where everything goes, worked out once from the window's size when the screen opens. */
    static final class Geo {
        final int W, H, C;
        final int top = 16, menuY, quickY, contentTop, contentBottom;
        final int sx, sy, sw, sh, gx, gy;               // backpack panel and its grid
        final int m0, mw, craftY, overflowY, overflowCols, overflowRows;
        final int e0, ew, F, colL, colR;                 // gear area and its frames
        final int quickX, statsY;
        final int[] sling;                               // x, y, w, h
        final Map<Integer, int[]> frames = new HashMap<>();   // slot index -> x, y, w, h, label index
        final Map<Long, int[]> hitBoxes = new HashMap<>();    // slot position -> its frame

        Geo(int W, int H) {
            this.W = W;
            this.H = H;
            menuY = H - 14;
            quickY = menuY - 26;
            contentTop = top + 8;
            contentBottom = quickY - 14;
            // Backpack: the right half (leaving the middle column room for the crafting grid)
            sx = Math.max(W / 2 + 4, (int) (W * 0.335) + 8 + 78);
            sw = W - 6 - sx;
            sy = contentTop;
            sh = contentBottom - sy;
            int availW = sw - 26, availH = sh - 18;
            C = Math.max(10, Math.min(22, Math.min(availW / TarkovLayout.width(), availH / TarkovLayout.height())));
            gx = sx + 22;
            gy = sy + 15;
            // Middle column: crafting, then the overflow
            m0 = (int) (W * 0.335) + 8;
            mw = sx - 8 - m0;
            craftY = contentTop + 11;
            overflowY = craftY + 36 + 22;
            overflowCols = Math.max(1, (mw - 8) / 18);
            overflowRows = Math.max(1, (contentBottom - overflowY - 4) / 18);
            // Gear: two columns of frames around the silhouette
            e0 = 6;
            ew = m0 - 12;
            int fitH = (contentBottom - contentTop) / 4 - 12;
            F = Math.max(22, Math.min(40, Math.min(fitH, (ew - 16) / 4)));
            colL = e0 + 2;
            colR = e0 + ew - F - 2;
            int row = F + 12;
            frame(5, colL, contentTop + 9, F, F, 0);                 // headwear
            frame(6, colL, contentTop + 9 + row, F, F, 1);           // body armor
            frame(7, colR, contentTop + 9, F, F, 2);                 // leg armor
            frame(8, colR, contentTop + 9 + row, F, F, 3);           // footwear
            frame(45, colR, contentTop + 9 + row * 2, F, F, 4);      // holster
            sling = new int[] {colL, contentTop + 9 + row * 2, Math.min(ew - F - 10, F * 2 + 6), F};
            // Quick use: nine numbered cells along the bottom middle
            // Stats at the bottom left, the quick use row beside them (or the stats above it, on a narrow window)
            var font = Minecraft.getInstance().font;
            int statsW = font.width(ui("\u2696 100.0/100  \u2764 20/20  \u2615 20/20  \u26E8 20")) + 40;
            int qx = Math.max(W / 2 - 9 * 22 / 2, statsW + 8);
            if (qx + 9 * 22 > W - 4) { quickX = W / 2 - 9 * 22 / 2; statsY = quickY - 10; }
            else { quickX = qx; statsY = quickY + 6; }
            for (int i = 0; i < 9; i++) frame(36 + i, quickX + i * 22, quickY, 20, 20, -1);
        }

        private void frame(int index, int x, int y, int w, int h, int label) {
            int[] f = {x, y, w, h, label};
            frames.put(index, f);
            hitBoxes.put(key(x + w / 2 - 8, y + h / 2 - 8), f);
        }

        static long key(int x, int y) {
            return ((long) x << 32) | (y & 0xFFFFFFFFL);
        }

        int[] place(int index) {
            int[] f = frames.get(index);
            if (f != null) return new int[] {f[0] + f[2] / 2 - 8, f[1] + f[3] / 2 - 8, f[4] >= 0 ? 1 : 0};
            if (index == 0) return new int[] {m0 + 4 + 36 + 12, craftY + 10};                       // crafting result
            if (index >= 1 && index <= 4) return new int[] {m0 + 5 + (index - 1) % 2 * 18, craftY + 1 + (index - 1) / 2 * 18};
            return null;                                                                            // the 27 main slots: the grid
        }
    }

    static final String[] FRAME_LABELS = {"HEADWEAR", "BODY ARMOR", "LEG ARMOR", "FOOTWEAR", "HOLSTER"};

    private final LocalPlayer player;
    private final Geo geo;
    private final boolean skipOpening;
    private boolean carriedTurned = false;
    private int carriedFrom = TarkovLayout.NONE;   // the slot the held item came from (it goes back to that slot number)
    private boolean opened = false;
    private int filter = 0;
    private final List<Integer> unsorted = new ArrayList<>();
    private boolean hideCarried = false;

    public TarkovScreen(LocalPlayer player) {
        this(player, new Geo(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight()), false);
    }

    private TarkovScreen(LocalPlayer player, Geo geo, boolean skipOpening) {
        super(new TarkovView(player, geo::place), player.getInventory(), Component.literal("Inventory"));
        this.player = player;
        this.geo = geo;
        this.skipOpening = skipOpening;
        menu.hide = () -> hideCarried;
        imageWidth = geo.W;
        imageHeight = geo.H;
    }

    int C() { return geo.C; }
    int gridW() { return TarkovLayout.width() * geo.C; }
    int gridH() { return TarkovLayout.height() * geo.C; }

    @Override
    protected void init() {
        super.init();
        if (width != geo.W || height != geo.H) {
            // The window changed size: lay it out again (the held item stays held; no opening this time)
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> { if (mc.screen == this) mc.setScreen(new TarkovScreen(player, new Geo(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()), true)); });
            return;
        }
        if (!opened) {
            opened = true;
            if (skipOpening) TarkovClient.openedAlready();
            else {
                TarkovClient.opened();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER, 0.8f, 0.7f));
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        TarkovClient.closed();
        freeBlur();
    }

    private boolean ready() {
        return TarkovClient.grown() > 0.6f;
    }


    // ---- Text in the UI font ----
    public static net.minecraft.network.chat.Component ui(String s) {
        return net.minecraft.network.chat.Component.literal(s);
    }

    public static net.minecraft.network.chat.Component uiBold(String s) {
        return net.minecraft.network.chat.Component.literal(s).withStyle(net.minecraft.ChatFormatting.BOLD);
    }

    int tw(String s) {
        return font.width(ui(s));
    }

    int tw(net.minecraft.network.chat.FormattedText t) {
        return font.width(t);
    }

    void text(GuiGraphics g, String s, int x, int y, int colour, boolean shadow) {
        g.drawString(font, ui(s), x, y, colour, shadow);
    }

    void bold(GuiGraphics g, String s, int x, int y, int colour) {
        g.drawString(font, uiBold(s), x, y, colour, false);
    }

    // ---- The look: soft shadows, gradients, a lit top edge ----
    public static void shadowBox(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y + 1, x + w + 2, y + h + 3, 0x40000000);
        g.fill(x, y + 2, x + w + 1, y + h + 2, SHADOW);
    }

    /** Behind the screen: the world, blurred (a frosted pane, like a real game's menus). Off if it ever fails. */
    @org.jetbrains.annotations.Nullable private static net.minecraft.client.renderer.PostChain blurChain;
    private static int blurW = -1, blurH = -1;
    private static boolean blurBroken = false;

    public static void blurWorld(float amount) {
        if (blurBroken || amount <= 0.01f || dev.factoryascension.client.FAClientConfig.SAFE_MODE.get()) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            var main = mc.getMainRenderTarget();
            if (blurChain == null) {
                blurChain = new net.minecraft.client.renderer.PostChain(mc.getTextureManager(), mc.getResourceManager(), main,
                    new net.minecraft.resources.ResourceLocation("shaders/post/blur.json"));
                blurW = blurH = -1;
            }
            if (blurW != main.width || blurH != main.height) {
                blurChain.resize(main.width, main.height);
                blurW = main.width;
                blurH = main.height;
            }
            java.util.List<net.minecraft.client.renderer.PostPass> passes = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.client.renderer.PostChain.class, blurChain, "f_110009_");
            for (var pass : passes) pass.getEffect().safeGetUniform("Radius").set(Math.max(1f, 10f * amount));
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.resetTextureMatrix();
            blurChain.process(mc.getFrameTime());
        } catch (Throwable err) {
            blurBroken = true;
            freeBlur();
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Inventory background blur failed; it stays off", err);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }

    public static void freeBlur() {
        if (blurChain != null) blurChain.close();
        blurChain = null;
    }

    // ---- Drawing ----
    @Override
    public void renderBackground(GuiGraphics g) {
        float grow = TarkovClient.grown();
        blurWorld(grow);
        // Dark, a little lighter in the middle (a vignette), so the panels sit over the blurred world
        int a = (int) (0xD0 * grow), b = (int) (0xF4 * grow);
        g.fillGradient(0, 0, width, height / 2, b << 24 | 0x070808, a << 24 | 0x0E0F0F);
        g.fillGradient(0, height / 2, width, height, a << 24 | 0x0E0F0F, b << 24 | 0x060606);
    }


    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        float grow = TarkovClient.grown();
        if (grow <= 0) return;   // still coming out of the bag
        renderBackground(g);
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
        hideCarried = overGrid;
        TarkovView.hideFramed = true;
        try {
            super.render(g, mx, my, partial);
        } finally {
            hideCarried = false;
            TarkovView.hideFramed = false;
        }
        if (ready()) {
            int slot = slotUnder(mx, my);
            if (slot != TarkovLayout.NONE) hoveredSlot = menu.getSlot(TarkovLayout.FIRST + slot);
            if (inSling(mx, my)) hoveredSlot = menu.getSlot(36 + player.getInventory().selected);
            if (overGrid && !menu.getCarried().isEmpty()) drawPlacing(g, mx, my);
            else if (slot != TarkovLayout.NONE) {
                int p = TarkovClient.LAYOUT.pos[slot];
                ItemStack st = items().get(TarkovLayout.FIRST + slot);
                if (p != TarkovLayout.NONE && !unsorted.contains(slot)) {
                    int x = geo.gx + TarkovLayout.x(p) * C(), y = geo.gy + TarkovLayout.y(p) * C();
                    g.fill(x, y, x + TarkovLayout.w(st, TarkovLayout.turned(p)) * C(), y + TarkovLayout.h(st, TarkovLayout.turned(p)) * C(), 0x30FFFFFF);
                }
            }
            drawFrameHover(g, mx, my);
            renderTooltip(g, mx, my);
        }
        g.pose().popPose();
    }

    List<ItemStack> items() {
        return player.getInventory().items;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        drawBars(g, mx, my);
        drawGear(g, mx, my);
        drawMiddle(g);
        drawBackpack(g, mx, my);
        drawStats(g);
        drawQuickUse(g);
    }

    // Tab bar along the top, menu bar along the bottom
    private void drawBars(GuiGraphics g, int mx, int my) {
        g.fillGradient(0, 0, width, geo.top, 0xF81A1A19, 0xF80B0B0B);
        g.fill(0, geo.top, width, geo.top + 1, EDGE);
        g.fill(0, geo.top + 1, width, geo.top + 3, 0x50000000);
        int x = 4;
        for (String t : TABS) {
            boolean on = t.equals("GEAR");
            int w = tw(t) + 12;
            if (on) {
                g.fillGradient(x, 1, x + w, geo.top, 0xFF34332F, 0xFF22211F);
                g.fill(x, geo.top - 2, x + w, geo.top, ACCENT);
                bold(g, t, x + 6, 4, 0xFFF2EFE6);
            } else text(g, t, x + 6, 4, FAINT, false);
            x += w + 2;
        }
        String back = "◀ BACK";
        boolean hb = mx >= width - tw(back) - 12 && my < geo.top;
        text(g, back, width - tw(back) - 6, 4, hb ? TEXT : DIM, false);
        // Menu bar
        g.fill(0, geo.menuY - 2, width, geo.menuY, 0x50000000);
        g.fillGradient(0, geo.menuY, width, height, 0xF8161615, 0xF8080808);
        g.fill(0, geo.menuY, width, geo.menuY + 1, EDGE);
        menuButton(g, "MAIN MENU", 6, mx, my, false);
        int cw = tw("CHARACTER") + tw("SETTINGS") + 30;
        menuButton(g, "CHARACTER", width / 2 - cw / 2, mx, my, true);
        menuButton(g, "SETTINGS", width / 2 - cw / 2 + tw("CHARACTER") + 30, mx, my, false);
        String ver = "Factory Ascension";
        text(g, ver, width - tw(ver) - 6, geo.menuY + 4, FAINT, false);
    }

    private void menuButton(GuiGraphics g, String text, int x, int mx, int my, boolean active) {
        int w = tw(text) + 8;
        boolean hover = mx >= x - 4 && mx < x + w - 4 && my >= geo.menuY;
        if (active) {
            g.fillGradient(x - 4, geo.menuY + 1, x + w - 4, height, 0xFF34332F, 0xFF1C1B19);
            g.fill(x - 4, geo.menuY + 1, x + w - 4, geo.menuY + 3, ACCENT);
            bold(g, text, x, geo.menuY + 4, 0xFFF2EFE6);
        } else text(g, text, x, geo.menuY + 4, hover ? TEXT : DIM, false);
    }

    /** Which menu-bar or tab-bar button is under the mouse: 1 main menu, 2 settings, 3 back, else 0. */
    private int barButton(double mx, double my) {
        if (my < geo.top && mx >= width - tw("◀ BACK") - 12) return 3;
        if (my < geo.menuY) return 0;
        if (mx >= 2 && mx < 10 + tw("MAIN MENU")) return 1;
        int cw = tw("CHARACTER") + tw("SETTINGS") + 30;
        int sx = width / 2 - cw / 2 + tw("CHARACTER") + 30;
        if (mx >= sx - 4 && mx < sx + tw("SETTINGS") + 4) return 2;
        return 0;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        shadowBox(g, x, y, w, h);
        g.fillGradient(x, y, x + w, y + h, 0xE21C1C1B, 0xE20D0D0D);
        g.renderOutline(x, y, w, h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, HILITE);           // the lit top edge
    }

    /** Tarkov's small labels: the game font at two thirds size. */
    private void label(GuiGraphics g, String text, int x, int y) {
        small(g, text, x, y + 2, DIM);
    }

    private void small(GuiGraphics g, String text, int x, int y, int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2 / 3f, 2 / 3f, 1);
        text(g, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    private void cell(GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, CELL_LINE);
        g.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF0B0B0B, 0xFF151514);
        g.fill(x + 1, y + 1, x + size - 1, y + 2, 0x60000000);   // inset: a shadow along the top
    }

    // Left: the silhouette and the gear frames
    private void drawGear(GuiGraphics g, int mx, int my) {
        int top = geo.contentTop, bottom = geo.sling[1] - 2;
        int cx = geo.e0 + geo.ew / 2;
        int size = Math.max(20, (bottom - top) * 9 / 20);
        // A dim, grey figure of you behind the frames, like Tarkov's
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, bottom - 4, size, cx - mx, top + 30 - my, player);
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        g.fill(geo.e0, top - 2, geo.e0 + geo.ew, bottom + 1, 0xB8101010);   // a dim grey figure, like Tarkov's
        g.pose().popPose();
        for (Map.Entry<Integer, int[]> e : geo.frames.entrySet()) {
            int[] f = e.getValue();
            if (f[4] < 0) continue;
            g.pose().pushPose();
            g.pose().translate(0, 0, 160);
            label(g, FRAME_LABELS[f[4]], f[0], f[1] - 9);
            panel(g, f[0], f[1], f[2], f[3]);
            g.pose().popPose();
            ItemStack st = menu.real.getSlot(e.getKey()).getItem();
            g.pose().pushPose();
            g.pose().translate(0, 0, 160);
            if (!st.isEmpty()) bigIcon(g, st, f[0], f[1], f[2], f[3]);
            else g.drawCenteredString(font, "-", f[0] + f[2] / 2, f[1] + f[3] / 2 - 4, FAINT);
            g.pose().popPose();
        }
        int[] s = geo.sling;
        g.pose().pushPose();
        g.pose().translate(0, 0, 160);
        label(g, "ON SLING", s[0], s[1] - 9);
        panel(g, s[0], s[1], s[2], s[3]);
        ItemStack held = player.getInventory().getSelected();
        if (!held.isEmpty()) bigIcon(g, held, s[0], s[1], s[2], s[3]);
        text(g, String.valueOf(player.getInventory().selected + 1), s[0] + 3, s[1] + 3, FAINT, false);
        g.pose().popPose();
    }

    /** An item drawn large in a frame, with its count and durability bar in the corner. */
    private void bigIcon(GuiGraphics g, ItemStack st, int x, int y, int w, int h) {
        float scale = Math.min(w, h) / 16f * 0.8f;
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(st, -8, -8);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.renderItemDecorations(font, st, x + w - 17, y + h - 17);
        g.pose().popPose();
    }

    private void drawFrameHover(GuiGraphics g, int mx, int my) {
        for (int[] f : geo.frames.values()) if (mx >= f[0] && mx < f[0] + f[2] && my >= f[1] && my < f[1] + f[3]) g.renderOutline(f[0], f[1], f[2], f[3], TEXT);
        int[] s = geo.sling;
        if (inSling(mx, my)) g.renderOutline(s[0], s[1], s[2], s[3], TEXT);
    }

    private boolean inSling(double mx, double my) {
        int[] s = geo.sling;
        return mx >= s[0] && mx < s[0] + s[2] && my >= s[1] && my < s[1] + s[3];
    }

    // Middle: crafting and the overflow
    private void drawMiddle(GuiGraphics g) {
        int x = geo.m0;

        panel(g, x, geo.craftY - 1, Math.min(geo.mw, 4 + 36 + 12 + 18 + 4), 40);
        for (int i = 0; i < 4; i++) cell(g, x + 4 + i % 2 * 18, geo.craftY + i / 2 * 18, 18);
        text(g, "→", x + 4 + 36 + 2, geo.craftY + 13, DIM, false);
        cell(g, x + 4 + 36 + 11, geo.craftY + 9, 18);
        // Overflow: what has no spot in the backpack
        unsorted.clear();
        List<ItemStack> main = items();
        int[][] occ = TarkovClient.LAYOUT.occupancy(main);
        for (int s = 0; s < TarkovLayout.SLOTS; s++) {
            ItemStack st = main.get(TarkovLayout.FIRST + s);
            int p = TarkovClient.LAYOUT.pos[s];
            if (!st.isEmpty() && (p == TarkovLayout.NONE || !owns(occ, s, st, p))) unsorted.add(s);
        }
        if (unsorted.isEmpty()) label(g, "OVERFLOW", x, geo.overflowY - 10);
        if (!unsorted.isEmpty()) small(g, "OVERFLOW - NO ROOM", x, geo.overflowY - 8, BAD);
        int cols = geo.overflowCols, rows = geo.overflowRows;
        panel(g, x, geo.overflowY - 1, cols * 18 + 2, Math.min(rows, 3) * 18 + 2);
        for (int i = 0; i < cols * Math.min(rows, 3); i++) {
            int cx = x + 1 + i % cols * 18, cy = geo.overflowY + i / cols * 18;
            cell(g, cx, cy, 18);
            if (i < unsorted.size()) {
                ItemStack st = main.get(TarkovLayout.FIRST + unsorted.get(i));
                g.renderItem(st, cx + 1, cy + 1);
                g.renderItemDecorations(font, st, cx + 1, cy + 1);
            }
        }
    }

    // Right: the backpack and its filter column
    private void drawBackpack(GuiGraphics g, int mx, int my) {
        panel(g, geo.sx, geo.sy, geo.sw, geo.sh);
        g.fillGradient(geo.sx + 1, geo.sy + 1, geo.sx + geo.sw - 1, geo.sy + 12, 0xFF2A2927, 0xFF1A1918);
        bold(g, "BACKPACK", geo.sx + 6, geo.sy + 3, 0xFFF2EFE6);
        String lvl = "LVL " + player.experienceLevel;
        text(g, lvl, geo.sx + geo.sw - tw(lvl) - 4, geo.sy + 3, DIM, false);
        // Filter column
        for (int i = 0; i < FILTERS.length; i++) {
            int fx = geo.sx + 3, fy = geo.gy + i * 18;
            if (fy + 16 > geo.sy + geo.sh) break;
            boolean on = filter == i, hover = mx >= fx && mx < fx + 16 && my >= fy && my < fy + 16;
            g.fillGradient(fx, fy, fx + 16, fy + 16, on ? 0xFF46443E : hover ? 0xFF2E2D2A : 0xFF1C1C1B, on ? 0xFF2E2C28 : hover ? 0xFF1E1D1B : 0xFF111111);
            if (on) g.fill(fx, fy + 15, fx + 16, fy + 16, ACCENT);
            g.pose().pushPose();
            g.pose().translate(fx + 8, fy + 8, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.renderItem(FILTER_ICONS[i], -8, -8);
            g.pose().popPose();
        }
        g.fill(geo.sx + 21, geo.gy, geo.sx + 22, geo.sy + geo.sh - 2, EDGE);
        // The grid
        int gx = geo.gx, gy = geo.gy, C = C();
        g.fill(gx - 1, gy - 1, gx + gridW() + 1, gy + gridH() + 1, EDGE);
        for (int cx = 0; cx < TarkovLayout.width(); cx++) for (int cy = 0; cy < TarkovLayout.height(); cy++) {
            int x = gx + cx * C, y = gy + cy * C;
            g.fill(x, y, x + C, y + C, CELL_LINE);
            g.fillGradient(x + 1, y + 1, x + C, y + C, 0xFF0B0B0B, 0xFF141413);
            g.fill(x + 1, y + 1, x + C, y + 2, 0x50000000);
        }
        List<ItemStack> main = items();
        for (int s = 0; s < TarkovLayout.SLOTS; s++) {
            ItemStack st = main.get(TarkovLayout.FIRST + s);
            if (st.isEmpty() || unsorted.contains(s)) continue;
            int p = TarkovClient.LAYOUT.pos[s];
            if (p == TarkovLayout.NONE) continue;
            boolean t = TarkovLayout.turned(p);
            drawItem(g, st, gx + TarkovLayout.x(p) * C, gy + TarkovLayout.y(p) * C, TarkovLayout.w(st, t), TarkovLayout.h(st, t), t, 1);
            if (filter != 0 && category(st) != filter) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 250);
                g.fill(gx + TarkovLayout.x(p) * C, gy + TarkovLayout.y(p) * C, gx + (TarkovLayout.x(p) + TarkovLayout.w(st, t)) * C, gy + (TarkovLayout.y(p) + TarkovLayout.h(st, t)) * C, 0xC0080808);
                g.pose().popPose();
            }
        }
    }

    /** Filter category: 1 weapons, 2 armor, 3 tools, 4 blocks, 5 food, 6 other. */
    static int category(ItemStack st) {
        var it = st.getItem();
        if (it instanceof SwordItem || it instanceof dev.factoryascension.item.ShotgunItem || it instanceof dev.factoryascension.gun.GunItem
            || it instanceof dev.factoryascension.gun.CartridgeItem || it instanceof net.minecraft.world.item.ProjectileWeaponItem
            || it instanceof net.minecraft.world.item.TridentItem) return 1;
        if (it instanceof ArmorItem || it instanceof net.minecraft.world.item.ShieldItem || it instanceof net.minecraft.world.item.ElytraItem) return 2;
        if (it instanceof TieredItem || it instanceof net.minecraft.world.item.FishingRodItem || it instanceof net.minecraft.world.item.BucketItem) return 3;
        if (it instanceof BlockItem) return 4;
        if (st.isEdible()) return 5;
        return 6;
    }

    // Stats row: weight, health, food, armor, effects
    private void drawStats(GuiGraphics g) {
        int x = 8, y = geo.statsY;
        float kg = TarkovServer.weight(player);
        float slow = TarkovServer.slowdown(kg);
        x = stat(g, x, y, "⚖", String.format("%.1f", kg), FAConfig.weightEnabled ? String.format("/%.0f", FAConfig.weightCarry) : "", slow > 0 ? BAD : TEXT);
        x = stat(g, x, y, "❤", String.valueOf(Math.round(player.getHealth())), "/" + Math.round(player.getMaxHealth()), 0xFFE06050);
        x = stat(g, x, y, "☕", String.valueOf(player.getFoodData().getFoodLevel()), "/20", 0xFFE0B050);
        x = stat(g, x, y, "⛨", String.valueOf(player.getArmorValue()), "", 0xFF9AB0D0);
        if (slow > 0) {
            text(g, String.format("-%d%% speed", Math.round(slow * 100)), x, y, BAD, false);
            x += tw("-00% speed") + 8;
        }
        var textures = Minecraft.getInstance().getMobEffectTextures();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (x + 10 > (geo.statsY == geo.quickY + 6 ? geo.quickX - 4 : geo.m0)) break;
            g.blit(x, y - 1, 0, 9, 9, textures.get(e.getEffect()));
            x += 11;
        }
    }

    private int stat(GuiGraphics g, int x, int y, String icon, String value, String max, int colour) {
        text(g, icon, x, y, colour, false);
        x += tw(icon) + 2;
        text(g, value, x, y, TEXT, false);
        x += tw(value);
        if (!max.isEmpty()) {
            text(g, max, x, y, FAINT, false);
            x += tw(max);
        }
        return x + 8;
    }

    // Quick use: the hotbar, numbered
    private void drawQuickUse(GuiGraphics g) {
        if (geo.statsY != geo.quickY - 10) label(g, "QUICK USE", geo.quickX, geo.quickY - 10);
        int sel = player.getInventory().selected;
        for (int i = 0; i < 9; i++) {
            int x = geo.quickX + i * 22, y = geo.quickY;
            shadowBox(g, x, y, 20, 20);
            g.fill(x, y, x + 20, y + 20, i == sel ? ACCENT : EDGE);
            g.fillGradient(x + 1, y + 1, x + 19, y + 19, 0xFF181817, 0xFF0C0C0C);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.pose().scale(0.5f, 0.5f, 1);
            text(g, String.valueOf(i + 1), (x + 2) * 2, (y + 2) * 2, i == sel ? TEXT : FAINT, false);
            g.pose().popPose();
        }
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

    /** Tarkov's muted backings: weapons rust, armor slate, tools bronze, blocks grey, food olive, the rest dark teal. */
    private static int tint(ItemStack st) {
        return switch (category(st)) {
            case 1 -> 0x3C2622;
            case 2 -> 0x232C38;
            case 3 -> 0x342E24;
            case 4 -> 0x2A2A28;
            case 5 -> 0x2C3222;
            default -> 0x20302E;
        };
    }

    /** An item filling its cells: muted backing, the icon scaled up (long items laid along their length), count and bar. */
    void drawItem(GuiGraphics g, ItemStack st, int x, int y, int w, int h, boolean turned, float alpha) {
        int C = C(), pw = w * C, ph = h * C;
        int a = (int) (0xE0 * alpha);
        int tc = tint(st);
        int lighter = ((Math.min(255, (tc >> 16 & 255) + 22)) << 16) | ((Math.min(255, (tc >> 8 & 255) + 22)) << 8) | Math.min(255, (tc & 255) + 22);
        g.fillGradient(x + 1, y + 1, x + pw, y + ph, a << 24 | lighter, a << 24 | tc);
        g.renderOutline(x + 1, y + 1, pw - 1, ph - 1, (int) (0xC0 * alpha) << 24 | 0x5A5A56);
        g.fill(x + 2, y + 2, x + pw - 1, y + 3, (int) (0x30 * alpha) << 24 | 0xFFFFFF);
        int[] up = TarkovLayout.size(st);
        float angle = turned ? 90 : 0;
        float scale;
        // Tools, weapons, bows and rods are drawn on the diagonal; everything else (armor, shields, blocks) upright
        net.minecraft.world.item.Item it = st.getItem();
        boolean diagonal = it instanceof TieredItem || it instanceof net.minecraft.world.item.ProjectileWeaponItem || it instanceof net.minecraft.world.item.FishingRodItem
            || it instanceof net.minecraft.world.item.TridentItem || it instanceof dev.factoryascension.item.ShotgunItem && !(it instanceof dev.factoryascension.item.FlareGunItem);
        if (it instanceof dev.factoryascension.gun.GunItem) {
            scale = Math.min(Math.max(pw, ph) / 16f * 0.95f, Math.min(pw, ph) / 16f * 2.2f);   // drawn level, filling its length
        } else if (up[0] == up[1] || !diagonal) {
            scale = Math.min(pw, ph) / 16f * 0.86f;
        } else {
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
        g.pose().translate(0, 0, 200);
        g.renderItemDecorations(font, st, x + pw - 17, y + ph - 17);
        // Tarkov's short name in the top right corner, on items two or more cells wide
        if (pw >= C * 2 && C >= 14) {
            String name = shortName(st, pw - 4);
            g.pose().pushPose();
            g.pose().scale(0.5f, 0.5f, 1);
            text(g, name, (x + pw - 2) * 2 - tw(name), (y + 2) * 2, (int) (0xFF * alpha) << 24 | 0xD6D4CC, false);
            g.pose().popPose();
        }
        g.pose().popPose();
    }

    private String shortName(ItemStack st, int maxPx) {
        String n = st.getHoverName().getString();
        while (tw(n) / 2 > maxPx && n.length() > 3) n = n.substring(0, n.length() - 1);
        return n;
    }

    /** Where the held item would go: its outline green (fits / stacks) or red, with the item drawn at its size. */
    private void drawPlacing(GuiGraphics g, int mx, int my) {
        ItemStack held = menu.getCarried();
        int[] spot = spotFor(held, mx, my);
        int w = TarkovLayout.w(held, carriedTurned), h = TarkovLayout.h(held, carriedTurned);
        int C = C(), x = geo.gx + spot[0] * C, y = geo.gy + spot[1] * C;
        boolean ok = spot[2] != 0;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x, y, x + w * C, y + h * C, (ok ? OK : BAD) & 0x40FFFFFF);
        g.renderOutline(x, y, w * C, h * C, ok ? OK : BAD);
        drawItem(g, held, x, y, w, h, carriedTurned, 0.6f);
        g.pose().popPose();
    }

    // ---- What's under the mouse ----
    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        int[] f = geo.hitBoxes.get(Geo.key(x, y));
        if (f != null) return mx >= f[0] && mx < f[0] + f[2] && my >= f[1] && my < f[1] + f[3];
        return super.isHovering(x, y, w, h, mx, my);
    }

    @Override
    public int getSlotColor(int index) {
        int[] f = geo.frames.get(index);
        return f != null && f[4] >= 0 ? 0 : super.getSlotColor(index);   // gear frames draw their own outline
    }

    private boolean inGrid(double mx, double my) {
        double x = mx - geo.gx, y = my - geo.gy;
        return x >= 0 && y >= 0 && x < gridW() && y < gridH();
    }

    /** The main slot under the mouse: in the grid, or in the overflow. */
    private int slotUnder(double mx, double my) {
        if (inGrid(mx, my)) {
            int cx = (int) ((mx - geo.gx) / C()), cy = (int) ((my - geo.gy) / C());
            int s = TarkovClient.LAYOUT.slotAt(items(), cx, cy);
            return s != TarkovLayout.NONE && !unsorted.contains(s) ? s : TarkovLayout.NONE;
        }
        double ox = mx - geo.m0 - 1, oy = my - geo.overflowY;
        if (ox >= 0 && oy >= 0 && ox < geo.overflowCols * 18 && oy < Math.min(geo.overflowRows, 3) * 18) {
            int i = (int) (oy / 18) * geo.overflowCols + (int) (ox / 18);
            if (i < unsorted.size()) return unsorted.get(i);
        }
        return TarkovLayout.NONE;
    }

    /** {x, y, fits (1/0), merge slot or NONE} for the held item centred on the mouse. */
    private int[] spotFor(ItemStack held, double mx, double my) {
        int w = TarkovLayout.w(held, carriedTurned), h = TarkovLayout.h(held, carriedTurned);
        int x = Math.round((float) ((mx - geo.gx) / C() - w / 2.0)), y = Math.round((float) ((my - geo.gy) / C() - h / 2.0));
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
        return new double[] {geo.gx + cx * C() + C() / 2.0, geo.gy + cy * C() + C() / 2.0};
    }

    public double[] devCellCenter(int slot) {
        int p = TarkovClient.LAYOUT.pos[slot];
        return devCell(TarkovLayout.x(p), TarkovLayout.y(p));
    }

    // ---- Clicks ----
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!ready()) return true;
        Minecraft mc = Minecraft.getInstance();
        switch (barButton(mx, my)) {
            case 1 -> { onClose(); mc.setScreen(new PauseScreen(true)); return true; }
            case 2 -> { onClose(); mc.setScreen(new dev.factoryascension.client.FASettingsScreen(null)); return true; }
            case 3 -> { onClose(); return true; }
            default -> { }
        }
        for (int i = 0; i < FILTERS.length; i++) {
            int fx = geo.sx + 3, fy = geo.gy + i * 18;
            if (mx >= fx && mx < fx + 16 && my >= fy && my < fy + 16 && fy + 16 <= geo.sy + geo.sh) {
                filter = i;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4f, 0.3f));
                return true;
            }
        }
        if (inSling(mx, my)) {
            Slot slot = menu.getSlot(36 + player.getInventory().selected);
            slotClicked(slot, slot.index, button, hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            return true;
        }
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
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 0.6f, 1.4f));
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
        if (inGrid(mx, my) || slotUnder(mx, my) != TarkovLayout.NONE || inSling(mx, my)) return true;
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
        // Outside every panel (empty background): the held item is thrown, like clicking outside the normal inventory
        return my > geo.top && my < geo.menuY && !(mx >= geo.sx && mx < geo.sx + geo.sw && my >= geo.sy && my < geo.sy + geo.sh)
            && !(mx >= geo.m0 && mx < geo.sx && my >= geo.contentTop) && !(mx < geo.m0 && my >= geo.contentTop && my < geo.quickY - 12)
            && !(my >= geo.quickY - 12 && my < geo.menuY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        if (!menu.getCarried().isEmpty()) {
            String hint = "R: turn";
            text(g, hint, geo.sx + geo.sw - tw(hint) - 4, geo.sy + geo.sh + 3, TEXT, false);
        }
    }
}
