package dev.factoryascension.client.ui;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import dev.factoryascension.network.FANetwork;
import dev.factoryascension.storage.LootFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Two buttons in the top-left corner of inventory and container screens:
 *  - Sort: the open chest (or your main inventory) merged and in order (storage/LootFilter.sort, on the server).
 *  - Filter: a panel beside the screen; drop an item on it and you never pick that item up again (it's deleted
 *    instead, storage/LootFilter), click one to take it off. Kept in the client config, sent on joining.
 * Nothing here ever covers the screen it belongs to: the buttons only show when there's room above it (never on the
 * creative screen, whose tabs and page arrows are up there), and the panel opens on whichever side has room (not over
 * the recipe book either). With no room either side it doesn't open and says so.
 */
public final class InvToolbar {
    static final int MARGIN = 6, BTN_H = 16, CELL = 18, MIN_COLS = 5;
    static final String HINT = "Drop an item here and you'll never pick it up again (it's deleted). Click one to take it off.";
    private static boolean filterOpen, broken;
    private static long noRoomUntil;

    private InvToolbar() {
    }

    record Box(int x, int y, int w, int h) {
        boolean has(double mx, double my) {
            return UiKit.inside(mx, my, x, y, w, h);
        }
    }

    static boolean applies(Screen s) {
        return !broken && FAClientConfig.on(FAClientConfig.DARK_CONTAINERS) && s instanceof AbstractContainerScreen<?> c
            && !(s instanceof CreativeModeInventoryScreen) && c.getGuiTop() >= MARGIN + BTN_H + UiKit.GAP;
    }

    /** For tests: open the filter with these items on it. */
    public static void devFilter(List<String> items) {
        save(new ArrayList<>(items));
        filterOpen = true;
    }

    static Box sortBox() {
        return new Box(MARGIN, MARGIN, 40, BTN_H);
    }

    static Box filterBox() {
        return new Box(MARGIN + 40 + UiKit.GAP, MARGIN, 48, BTN_H);
    }

    static List<String> ids() {
        return new ArrayList<>(FAClientConfig.LOOT_FILTER.get());
    }

    static void save(List<String> ids) {
        FAClientConfig.LOOT_FILTER.set(ids);
        send();
    }

    static void send() {
        if (Minecraft.getInstance().getConnection() == null) return;
        FANetwork.CHANNEL.sendToServer(new FANetwork.InvAction(1, ids()));
    }

    /** Where the filter panel goes: the wider free side of the screen (left of it, or right), or null if neither fits. */
    static Box panel(AbstractContainerScreen<?> s) {
        int left = s.getGuiLeft() - MARGIN * 2;
        if (s instanceof RecipeUpdateListener r && r.getRecipeBookComponent().isVisible()) left = 0;   // the book's there
        int right = s.width - (s.getGuiLeft() + s.getXSize()) - MARGIN * 2;
        int need = MIN_COLS * CELL + UiKit.PAD * 2;
        int room = Math.max(left, right);
        if (room < need) return null;
        int w = Math.min(room, 9 * CELL + UiKit.PAD * 2);
        Font font = Minecraft.getInstance().font;
        int lines = font.split(Component.literal(HINT), w - UiKit.PAD * 2).size();
        int cols = (w - UiKit.PAD * 2) / CELL, rows = Math.max(1, (ids().size() + cols - 1) / cols);
        int h = UiKit.HEADER_H + UiKit.GAP + lines * UiKit.LINE + UiKit.GAP * 2 + rows * CELL + UiKit.PAD;
        int y = Math.max(MARGIN, s.getGuiTop());
        h = Math.min(h, s.height - y - MARGIN);
        int x = left >= right ? s.getGuiLeft() - MARGIN - w : s.getGuiLeft() + s.getXSize() + MARGIN;
        return new Box(x, y, w, h);
    }

    static Box grid(Box p) {
        Font font = Minecraft.getInstance().font;
        int lines = font.split(Component.literal(HINT), p.w - UiKit.PAD * 2).size();
        int cols = Math.max(1, (p.w - UiKit.PAD * 2) / CELL);
        int top = p.y + UiKit.HEADER_H + UiKit.GAP + lines * UiKit.LINE + UiKit.GAP * 2;
        int rows = Math.max(0, (p.y + p.h - UiKit.PAD - top) / CELL);
        return new Box(p.x + (p.w - cols * CELL) / 2, top, cols * CELL, rows * CELL);
    }

    static void draw(AbstractContainerScreen<?> s, GuiGraphics g, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        Box sb = sortBox(), fb = filterBox();
        UiKit.button(g, font, sb.x, sb.y, sb.w, sb.h, "Sort", sb.has(mx, my), true, UiKit.Tone.NORMAL);
        UiKit.button(g, font, fb.x, fb.y, fb.w, fb.h, "Filter", fb.has(mx, my) || filterOpen, true, UiKit.Tone.NORMAL);
        if (net.minecraft.Util.getMillis() < noRoomUntil)
            g.drawString(font, "No room beside this screen for the filter: make the window wider or the GUI scale smaller.",
                fb.x + fb.w + UiKit.PAD, fb.y + 4, UiKit.DIM, false);
        ItemStack hover = ItemStack.EMPTY;
        Box p = filterOpen ? panel(s) : null;
        if (p != null) {
            List<String> ids = ids();
            UiKit.panel(g, p.x, p.y, p.w, p.h);
            UiKit.header(g, font, p.x, p.y, p.w, "Pickup filter", ids.size() + "");
            boolean dropping = !s.getMenu().getCarried().isEmpty() && p.has(mx, my);
            int ty = p.y + UiKit.HEADER_H + UiKit.GAP;
            for (FormattedCharSequence line : font.split(Component.literal(dropping ? "Let go to filter this item." : HINT), p.w - UiKit.PAD * 2)) {
                g.drawString(font, line, p.x + UiKit.PAD, ty, dropping ? UiKit.TEXT : UiKit.DIM, false);
                ty += UiKit.LINE;
            }
            if (dropping) g.renderOutline(p.x + 1, p.y + 1, p.w - 2, p.h - 2, UiKit.ACCENT);
            Box gb = grid(p);
            int cols = gb.w / CELL, max = cols * (gb.h / CELL);
            for (int i = 0; i < Math.min(ids.size(), max); i++) {
                int x = gb.x + i % cols * CELL, y = gb.y + i / cols * CELL;
                UiKit.cell(g, x, y);
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(ids.get(i)));
                if (item == null) continue;
                ItemStack st = new ItemStack(item);
                g.renderItem(st, x + 1, y + 1);
                if (UiKit.inside(mx, my, x, y, CELL, CELL)) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, 0x50C8452E);
                    hover = st;
                }
            }
        }
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
        else if (sb.has(mx, my)) g.renderTooltip(font, Component.literal(s.getMenu() == Minecraft.getInstance().player.inventoryMenu ? "Sort your inventory" : "Sort this container"), mx, my);
    }

    /** The item list index under the mouse, or -1. */
    static int at(AbstractContainerScreen<?> s, double mx, double my) {
        Box p = panel(s);
        if (p == null) return -1;
        Box gb = grid(p);
        if (!gb.has(mx, my)) return -1;
        return (int) (my - gb.y) / CELL * (gb.w / CELL) + (int) (mx - gb.x) / CELL;
    }

    static void add(ItemStack carried) {
        List<String> ids = ids();
        String id = ForgeRegistries.ITEMS.getKey(carried.getItem()).toString();
        if (!ids.contains(id) && ids.size() < LootFilter.MAX) {
            ids.add(id);
            save(ids);
            click();
        }
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f));
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void render(ScreenEvent.Render.Post event) {
            if (!applies(event.getScreen())) return;
            try {
                var g = event.getGuiGraphics();
                g.pose().pushPose();
                g.pose().translate(0, 0, 400);
                draw((AbstractContainerScreen<?>) event.getScreen(), g, event.getMouseX(), event.getMouseY());
                g.pose().popPose();
            } catch (Throwable err) {
                broken = true;
                FAClientConfig.failSafe("Inventory toolbar", err);
            }
        }

        @SubscribeEvent
        public static void press(ScreenEvent.MouseButtonPressed.Pre event) {
            if (!applies(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            double mx = event.getMouseX(), my = event.getMouseY();
            if (sortBox().has(mx, my)) {
                if (s.getMenu().getCarried().isEmpty()) FANetwork.CHANNEL.sendToServer(new FANetwork.InvAction(0, List.of()));
                click();
                event.setCanceled(true);
                return;
            }
            if (filterBox().has(mx, my)) {
                if (!filterOpen && panel(s) == null) noRoomUntil = net.minecraft.Util.getMillis() + 3000;
                else filterOpen = !filterOpen;
                click();
                event.setCanceled(true);
                return;
            }
            Box p = filterOpen ? panel(s) : null;
            if (p == null || !p.has(mx, my)) return;
            event.setCanceled(true);
            ItemStack carried = s.getMenu().getCarried();
            if (!carried.isEmpty()) { add(carried); return; }
            List<String> ids = ids();
            int i = at(s, mx, my);
            if (i >= 0 && i < ids.size()) {
                ids.remove(i);
                save(ids);
                click();
            }
        }

        /** Dragged an item over the filter and let go: filter it (it stays in your hand to put back). */
        @SubscribeEvent
        public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
            if (!filterOpen || !applies(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            Box p = panel(s);
            ItemStack carried = s.getMenu().getCarried();
            if (p == null || carried.isEmpty() || !p.has(event.getMouseX(), event.getMouseY())) return;
            add(carried);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void join(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            try {
                send();
            } catch (Throwable ignored) {
                // a server without the mod: no filter there
            }
        }
    }
}
