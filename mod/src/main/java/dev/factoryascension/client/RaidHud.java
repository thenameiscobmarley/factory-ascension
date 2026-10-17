package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
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
            int col = ended.equals("extracted") ? 0xFF6FB04A : 0xFFC8452E;
            g.pose().pushPose();
            g.pose().translate(w / 2f, 40, 0);
            g.pose().scale(2, 2, 1);
            g.drawCenteredString(mc.font, t, 0, 0, col);
            g.pose().popPose();
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
        int cx = w / 2, y = 6;
        g.fill(cx - 90, y - 3, cx + 90, y + 24, 0x90000000);
        g.fill(cx - 90, y - 3, cx - 88, y + 24, dev.factoryascension.client.tarkov.TarkovScreen.ACCENT);
        g.drawString(mc.font, "RAID " + time, cx - 82, y + 1, secs < 300 ? 0xFFC8452E : 0xFFF2EFE6, false);
        String where = names.length > best ? names[best] : "Extraction";
        g.drawString(mc.font, where + "  " + Math.round(bd) + "m", cx - 82, y + 12, 0xFF8FD46A, false);
        // The arrow
        g.pose().pushPose();
        g.pose().translate(cx + 72, y + 10, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rel));
        g.fill(-1, -8, 1, 6, 0xFF8FD46A);
        g.fill(-4, -5, 4, -3, 0xFF8FD46A);
        g.fill(-2, -7, 2, -5, 0xFF8FD46A);
        g.pose().popPose();
        if (hold > 0) g.drawCenteredString(mc.font, "EXTRACTING", cx, y + 30, 0xFF8FD46A);
    }
}
