package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The /ascension screen. The pages are the pack's own /ascension replies (KubeJS: the Essence bank and your gear's
 * enchants, danger, milestones...): while this is open, those chat lines come here instead, and every clickable
 * "[+1]" / "[Max]" / "[Extract]" in them becomes a real button. So whatever the pack prints, this shows, and the chat
 * menu still works without the mod. Typing /ascension opens it (network/FANetwork.OpenMenu). If drawing ever fails,
 * it closes and the menu goes back to chat.
 */
public class AscensionScreen extends Screen {
    record Tab(String label, String command) {
    }

    static final Tab[] TABS = {
        new Tab("Gear & Essence", "ascension menu"), new Tab("Danger", "ascension danger"), new Tab("Milestones", "ascension milestones"),
        new Tab("Legendary", "ascension legendary"), new Tab("Loot", "ascension loot"), new Tab("World event", "ascension event")};
    static final int NAV_W = 104, MAX_W = 460, MAX_H = 320;

    record Btn(String label, String command, Component hover) {
    }

    /** One line the server sent: its text (colours kept, links taken out) and its buttons. */
    static final class Row {
        MutableComponent text = Component.empty();
        final List<Btn> buttons = new ArrayList<>();
        // Laid out for the current width
        List<FormattedCharSequence> lines = List.of();
        boolean buttonsBelow;
        int height;
    }

    private static AscensionScreen current;
    private final List<Row> rows = new ArrayList<>();
    private final List<Row> incoming = new ArrayList<>();
    private int tab = 0, scroll = 0, contentH = 0, quiet = -1, layoutW = -1;
    /** A button was pressed: its reply is a short notice (then the page is fetched again) unless it's a whole page. */
    private boolean actionPending;
    private Component notice;
    private long noticeUntil, lastLine;
    private int px, py, pw, ph;

    public AscensionScreen() {
        super(Component.literal("Ascension"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new AscensionScreen());
    }

    @Override
    protected void init() {
        current = this;
        pw = Math.min(width - UiKit.PAD_L * 2, MAX_W);
        ph = Math.min(height - UiKit.PAD_L * 2, MAX_H);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        layoutW = -1;
        if (rows.isEmpty() && quiet < 0) send(TABS[tab].command, false);
    }

    @Override
    public void removed() {
        if (current == this) current = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Talking to the server ----

    private void send(String command, boolean action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        incoming.clear();
        actionPending = action;
        quiet = 0;
        lastLine = net.minecraft.Util.getMillis();
        mc.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    /** A system chat line while open: it's part of the reply. */
    private void receive(Component msg) {
        incoming.add(parse(msg));
        quiet = 0;
        lastLine = net.minecraft.Util.getMillis();
    }

    /** Replies come in a burst; 200 ms after the last line, it's complete (checked every frame). */
    private void tickReplies() {
        long now = net.minecraft.Util.getMillis();
        if (notice != null && now > noticeUntil) notice = null;
        if (quiet < 0) return;
        long since = now - lastLine;
        if (incoming.isEmpty()) { if (since > 3000) quiet = -1; return; }   // no answer: stop waiting
        if (since < 200) return;
        quiet = -1;
        if (actionPending && incoming.size() <= 2) {
            // "Not enough Essence", "Upgraded!"...: a notice on top, and the page again, fresh
            MutableComponent n = Component.empty();
            for (int i = 0; i < incoming.size(); i++) n.append(i == 0 ? Component.empty() : Component.literal("  ")).append(incoming.get(i).text);
            notice = n;
            noticeUntil = net.minecraft.Util.getMillis() + 5000;
            send(TABS[tab].command, false);
            return;
        }
        rows.clear();
        rows.addAll(incoming);
        incoming.clear();
        actionPending = false;
        layoutW = -1;
        scroll = Math.min(scroll, Math.max(0, contentH));
    }

    public String devState() {
        return "rows " + rows.size() + ", incoming " + incoming.size() + ", quiet " + quiet + ", current " + (current == this);
    }

    /** For tests (no pack scripts in the dev client): take these lines as the server's reply. */
    public void devFeed(List<Component> lines) {
        incoming.clear();
        for (Component c : lines) incoming.add(parse(c));
        actionPending = false;
        quiet = 0;
        lastLine = net.minecraft.Util.getMillis();
    }

    static Row parse(Component msg) {
        Row row = new Row();
        msg.visit((style, str) -> {
            ClickEvent ce = style.getClickEvent();
            if (ce != null && (ce.getAction() == ClickEvent.Action.RUN_COMMAND || ce.getAction() == ClickEvent.Action.SUGGEST_COMMAND)) {
                String label = str.replace("[", "").replace("]", "").trim();
                Btn last = row.buttons.isEmpty() ? null : row.buttons.get(row.buttons.size() - 1);
                HoverEvent he = style.getHoverEvent();
                Component hover = he != null ? he.getValue(HoverEvent.Action.SHOW_TEXT) : null;
                if (last != null && last.command.equals(ce.getValue())) row.buttons.set(row.buttons.size() - 1, new Btn(last.label + label, last.command, last.hover));
                else if (!label.isEmpty()) row.buttons.add(new Btn(label, ce.getValue(), hover));
            } else {
                row.text.append(Component.literal(str).withStyle(style.withClickEvent(null).withHoverEvent(null)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return row;
    }

    // ---- Layout ----

    private int contentX() { return px + NAV_W + UiKit.PAD_L; }
    private int contentY() { return py + UiKit.HEADER_H + UiKit.PAD + (notice != null ? 20 : 0); }
    private int contentW() { return pw - NAV_W - UiKit.PAD_L * 2 - 4; }
    private int contentH() { return py + ph - UiKit.PAD - contentY(); }

    private int buttonsWidth(Row r) {
        int w = 0;
        for (Btn b : r.buttons) w += UiKit.buttonWidth(font, b.label) + UiKit.GAP;
        return Math.max(0, w - UiKit.GAP);
    }

    private void layout() {
        int cw = contentW();
        if (cw == layoutW) return;
        layoutW = cw;
        contentH = 0;
        for (Row r : rows) {
            int bw = buttonsWidth(r);
            boolean blank = r.text.getString().isBlank();
            int textW = cw - (bw > 0 ? bw + UiKit.PAD : 0);
            r.buttonsBelow = bw > 0 && !blank && (font.width(r.text) > textW || textW < cw / 3);
            r.lines = blank ? List.of() : font.split(r.text, r.buttonsBelow || bw == 0 ? cw : textW);
            int textH = r.lines.size() * UiKit.LINE;
            r.height = r.buttonsBelow ? textH + UiKit.GAP + UiKit.BUTTON_H : Math.max(textH, bw > 0 ? UiKit.BUTTON_H : 0);
            r.height += bw > 0 ? UiKit.GAP * 2 : 2;
            contentH += r.height;
        }
    }

    // ---- Drawing ----

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        try {
            draw(g, mx, my);
        } catch (Throwable err) {
            com.mojang.logging.LogUtils.getLogger().error("Ascension screen failed; back to the chat menu", err);
            onClose();
            Minecraft.getInstance().player.connection.sendCommand("ascension menu");
        }
    }

    private void draw(GuiGraphics g, int mx, int my) {
        tickReplies();
        renderBackground(g);
        UiKit.scrim(g, width, height);
        UiKit.panel(g, px, py, pw, ph);
        UiKit.header(g, font, px, py, pw, "Ascension", "Esc to close");
        // Navigation
        int ny = py + UiKit.HEADER_H + UiKit.PAD;
        UiKit.section(g, font, px + UiKit.PAD_L, ny, NAV_W - UiKit.PAD_L - UiKit.PAD, "Pages");
        ny += UiKit.LINE + UiKit.GAP;
        for (int i = 0; i < TABS.length; i++) {
            int ty = ny + i * UiKit.ROW;
            UiKit.tab(g, font, px + 1, ty, NAV_W - UiKit.PAD, UiKit.ROW, TABS[i].label, i == tab, UiKit.inside(mx, my, px + 1, ty, NAV_W - UiKit.PAD, UiKit.ROW));
        }
        g.fill(px + NAV_W, py + UiKit.HEADER_H, px + NAV_W + 1, py + ph - 1, UiKit.EDGE_SOFT);
        // A notice from the last button
        if (notice != null) {
            int nx = contentX(), nyy = py + UiKit.HEADER_H + UiKit.PAD;
            UiKit.well(g, nx, nyy, contentW() + 4, 16);
            g.drawString(font, font.substrByWidth(notice, contentW() - UiKit.PAD).getString(), nx + UiKit.PAD, nyy + 5, UiKit.TEXT, false);
        }
        // The page
        layout();
        int cx = contentX(), cy = contentY(), cw = contentW(), ch = contentH();
        scroll = Math.max(0, Math.min(scroll, contentH - ch));
        Btn hovered = null;
        g.enableScissor(cx - 2, cy, cx + cw + 6, cy + ch);
        if (rows.isEmpty()) {
            String s = quiet >= 0 ? "Asking the server…" : "Nothing here yet.";
            g.drawString(font, s, cx, cy + UiKit.PAD, UiKit.DIM, false);
        }
        int y = cy - scroll;
        for (Row r : rows) {
            if (y + r.height >= cy && y <= cy + ch) {
                boolean rowHover = !r.buttons.isEmpty() && UiKit.inside(mx, my, cx - 2, y, cw + 4, r.height) && my >= cy && my < cy + ch;
                if (rowHover) g.fill(cx - 2, y, cx + cw + 2, y + r.height, 0x10FFFFFF);
                int ty = y + (r.buttons.isEmpty() ? 1 : UiKit.GAP) + (r.buttonsBelow || r.lines.size() > 1 || r.buttons.isEmpty() ? 0 : (UiKit.BUTTON_H - 8) / 2);
                for (FormattedCharSequence line : r.lines) {
                    g.drawString(font, line, cx, ty, UiKit.TEXT, false);
                    ty += UiKit.LINE;
                }
                int bx = r.buttonsBelow ? cx : cx + cw - buttonsWidth(r), by = r.buttonsBelow ? ty + UiKit.GAP - 2 : y + UiKit.GAP;
                for (Btn b : r.buttons) {
                    int bw = UiKit.buttonWidth(font, b.label);
                    boolean over = UiKit.inside(mx, my, bx, by, bw, UiKit.BUTTON_H) && my >= cy && my < cy + ch;
                    if (over) hovered = b;
                    UiKit.button(g, font, bx, by, bw, UiKit.BUTTON_H, b.label, over, quiet < 0, tone(b.label));
                    bx += bw + UiKit.GAP;
                }
                if (!r.buttons.isEmpty()) g.fill(cx, y + r.height - 1, cx + cw, y + r.height, 0x40262626);
            }
            y += r.height;
        }
        g.disableScissor();
        UiKit.scrollbar(g, cx + cw + 4, cy, ch, scroll, ch, contentH);
        if (hovered != null && hovered.hover != null) g.renderTooltip(font, font.split(hovered.hover, 220), mx, my);
    }

    static UiKit.Tone tone(String label) {
        String l = label.toLowerCase();
        if (l.contains("rebirth") || l.contains("confirm") || l.contains("reset")) return UiKit.Tone.DANGER;
        if (l.equals("max") || l.contains("awaken")) return UiKit.Tone.PRIMARY;
        return UiKit.Tone.NORMAL;
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int ny = py + UiKit.HEADER_H + UiKit.PAD + UiKit.LINE + UiKit.GAP;
        for (int i = 0; i < TABS.length; i++) {
            if (UiKit.inside(mx, my, px + 1, ny + i * UiKit.ROW, NAV_W - UiKit.PAD, UiKit.ROW)) {
                click();
                tab = i;
                scroll = 0;
                rows.clear();
                notice = null;
                send(TABS[i].command, false);
                return true;
            }
        }
        if (quiet >= 0) return true;   // waiting for a reply: no double clicks
        int cx = contentX(), cy = contentY(), cw = contentW(), ch = contentH();
        if (!UiKit.inside(mx, my, cx, cy, cw + 4, ch)) return super.mouseClicked(mx, my, button);
        int y = cy - scroll;
        for (Row r : rows) {
            int ty = y + (r.buttons.isEmpty() ? 1 : UiKit.GAP) + r.lines.size() * UiKit.LINE;
            int bx = r.buttonsBelow ? cx : cx + cw - buttonsWidth(r), by = r.buttonsBelow ? ty + UiKit.GAP - 2 : y + UiKit.GAP;
            for (Btn b : r.buttons) {
                int bw = UiKit.buttonWidth(font, b.label);
                if (UiKit.inside(mx, my, bx, by, bw, UiKit.BUTTON_H)) {
                    click();
                    send(b.command, true);
                    return true;
                }
                bx += bw + UiKit.GAP;
            }
            y += r.height;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= (int) (delta * UiKit.ROW);
        return true;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.6f));
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void chat(ClientChatReceivedEvent event) {
            AscensionScreen s = current;
            if (s == null || !event.isSystem() || Minecraft.getInstance().screen != s) return;
            if (s.quiet < 0 && s.incoming.isEmpty()) return;   // not waiting for anything: let it through
            s.receive(event.getMessage());
            event.setCanceled(true);
        }

    }
}
