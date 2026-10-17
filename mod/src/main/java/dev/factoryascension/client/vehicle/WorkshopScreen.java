package dev.factoryascension.client.vehicle;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.network.FANetwork;
import dev.factoryascension.vehicle.BikePartItem;
import dev.factoryascension.vehicle.BikeParts;
import dev.factoryascension.vehicle.DirtBike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The bike workshop (a wrench on a dirt bike): the bike turning slowly on the left; on the right what's fitted and how
 * it rides, and below, every part you carry: hover one to see what it would change, click to fit it.
 */
public class WorkshopScreen extends Screen {
    final DirtBike bike;
    private float spin = 0;
    private final List<int[]> rows = new ArrayList<>();   // x, y, w, h, inventory slot

    public WorkshopScreen(DirtBike bike) {
        super(Component.literal("Workshop"));
        this.bike = bike;
    }

    public static void open(DirtBike b) {
        Minecraft.getInstance().setScreen(new WorkshopScreen(b));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (bike.isRemoved() || minecraft.player == null || bike.distanceToSqr(minecraft.player) > 49) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = Math.min(width - 32, 460), h = Math.min(height - 32, 260);
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        TarkovScreen.shadowBox(g, x0, y0, w, h);
        g.fillGradient(x0, y0, x0 + w, y0 + h, 0xF01E1E1D, 0xF0101010);
        g.fill(x0, y0, x0 + w, y0 + 1, 0x30FFFFFF);
        g.drawString(font, TarkovScreen.uiBold("WORKSHOP"), x0 + 12, y0 + 10, 0xFFF2EFE6, false);
        g.drawString(font, TarkovScreen.ui("Dirt bike"), x0 + 12 + font.width(TarkovScreen.uiBold("WORKSHOP")) + 8, y0 + 10, 0xFF8C8A84, false);
        // The bike
        int bx = x0 + w / 4 + 6, by = y0 + h / 2 + 30;
        g.fill(x0 + 10, y0 + 26, x0 + w / 2 - 4, y0 + h - 10, 0x30000000);
        spin += partial * 0.6f;
        try {
            g.pose().pushPose();
            g.pose().translate(bx, by, 150);
            g.pose().scale(44, -44, 44);
            g.pose().mulPose(Axis.XP.rotationDegrees(-12));
            g.pose().mulPose(Axis.YP.rotationDegrees(spin));
            Lighting.setupForEntityInInventory();
            var disp = minecraft.getEntityRenderDispatcher();
            disp.setRenderShadow(false);
            var buf = minecraft.renderBuffers().bufferSource();
            disp.getRenderer(bike).render(bike, 0, 1, g.pose(), buf, 0xF000F0);
            buf.endBatch();
            disp.setRenderShadow(true);
            g.pose().popPose();
            Lighting.setupFor3DItems();
        } catch (Throwable ignored) {
            g.pose().popPose();
        }
        // What's fitted, and how it rides
        int rx = x0 + w / 2 + 6, ry = y0 + 28;
        BikeParts.Stats st = bike.stats();
        String[][] fitted = {{"ENGINE", BikeParts.engine(bike.engine()).name()}, {"FRAME", BikeParts.frame(bike.frame()).name()},
            {"GEARBOX", BikeParts.gearbox(bike.gearbox()).name()}};
        for (String[] f : fitted) {
            g.drawString(font, TarkovScreen.ui(f[0]), rx, ry, 0xFF8C8A84, false);
            g.drawString(font, TarkovScreen.uiBold(f[1]), rx + 52, ry, 0xFFF2EFE6, false);
            ry += 12;
        }
        ry += 2;
        // Hovered part: what it would make
        BikeParts.Stats hover = null;
        for (int[] r : rows) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                ItemStack s = minecraft.player.getInventory().items.get(r[4]);
                if (s.getItem() instanceof BikePartItem p) hover = BikeParts.stats(p.kind.equals("engine") ? p.id : bike.engine(),
                    p.kind.equals("frame") ? p.id : bike.frame(), p.kind.equals("gearbox") ? p.id : bike.gearbox(), 1, 1);
            }
        }
        stat(g, rx, ry, "Top speed", st.top() * 72, hover == null ? -1 : hover.top() * 72, "%.0f km/h", w / 2 - 20);
        stat(g, rx, ry + 11, "Pull", st.accel() * 1000, hover == null ? -1 : hover.accel() * 1000, "%.0f", w / 2 - 20);
        stat(g, rx, ry + 22, "Handling", st.turn() * 100, hover == null ? -1 : hover.turn() * 100, "%.0f", w / 2 - 20);
        stat(g, rx, ry + 33, "Weight", st.mass() - 80, hover == null ? -1 : hover.mass() - 80, "%.0f kg", w / 2 - 20);
        ry += 50;
        g.drawString(font, TarkovScreen.uiBold("YOUR PARTS"), rx, ry, TarkovScreen.ACCENT, false);
        ry += 12;
        rows.clear();
        var inv = minecraft.player.getInventory();
        int shown = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (!(s.getItem() instanceof BikePartItem p)) continue;
            if (ry + 20 > y0 + h - 8) break;
            boolean on = mx >= rx && mx < x0 + w - 10 && my >= ry && my < ry + 19;
            boolean same = p.id.equals(bike.engine()) || p.id.equals(bike.frame()) || p.id.equals(bike.gearbox());
            g.fill(rx, ry, x0 + w - 10, ry + 19, on && !same ? 0x40C9A55A : 0x22FFFFFF);
            g.renderItem(s, rx + 2, ry + 1);
            g.drawString(font, TarkovScreen.ui(s.getHoverName().getString()), rx + 22, ry + 6, same ? 0xFF6A6864 : 0xFFE6E3DA, false);
            String tag = same ? "FITTED" : "FIT";
            g.drawString(font, TarkovScreen.uiBold(tag), x0 + w - 14 - font.width(TarkovScreen.uiBold(tag)), ry + 6, same ? 0xFF6A6864 : TarkovScreen.ACCENT, false);
            if (!same) rows.add(new int[] {rx, ry, x0 + w - 10 - rx, 19, i});
            ry += 21;
            shown++;
        }
        if (shown == 0) g.drawString(font, TarkovScreen.ui("No bike parts on you. Make them at a bench or buy them."), rx, ry + 2, 0xFF8C8A84, false);
        super.render(g, mx, my, partial);
    }

    void stat(GuiGraphics g, int x, int y, String name, float now, float then, String fmt, int w) {
        g.drawString(font, TarkovScreen.ui(name), x, y, 0xFF8C8A84, false);
        String v = String.format(fmt, now);
        g.drawString(font, TarkovScreen.ui(v), x + 62, y, 0xFFF2EFE6, false);
        if (then >= 0 && Math.abs(then - now) > 0.05f) {
            boolean better = name.equals("Weight") ? then < now : then > now;
            g.drawString(font, TarkovScreen.ui("-> " + String.format(fmt, then)), x + 64 + font.width(v) + 4, y, better ? 0xFF6FB04A : 0xFFC8452E, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int[] r : rows) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.VehicleAction(bike.getId(), r[4]));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
