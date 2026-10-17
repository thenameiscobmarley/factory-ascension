package dev.factoryascension.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Every container screen in the black Tarkov look, with drag and drop, the game's and other mods' alike:
 *  - Looks: when resources load, the GUI textures (any mod's textures/gui/container/..., Sophisticated Backpacks')
 *    have the game's grey GUI palette swapped for ours: near-black panels, darker sunken cells, thin grey edges.
 *    Coloured parts (flames, arrows, bubbles) stay. Nothing is shipped: it's worked out from whatever pack is loaded.
 *  - Labels: the game's dark-grey titles are drawn light instead (mixin/DarkLabelMixin asks labelColor).
 *  - Drag and drop: press on an item, drag it, let go over another slot: it lands there (swapping if taken).
 * Off in Pause > Factory Ascension (Dark container screens); a texture that fails just stays as it was.
 */
public final class DarkContainers {
    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    /** The game's dark-grey label colour (4210752). */
    public static final int VANILLA_LABEL = 0x404040;

    private DarkContainers() {
    }

    static boolean on() {
        return FAClientConfig.on(FAClientConfig.DARK_CONTAINERS);
    }

    /** Is a vanilla-styled container screen open right now (so its labels should be light)? */
    public static boolean active() {
        return on() && Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;
    }

    /** DarkLabelMixin: the colour a label is really drawn in. */
    public static int labelColor(int color) {
        return (color & 0xFFFFFF) == VANILLA_LABEL && active() ? (color & 0xFF000000) | (UiKit.DIM & 0xFFFFFF) : color;
    }

    // ---- The textures ----

    /** Which GUI textures get the dark palette. */
    static boolean restyles(ResourceLocation loc) {
        String ns = loc.getNamespace(), path = loc.getPath();
        if (ns.equals(FactoryAscension.MODID)) return false;
        return path.startsWith("textures/gui/container/") || path.equals("textures/gui/recipe_book.png") || isWidgets(loc)
            || (ns.startsWith("sophisticated") && path.startsWith("textures/gui/"));
    }

    /** The game's GUI greys (input) and what each becomes: outline, slot shadow, bevel shadow, slot, panel, bevel light. */
    private static final int[][] GREYS = {{0, 0x3A}, {55, 0x26}, {85, 0x0B}, {139, 0x0F}, {198, 0x17}, {255, 0x2E}};

    /** One pixel (NativeImage's ABGR): greys go through GREYS, colours stay. */
    static int recolor(int abgr) {
        int a = abgr >>> 24, b = abgr >> 16 & 255, g = abgr >> 8 & 255, r = abgr & 255;
        if (a == 0) return abgr;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        // Greys, and the game's dull warm greys (enchanting's option slots): anything this unsaturated goes dark
        if (max - min > 40 || (max - min > 14 && max > 0xD0)) return abgr;
        int v = (r + g + b) / 3, out = GREYS[GREYS.length - 1][1];
        for (int i = 1; i < GREYS.length; i++) {
            if (v <= GREYS[i][0]) {
                float t = (v - GREYS[i - 1][0]) / (float) (GREYS[i][0] - GREYS[i - 1][0]);
                out = Math.round(GREYS[i - 1][1] + (GREYS[i][1] - GREYS[i - 1][1]) * t);
                break;
            }
        }
        // A touch warm, like the Tarkov panels
        int ro = Math.min(255, out + 1), go = out, bo = Math.max(0, out - 1);
        return a << 24 | bo << 16 | go << 8 | ro;
    }

    /** The game's buttons, sliders and hotbar (villager trades, every vanilla button): dark too, with their own greys. */
    static boolean isWidgets(ResourceLocation loc) {
        return loc.getNamespace().equals("minecraft") && loc.getPath().equals("textures/gui/widgets.png");
    }

    /**
     * widgets.png: buttons are mid greys (normal), blue-greys (hovered) and dark greys (disabled), so they need a
     * different mapping from panels: everything dark steel, hovered a clear step lighter, nothing blue.
     */
    static int recolorWidget(int abgr) {
        int a = abgr >>> 24, b = abgr >> 16 & 255, g = abgr >> 8 & 255, r = abgr & 255;
        if (a == 0) return abgr;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        boolean hover = b > r + 20 && b > g + 10;
        if (!hover && max - min > 40) return abgr;   // icons keep their colours
        int lum = (r * 3 + g * 6 + b) / 10;
        int out = hover ? 0x22 + lum * 40 / 100 : 0x12 + lum * 26 / 100;
        out = Math.min(255, out);
        int ro = Math.min(255, out + 1), go = out, bo = Math.max(0, out - 1);
        return a << 24 | bo << 16 | go << 8 | ro;
    }

    /** Only textures that really use the game's GUI panel or slot greys (so other art in those folders is left alone). */
    static boolean looksVanilla(NativeImage img) {
        int panel = 0, seen = 0;
        for (int y = 0; y < img.getHeight(); y += 2)
            for (int x = 0; x < img.getWidth(); x += 2) {
                int p = img.getPixelRGBA(x, y);
                if (p >>> 24 == 0) continue;
                seen++;
                int rgb = p & 0xFFFFFF;
                // The panel grey, or the slot greys (Sophisticated Core keeps its slot grid in a texture of its own)
                if (rgb == 0xC6C6C6 || rgb == 0x8B8B8B || rgb == 0x373737) panel++;
            }
        return seen > 0 && panel > seen / 20;
    }

    static final class Reload extends SimplePreparableReloadListener<Map<ResourceLocation, NativeImage>> {
        @Override
        protected Map<ResourceLocation, NativeImage> prepare(ResourceManager rm, ProfilerFiller profiler) {
            Map<ResourceLocation, NativeImage> out = new HashMap<>();
            if (!on()) return out;
            for (var e : rm.listResources("textures/gui", l -> l.getPath().endsWith(".png")).entrySet()) {
                if (!restyles(e.getKey())) continue;
                NativeImage img = null;
                try (InputStream in = e.getValue().open()) {
                    img = NativeImage.read(in);
                    boolean widgets = isWidgets(e.getKey());
                    if (widgets || looksVanilla(img)) {
                        for (int y = 0; y < img.getHeight(); y++)
                            for (int x = 0; x < img.getWidth(); x++) {
                                int p = img.getPixelRGBA(x, y);
                                img.setPixelRGBA(x, y, widgets ? recolorWidget(p) : recolor(p));
                            }
                        out.put(e.getKey(), img);   // the DynamicTexture made from it owns it from here
                        img = null;
                    }
                } catch (Throwable err) {
                    LOG.warn("Dark container screens: left {} as it was ({})", e.getKey(), err.toString());
                } finally {
                    if (img != null) img.close();
                }
            }
            return out;
        }

        @Override
        protected void apply(Map<ResourceLocation, NativeImage> images, ResourceManager rm, ProfilerFiller profiler) {
            var tm = Minecraft.getInstance().getTextureManager();
            for (var e : images.entrySet()) {
                if (isWidgets(e.getKey())) {
                    // The game preloads widgets.png itself and registers that copy at the very end of the reload,
                    // after this: so ours goes in once the reload screen is gone (Events.lateWidgets)
                    if (lateWidgets != null) lateWidgets.close();
                    lateWidgets = e.getValue();
                    continue;
                }
                try {
                    tm.register(e.getKey(), new DynamicTexture(e.getValue()));
                } catch (Throwable err) {
                    LOG.warn("Dark container screens: couldn't use {} ({})", e.getKey(), err.toString());
                }
            }
            if (!images.isEmpty()) LOG.info("Dark container screens: {} GUI textures restyled", images.size());
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void reloaders(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new Reload());
        }
    }

    // ---- Drag and drop (every container screen but creative, which has its own rules) ----

    private static NativeImage lateWidgets;
    private static Slot dragFrom;
    private static int dragButton;
    private static boolean pressEmptyHand;
    private static Method slotClicked;
    private static java.lang.reflect.Field slotColor;
    private static boolean slotColorFailed;
    private static boolean slotClickedFailed;

    static boolean drags(Object screen) {
        return on() && screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen);
    }

    /** The screen's own slotClicked (some screens change what a click does; going round it would break them). */
    static void click(AbstractContainerScreen<?> s, Slot slot) {
        try {
            if (slotClicked == null && !slotClickedFailed) {
                for (String name : new String[] {"slotClicked", "m_6597_"}) {
                    try {
                        slotClicked = AbstractContainerScreen.class.getDeclaredMethod(name, Slot.class, int.class, int.class, ClickType.class);
                        slotClicked.setAccessible(true);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                slotClickedFailed = slotClicked == null;
            }
            if (slotClicked != null) { slotClicked.invoke(s, slot, slot.index, 0, ClickType.PICKUP); return; }
        } catch (Throwable err) {
            slotClickedFailed = true;
            slotClicked = null;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.handleInventoryMouseClick(s.getMenu().containerId, slot.index, 0, ClickType.PICKUP, mc.player);
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void pressBefore(ScreenEvent.MouseButtonPressed.Pre event) {
            dragFrom = null;
            if (!drags(event.getScreen())) return;
            pressEmptyHand = ((AbstractContainerScreen<?>) event.getScreen()).getMenu().getCarried().isEmpty();
        }

        @SubscribeEvent
        public static void pressAfter(ScreenEvent.MouseButtonPressed.Post event) {
            if (!drags(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            Slot at = s.getSlotUnderMouse();
            int b = event.getButton();
            // Only a plain press that picked something up starts a drag (shift-clicks, number keys act as usual)
            dragFrom = pressEmptyHand && at != null && !net.minecraft.client.gui.screens.Screen.hasShiftDown() && !s.getMenu().getCarried().isEmpty() && (b == 0 || b == 1) ? at : null;
            dragButton = b;
        }

        @SubscribeEvent
        public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
            Slot from = dragFrom;
            dragFrom = null;
            if (from == null || !drags(event.getScreen()) || event.getButton() != dragButton) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            var carried = s.getMenu().getCarried();
            Slot to = s.getSlotUnderMouse();
            if (carried.isEmpty() || to == null || to == from || !to.mayPlace(carried)) return;
            // Drop it on the target (places, merges, or swaps: the target's item comes up into the hand)...
            click(s, to);
            // ...and a swapped-out item goes back where the dragged one came from
            var back = s.getMenu().getCarried();
            if (!back.isEmpty() && !from.hasItem() && from.mayPlace(back)) click(s, from);
            event.setCanceled(true);
        }

        /** Every tooltip in the dark look too: near-black glass, a thin grey edge (instead of the game's purple). */
        @SubscribeEvent
        public static void tooltip(net.minecraftforge.client.event.RenderTooltipEvent.Color event) {
            if (!on()) return;
            event.setBackground(0xF00E0E0F);
            event.setBorderStart(0xFF4A4A48);
            event.setBorderEnd(0xFF2A2A29);
        }

        /** The slot under the mouse: a soft steel glow instead of the game's bright white square (Forge's slotColor). */
        @SubscribeEvent
        public static void init(ScreenEvent.Init.Post event) {
            if (!on() || !(event.getScreen() instanceof AbstractContainerScreen<?> s)) return;
            try {
                if (slotColor == null && !slotColorFailed) {
                    slotColor = AbstractContainerScreen.class.getDeclaredField("slotColor");
                    slotColor.setAccessible(true);
                }
                if (slotColor != null) slotColor.setInt(s, 0x40B4BEC8);
            } catch (Throwable err) {
                slotColorFailed = true;
            }
        }

        /** The dark widgets.png from the last reload, put in after the game's own copy (see Reload.apply). */
        @SubscribeEvent
        public static void lateWidgets(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
            NativeImage img = lateWidgets;
            if (img == null || event.phase != net.minecraftforge.event.TickEvent.Phase.START || Minecraft.getInstance().getOverlay() != null) return;
            lateWidgets = null;
            try {
                Minecraft.getInstance().getTextureManager().register(new ResourceLocation("textures/gui/widgets.png"), new DynamicTexture(img));
            } catch (Throwable err) {
                img.close();
                LOG.warn("Dark container screens: couldn't use the dark buttons ({})", err.toString());
            }
        }

        /** Where a dragged item will land: an amber outline on the slot under the mouse. */
        @SubscribeEvent
        public static void drawTarget(ScreenEvent.Render.Post event) {
            if (dragFrom == null || !drags(event.getScreen())) return;
            var s = (AbstractContainerScreen<?>) event.getScreen();
            var carried = s.getMenu().getCarried();
            Slot to = s.getSlotUnderMouse();
            if (carried.isEmpty() || to == null || to == dragFrom) return;
            var g = event.getGuiGraphics();
            int x = s.getGuiLeft() + to.x - 1, y = s.getGuiTop() + to.y - 1;
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.renderOutline(x, y, 18, 18, to.mayPlace(carried) ? UiKit.ACCENT : UiKit.BAD);
            g.renderOutline(x - 1, y - 1, 20, 20, 0x80000000 | (UiKit.ACCENT & 0xFFFFFF));
            g.pose().popPose();
        }
    }
}
