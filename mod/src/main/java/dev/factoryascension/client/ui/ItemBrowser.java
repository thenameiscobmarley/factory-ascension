package dev.factoryascension.client.ui;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Factory Ascension item browser: the emblem in the top-left corner of every inventory and container screen opens
 * a panel with only this pack's own items (the mod's and the KubeJS ones), searchable, in the Tarkov look. Click an
 * item for how it's made (its recipes, a card at the bottom; arrows step through several), right-click for what it's
 * used in. It reads the recipes the server sent, so it works with or without JEI. Drawing errors switch it off.
 * Next to the emblem: Sort (the open chest, or your main inventory) and Filter (a panel: drop an item on it and you'll
 * never pick that item up again, it's deleted instead: storage/LootFilter; click one to take it off the list).
 */
public final class ItemBrowser {
    static final int ICON = 20, MARGIN = 6, CELL = 18;
    static final ResourceLocation EMBLEM = new ResourceLocation(FactoryAscension.MODID, "textures/gui/fa_emblem.png");

    private static boolean open, broken, searchFocused, filterOpen;
    private static String query = "";
    private static int scroll;
    private static List<ItemStack> all, shown = List.of();
    /** The recipe card: what it's for, whether it shows uses rather than how it's made, which one of them. */
    private static ItemStack picked = ItemStack.EMPTY;
    private static boolean uses;
    private static int recipeIndex;
    private static List<View> recipes = List.of();

    /** One recipe as the card shows it: each input's options (with counts), the grid width, what comes out, where. */
    record View(List<ItemStack[]> inputs, int width, ItemStack output, String where) {
        static View of(Recipe<?> r, net.minecraft.core.RegistryAccess access) {
            List<ItemStack[]> in = new ArrayList<>();
            for (Ingredient ing : r.getIngredients()) in.add(ing.getItems());
            int w = r instanceof ShapedRecipe sr ? sr.getWidth() : Math.min(3, Math.max(1, (int) Math.ceil(Math.sqrt(in.size()))));
            String type = r.getType().toString();
            return new View(in, w, r.getResultItem(access), type.substring(type.indexOf(':') + 1).replace('_', ' '));
        }

        /** A Chem Lab recipe (drugs: they're mixed there, so they're not in the game's recipe list). */
        static View of(dev.factoryascension.FAConfig.ChemRecipe r) {
            List<ItemStack[]> in = new ArrayList<>();
            for (var e : r.inputs()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.getKey()));
                in.add(item == null ? new ItemStack[0] : new ItemStack[] {new ItemStack(item, e.getValue())});
            }
            Item out = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(r.output()));
            return new View(in, 3, out == null ? ItemStack.EMPTY : new ItemStack(out, r.count()), "chem lab");
        }
    }

    private ItemBrowser() {
    }

    static boolean applies(Screen s) {
        return !broken && s instanceof AbstractContainerScreen<?>;
    }

    /** Every item the mod and the KubeJS scripts add, the mod's first, by name. */
    static List<ItemStack> all() {
        if (all == null) {
            List<ItemStack> list = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && (id.getNamespace().equals(FactoryAscension.MODID) || id.getNamespace().equals("kubejs"))) list.add(new ItemStack(item));
            }
            list.sort(Comparator.comparing((ItemStack s) -> !ForgeRegistries.ITEMS.getKey(s.getItem()).getNamespace().equals(FactoryAscension.MODID))
                .thenComparing(s -> s.getHoverName().getString()));
            all = list;
        }
        return all;
    }

    static void filter() {
        String q = query.toLowerCase(Locale.ROOT).trim();
        shown = q.isEmpty() ? all() : all().stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        scroll = 0;
    }

    static void pick(ItemStack stack, boolean wantUses) {
        picked = stack;
        uses = wantUses;
        recipeIndex = 0;
        var level = Minecraft.getInstance().level;
        if (level == null) { recipes = List.of(); return; }
        List<View> found = new ArrayList<>();
        for (Recipe<?> r : level.getRecipeManager().getRecipes()) {
            try {
                if (wantUses) {
                    for (Ingredient ing : r.getIngredients()) if (ing.test(stack)) { found.add(View.of(r, level.registryAccess())); break; }
                } else if (r.getResultItem(level.registryAccess()).is(stack.getItem())) {
                    found.add(View.of(r, level.registryAccess()));
                }
            } catch (Throwable ignored) {
                // some mods' recipes can't answer on the client
            }
        }
        for (var r : dev.factoryascension.FAConfig.chemRecipes) {
            View v = View.of(r);
            boolean match = wantUses ? v.inputs.stream().anyMatch(o -> o.length > 0 && o[0].is(stack.getItem())) : v.output.is(stack.getItem());
            if (match) found.add(v);
        }
        recipes = found;
    }

    /** For tests: open it, searching for `q`, with the first match's recipes on the card. */
    public static void devOpen(String q) {
        open = true;
        query = q;
        filter();
        if (!shown.isEmpty()) pick(shown.get(0), false);
    }

    // ---- Layout ----

    record Box(int x, int y, int w, int h) {
        boolean has(double mx, double my) { return UiKit.inside(mx, my, x, y, w, h); }
    }

    static Box iconBox() {
        return new Box(MARGIN, MARGIN, ICON, ICON);
    }

    static Box sortBox() {
        return new Box(MARGIN + ICON + UiKit.GAP, MARGIN + 2, 36, 16);
    }

    static Box filterBox() {
        return new Box(MARGIN + ICON + UiKit.GAP + 36 + UiKit.GAP, MARGIN + 2, 44, 16);
    }

    static List<String> filterIds() {
        return new ArrayList<>(dev.factoryascension.client.FAClientConfig.LOOT_FILTER.get());
    }

    /** Save the filter here and hand the server the whole list. */
    static void saveFilter(List<String> ids) {
        dev.factoryascension.client.FAClientConfig.LOOT_FILTER.set(ids);
        sendFilter();
    }

    public static void sendFilter() {
        if (Minecraft.getInstance().getConnection() == null) return;
        dev.factoryascension.network.FANetwork.CHANNEL.sendToServer(new dev.factoryascension.network.FANetwork.InvAction(1, filterIds()));
    }

    static Box panel(AbstractContainerScreen<?> s) {
        int room = s.getGuiLeft() - MARGIN * 2;
        int w = Math.max(7 * CELL + 2 * UiKit.PAD, Math.min(9 * CELL + 2 * UiKit.PAD, room));
        int y = MARGIN + ICON + UiKit.GAP;
        return new Box(MARGIN, y, w, s.height - y - MARGIN);
    }

    static Box search(Box p) {
        return new Box(p.x + UiKit.PAD, p.y + UiKit.HEADER_H + UiKit.GAP, p.w - UiKit.PAD * 2, 14);
    }

    static final int CARD_H = 84;

    static Box grid(Box p) {
        Box s = search(p);
        int top = s.y + s.h + UiKit.GAP * 2;
        int bottom = p.y + p.h - UiKit.PAD - (picked.isEmpty() ? 0 : CARD_H + UiKit.GAP);
        int cols = Math.max(1, (p.w - UiKit.PAD * 2) / CELL);
        return new Box(p.x + (p.w - cols * CELL) / 2, top, cols * CELL, Math.max(CELL, (bottom - top) / CELL * CELL));
    }

    static Box card(Box p) {
        return new Box(p.x + UiKit.PAD, p.y + p.h - UiKit.PAD - CARD_H, p.w - UiKit.PAD * 2, CARD_H);
    }

    // ---- Drawing ----

    static void draw(AbstractContainerScreen<?> s, GuiGraphics g, int mx, int my) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);   // above the screen's items and tooltips' layer below
        Box ib = iconBox();
        boolean iconHover = ib.has(mx, my);
        g.fill(ib.x, ib.y, ib.x + ib.w, ib.y + ib.h, open || iconHover ? UiKit.ACCENT : UiKit.EDGE);
        g.fillGradient(ib.x + 1, ib.y + 1, ib.x + ib.w - 1, ib.y + ib.h - 1, 0xFF1E1E1D, 0xFF0E0E0D);
        g.blit(EMBLEM, ib.x + 2, ib.y + 2, 0, 0, 16, 16, 16, 16);
        Box sb0 = sortBox(), fb0 = filterBox();
        UiKit.button(g, font, sb0.x, sb0.y, sb0.w, sb0.h, "Sort", sb0.has(mx, my), true, UiKit.Tone.NORMAL);
        UiKit.button(g, font, fb0.x, fb0.y, fb0.w, fb0.h, "Filter", fb0.has(mx, my) || filterOpen, true, UiKit.Tone.NORMAL);
        ItemStack hover = ItemStack.EMPTY;
        if (filterOpen) hover = drawFilter(s, g, font, mx, my);
        if (open) {
            Box p = panel(s);
            UiKit.panel(g, p.x, p.y, p.w, p.h);
            UiKit.header(g, font, p.x, p.y, p.w, "Ascension items", shown.size() + "");
            // Search
            Box sb = search(p);
            UiKit.well(g, sb.x, sb.y, sb.w, sb.h);
            if (searchFocused) g.renderOutline(sb.x, sb.y, sb.w, sb.h, UiKit.ACCENT_DIM);
            String text = query.isEmpty() && !searchFocused ? "Search…" : query + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            g.drawString(font, font.plainSubstrByWidth(text, sb.w - 8), sb.x + 4, sb.y + 3, query.isEmpty() && !searchFocused ? UiKit.FAINT : UiKit.TEXT, false);
            // The grid
            Box gb = grid(p);
            int cols = gb.w / CELL, rows = gb.h / CELL, total = (shown.size() + cols - 1) / cols;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rows)));
            for (int i = 0; i < rows * cols; i++) {
                int idx = (scroll + i / cols) * cols + i % cols;
                if (idx >= shown.size()) break;
                int x = gb.x + i % cols * CELL, y = gb.y + i / cols * CELL;
                UiKit.cell(g, x, y);
                ItemStack st = shown.get(idx);
                g.renderItem(st, x + 1, y + 1);
                if (UiKit.inside(mx, my, x, y, CELL, CELL)) { g.fill(x + 1, y + 1, x + 17, y + 17, 0x40FFFFFF); hover = st; }
                if (!picked.isEmpty() && ItemStack.isSameItem(st, picked)) g.renderOutline(x, y, CELL, CELL, UiKit.ACCENT);
            }
            UiKit.scrollbar(g, gb.x + gb.w + 2, gb.y, gb.h, scroll, rows, total);
            if (shown.isEmpty()) g.drawString(font, "Nothing matches.", gb.x, gb.y + 4, UiKit.DIM, false);
            if (!picked.isEmpty()) {
                ItemStack h = drawCard(g, font, card(p), mx, my);
                if (!h.isEmpty()) hover = h;
            }
        }
        g.pose().popPose();
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
        else if (iconHover) g.renderTooltip(font, Component.literal(open ? "Hide Ascension items" : "Ascension items"), mx, my);
    }

    /** The pickup filter: what's on it, and where to drop more. */
    static ItemStack drawFilter(AbstractContainerScreen<?> s, GuiGraphics g, net.minecraft.client.gui.Font font, int mx, int my) {
        Box p = panel(s);
        List<String> ids = filterIds();
        UiKit.panel(g, p.x, p.y, p.w, p.h);
        UiKit.header(g, font, p.x, p.y, p.w, "Pickup filter", ids.size() + "");
        int ty = p.y + UiKit.HEADER_H + UiKit.GAP;
        boolean dropping = !s.getMenu().getCarried().isEmpty() && p.has(mx, my);
        for (var line : font.split(Component.literal(dropping ? "Let go to filter this item." : "Drop an item here: you'll never pick it up again (it's deleted). Click one to remove it."), p.w - UiKit.PAD * 2)) {
            g.drawString(font, line, p.x + UiKit.PAD, ty, dropping ? UiKit.TEXT : UiKit.DIM, false);
            ty += UiKit.LINE;
        }
        if (dropping) g.renderOutline(p.x + 2, p.y + 2, p.w - 4, p.h - 4, UiKit.ACCENT);
        int cols = Math.max(1, (p.w - UiKit.PAD * 2) / CELL), gx = p.x + (p.w - cols * CELL) / 2, gy = ty + UiKit.GAP;
        ItemStack hover = ItemStack.EMPTY;
        for (int i = 0; i < ids.size(); i++) {
            int x = gx + i % cols * CELL, y = gy + i / cols * CELL;
            if (y + CELL > p.y + p.h - UiKit.PAD) break;
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
        return hover;
    }

    /** Which filter entry is under the mouse (-1: none). */
    static int filterAt(AbstractContainerScreen<?> s, double mx, double my) {
        Box p = panel(s);
        var font = Minecraft.getInstance().font;
        int lines = font.split(Component.literal("Drop an item here: you'll never pick it up again (it's deleted). Click one to remove it."), p.w - UiKit.PAD * 2).size();
        int cols = Math.max(1, (p.w - UiKit.PAD * 2) / CELL), gx = p.x + (p.w - cols * CELL) / 2;
        int gy = p.y + UiKit.HEADER_H + UiKit.GAP + lines * UiKit.LINE + UiKit.GAP;
        if (mx < gx || my < gy || mx >= gx + cols * CELL) return -1;
        return (int) (my - gy) / CELL * cols + (int) (mx - gx) / CELL;
    }

    /** The recipe card: ingredients (cycling through each one's options), an arrow, what comes out. */
    static ItemStack drawCard(GuiGraphics g, net.minecraft.client.gui.Font font, Box c, int mx, int my) {
        UiKit.well(g, c.x, c.y, c.w, c.h);
        String title = (uses ? "Used in" : "Made by") + (recipes.isEmpty() ? "" : "  " + (recipeIndex + 1) + "/" + recipes.size());
        UiKit.section(g, font, c.x + UiKit.PAD, c.y + UiKit.PAD - 2, c.w - UiKit.PAD * 2, title);
        if (recipes.isEmpty()) {
            g.drawString(font, uses ? "Not used in any recipe." : "No recipe (found or dropped).", c.x + UiKit.PAD, c.y + 24, UiKit.DIM, false);
            return ItemStack.EMPTY;
        }
        if (recipes.size() > 1) {
            g.drawString(font, "<", c.x + c.w - 22, c.y + UiKit.PAD - 2, UiKit.ACCENT, false);
            g.drawString(font, ">", c.x + c.w - 10, c.y + UiKit.PAD - 2, UiKit.ACCENT, false);
        }
        View r = recipes.get(Math.min(recipeIndex, recipes.size() - 1));
        var ings = r.inputs;
        int w = Math.max(1, r.width);
        int gx = c.x + UiKit.PAD, gy = c.y + 20;
        long tick = System.currentTimeMillis() / 1000;
        ItemStack hover = ItemStack.EMPTY;
        for (int i = 0; i < Math.min(9, ings.size()); i++) {
            int x = gx + i % w * CELL, y = gy + i / w * CELL;
            UiKit.cell(g, x, y);
            ItemStack[] opts = ings.get(i);
            if (opts.length == 0) continue;
            ItemStack st = opts[(int) (tick % opts.length)];
            g.renderItem(st, x + 1, y + 1);
            g.renderItemDecorations(font, st, x + 1, y + 1);
            if (UiKit.inside(mx, my, x, y, CELL, CELL)) hover = st;
        }
        int ax = gx + 3 * CELL + UiKit.GAP * 2, ay = gy + CELL + 4;
        g.drawString(font, "→", ax, ay, UiKit.DIM, false);
        ItemStack out = r.output;
        int ox = ax + 16, oy = gy + CELL - 1;
        g.fill(ox - 1, oy - 1, ox + 19, oy + 19, UiKit.ACCENT_DIM);
        UiKit.cell(g, ox, oy);
        g.renderItem(out, ox + 1, oy + 1);
        g.renderItemDecorations(font, out, ox + 1, oy + 1);
        if (UiKit.inside(mx, my, ox, oy, CELL, CELL)) hover = out;
        String type = r.where;
        g.drawString(font, font.plainSubstrByWidth(type, c.w - (ox + 24 - c.x) - UiKit.PAD), ox + 24, oy + 5, UiKit.FAINT, false);
        return hover;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f));
    }

    private static void fail(Throwable err) {
        broken = true;
        open = false;
        dev.factoryascension.client.FAClientConfig.failSafe("Item browser", err);
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void render(ScreenEvent.Render.Post event) {
            if (!applies(event.getScreen())) return;
            try {
                draw((AbstractContainerScreen<?>) event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
            } catch (Throwable err) {
                fail(err);
            }
        }

        @SubscribeEvent
        public static void press(ScreenEvent.MouseButtonPressed.Pre event) {
            if (!applies(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            double mx = event.getMouseX(), my = event.getMouseY();
            if (sortBox().has(mx, my)) {
                if (s.getMenu().getCarried().isEmpty()) dev.factoryascension.network.FANetwork.CHANNEL.sendToServer(new dev.factoryascension.network.FANetwork.InvAction(0, List.of()));
                click();
                event.setCanceled(true);
                return;
            }
            if (filterBox().has(mx, my)) {
                filterOpen = !filterOpen;
                if (filterOpen) open = false;
                click();
                event.setCanceled(true);
                return;
            }
            if (filterOpen && panel(s).has(mx, my)) {
                event.setCanceled(true);
                List<String> ids = filterIds();
                ItemStack carried = s.getMenu().getCarried();
                if (!carried.isEmpty()) {
                    String id = ForgeRegistries.ITEMS.getKey(carried.getItem()).toString();
                    if (!ids.contains(id) && ids.size() < dev.factoryascension.storage.LootFilter.MAX) { ids.add(id); saveFilter(ids); click(); }
                    return;
                }
                int at = filterAt(s, mx, my);
                if (at >= 0 && at < ids.size()) { ids.remove(at); saveFilter(ids); click(); }
                return;
            }
            if (iconBox().has(mx, my)) {
                open = !open;
                if (open) { filter(); filterOpen = false; }
                searchFocused = false;
                click();
                event.setCanceled(true);
                return;
            }
            if (!open) return;
            Box p = panel(s);
            if (!p.has(mx, my)) { searchFocused = false; return; }
            event.setCanceled(true);
            if (!s.getMenu().getCarried().isEmpty()) return;
            searchFocused = search(p).has(mx, my);
            if (searchFocused && event.getButton() == 1) { query = ""; filter(); }
            Box gb = grid(p);
            if (gb.has(mx, my)) {
                int cols = gb.w / CELL, idx = (scroll + (int) (my - gb.y) / CELL) * cols + (int) (mx - gb.x) / CELL;
                if (idx >= 0 && idx < shown.size()) { pick(shown.get(idx), event.getButton() == 1); click(); }
            }
            if (!picked.isEmpty() && recipes.size() > 1) {
                Box c = card(p);
                if (UiKit.inside(mx, my, c.x + c.w - 24, c.y, 10, 14)) recipeIndex = (recipeIndex + recipes.size() - 1) % recipes.size();
                if (UiKit.inside(mx, my, c.x + c.w - 12, c.y, 10, 14)) recipeIndex = (recipeIndex + 1) % recipes.size();
            }
        }

        /** Dragged an item over the filter and let go: filter it (it stays in your hand to put back). */
        @SubscribeEvent
        public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
            if (!filterOpen || !applies(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            ItemStack carried = s.getMenu().getCarried();
            if (carried.isEmpty() || !panel(s).has(event.getMouseX(), event.getMouseY())) return;
            List<String> ids = filterIds();
            String id = ForgeRegistries.ITEMS.getKey(carried.getItem()).toString();
            if (!ids.contains(id) && ids.size() < dev.factoryascension.storage.LootFilter.MAX) { ids.add(id); saveFilter(ids); click(); }
            event.setCanceled(true);
        }

        /** Joining a world: the server gets this player's filter. */
        @SubscribeEvent
        public static void join(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            try {
                sendFilter();
            } catch (Throwable ignored) {
                // a server without the mod: no filter there
            }
        }

        @SubscribeEvent
        public static void scrolled(ScreenEvent.MouseScrolled.Pre event) {
            if (!open || !applies(event.getScreen())) return;
            if (!panel((AbstractContainerScreen<?>) event.getScreen()).has(event.getMouseX(), event.getMouseY())) return;
            scroll -= (int) Math.signum(event.getScrollDelta());
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void key(ScreenEvent.KeyPressed.Pre event) {
            if (!open || !searchFocused || !applies(event.getScreen())) return;
            int k = event.getKeyCode();
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_ENTER) { searchFocused = false; event.setCanceled(true); return; }
            if (k == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) { query = query.substring(0, query.length() - 1); filter(); }
            event.setCanceled(true);   // typing "e" mustn't close the inventory
        }

        @SubscribeEvent
        public static void typed(ScreenEvent.CharacterTyped.Pre event) {
            if (!open || !searchFocused || !applies(event.getScreen())) return;
            if (query.length() < 40 && net.minecraft.SharedConstants.isAllowedChatCharacter(event.getCodePoint())) { query += event.getCodePoint(); filter(); }
            event.setCanceled(true);
        }
    }
}
