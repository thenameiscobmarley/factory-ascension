package dev.factoryascension.client.gun;

import dev.factoryascension.gun.AmmoPress;
import dev.factoryascension.gun.AmmoType;
import dev.factoryascension.gun.AttachmentItem;
import dev.factoryascension.gun.Attachments;
import dev.factoryascension.gun.Bench;
import dev.factoryascension.gun.Caliber;
import dev.factoryascension.gun.CartridgeItem;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import dev.factoryascension.gun.GunStats;
import dev.factoryascension.gun.WeaponsBenchMenu;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Weapons Workbench, in the Tarkov inventory's style. MODDING: the gun large in the middle, its twelve attachment
 * slots under it (click one to browse what goes there, right-click to take the last one off), and the catalog on the
 * right with a search box, each attachment's numbers and cost, and INSTALL. A Jury-Rigged gun is offered everything.
 * AMMO PRESS: pick a caliber, a type and a colour; the case and powder slots take any item; PRESS makes the rounds.
 * GUNSMITH: every gun, filtered by kind or searched, shown large (its smooth mesh) with its numbers and what it costs;
 * BUILD makes it. SKINS: the gun in the GUN slot shown wearing the picked skin, live; APPLY puts it on.
 */
public class WeaponsBenchScreen extends AbstractContainerScreen<WeaponsBenchMenu> {
    static final int BG = 0xF00E0E0E, PANEL = 0xC0161616, EDGE = 0xFF3A3A38, CELL = 0xFF121212, CELL_LINE = 0xFF262626;
    static final int TEXT = 0xFFD6D4CC, DIM = 0xFF8C8A84, FAINT = 0xFF56544F, BAD = 0xFFC8452E, OK = 0xFF6FB04A, GOLD = 0xFFE0B040;
    static final int[] TIER = {0xFFB8B6AE, 0xFF7FC060, 0xFF60A8E8, 0xFFC080F0};
    static final int[] SWATCH = {-1, 0xC8803C, 0xB8B8BC, 0xE8C040, 0x2A2A2C, 0xE8E4D8, 0xC02828, 0xE06010, 0xE8C020, 0x40A040, 0x30A090,
        0x3060C0, 0x8040C0, 0xE060A0, 0x60E0E8, 0x40C8B8};
    static final int ROW = 12, LIST_X = 242, LIST_Y = 34, LIST_ROWS = 11;

    static final String[] TABS = {"MODDING", "AMMO PRESS", "GUNSMITH", "SKINS"};
    static final String[] KINDS = {"ALL", "PISTOL", "REVOLVER", "SMG", "RIFLE", "DMR", "SNIPER", "SHOTGUN", "LMG"};
    private int tab = 0;                         // 0 modding, 1 ammo press, 2 gunsmith, 3 skins
    private int kindFilter = 0, gunPick = 0, gunScroll = 0, skinPick = 0, skinScroll = 0;
    private final List<GunSpec> guns = new ArrayList<>();
    private final List<String> skins = new ArrayList<>(dev.factoryascension.gun.Skins.ALL.keySet());
    private String lastGunQuery = null;
    private int lastKind = -1;
    private Attachments.Slot slot = Attachments.Slot.MUZZLE;
    private final List<Attachments.Att> shown = new ArrayList<>();
    private int scroll = 0, picked = -1;
    private EditBox search;
    private String lastQuery = "", lastGunKey = "";
    private Attachments.Slot lastSlot = null;
    private int cal = 0, type = 0, colour = 0;

    public WeaponsBenchScreen(WeaponsBenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = WeaponsBenchMenu.W;
        imageHeight = WeaponsBenchMenu.H;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + LIST_X, topPos + 19, 152, 12, Component.literal("Search"));
        search.setHint(Component.literal("Search attachments..."));
        search.setBordered(true);
        addRenderableWidget(search);
    }

    // ---- The catalog list ----
    private void refresh() {
        ItemStack gun = menu.gun();
        GunSpec spec = GunItem.spec(gun);
        boolean jury = !gun.isEmpty() && Bench.juryRigged(gun);
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        String gunKey = (spec == null ? "" : spec.id) + jury;
        if (q.equals(lastQuery) && slot == lastSlot && gunKey.equals(lastGunKey)) return;
        lastQuery = q;
        lastSlot = slot;
        lastGunKey = gunKey;
        shown.clear();
        for (Attachments.Att a : Attachments.ALL) {
            if (a.slot != slot) continue;
            if (spec != null && !jury && !a.fits(spec)) continue;
            if (!q.isEmpty() && !a.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            shown.add(a);
        }
        scroll = 0;
        picked = shown.isEmpty() ? -1 : 0;
    }

    private void refreshGuns() {
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        if (q.equals(lastGunQuery) && kindFilter == lastKind) return;
        lastGunQuery = q;
        lastKind = kindFilter;
        guns.clear();
        for (GunSpec g : dev.factoryascension.gun.Guns.ALL) {
            if (kindFilter > 0 && !g.kind.name().equals(KINDS[kindFilter])) continue;
            if (!q.isEmpty() && !g.name.toLowerCase(Locale.ROOT).contains(q) && !g.caliber.label.toLowerCase(Locale.ROOT).contains(q)) continue;
            guns.add(g);
        }
        gunScroll = 0;
        gunPick = guns.isEmpty() ? -1 : 0;
    }

    /** Draws an item big; guns as their smooth mesh. */
    private void bigItem(GuiGraphics g, ItemStack s, int cx, int cy, float scale) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 100);
        g.pose().scale(scale, scale, scale);
        GunRenderer.preview = true;
        try {
            g.renderItem(s, -8, -8);
        } finally {
            GunRenderer.preview = false;
        }
        g.pose().popPose();
    }

    // ---- Drawing ----
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        search.visible = tab == 0 || tab == 2;
        search.setHint(Component.literal(tab == 2 ? "Search guns..." : "Search attachments..."));
        if (tab == 0) refresh();
        if (tab == 2) refreshGuns();
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
        if (tab == 0) {
            int row = listRow(mx, my);
            if (row >= 0) g.renderComponentTooltip(font, tooltipFor(shown.get(row)), mx, my);
        }
    }

    private List<Component> tooltipFor(Attachments.Att a) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal(a.name));
        for (String line : Bench.describe(a)) out.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
        return out;
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, EDGE);
    }

    private void small(GuiGraphics g, String s, int x, int y, int c) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2 / 3f, 2 / 3f, 1);
        g.drawString(font, s, 0, 0, c, false);
        g.pose().popPose();
    }

    private void cell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, CELL_LINE);
        g.fill(x, y, x + 16, y + 16, CELL);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x0 = leftPos, y0 = topPos;
        g.fill(x0 - 1, y0 - 1, x0 + imageWidth + 1, y0 + imageHeight + 1, EDGE);
        g.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, BG);
        // Tabs
        String[] tabs = TABS;
        int tx = x0 + 4;
        for (int i = 0; i < tabs.length; i++) {
            int w = font.width(tabs[i]) + 12;
            if (tab == i) { g.fill(tx, y0 + 1, tx + w, y0 + 14, 0xFF2A2A28); g.fill(tx, y0 + 13, tx + w, y0 + 14, TEXT); }
            g.drawString(font, tabs[i], tx + 6, y0 + 4, tab == i ? TEXT : FAINT, false);
            tx += w + 2;
        }
        String title = "WEAPONS WORKBENCH";
        g.drawString(font, title, x0 + imageWidth - font.width(title) - 6, y0 + 4, DIM, false);
        g.fill(x0, y0 + 14, x0 + imageWidth, y0 + 15, EDGE);
        // Bench slots
        small(g, "GUN", x0 + 12, y0 + 17, DIM);
        cell(g, x0 + 12, y0 + 24);
        small(g, "CASE", x0 + 12, y0 + 63, DIM);
        cell(g, x0 + 12, y0 + 70);
        small(g, "POWDER", x0 + 8, y0 + 87, DIM);
        cell(g, x0 + 12, y0 + 94);
        // Your inventory
        small(g, "INVENTORY", x0 + 8, y0 + 147, DIM);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) cell(g, x0 + 8 + c * 18, y0 + 154 + r * 18);
        for (int c = 0; c < 9; c++) cell(g, x0 + 8 + c * 18, y0 + 212);
        if (tab == 0) drawModding(g, mx, my);
        else if (tab == 1) drawPress(g, mx, my);
        else if (tab == 2) drawGunsmith(g, mx, my);
        else drawSkins(g, mx, my);
    }

    private void drawModding(GuiGraphics g, int mx, int my) {
        int x0 = leftPos, y0 = topPos;
        ItemStack gun = menu.gun();
        panel(g, x0 + 34, y0 + 18, 204, 82);
        if (gun.isEmpty()) {
            g.drawCenteredString(font, "Put a gun in the GUN slot", x0 + 136, y0 + 55, FAINT);
        } else {
            bigItem(g, gun, x0 + 136, y0 + 58, 9);
            GunStats st = GunStats.of(gun);
            small(g, gun.getHoverName().getString().toUpperCase(Locale.ROOT), x0 + 38, y0 + 21, TEXT);
            if (Bench.juryRigged(gun)) small(g, "JURY-RIGGED", x0 + 234 - font.width("JURY-RIGGED") * 2 / 3, y0 + 21, 0xFFD070F0);
            if (st != null) small(g, String.format("%d rpm  %.1f dmg  %d rds  recoil %.1f  spread %.1f", st.rpm,
                dev.factoryascension.gun.Guns.BASE_DAMAGE * st.spec.caliber.damage * st.damage, st.mag, st.recoilUp, st.spreadAim), x0 + 38, y0 + 93, DIM);
        }
        // The twelve slots
        List<String> ids = GunStats.ids(gun);
        Attachments.Slot[] slots = Attachments.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            int sx = x0 + 34 + i % 6 * 34, sy = y0 + 103 + i / 6 * 21;
            boolean sel = slots[i] == slot, hover = mx >= sx && mx < sx + 32 && my >= sy && my < sy + 19;
            g.fill(sx, sy, sx + 32, sy + 19, sel ? 0xFF2E2E2A : hover ? 0xFF222220 : CELL);
            g.renderOutline(sx, sy, 32, 19, sel ? TEXT : EDGE);
            small(g, slots[i].label.toUpperCase(Locale.ROOT), sx + 2, sy + 2, sel ? TEXT : DIM);
            int n = 0;
            for (String id : ids) {
                Attachments.Att a = Attachments.BY_ID.get(id);
                if (a == null || a.slot != slots[i]) continue;
                g.pose().pushPose();
                g.pose().translate(sx + 4 + n * 7, sy + 8, 0);
                g.pose().scale(0.6f, 0.6f, 1);
                g.renderItem(AttachmentItem.of(id), 0, 0);
                g.pose().popPose();
                n++;
            }
        }
        // The catalog
        panel(g, x0 + LIST_X - 2, y0 + 32, 156, LIST_ROWS * ROW + 4);
        small(g, shown.size() + " for " + slot.label.toUpperCase(Locale.ROOT), x0 + LIST_X, y0 + 17 - 0, FAINT);
        for (int r = 0; r < LIST_ROWS; r++) {
            int i = scroll + r;
            if (i >= shown.size()) break;
            Attachments.Att a = shown.get(i);
            int ry = y0 + LIST_Y + r * ROW;
            if (i == picked) g.fill(x0 + LIST_X - 1, ry, x0 + LIST_X + 153, ry + ROW, 0xFF2E2E2A);
            g.fill(x0 + LIST_X, ry + 3, x0 + LIST_X + 5, ry + 8, 0xFF000000 | a.colour);
            String name = a.name;
            while (font.width(name) * 2 / 3 > 140 && name.length() > 4) name = name.substring(0, name.length() - 1);
            small(g, name, x0 + LIST_X + 8, ry + 3, TIER[Math.min(3, a.tier)]);
        }
        if (shown.size() > LIST_ROWS) {
            int barH = Math.max(8, LIST_ROWS * ROW * LIST_ROWS / shown.size());
            int barY = y0 + LIST_Y + (LIST_ROWS * ROW - barH) * scroll / Math.max(1, shown.size() - LIST_ROWS);
            g.fill(x0 + LIST_X + 151, barY, x0 + LIST_X + 153, barY + barH, DIM);
        }
        // The one picked: numbers, cost, INSTALL
        int iy = y0 + LIST_Y + LIST_ROWS * ROW + 6;
        if (picked >= 0 && picked < shown.size()) {
            Attachments.Att a = shown.get(picked);
            List<String> lines = Bench.describe(a);
            for (int k = 0; k < Math.min(4, lines.size()); k++) small(g, lines.get(k), x0 + LIST_X, iy + k * 7, DIM);
            int[] c = Bench.cost(a);
            String cost = c[0] + " parts" + (c[1] > 0 ? " + " + c[1] + " tier" : "");
            small(g, cost, x0 + LIST_X, iy + 30, GOLD);
            String why = gun.isEmpty() ? "No gun" : Bench.canInstall(gun, a);
            boolean ok = why == null;
            int bx = x0 + LIST_X + 92, by = iy + 26, bw = 60;
            boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 14;
            g.fill(bx, by, bx + bw, by + 14, ok ? (hb ? 0xFF3C5A30 : 0xFF2C4424) : 0xFF2A2020);
            g.renderOutline(bx, by, bw, 14, ok ? OK : BAD);
            g.drawCenteredString(font, "INSTALL", bx + bw / 2, by + 3, ok ? TEXT : FAINT);
            if (!ok && why != null) small(g, why.length() > 38 ? why.substring(0, 38) : why, x0 + LIST_X, iy + 44, BAD);
        }
    }

    private void drawPress(GuiGraphics g, int mx, int my) {
        int x0 = leftPos, y0 = topPos;
        panel(g, x0 + 34, y0 + 18, 204, 124);
        ItemStack caseIn = menu.bench.getItem(WeaponsBenchMenu.CASE), powder = menu.bench.getItem(WeaponsBenchMenu.POWDER);
        Caliber c = Caliber.values()[cal];
        AmmoType t = AmmoType.values()[type];
        int col = SWATCH[colour] < 0 ? (caseIn.isEmpty() ? 0xC8803C : AmmoPress.colour(caseIn)) : SWATCH[colour];
        ItemStack preview = CartridgeItem.make(c, t, "", "", col, 1, 1, AmmoPress.perPress(c));
        g.pose().pushPose();
        g.pose().translate(x0 + 80, y0 + 70, 100);
        g.pose().scale(5, 5, 5);
        g.renderItem(preview, -8, -8);
        g.pose().popPose();
        int tx = x0 + 128;
        g.drawString(font, c.label, tx, y0 + 26, TEXT, false);
        g.drawString(font, t.label, tx, y0 + 38, TIER[2], false);
        small(g, AmmoPress.perPress(c) + " rounds a press", tx, y0 + 52, DIM);
        if (!powder.isEmpty()) small(g, String.format("Powder: %s (x%.2f)", powder.getHoverName().getString(), AmmoPress.power(powder)), tx, y0 + 62, DIM);
        else small(g, "Powder: any item", tx, y0 + 62, FAINT);
        if (!caseIn.isEmpty()) small(g, String.format("Case: %s (%d%%)", caseIn.getHoverName().getString(), Math.round(AmmoPress.reliability(caseIn) * 100)), tx, y0 + 70, DIM);
        else small(g, "Case: any item", tx, y0 + 70, FAINT);
        // A few lines about the type
        String d = t.desc;
        int line = 0;
        while (!d.isEmpty() && line < 4) {
            int cut = d.length();
            while (font.width(d.substring(0, cut)) * 2 / 3 > 104 && cut > 1) cut = d.lastIndexOf(' ', cut - 1) > 0 ? d.lastIndexOf(' ', cut - 1) : cut - 1;
            small(g, d.substring(0, cut), tx, y0 + 82 + line * 7, FAINT);
            d = d.substring(cut).trim();
            line++;
        }
        // Press
        int bx = x0 + 128, by = y0 + 120, bw = 100;
        boolean ready = !caseIn.isEmpty() && !powder.isEmpty();
        boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
        g.fill(bx, by, bx + bw, by + 16, ready ? (hb ? 0xFF3C5A30 : 0xFF2C4424) : 0xFF2A2020);
        g.renderOutline(bx, by, bw, 16, ready ? OK : EDGE);
        g.drawCenteredString(font, "PRESS", bx + bw / 2, by + 4, ready ? TEXT : FAINT);
        // Pickers on the right: caliber, type, colour
        int px = x0 + LIST_X - 2;
        small(g, "CALIBER", px, y0 + 18, DIM);
        for (int i = 0; i < Caliber.values().length; i++) chip(g, px + i % 4 * 39, y0 + 25 + i / 4 * 13, 37, Caliber.values()[i].label, i == cal, mx, my);
        small(g, "TYPE", px, y0 + 92, DIM);
        for (int i = 0; i < AmmoType.values().length; i++) chip(g, px + i % 4 * 39, y0 + 99 + i / 4 * 13, 37, AmmoType.values()[i].label, i == type, mx, my);
        small(g, "COLOUR", px, y0 + 167, DIM);
        for (int i = 0; i < SWATCH.length; i++) {
            int sx = px + i % 8 * 19, sy = y0 + 174 + i / 8 * 14;
            if (SWATCH[i] < 0) { g.fill(sx, sy, sx + 17, sy + 12, 0xFF202020); small(g, "case", sx + 2, sy + 4, DIM); }
            else g.fill(sx, sy, sx + 17, sy + 12, 0xFF000000 | SWATCH[i]);
            g.renderOutline(sx, sy, 17, 12, i == colour ? TEXT : EDGE);
        }
    }

    private void chip(GuiGraphics g, int x, int y, int w, String text, boolean on, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + 12;
        g.fill(x, y, x + w, y + 12, on ? 0xFF2E2E2A : hover ? 0xFF222220 : CELL);
        g.renderOutline(x, y, w, 12, on ? TEXT : EDGE);
        String s = text;
        while (font.width(s) * 2 / 3 > w - 4 && s.length() > 2) s = s.substring(0, s.length() - 1);
        small(g, s, x + 2, y + 4, on ? TEXT : DIM);
    }

    // ---- GUNSMITH ----
    static final int GROWS = 11, GLIST_Y = 62;

    private void drawGunsmith(GuiGraphics g, int mx, int my) {
        int x0 = leftPos, y0 = topPos;
        // Kind filter chips over the list
        int px = x0 + LIST_X - 2;
        for (int i = 0; i < KINDS.length; i++) chip(g, px + i % 5 * 31, y0 + 34 + i / 5 * 13, 29, KINDS[i], i == kindFilter, mx, my);
        panel(g, px, y0 + GLIST_Y - 1, 156, GROWS * ROW + 2);
        for (int r = 0; r < GROWS; r++) {
            int i = gunScroll + r;
            if (i >= guns.size()) break;
            GunSpec gs = guns.get(i);
            int ry = y0 + GLIST_Y + r * ROW;
            if (i == gunPick) g.fill(px + 1, ry, px + 155, ry + ROW, 0xFF2E2E2A);
            small(g, gs.name, px + 4, ry + 3, i == gunPick ? TEXT : DIM);
            small(g, gs.caliber.label, px + 152 - font.width(gs.caliber.label) * 2 / 3, ry + 3, FAINT);
        }
        small(g, guns.size() + " guns", px, y0 + GLIST_Y + GROWS * ROW + 3, FAINT);
        // The picked gun, big, with its numbers and cost
        panel(g, x0 + 34, y0 + 18, 204, 124);
        if (gunPick < 0 || gunPick >= guns.size()) return;
        GunSpec gs = guns.get(gunPick);
        var reg = dev.factoryascension.FactoryAscension.GUNS.get(gs.id);
        if (reg != null) bigItem(g, new ItemStack(reg.get()), x0 + 136, y0 + 60, 8);
        g.drawString(font, gs.name.toUpperCase(Locale.ROOT), x0 + 38, y0 + 21, TEXT, false);
        small(g, gs.kind.name() + "  " + gs.caliber.label, x0 + 38, y0 + 31, DIM);
        small(g, String.format("%s %d rpm   %.1f dmg%s   %d rds   %.0f m   %.1f kg", gs.auto ? "Auto" : gs.manual ? "Manual" : "Semi", gs.rpm,
            dev.factoryascension.gun.Guns.BASE_DAMAGE * gs.caliber.damage * gs.damage, gs.pellets > 1 ? " x" + gs.pellets : "", gs.mag, gs.range, gs.weight), x0 + 38, y0 + 106, DIM);
        String d = gs.desc;
        if (font.width(d) * 2 / 3 > 196) d = d.substring(0, Math.min(d.length(), 52)) + "...";
        small(g, d, x0 + 38, y0 + 114, FAINT);
        // Cost and BUILD
        int cy = y0 + 124;
        int cx = x0 + 38;
        boolean afford = true;
        var inv = Minecraft.getInstance().player.getInventory();
        for (String[] c : Bench.buildCost(gs)) {
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(c[0]));
            int need = Integer.parseInt(c[1]), have = 0;
            for (ItemStack st : inv.items) if (st.is(it)) have += st.getCount();
            boolean ok = have >= need || Minecraft.getInstance().player.isCreative();
            afford &= ok;
            g.renderItem(new ItemStack(it), cx, cy - 2);
            small(g, have + "/" + need, cx + 17, cy + 4, ok ? OK : BAD);
            cx += 44;
        }
        int bx = x0 + 186, by = y0 + 124, bw = 48;
        boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 14;
        g.fill(bx, by, bx + bw, by + 14, afford ? (hb ? 0xFF3C5A30 : 0xFF2C4424) : 0xFF2A2020);
        g.renderOutline(bx, by, bw, 14, afford ? OK : BAD);
        g.drawCenteredString(font, "BUILD", bx + bw / 2, by + 3, afford ? TEXT : FAINT);
    }

    private boolean clickGunsmith(double mx, double my) {
        int x0 = leftPos, y0 = topPos, px = x0 + LIST_X - 2;
        for (int i = 0; i < KINDS.length; i++) {
            int cx = px + i % 5 * 31, cy = y0 + 34 + i / 5 * 13;
            if (mx >= cx && mx < cx + 29 && my >= cy && my < cy + 12) { kindFilter = i; click(); return true; }
        }
        if (mx >= px && mx < px + 156 && my >= y0 + GLIST_Y && my < y0 + GLIST_Y + GROWS * ROW) {
            int i = gunScroll + (int) ((my - y0 - GLIST_Y) / ROW);
            if (i < guns.size()) { gunPick = i; click(); }
            return true;
        }
        int bx = x0 + 186, by = y0 + 124;
        if (gunPick >= 0 && gunPick < guns.size() && mx >= bx && mx < bx + 48 && my >= by && my < by + 14) {
            FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.BUILD, guns.get(gunPick).id, 0, 0, 0, 0));
            click();
            return true;
        }
        return false;
    }

    // ---- SKINS ----
    private void drawSkins(GuiGraphics g, int mx, int my) {
        int x0 = leftPos, y0 = topPos, px = x0 + LIST_X - 2;
        small(g, skins.size() + " SKINS", px, y0 + 20, DIM);
        panel(g, px, y0 + 29, 156, GROWS * ROW + 2);
        for (int r = 0; r < GROWS + 1; r++) {
            int i = skinScroll + r;
            if (i >= skins.size()) break;
            var info = dev.factoryascension.gun.Skins.ALL.get(skins.get(i));
            int ry = y0 + 30 + r * ROW;
            if (ry + ROW > y0 + 29 + GROWS * ROW + 2) break;
            if (i == skinPick) g.fill(px + 1, ry, px + 155, ry + ROW, 0xFF2E2E2A);
            GunSkins.Skin sk = GunSkins.byId(info.id());
            g.fill(px + 4, ry + 3, px + 9, ry + 8, 0xFF000000 | (sk != null ? sk.iconTint : 0x808080));
            small(g, info.name(), px + 12, ry + 3, i == skinPick ? TEXT : DIM);
        }
        panel(g, x0 + 34, y0 + 18, 204, 124);
        ItemStack gun = menu.gun();
        if (gun.isEmpty()) {
            g.drawCenteredString(font, "Put a gun in the GUN slot", x0 + 136, y0 + 70, FAINT);
        } else if (skinPick >= 0 && skinPick < skins.size()) {
            ItemStack shown = gun.copy();
            dev.factoryascension.gun.Skins.set(shown, skins.get(skinPick));
            bigItem(g, shown, x0 + 136, y0 + 62, 8);
        }
        if (skinPick >= 0 && skinPick < skins.size()) {
            var info = dev.factoryascension.gun.Skins.ALL.get(skins.get(skinPick));
            g.drawString(font, info.name().toUpperCase(Locale.ROOT), x0 + 38, y0 + 21, TEXT, false);
            small(g, info.blurb(), x0 + 38, y0 + 31, DIM);
            String cur = gun.isEmpty() ? "" : dev.factoryascension.gun.Skins.of(gun);
            small(g, cur.isEmpty() ? "No skin on it now" : "Wearing: " + dev.factoryascension.gun.Skins.ALL.get(cur).name(), x0 + 38, y0 + 112, FAINT);
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(dev.factoryascension.FAConfig.skinItem));
            if (dev.factoryascension.FAConfig.skinCost > 0) {
                g.renderItem(new ItemStack(it), x0 + 38, y0 + 122);
                small(g, "x" + dev.factoryascension.FAConfig.skinCost, x0 + 55, y0 + 128, GOLD);
            }
            button(g, x0 + 132, y0 + 124, 50, "APPLY", !gun.isEmpty(), mx, my);
            button(g, x0 + 186, y0 + 124, 48, "REMOVE", !gun.isEmpty() && !cur.isEmpty(), mx, my);
        }
    }

    private void button(GuiGraphics g, int bx, int by, int bw, String text, boolean ok, int mx, int my) {
        boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 14;
        g.fill(bx, by, bx + bw, by + 14, ok ? (hb ? 0xFF3C5A30 : 0xFF2C4424) : 0xFF2A2020);
        g.renderOutline(bx, by, bw, 14, ok ? OK : EDGE);
        g.drawCenteredString(font, text, bx + bw / 2, by + 3, ok ? TEXT : FAINT);
    }

    private boolean clickSkins(double mx, double my) {
        int x0 = leftPos, y0 = topPos, px = x0 + LIST_X - 2;
        if (mx >= px && mx < px + 156 && my >= y0 + 30 && my < y0 + 30 + GROWS * ROW) {
            int i = skinScroll + (int) ((my - y0 - 30) / ROW);
            if (i < skins.size()) { skinPick = i; click(); }
            return true;
        }
        if (skinPick >= 0 && skinPick < skins.size() && my >= y0 + 124 && my < y0 + 138) {
            if (mx >= x0 + 132 && mx < x0 + 182) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.SKIN, skins.get(skinPick), 0, 0, 0, 0));
                click();
                return true;
            }
            if (mx >= x0 + 186 && mx < x0 + 234) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.SKIN, "", 0, 0, 0, 0));
                click();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }

    // ---- Input ----
    private int listRow(double mx, double my) {
        double x = mx - leftPos - LIST_X, y = my - topPos - LIST_Y;
        if (x < 0 || x >= 152 || y < 0 || y >= LIST_ROWS * ROW) return -1;
        int i = scroll + (int) (y / ROW);
        return i < shown.size() ? i : -1;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.3f, 0.3f));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x0 = leftPos, y0 = topPos;
        // Tabs
        if (my >= y0 && my < y0 + 14) {
            int tx = x0 + 4;
            String[] tabs = TABS;
            for (int i = 0; i < tabs.length; i++) {
                int w = font.width(tabs[i]) + 12;
                if (mx >= tx && mx < tx + w) { tab = i; click(); return true; }
                tx += w + 2;
            }
        }
        if (tab == 0) {
            Attachments.Slot[] slots = Attachments.Slot.values();
            for (int i = 0; i < slots.length; i++) {
                int sx = x0 + 34 + i % 6 * 34, sy = y0 + 103 + i / 6 * 21;
                if (mx >= sx && mx < sx + 32 && my >= sy && my < sy + 19) {
                    if (button == 1) {
                        // Take the last one in this slot off
                        List<String> ids = GunStats.ids(menu.gun());
                        for (int k = ids.size() - 1; k >= 0; k--) {
                            Attachments.Att a = Attachments.BY_ID.get(ids.get(k));
                            if (a != null && a.slot == slots[i]) {
                                FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.REMOVE, "", k, 0, 0, 0));
                                break;
                            }
                        }
                    } else slot = slots[i];
                    click();
                    return true;
                }
            }
            int row = listRow(mx, my);
            if (row >= 0) { picked = row; click(); return true; }
            int iy = y0 + LIST_Y + LIST_ROWS * ROW + 6, bx = x0 + LIST_X + 92, by = iy + 26;
            if (picked >= 0 && picked < shown.size() && mx >= bx && mx < bx + 60 && my >= by && my < by + 14) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.INSTALL, shown.get(picked).id, 0, 0, 0, 0));
                click();
                return true;
            }
        } else if (tab == 2) {
            if (clickGunsmith(mx, my)) return true;
        } else if (tab == 3) {
            if (clickSkins(mx, my)) return true;
        } else {
            int px = x0 + LIST_X - 2;
            for (int i = 0; i < Caliber.values().length; i++) {
                int cx = px + i % 4 * 39, cy = y0 + 25 + i / 4 * 13;
                if (mx >= cx && mx < cx + 37 && my >= cy && my < cy + 12) { cal = i; click(); return true; }
            }
            for (int i = 0; i < AmmoType.values().length; i++) {
                int cx = px + i % 4 * 39, cy = y0 + 99 + i / 4 * 13;
                if (mx >= cx && mx < cx + 37 && my >= cy && my < cy + 12) { type = i; click(); return true; }
            }
            for (int i = 0; i < SWATCH.length; i++) {
                int sx = px + i % 8 * 19, sy = y0 + 174 + i / 8 * 14;
                if (mx >= sx && mx < sx + 17 && my >= sy && my < sy + 12) { colour = i; click(); return true; }
            }
            int bx = x0 + 128, by = y0 + 120;
            if (mx >= bx && mx < bx + 100 && my >= by && my < by + 16) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.BenchAction(FANetwork.BenchAction.PRESS, "", 0, cal, type, SWATCH[colour]));
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if ((tab == 2 || tab == 3) && mx >= leftPos + LIST_X && mx < leftPos + LIST_X + 156) {
            if (tab == 2) gunScroll = Math.max(0, Math.min(Math.max(0, guns.size() - GROWS), gunScroll - (int) Math.signum(delta) * 2));
            else skinScroll = Math.max(0, Math.min(Math.max(0, skins.size() - GROWS), skinScroll - (int) Math.signum(delta) * 2));
            return true;
        }
        if (tab == 0 && listRow(mx, my) >= -1 && mx >= leftPos + LIST_X && mx < leftPos + LIST_X + 156) {
            scroll = Math.max(0, Math.min(Math.max(0, shown.size() - LIST_ROWS), scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search != null && search.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** For the dev screenshots. */
    public void devTab(int t, Attachments.Slot s) {
        tab = t;
        if (s != null) slot = s;
    }

    /** For the dev screenshots: pick a gun (gunsmith) or a skin by id. */
    public void devPick(String id) {
        refreshGuns();
        for (int i = 0; i < guns.size(); i++) if (guns.get(i).id.equals(id)) { gunPick = i; gunScroll = Math.max(0, i - 3); }
        int k = skins.indexOf(id);
        if (k >= 0) { skinPick = k; skinScroll = Math.max(0, k - 3); }
    }
}
