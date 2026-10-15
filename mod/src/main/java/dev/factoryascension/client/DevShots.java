package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.render.AscensionLighting;
import dev.factoryascension.client.render.FxParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer test run (only with -Dfa.devshots=true, never in a player's game): builds a scene in the loaded world, then
 * screenshots and times the graphics at every setting, sets off TNT for smoke and debris, opens the pause menu and the
 * settings screen, makes the depth pack, logs "[FA devshots]" lines and quits. Used to check the effects without a
 * person at the screen.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DevShots {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static final boolean ON = "true".equals(System.getProperty("fa.devshots"));
    /** -Dfa.devshots=pack: the shader-pack check (Oculus + a parallax shader + the depth pack) */
    private static final boolean PACK = "pack".equals(System.getProperty("fa.devshots"));
    private static int packStep = 0, packWait = 0, drawnBefore = 0;

    @SubscribeEvent
    public static void packTick(TickEvent.ClientTickEvent event) {
        if (!PACK || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (packWait > 0) { packWait--; return; }
        switch (packStep++) {
            case 0 -> { for (String c : SCENE) cmd(mc, c); cmd(mc, "tp @a 6.5 -58.4 1.6 0 10"); packWait = 400; }   // shaders take a while to compile
            case 1 -> { drawnBefore = AscensionLighting.framesDrawn; packWait = 60; }
            case 2 -> { log("shader pack active " + AscensionLighting.shaderPackActive() + "; Ascension Lighting frames while it was on: "
                    + (AscensionLighting.framesDrawn - drawnBefore) + "; resource packs " + mc.options.resourcePacks); shot(mc, "shaderpack_wall"); cmd(mc, "tp @a -2 -57.5 -6 0 12"); packWait = 80; }
            case 3 -> { shot(mc, "shaderpack_scene"); packWait = 10; }
            case 4 -> { log("done"); mc.stop(); }
            default -> { }
        }
    }
    private static int tick = 0, step = 0, wait = 0, waitedBoom = 0;
    private static long frameStart = 0, frameNanos = 0, lightNanos = 0;
    private static int frames = 0;
    private static final List<String> report = new ArrayList<>();

    private DevShots() {
    }

    private static void cmd(Minecraft mc, String c) {
        var server = mc.getSingleplayerServer();
        if (server != null) server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "fa_" + name + ".png", mc.getMainRenderTarget(), msg -> { });
        log("screenshot " + name);
    }

    private static void log(String s) {
        LOG.info("[FA devshots] {}", s);
        report.add(s);
    }

    // Frame timing: whole frames (render tick to render tick) and the lighting passes alone
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        if (frameStart > 0 && frames >= 0) { frameNanos += now - frameStart; frames++; }
        frameStart = now;
    }

    @SubscribeEvent
    public static void lighting(RenderLevelStageEvent event) {
        if (ON && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) lightNanos += AscensionLighting.lastNanos;
    }

    private static void resetTiming() {
        frameNanos = 0; lightNanos = 0; frames = 0; frameStart = 0;
    }

    private static String timing() {
        return frames == 0 ? "no frames" : String.format("%.1f ms/frame over %d frames (lighting CPU side %.2f ms)", frameNanos / 1e6 / frames, frames, lightNanos / 1e6 / Math.max(1, frames));
    }

    private static final String[] SCENE = {
        "time set 6000", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "gamerule doMobSpawning false",
        "fill 4 -60 4 9 -55 9 stone_bricks hollow", "fill 4 -60 6 4 -58 7 air", "fill 2 -55 2 11 -55 11 quartz_block",
        "fill -12 -61 -4 -4 -62 4 water", "fill -8 -60 6 -6 -52 8 cobblestone", "setblock -7 -51 7 water",
        "fill -3 -60 12 3 -57 12 red_wool", "fill 12 -60 2 13 -59 3 tnt", "fill 10 -60 -2 11 -57 -1 oak_planks",
        "gamemode spectator @a", "tp @a -2 -57.5 -6 0 12"
    };

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        tick++;
        if (wait > 0) { wait--; return; }
        try {
            switch (step++) {
                case 0 -> { for (String c : SCENE) cmd(mc, c); FAClientConfig.SAFE_MODE.set(false); wait = 100;
                    log("mods: oculus " + net.minecraftforge.fml.ModList.get().isLoaded("oculus") + ", embeddium " + net.minecraftforge.fml.ModList.get().isLoaded("embeddium")
                        + ", flywheel compat " + net.minecraftforge.fml.ModList.get().isLoaded("irisflw") + "; shader pack active " + AscensionLighting.shaderPackActive()); }
                case 1 -> { FAClientConfig.LIGHTING.set(0); resetTiming(); wait = 60; }
                case 2 -> { log("lighting off: " + timing()); shot(mc, "lighting_off"); FAClientConfig.LIGHTING.set(1); resetTiming(); wait = 60; }
                case 3 -> { log("lighting low: " + timing() + ", sun " + java.util.Arrays.toString(AscensionLighting.lastSun)); shot(mc, "lighting_low"); FAClientConfig.LIGHTING.set(2); resetTiming(); wait = 60; }
                case 4 -> { log("lighting medium: " + timing()); shot(mc, "lighting_medium"); FAClientConfig.LIGHTING.set(3); resetTiming(); wait = 60; }
                case 5 -> { log("lighting high: " + timing() + ", targets held " + AscensionLighting.targetsHeld() + ", frames drawn " + AscensionLighting.framesDrawn);
                    shot(mc, "lighting_high"); AscensionLighting.debugView = 1; step = 50; wait = 20; }
                case 50 -> { shot(mc, "debug_shadow"); AscensionLighting.debugView = 2; wait = 20; }
                case 51 -> { shot(mc, "debug_bounce"); AscensionLighting.debugView = 4; step = 52; wait = 20; }
                case 52 -> { shot(mc, "debug_copy"); AscensionLighting.debugView = 0; FAClientConfig.LIGHTING.set(0); step = 53; wait = 20; }
                case 53 -> { shot(mc, "lighting_off2"); FAClientConfig.LIGHTING.set(3); step = 54; wait = 20; }
                case 54 -> { shot(mc, "lighting_high2"); FAClientConfig.LIGHTING.set(2); step = 6;
                    cmd(mc, "time set 12400"); cmd(mc, "tp @a -2 -56 -6 90 -4"); wait = 60; }
                case 6 -> { log("sunset sun " + java.util.Arrays.toString(AscensionLighting.lastSun)); shot(mc, "sunset_rays"); cmd(mc, "time set 6000"); cmd(mc, "tp @a 12.5 -56 -7 0 18"); wait = 40; }
                case 7 -> { cmd(mc, "setblock 12 -60 4 redstone_block"); wait = 84; }
                case 8 -> { if (FxParticles.liveDebris() == 0 && wait == 0 && tick < 100000 && waitedBoom++ < 40) { step--; wait = 1; return; }
                    log("explosion: debris " + FxParticles.liveDebris() + ", smoke " + FxParticles.liveSmoke() + ", foam " + FxParticles.liveFoam()); wait = 6; }
                case 9 -> { shot(mc, "explosion"); step = 90; wait = 30; }
                case 90 -> { shot(mc, "explosion_after"); cmd(mc, "tp @a -4 -58 0 30 25"); step = 10; wait = 60; }
                case 10 -> { log("water: foam " + FxParticles.liveFoam()); shot(mc, "water"); mc.setScreen(new PauseScreen(true)); wait = 20; }
                case 11 -> { shot(mc, "pause_menu"); mc.setScreen(new FASettingsScreen(mc.screen)); wait = 20; }
                case 12 -> { shot(mc, "settings"); mc.setScreen(null);
                    DepthPack.generate(msg -> log("depth: " + msg)); wait = 40; }
                case 13 -> { if (DepthPack.running()) { step--; wait = 20; return; } wait = 100; }
                case 14 -> {
                    FAClientConfig.LIGHTING.set(0);
                    wait = 10;
                }
                case 15 -> { log("after turning lighting off, targets held " + AscensionLighting.targetsHeld() + "; safe mode " + FAClientConfig.SAFE_MODE.get());
                    AscensionLighting.free(); log("freed: targets held " + AscensionLighting.targetsHeld()); wait = 10; }
                case 16 -> { log("done"); mc.stop(); }
                default -> { }
            }
        } catch (Throwable err) {
            log("step " + (step - 1) + " failed: " + err);
            LOG.error("[FA devshots] failed", err);
            mc.stop();
        }
    }
}
