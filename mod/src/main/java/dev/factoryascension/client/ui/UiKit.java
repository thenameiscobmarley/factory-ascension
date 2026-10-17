package dev.factoryascension.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The one look for every Factory Ascension screen (the black Tarkov style), so they all line up:
 *  - a 4 px grid: every gap, padding and size is a multiple of GRID;
 *  - three text sizes: TITLE (bold, upper case), BODY and CAPTION (dim), all the game's font at 1x, never scaled;
 *  - one surface (near-black, thin grey edge, a lit top line), one button, one tab, one cell;
 *  - accents are cool steel, never amber, and never a coloured strip down the left side (the user dislikes both).
 * Colours are the Tarkov screen's, in one place.
 */
public final class UiKit {
    private UiKit() {
    }

    // ---- Spacing (4 px grid) ----
    public static final int GRID = 4, GAP = 4, PAD = 8, PAD_L = 12, ROW = 20, BUTTON_H = 16, HEADER_H = 24, LINE = 11;

    // ---- Colours ----
    public static final int SCRIM_TOP = 0xE8070808, SCRIM_BOTTOM = 0xE8060606;
    public static final int SURFACE_TOP = 0xF41C1C1B, SURFACE_BOTTOM = 0xF40B0B0B, SURFACE_RAISED = 0xFF181817, SURFACE_SUNK = 0xFF0B0B0B;
    public static final int EDGE = 0xFF3A3A38, EDGE_SOFT = 0xFF262626, EDGE_LIGHT = 0xFF5A5A56, HILITE = 0x22FFFFFF, SHADOW = 0x70000000;
    public static final int ACCENT = 0xFFB4BEC8, ACCENT_DIM = 0xFF5C646C, ACCENT_GLOW = 0x30B4BEC8;
    public static final int TEXT = 0xFFD6D4CC, DIM = 0xFF8C8A84, FAINT = 0xFF56544F, BAD = 0xFFC8452E, OK = 0xFF6FB04A, INFO = 0xFF5A9BC9;

    /** Darkens the whole window behind a screen (top a little lighter, like a vignette). */
    public static void scrim(GuiGraphics g, int w, int h) {
        g.fillGradient(0, 0, w, h / 2, SCRIM_TOP, 0xB80E0F0F);
        g.fillGradient(0, h / 2, w, h, 0xB80E0F0F, SCRIM_BOTTOM);
    }

    /** The surface every panel uses: soft drop shadow, shaded fill, thin edge, lit top line and an amber tick. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y + 1, x + w + 2, y + h + 3, 0x40000000);
        g.fill(x, y + 2, x + w + 1, y + h + 2, SHADOW);
        g.fillGradient(x, y, x + w, y + h, SURFACE_TOP, SURFACE_BOTTOM);
        g.renderOutline(x, y, w, h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, HILITE);
    }

    /** A HUD card (over the game, so see-through): shaded fill, faint edge, lit top line. */
    public static void hudCard(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xC81C1C1B, 0xC80B0B0B);
        g.renderOutline(x, y, w, h, 0x803A3A38);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, HILITE);
    }

    /** A panel's header: the title (bold, upper case), an optional caption on the right, and a rule under it. */
    public static void header(GuiGraphics g, Font font, int x, int y, int w, String title, String caption) {
        Component t = Component.literal(title.toUpperCase()).withStyle(net.minecraft.ChatFormatting.BOLD);
        g.drawString(font, t, x + PAD_L, y + PAD, TEXT, false);
        // The caption only where it fits beside the title
        if (caption != null && !caption.isEmpty() && font.width(t) + font.width(caption) + PAD_L * 2 + PAD < w)
            g.drawString(font, caption, x + w - PAD_L - font.width(caption), y + PAD, DIM, false);
        g.fill(x + PAD, y + HEADER_H - 1, x + w - PAD, y + HEADER_H, EDGE_SOFT);
    }

    /** A section label inside a panel: small, dim, upper case, with a hairline to the right. */
    public static void section(GuiGraphics g, Font font, int x, int y, int w, String label) {
        String s = label.toUpperCase();
        g.drawString(font, s, x, y, FAINT, false);
        int lx = x + font.width(s) + GAP * 2;
        if (lx < x + w) g.fill(lx, y + 4, x + w, y + 5, EDGE_SOFT);
    }

    /** A sunken box (lists, readouts, slots' surroundings). */
    public static void well(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE_SOFT);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, SURFACE_SUNK, 0xFF121211);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x60000000);
    }

    /** An item cell (18 x 18 around a slot whose item sits at x + 1, y + 1). */
    public static void cell(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, EDGE_SOFT);
        g.fillGradient(x + 1, y + 1, x + 17, y + 17, 0xFF0B0B0B, 0xFF151514);
        g.fill(x + 1, y + 1, x + 17, y + 2, 0x60000000);
    }

    public enum Tone { NORMAL, PRIMARY, DANGER }

    /** The one button: flat dark, lit edge on hover, amber for the main action, red for destructive ones. */
    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean hover, boolean enabled, Tone tone) {
        int edge = !enabled ? EDGE_SOFT : tone == Tone.PRIMARY ? ACCENT_DIM : tone == Tone.DANGER ? 0xFF6A2A20 : EDGE;
        if (enabled && hover) edge = tone == Tone.DANGER ? BAD : ACCENT;
        g.fill(x, y, x + w, y + h, edge);
        int top = tone == Tone.PRIMARY && enabled ? 0xFF2A2D30 : 0xFF1E1E1D, bottom = tone == Tone.PRIMARY && enabled ? 0xFF181A1C : 0xFF111110;
        if (enabled && hover) { top = 0xFF2C2D2E; bottom = 0xFF1A1A1B; }
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
        if (enabled) g.fill(x + 1, y + 1, x + w - 1, y + 2, HILITE);
        int color = !enabled ? FAINT : tone == Tone.PRIMARY ? ACCENT : tone == Tone.DANGER ? 0xFFE07A66 : hover ? 0xFFFFFFFF : TEXT;
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, color, false);
    }

    /** How wide a button needs to be for its label (padded, rounded up to the grid). */
    public static int buttonWidth(Font font, String label) {
        int w = font.width(label) + PAD_L * 2 - 4;
        return (w + GRID - 1) / GRID * GRID;
    }

    /** A navigation tab (a row in a side list): amber bar and lighter fill when selected. */
    public static void tab(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean selected, boolean hover) {
        if (selected) {
            g.fillGradient(x, y, x + w, y + h, 0xFF262728, 0xFF1A1B1C);
            g.fill(x, y + h - 1, x + w, y + h, EDGE);
        } else if (hover) {
            g.fill(x, y, x + w, y + h, 0xFF1A1A19);
        }
        g.drawString(font, label, x + PAD_L, y + (h - 8) / 2 + 1, selected ? TEXT : hover ? 0xFFBDBBB4 : DIM, false);
    }

    /** A thin vertical scrollbar: `first` and `shown` of `total` rows. */
    public static void scrollbar(GuiGraphics g, int x, int y, int h, int first, int shown, int total) {
        if (total <= shown) return;
        g.fill(x, y, x + 2, y + h, EDGE_SOFT);
        int bh = Math.max(12, h * shown / total), by = y + (h - bh) * first / Math.max(1, total - shown);
        g.fill(x, by, x + 2, by + bh, EDGE_LIGHT);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
