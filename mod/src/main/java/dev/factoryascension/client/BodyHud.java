package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import dev.factoryascension.client.ui.UiKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Bottom left: your body, Tarkov style: a little figure whose parts go green to red as they're hurt and black when
 * destroyed (a white mark on a broken one), your water and your stamina. From body/Body (BodySync) and client/Stamina.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BodyHud {
    /** From the server: 7 part healths, 7 broken flags, water. */
    public static float[] state = null;
    static final float[] MAX = {35, 85, 70, 60, 60, 65, 65};
    // x, y, w, h of each part on the little figure (head, thorax, stomach, left arm, right arm, left leg, right leg)
    static final int[][] RECT = {{6, 0, 6, 6}, {4, 7, 10, 7}, {5, 15, 8, 5}, {0, 7, 3, 12}, {15, 7, 3, 12}, {4, 21, 4, 12}, {10, 21, 4, 12}};

    private BodyHud() {
    }

    static int colour(float k) {
        if (k <= 0) return 0xFF101010;
        int r = (int) (k < 0.5f ? 220 : 220 - (k - 0.5f) * 2 * 140), g = (int) (k < 0.5f ? 60 + k * 2 * 140 : 200);
        return 0xFF000000 | r << 16 | g << 8 | 50;
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || state == null || state.length < 15 || mc.player.isCreative() || mc.player.isSpectator()) return;
        GuiGraphics g = event.getGuiGraphics();
        var font = mc.font;
        int h = event.getWindow().getGuiScaledHeight();
        // A card on the 4 px grid: figure on the left, two labelled meters on the right
        int w = 112, ch = 44, x = UiKit.PAD, y = h - ch - UiKit.PAD;
        UiKit.hudCard(g, x, y, w, ch);
        // The figure: each part filled by its health, with a dark rim; a broken one gets a white rim
        int fx = x + UiKit.PAD, fy = y + 5;
        for (int i = 0; i < 7; i++) {
            int[] r = RECT[i];
            float k = state[i] / MAX[i];
            int px = fx + r[0], py = fy + r[1];
            g.fill(px, py, px + r[2], py + r[3], 0xFF0A0A0A);
            g.fill(px + 1, py + 1, px + r[2] - 1, py + r[3] - 1, colour(k));
            g.fill(px + 1, py + 1, px + r[2] - 1, py + 2, 0x30FFFFFF);
            if (state[7 + i] > 0) g.renderOutline(px - 1, py - 1, r[2] + 2, r[3] + 2, 0xFFE8E6DE);
        }
        g.fill(x + 30, y + UiKit.PAD, x + 31, y + ch - UiKit.PAD, UiKit.EDGE_SOFT);
        // Meters: label (small caps, dim), value on the right, a slim bar under
        int mx = x + 36, mw = w - 36 - UiKit.PAD;
        meter(g, font, mx, y + 6, mw, "WATER", state[14] / 100f, state[14] < 20 ? UiKit.BAD : UiKit.INFO);
        meter(g, font, mx, y + 25, mw, "STAMINA", Stamina.value / 100f, Stamina.exhausted ? UiKit.BAD : UiKit.ACCENT);
    }

    static void meter(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, String label, float frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        g.drawString(font, label, x, y, UiKit.DIM, false);
        String v = Math.round(frac * 100) + "";
        g.drawString(font, v, x + w - font.width(v), y, UiKit.TEXT, false);
        int by = y + 10;
        g.fill(x, by, x + w, by + 3, 0xFF0A0A0A);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            g.fill(x, by, x + fw, by + 3, color);
            g.fill(x, by, x + fw, by + 1, 0x40FFFFFF);
        }
    }
}
