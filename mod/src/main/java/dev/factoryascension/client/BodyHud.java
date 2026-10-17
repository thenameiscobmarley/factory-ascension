package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
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
        int h = event.getWindow().getGuiScaledHeight();
        int x = 6, y = h - 52;
        g.fill(x - 3, y - 3, x + 70, y + 37, 0x90000000);
        for (int i = 0; i < 7; i++) {
            int[] r = RECT[i];
            float k = state[i] / MAX[i];
            g.fill(x + r[0], y + r[1], x + r[0] + r[2], y + r[1] + r[3], colour(k));
            if (state[7 + i] > 0) g.renderOutline(x + r[0] - 1, y + r[1] - 1, r[2] + 2, r[3] + 2, 0xFFFFFFFF);
        }
        // Water and stamina bars
        int bx = x + 24, bw = 40;
        float water = state[14] / 100f;
        g.drawString(mc.font, "H2O", bx, y + 2, 0xFF7FB4E6, false);
        g.fill(bx, y + 12, bx + bw, y + 15, 0xFF202020);
        g.fill(bx, y + 12, bx + Math.round(bw * water), y + 15, water < 0.2f ? 0xFFC8452E : 0xFF4E8EC8);
        g.drawString(mc.font, "STA", bx, y + 19, 0xFFE0C060, false);
        g.fill(bx, y + 29, bx + bw, y + 32, 0xFF202020);
        float st = Stamina.value / 100f;
        g.fill(bx, y + 29, bx + Math.round(bw * st), y + 32, Stamina.exhausted ? 0xFFC8452E : 0xFFC9A55A);
    }
}
