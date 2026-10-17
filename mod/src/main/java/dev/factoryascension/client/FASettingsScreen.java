package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * The Factory Ascension settings, from a button on the pause menu: Ascension Lighting, smoke, voxel debris, water
 * effects, Shiny Reflections, safe mode, and the depth pack for Oculus. Changes apply at once and are saved.
 */
public class FASettingsScreen extends Screen {

    private static final String[] QUALITY = {"Off", "Low", "Medium", "High"};
    private final Screen parent;
    private Component status = Component.empty();

    public FASettingsScreen(Screen parent) {
        super(Component.literal("Factory Ascension"));
        this.parent = parent;
    }

    // ---- The pause menu gets a button (top left, out of the way of other mods' buttons) ----
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class PauseButton {
        @SubscribeEvent
        public static void init(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof PauseScreen pause)
                event.addListener(Button.builder(Component.literal("Factory Ascension"), b -> Minecraft.getInstance().setScreen(new FASettingsScreen(pause)))
                    .bounds(6, 6, 130, 20).build());
        }
    }

    @Override
    protected void init() {
        // Laid out in as many columns as fit (three on a small window, so nothing runs off the bottom)
        int cols = width >= 420 ? 3 : 2;
        int w = cols == 3 ? Math.min(150, (width - 24) / 3) : 150, gap = 4;
        int x0 = width / 2 - (cols * w + (cols - 1) * gap) / 2;
        java.util.List<java.util.function.BiFunction<Integer, Integer, net.minecraft.client.gui.components.AbstractWidget>> items = new java.util.ArrayList<>();
        items.add((x, y) -> cycle(x, y, w, "Lighting", () -> QUALITY[FAClientConfig.LIGHTING.get()], () -> {
            FAClientConfig.LIGHTING.set((FAClientConfig.LIGHTING.get() + 1) % 4);
            FAClientConfig.LIGHTING.save();
        }));
        items.add((x, y) -> slider(x, y, w, "Light strength", FAClientConfig.LIGHT_STRENGTH, 0.1, 2.0, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> toggle(x, y, w, "Sun rays", FAClientConfig.SUN_RAYS));
        items.add((x, y) -> toggle(x, y, w, "Reflections (traced)", FAClientConfig.SCREEN_REFLECTIONS));
        items.add((x, y) -> toggle(x, y, w, "Sun shadows", FAClientConfig.SUN_SHADOWS));
        items.add((x, y) -> toggle(x, y, w, "Breaking chunks", FAClientConfig.BREAK_CHUNKS));
        items.add((x, y) -> toggle(x, y, w, "Dark container screens", FAClientConfig.DARK_CONTAINERS));
        items.add((x, y) -> toggle(x, y, w, "Pickup feed", FAClientConfig.PICKUP_FEED));
        items.add((x, y) -> toggle(x, y, w, "Cinematic grade + bloom", FAClientConfig.CINEMATIC));
        items.add((x, y) -> toggle(x, y, w, "Distance haze", FAClientConfig.HAZE));
        items.add((x, y) -> toggle(x, y, w, "Aim depth of field", FAClientConfig.AIM_DOF));
        items.add((x, y) -> toggle(x, y, w, "Glints (old)", FAClientConfig.REFLECTIONS));
        items.add((x, y) -> toggle(x, y, w, "Smoke", FAClientConfig.SMOKE));
        items.add((x, y) -> slider(x, y, w, "Smoke amount", FAClientConfig.SMOKE_AMOUNT, 0.1, 3.0, v -> String.format("x%.1f", v)));
        items.add((x, y) -> toggle(x, y, w, "Voxel debris", FAClientConfig.DEBRIS));
        items.add((x, y) -> intSlider(x, y, w, "Most debris", FAClientConfig.MAX_DEBRIS, 50, 5000));
        items.add((x, y) -> toggle(x, y, w, "Water effects", FAClientConfig.WATER));
        items.add((x, y) -> slider(x, y, w, "Foam amount", FAClientConfig.WATER_AMOUNT, 0.1, 3.0, v -> String.format("x%.1f", v)));
        items.add((x, y) -> toggle(x, y, w, "Realism camera (F8)", FAClientConfig.REALISM));
        items.add((x, y) -> slider(x, y, w, "Fisheye", FAClientConfig.FISHEYE, 0.0, 2.5, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> slider(x, y, w, "Film grain", FAClientConfig.GRAIN, 0.0, 3.0, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> slider(x, y, w, "Desaturate", FAClientConfig.DESATURATE, 0.0, 1.0, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> slider(x, y, w, "Vignette", FAClientConfig.VIGNETTE, 0.0, 2.5, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> toggle(x, y, w, "Dread sounds", FAClientConfig.DREAD));
        items.add((x, y) -> toggle(x, y, w, "Ambience", FAClientConfig.AMBIENCE));
        items.add((x, y) -> toggle(x, y, w, "Smooth guns", FAClientConfig.GUN_MESHES));
        items.add((x, y) -> toggle(x, y, w, "Inventory animation", FAClientConfig.TARKOV_ANIMATION));
        items.add((x, y) -> toggle(x, y, w, "Safe mode (all off)", FAClientConfig.SAFE_MODE));
        items.add((x, y) -> cycle(x, y, w, "Parallax depth", () -> DepthShaders.enabled() ? "On" : "Off", () -> {
            status = Component.literal("Reloading the game's resources...");
            DepthShaders.set(!DepthShaders.enabled());
        }));
        items.add((x, y) -> slider(x, y, w, "Depth smoothing", FAClientConfig.DEPTH_SMOOTH, 0.0, 1.0, v -> String.format("%.0f%%", v * 100)));
        items.add((x, y) -> slider(x, y, w, "Depth strength", FAClientConfig.DEPTH_STRENGTH, 0.0, 2.0, v -> String.format("%.0f%%", v * 100)));
        int rows = (items.size() + cols - 1) / cols;
        int step = Math.min(24, Math.max(21, (height - 70) / Math.max(1, rows)));
        int y0 = Math.max(26, Math.min(40, height - 36 - rows * step));
        for (int i = 0; i < items.size(); i++) {
            int c = i % cols, r = i / cols;
            addRenderableWidget(items.get(i).apply(x0 + c * (w + gap), y0 + r * step));
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private Button toggle(int x, int y, int w, String name, ForgeConfigSpec.BooleanValue value) {
        return cycle(x, y, w, name, () -> value.get() ? "On" : "Off", () -> {
            value.set(!value.get());
            value.save();
        });
    }

    private Button cycle(int x, int y, int w, String name, java.util.function.Supplier<String> state, Runnable next) {
        Button[] self = new Button[1];
        self[0] = Button.builder(Component.literal(name + ": " + state.get()), b -> {
            next.run();
            b.setMessage(Component.literal(name + ": " + state.get()));
        }).bounds(x, y, w, 20).build();
        return self[0];
    }

    /** A slider being dragged: committed when the mouse is let go anywhere (not only over it), and when the screen closes. */
    @org.jetbrains.annotations.Nullable static Runnable pendingCommit;

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean r = super.mouseReleased(mx, my, button);
        if (pendingCommit != null) pendingCommit.run();
        return r;
    }

    @Override
    public void removed() {
        if (pendingCommit != null) pendingCommit.run();
        super.removed();
    }

    private static AbstractSliderButton slider(int x, int y, int w, String name, ForgeConfigSpec.DoubleValue cfg, double min, double max, DoubleFunction<String> fmt) {
        return new AbstractSliderButton(x, y, w, 20, Component.empty(), (cfg.get() - min) / (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(name + ": " + fmt.apply(min + this.value * (max - min))));
            }

            // Forge's config file saves itself on every set(); doing that on every step of a drag races its file
            // watcher re-reading the half-written file and crashes the game. So: the value is written once, on release.
            @Override
            protected void applyValue() {
                if (!dragging) commit();
            }

            boolean dragging = false;

            @Override
            public void onClick(double mx, double my) {
                dragging = true;   // a click or a drag: written when the button's let go
                pendingCommit = () -> { dragging = false; commit(); };
                super.onClick(mx, my);
            }

            @Override
            protected void onDrag(double mx, double my, double dx, double dy) {
                dragging = true;
                super.onDrag(mx, my, dx, dy);
            }

            @Override
            public void onRelease(double mx, double my) {
                super.onRelease(mx, my);
                dragging = false;
                commit();
            }

            void commit() {
                double v = Mth.clamp(min + this.value * (max - min), min, max);
                if (Math.abs(v - cfg.get()) > 1e-9) {
                    cfg.set(v);
                    cfg.save();
                }
                pendingCommit = null;
            }
        };
    }

    private static AbstractSliderButton intSlider(int x, int y, int w, String name, ForgeConfigSpec.IntValue cfg, int min, int max) {
        return new AbstractSliderButton(x, y, w, 20, Component.empty(), (cfg.get() - min) / (double) (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(name + ": " + (int) Math.round(min + this.value * (max - min))));
            }

            @Override
            protected void applyValue() {
                if (!dragging) commit();
            }

            boolean dragging = false;

            @Override
            public void onClick(double mx, double my) {
                dragging = true;   // a click or a drag: written when the button's let go
                pendingCommit = () -> { dragging = false; commit(); };
                super.onClick(mx, my);
            }

            @Override
            protected void onDrag(double mx, double my, double dx, double dy) {
                dragging = true;
                super.onDrag(mx, my, dx, dy);
            }

            @Override
            public void onRelease(double mx, double my) {
                super.onRelease(mx, my);
                dragging = false;
                commit();
            }

            void commit() {
                int v = (int) Math.round(min + this.value * (max - min));
                if (v != cfg.get()) {
                    cfg.set(v);
                    cfg.save();
                }
                pendingCommit = null;
            }
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 16, 0xFFE68C);
        g.drawCenteredString(font, Component.literal("Every effect is optional and switches itself off if anything goes wrong."), width / 2, 27, 0xA0A0A0);
        g.drawCenteredString(font, status, width / 2, height - 48, 0x8BE38F);
        if (dev.factoryascension.client.render.AscensionLighting.shaderPackActive())
            g.drawCenteredString(font, Component.literal("A shader pack is on, so Ascension Lighting and parallax step aside for it."), width / 2, height - 60, 0xE8B03A);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
        // The depth sliders are baked into the depth pack's shaders: reload them if they changed
        if (DepthShaders.enabled() && DepthShaders.settingsChanged()) Minecraft.getInstance().reloadResourcePacks();
    }
}
