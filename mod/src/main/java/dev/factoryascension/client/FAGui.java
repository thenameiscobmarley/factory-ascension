package dev.factoryascension.client;

import dev.factoryascension.client.ui.UiKit;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Drawing for the mod's older machine screens (vault, forge, smelter...), now just the shared dark look (ui/UiKit):
 * near-black panels, thin grey edges, sunken cells and steel accents. The old names stay so those screens needn't change.
 */
public final class FAGui {
    public static final int STEEL = 0xFF1A1A19, STEEL_DARK = 0xFF0B0B0B, STEEL_LIGHT = UiKit.EDGE;
    /** Highlights and progress (formerly gold): steel. */
    public static final int GOLD = UiKit.ACCENT, GOLD_DARK = UiKit.ACCENT_DIM, TEAL = 0xFF6FB0C0, VIOLET = 0xFF9C8CC8;
    public static final int TEXT = UiKit.TEXT, TEXT_DIM = UiKit.DIM;

    private FAGui() {
    }

    /** The shared panel, with a hairline under the title row. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        UiKit.panel(g, x, y, w, h);
        g.fill(x + 6, y + 15, x + w - 6, y + 16, UiKit.EDGE_SOFT);
    }

    /** A sunken slot background (the slot's item sits at x, y). */
    public static void slot(GuiGraphics g, int x, int y) {
        UiKit.cell(g, x - 1, y - 1);
    }

    /** A grid of slot backgrounds. */
    public static void slots(GuiGraphics g, int x, int y, int cols, int rows) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) slot(g, x + c * 18, y + r * 18);
    }

    /** A slim horizontal bar, filled `frac` of the way in `color`. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, UiKit.EDGE_SOFT);
        g.fill(x, y, x + w, y + h, 0xFF0A0A0A);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, frac)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, color);
            g.fill(x, y, x + fw, y + 1, 0x40FFFFFF);
        }
    }
}
