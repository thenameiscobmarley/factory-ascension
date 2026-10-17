package dev.factoryascension.client.ui;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bottom right: what you just picked up. Each new item slides in as a see-through row (icon, name, how many), more of
 * the same adds to its count, and it fades after a few seconds. Worked out from your inventory's totals each tick, so
 * it needs nothing from the server (moving things around inside your inventory doesn't count: the totals stay the same).
 */
public final class PickupFeed {
    static final int MAX_ROWS = 6, SHOW_MS = 4500, FADE_MS = 600, SLIDE_MS = 180, ROW_H = 20, ROW_W = 150;

    static final class Entry {
        final ItemStack icon;
        int count;
        long since, last;

        Entry(ItemStack icon, int count, long now) {
            this.icon = icon.copyWithCount(1);
            this.count = count;
            since = last = now;
        }
    }

    private static final List<Entry> ROWS = new ArrayList<>();
    private static Map<Item, Integer> before;
    private static boolean broken, seen;
    public static int devRows() { return ROWS.size(); }

    private PickupFeed() {
    }

    static Map<Item, Integer> totals(net.minecraft.world.entity.player.Inventory inv) {
        Map<Item, Integer> m = new HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) m.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
        if (!carried.isEmpty()) m.merge(carried.getItem(), carried.getCount(), Integer::sum);
        return m;
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (!seen) { seen = true; com.mojang.logging.LogUtils.getLogger().info("Pickup feed: ticking"); }
            if (event.phase != TickEvent.Phase.END || broken) return;
            if (mc.player == null || mc.player.isCreative()) { before = null; return; }
            try {
                Map<Item, Integer> now = totals(mc.player.getInventory());
                long t = net.minecraft.Util.getMillis();
                if (before != null && mc.screen == null) {   // in a screen, gains are you moving things (from chests too): skip
                    for (var e : now.entrySet()) {
                        int gained = e.getValue() - before.getOrDefault(e.getKey(), 0);
                        if (gained > 0) add(new ItemStack(e.getKey()), gained, t);
                    }
                }
                before = now;
                ROWS.removeIf(r -> t - r.last > SHOW_MS + FADE_MS);
            } catch (Throwable err) {
                broken = true;
                FAClientConfig.failSafe("Pickup feed", err);
            }
        }

        @SubscribeEvent
        public static void draw(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (broken || ROWS.isEmpty() || mc.options.hideGui || !FAClientConfig.on(FAClientConfig.PICKUP_FEED)) return;
            GuiGraphics g = event.getGuiGraphics();
            int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
            long t = net.minecraft.Util.getMillis();
            int y = h - 48 - ROW_H;   // above the hotbar and the XP bar, whatever the window width
            // Driving: the vehicle's HUD has that corner (vehicle/CarClient, BikeClient), so stack above it
            var ride = mc.player == null ? null : mc.player.getVehicle();
            if (ride instanceof dev.factoryascension.vehicle.Car || ride instanceof dev.factoryascension.vehicle.DirtBike) y = h - 64 - ROW_H;
            for (int i = ROWS.size() - 1; i >= 0; i--) {
                Entry r = ROWS.get(i);
                float fade = 1 - Mth.clamp((t - r.last - SHOW_MS) / (float) FADE_MS, 0, 1);
                float slide = 1 - Mth.clamp((t - r.since) / (float) SLIDE_MS, 0, 1);
                int x = w - UiKit.PAD - ROW_W + Math.round(slide * slide * 40);
                row(g, mc, r, x, y, fade * (1 - slide * 0.6f));
                y -= ROW_H + 2;
            }
        }
    }

    static void add(ItemStack item, int n, long t) {
        for (Entry r : ROWS) {
            if (r.icon.is(item.getItem())) { r.count += n; r.last = t; return; }
        }
        ROWS.add(new Entry(item, n, t));
        while (ROWS.size() > MAX_ROWS) ROWS.remove(0);
    }

    /** One row: about 60% see-through dark glass, a hairline edge, the icon, the name and the count. */
    static void row(GuiGraphics g, Minecraft mc, Entry r, int x, int y, float a) {
        if (a <= 0.02f) return;
        int bg = Math.round(0x66 * a) << 24, edge = Math.round(0x50 * a) << 24;
        g.fill(x, y, x + ROW_W, y + ROW_H, bg | 0x0E0E0F);
        g.renderOutline(x, y, ROW_W, ROW_H, edge | 0x8A9096);
        g.fill(x + 1, y + 1, x + ROW_W - 1, y + 2, Math.round(0x18 * a) << 24 | 0xFFFFFF);
        int ta = Math.max(4, Math.round(255 * a)) << 24;
        g.renderItem(r.icon, x + 2, y + 2);
        String count = "x" + r.count;
        int cw = mc.font.width(count);
        String name = mc.font.plainSubstrByWidth(r.icon.getHoverName().getString(), ROW_W - 30 - cw - UiKit.PAD);
        g.drawString(mc.font, name, x + 22, y + 6, ta | (UiKit.TEXT & 0xFFFFFF), true);
        g.drawString(mc.font, count, x + ROW_W - UiKit.PAD - cw, y + 6, ta | (UiKit.DIM & 0xFFFFFF), true);
    }
}
