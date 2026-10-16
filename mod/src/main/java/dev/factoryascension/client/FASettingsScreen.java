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
                event.addListener(Button.builder(Component.literal("Factory Ascension..."), b -> Minecraft.getInstance().setScreen(new FASettingsScreen(pause)))
                    .bounds(6, 6, 130, 20).build());
        }
    }

    @Override
    protected void init() {
        int w = 150, gap = 4, left = width / 2 - w - gap / 2, right = width / 2 + gap / 2;
        int y = 40;
        addRenderableWidget(cycle(left, y, w, "Lighting", () -> QUALITY[FAClientConfig.LIGHTING.get()], () -> {
            FAClientConfig.LIGHTING.set((FAClientConfig.LIGHTING.get() + 1) % 4);
            FAClientConfig.LIGHTING.save();
        }));
        addRenderableWidget(slider(right, y, w, "Light strength", FAClientConfig.LIGHT_STRENGTH, 0.1, 2.0, v -> String.format("%.0f%%", v * 100)));
        y += 24;
        addRenderableWidget(toggle(left, y, w, "Sun rays", FAClientConfig.SUN_RAYS));
        addRenderableWidget(toggle(right, y, w, "Shiny Reflections", FAClientConfig.REFLECTIONS));
        y += 24;
        addRenderableWidget(toggle(left, y, w, "Smoke", FAClientConfig.SMOKE));
        addRenderableWidget(slider(right, y, w, "Smoke amount", FAClientConfig.SMOKE_AMOUNT, 0.1, 3.0, v -> String.format("x%.1f", v)));
        y += 24;
        addRenderableWidget(toggle(left, y, w, "Voxel debris", FAClientConfig.DEBRIS));
        addRenderableWidget(intSlider(right, y, w, "Most debris", FAClientConfig.MAX_DEBRIS, 50, 5000));
        y += 24;
        addRenderableWidget(toggle(left, y, w, "Water effects", FAClientConfig.WATER));
        addRenderableWidget(slider(right, y, w, "Foam amount", FAClientConfig.WATER_AMOUNT, 0.1, 3.0, v -> String.format("x%.1f", v)));
        y += 32;
        addRenderableWidget(toggle(left, y, w, "Safe mode (all off)", FAClientConfig.SAFE_MODE));
        addRenderableWidget(cycle(right, y, w, "Parallax depth", () -> DepthShaders.enabled() ? "On" : "Off", () -> {
            status = Component.literal("Reloading the game's resources...");
            DepthShaders.set(!DepthShaders.enabled());
        }));
        y += 24;
        addRenderableWidget(toggle(left, y, w, "Inventory animation", FAClientConfig.TARKOV_ANIMATION));
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width / 2 - 100, height - 30, 200, 20).build());
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

    private static AbstractSliderButton slider(int x, int y, int w, String name, ForgeConfigSpec.DoubleValue cfg, double min, double max, DoubleFunction<String> fmt) {
        return new AbstractSliderButton(x, y, w, 20, Component.empty(), (cfg.get() - min) / (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(name + ": " + fmt.apply(min + this.value * (max - min))));
            }

            @Override
            protected void applyValue() {
                cfg.set(Mth.clamp(min + this.value * (max - min), min, max));
                cfg.save();
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
                cfg.set((int) Math.round(min + this.value * (max - min)));
                cfg.save();
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
    }
}
