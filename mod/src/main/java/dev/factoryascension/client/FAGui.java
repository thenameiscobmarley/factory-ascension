package dev.factoryascension.client;

import net.minecraft.client.gui.GuiGraphics;

/** Drawing for the mod's screens, in the pack's gear style: gunmetal panels, gold frames, teal glow, violet accents. */
public final class FAGui {
    public static final int STEEL = 0xFF3A3F4E, STEEL_DARK = 0xFF1C1F28, STEEL_LIGHT = 0xFF5B6274;
    public static final int GOLD = 0xFFE8B03A, GOLD_DARK = 0xFF8C5416, TEAL = 0xFF40E8D4, VIOLET = 0xFFAA60FF;
    public static final int TEXT = 0xFFE8ECF4, TEXT_DIM = 0xFF9AA3B5;

    private FAGui() {
    }

    /** A gunmetal panel with a beveled gold frame and a teal line under the title. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, GOLD_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GOLD);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, STEEL);
        g.fill(x + 2, y + 2, x + w - 2, y + 3, STEEL_LIGHT);
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, STEEL_DARK);
        g.fill(x + 6, y + 14, x + w - 6, y + 15, TEAL);
    }

    /** A sunken slot background (18 x 18, the slot's item sits 1 px in). */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, STEEL_DARK);
        g.fill(x, y, x + 17, y + 17, STEEL_LIGHT);
        g.fill(x, y, x + 16, y + 16, 0xFF262A35);
    }

    /** A grid of slot backgrounds. */
    public static void slots(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) slot(g, x + c * 18, y + r * 18);
    }

    /** A horizontal bar (progress, collection), filled `frac` of the way in `color`. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, STEEL_DARK);
        g.fill(x, y, x + w, y + h, 0xFF262A35);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, frac)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, color);
            g.fill(x, y, x + fw, y + 1, 0x60FFFFFF);
        }
    }
}
