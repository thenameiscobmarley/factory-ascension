package dev.factoryascension.client.gun;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Looking through a scope (sniper and marksman rifles, the AUG's optic): once you're fully aimed the gun gives way to
 * the scope's view: a round sight picture with a mil-dot reticle, black around it, and it sways with your breathing
 * and shakes with stress like the gun does.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ScopeOverlay {
    private ScopeOverlay() {
    }

    static boolean scoped(LocalPlayer p) {
        if (p == null || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return false;
        GunSpec g = GunItem.spec(p.getMainHandItem());
        if (g == null) return false;
        dev.factoryascension.gun.GunStats st = dev.factoryascension.gun.GunStats.of(p.getMainHandItem());
        boolean built = (g.kind == GunSpec.Kind.SNIPER || g.kind == GunSpec.Kind.DMR || g.id.equals("aug")) && !g.id.equals("mosin") && !g.id.equals("kar98k");
        if (!built && st.zoom < 0.45f) return false;   // a 4x or stronger optic attachment counts too
        return p.isUsingItem() && p.getUseItem() == p.getMainHandItem() && p.getTicksUsingItem() >= 5 && !GunItem.reloading(p.getMainHandItem());
    }

    /** The gun itself is hidden while you look through it. */
    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (scoped(Minecraft.getInstance().player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!scoped(mc.player)) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        float t = (net.minecraft.Util.getMillis() % 100000) / 1000f;
        float sway = 1.5f + GunAnim.stress * 4;
        int cx = w / 2 + Math.round((float) Math.sin(t * 1.3) * sway), cy = h / 2 + Math.round((float) Math.sin(t * 0.9 + 1) * sway);
        int r = (int) (Math.min(w, h) * 0.46);
        // Black outside the circle (row by row), then a dark ring at its edge
        for (int y = 0; y < h; y++) {
            int dy = y - cy;
            if (Math.abs(dy) >= r) { g.fill(0, y, w, y + 1, 0xFF000000); continue; }
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(0, y, cx - half, y + 1, 0xFF000000);
            g.fill(cx + half, y, w, y + 1, 0xFF000000);
            int ring = Math.max(1, r / 22);
            g.fill(cx - half, y, Math.min(cx + half, cx - half + ring), y + 1, 0xC0000000);
            g.fill(Math.max(cx - half, cx + half - ring), y, cx + half, y + 1, 0xC0000000);
        }
        // Reticle: thick outer posts, fine centre cross, mil dots
        int line = 0xF0101010;
        g.fill(cx - r, cy - 1, cx - r / 4, cy + 1, line);
        g.fill(cx + r / 4, cy - 1, cx + r, cy + 1, line);
        g.fill(cx - 1, cy + r / 4, cx + 1, cy + r, line);
        g.fill(cx - 1, cy - r, cx + 1, cy - r / 4, line);
        g.fill(cx - r / 4, cy, cx + r / 4, cy + 1, line);
        g.fill(cx, cy - r / 4, cx + 1, cy + r / 4, line);
        for (int k = 1; k <= 4; k++) {
            int d = k * r / 18;
            g.fill(cx + d - 1, cy - 1, cx + d + 1, cy + 2, line);
            g.fill(cx - d - 1, cy - 1, cx - d + 1, cy + 2, line);
            g.fill(cx - 1, cy + d - 1, cx + 2, cy + d + 1, line);
            g.fill(cx - 1, cy - d - 1, cx + 2, cy - d + 1, line);
        }
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFE03020);   // a red centre dot
    }
}
