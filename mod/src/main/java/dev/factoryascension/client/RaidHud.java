package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Top middle while on a raid: time left, and an arrow to the nearest extraction point with its name and distance. */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RaidHud {
    static int[] xz = new int[0];
    static String[] names = new String[0];
    static int left = 0, hold = 0;
    static long syncedAt = 0;
    static String ended = "";
    static long endedAt = 0;

    private RaidHud() {
    }

    public static void update(int[] p, String[] n, int l, int h, String e) {
        xz = p;
        names = n;
        left = l;
        hold = h;
        syncedAt = net.minecraft.Util.getMillis();
        if (!e.isEmpty()) {
            ended = e;
            endedAt = syncedAt;
        }
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        long now = net.minecraft.Util.getMillis();
        if (!ended.isEmpty() && now - endedAt < 6000) {
            String t = switch (ended) {
                case "extracted" -> "EXTRACTED";
                case "mia" -> "MISSING IN ACTION";
                default -> "KILLED IN ACTION";
            };
            int col = ended.equals("extracted") ? UiKit.OK : UiKit.BAD;
            int tw = mc.font.width(t) + UiKit.PAD_L * 2;
            UiKit.hudCard(g, w / 2 - tw / 2, 44, tw, 20);
            g.drawCenteredString(mc.font, t, w / 2, 50, col);
        }
        if (xz.length < 2) return;
        // Time left, ticking down between updates
        int ticks = Math.max(0, left - (int) ((now - syncedAt) / 50));
        int secs = ticks / 20;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        // Nearest point
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < xz.length / 2; i++) {
            double d = Math.hypot(p.getX() - xz[i * 2], p.getZ() - xz[i * 2 + 1]);
            if (d < bd) { bd = d; best = i; }
        }
        double ang = Math.toDegrees(Math.atan2(xz[best * 2 + 1] - p.getZ(), xz[best * 2] - p.getX())) - 90;
        float rel = Mth.wrapDegrees((float) ang - p.getYRot());
        int cx = w / 2, y = UiKit.PAD, cw = 184, ch = 32, x0 = cx - cw / 2;
        UiKit.hudCard(g, x0, y, cw, ch);
        g.drawString(mc.font, "RAID", x0 + UiKit.PAD_L, y + 6, UiKit.DIM, false);
        g.drawString(mc.font, time, x0 + UiKit.PAD_L + mc.font.width("RAID") + UiKit.GAP * 2, y + 6, secs < 300 ? UiKit.BAD : UiKit.TEXT, false);
        String where = names.length > best ? names[best] : "Extraction";
        g.drawString(mc.font, mc.font.plainSubstrByWidth(where, cw - 60), x0 + UiKit.PAD_L, y + 18, UiKit.OK, false);
        String dist = Math.round(bd) + "m";
        g.drawString(mc.font, dist, x0 + cw - 28 - mc.font.width(dist), y + 18, UiKit.DIM, false);
        // The arrow to the nearest extraction
        g.fill(x0 + cw - 24, y + UiKit.GAP, x0 + cw - 23, y + ch - UiKit.GAP, UiKit.EDGE_SOFT);
        g.pose().pushPose();
        g.pose().translate(x0 + cw - 12, y + ch / 2f, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rel));
        g.fill(-1, -7, 1, 6, UiKit.OK);
        g.fill(-4, -4, 4, -2, UiKit.OK);
        g.fill(-2, -6, 2, -4, UiKit.OK);
        g.pose().popPose();
        if (hold > 0) g.drawCenteredString(mc.font, "EXTRACTING", cx, y + ch + UiKit.GAP * 2, UiKit.OK);
    }
}
