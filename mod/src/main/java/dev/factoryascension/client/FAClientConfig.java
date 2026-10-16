package dev.factoryascension.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client options (config/factoryascension-client.toml). */
public final class FAClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue REFLECTIONS;
    public static final ForgeConfigSpec.IntValue RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_GLINTS;
    public static final ForgeConfigSpec.DoubleValue STRENGTH;
    public static final ForgeConfigSpec.BooleanValue CONFLICT_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue SCREEN_REFLECTIONS, SUN_SHADOWS, CINEMATIC, HAZE, AIM_DOF, AMBIENCE;
    public static final ForgeConfigSpec.BooleanValue SAFE_MODE, SUN_RAYS, SMOKE, DEBRIS, WATER, OCULUS_KEY_MOVED, DEPTH_ADDED, PACK_DEFAULTS, TARKOV_ANIMATION;
    public static final ForgeConfigSpec.BooleanValue GUN_MESHES, REALISM, DREAD;
    public static final ForgeConfigSpec.DoubleValue FISHEYE, GRAIN, DESATURATE, VIGNETTE, DEPTH_SMOOTH, DEPTH_STRENGTH;
    public static final ForgeConfigSpec.IntValue LIGHTING, MAX_DEBRIS;
    public static final ForgeConfigSpec.DoubleValue LIGHT_STRENGTH, SMOKE_AMOUNT, WATER_AMOUNT;

    /** An effect is on: its own switch, and not in safe mode. */
    public static boolean on(ForgeConfigSpec.BooleanValue v) {
        return v.get() && !SAFE_MODE.get();
    }

    /** Smooth gun meshes up close (off: the cuboid models everywhere). */
    public static boolean gunMeshes() {
        return GUN_MESHES.get() && !SAFE_MODE.get();
    }

    public static int lighting() {
        return SAFE_MODE.get() ? 0 : LIGHTING.get();
    }

    private static final java.util.Set<String> RENDER_FAILED = new java.util.HashSet<>();

    /** Something failed while drawing: logged once per kind, and that one thing is skipped (the game keeps going). */
    public static void renderFailed(String what, Throwable err) {
        if (RENDER_FAILED.add(what)) org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("{} failed; skipping it", what, err);
    }

    /** Called when an effect fails: everything off, saved, and said once in chat. */
    public static void failSafe(String what, Throwable err) {
        org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Graphics: {} failed, turning effects off (safe mode)", what, err);
        if (SAFE_MODE.get()) return;
        SAFE_MODE.set(true);
        SAFE_MODE.save();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            "Factory Ascension: " + what + " failed, so its graphics are off (safe mode). Pause menu > Factory Ascension to try again.")
            .withStyle(net.minecraft.ChatFormatting.YELLOW), false);
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Glints: lights (and the sun) glint off shiny blocks as you move. Cheap, no shaders (the old reflections; Ascension Lighting's are better).").push("reflections");
        REFLECTIONS = b.comment("Turn it on or off (it also turns itself off if a shader mod is installed)").define("glints", false);
        RADIUS = b.comment("How far around you shiny blocks catch light (blocks)").defineInRange("radius", 12, 4, 32);
        MAX_GLINTS = b.comment("Most glints drawn per frame").defineInRange("maxGlints", 400, 16, 4000);
        STRENGTH = b.comment("How bright the glints are").defineInRange("strength", 1.0, 0.1, 3.0);
        b.pop();
        CONFLICT_MESSAGES = b.comment("Say in chat when another mod turned one of these features off").define("conflictMessages", true);
        b.comment("Ascension graphics (the pause menu's Factory Ascension button changes all of these)").push("graphics");
        SAFE_MODE = b.comment("Turns every effect below off. Set by itself if an effect ever fails, so the game keeps running").define("safeMode", false);
        LIGHTING = b.comment("Ascension Lighting: screen-space traced contact shadows, bounced light and sun rays. 0 off, 1 low, 2 medium, 3 high")
            .defineInRange("lighting", 3, 0, 3);
        LIGHT_STRENGTH = b.comment("How strong the shadows and bounced light are").defineInRange("lightingStrength", 1.0, 0.1, 2.0);
        SCREEN_REFLECTIONS = b.comment("Ascension Lighting: ray-traced reflections on water, wet ground in the rain, and the sky's sheen on flat ground").define("screenReflections", true);
        AMBIENCE = b.comment("A living soundscape: wind (stronger up high and in storms), birdsong by day, crickets at night, drips and rumbles underground").define("ambience", true);
        CINEMATIC = b.comment("Ascension Lighting: a filmic tone curve (highlights roll off like film) and bloom round bright lights and sky").define("cinematic", true);
        HAZE = b.comment("Ascension Lighting: aerial perspective, distant land fading into a sunlit haze").define("haze", true);
        AIM_DOF = b.comment("Ascension Lighting: depth of field while aiming down sights (what's far behind your target softens)").define("aimDepthOfField", true);
        SUN_SHADOWS = b.comment("Ascension Lighting: shadows cast by the sun (and moon) from whatever's on screen").define("sunShadows", true);
        SUN_RAYS = b.comment("Light shafts from the sun and moon").define("sunRays", true);
        SMOKE = b.comment("Real smoke from explosions, gunfire and fires").define("smoke", true);
        SMOKE_AMOUNT = b.comment("How much smoke").defineInRange("smokeAmount", 3.0, 0.1, 3.0);
        DEBRIS = b.comment("Broken blocks shatter into bouncing voxel pieces").define("debris", true);
        MAX_DEBRIS = b.comment("Most voxel pieces at once").defineInRange("maxDebris", 800, 50, 5000);
        WATER = b.comment("Water foams where it flows and falls, bubbles, and splashes").define("water", true);
        WATER_AMOUNT = b.comment("How much foam and bubbles").defineInRange("waterAmount", 1.0, 0.1, 3.0);
        GUN_MESHES = b.comment("Guns up close are smooth, high-detail meshes (off: the blocky models, for very slow computers)").define("gunMeshes", true);
        DEPTH_SMOOTH = b.comment("Depth pack: how smooth the relief on blocks and mobs is (0 sharp, per pixel; 1 soft, rounded)").defineInRange("depthSmoothing", 0.5, 0.0, 1.0);
        DEPTH_STRENGTH = b.comment("Depth pack: how deep the relief goes").defineInRange("depthStrength", 1.0, 0.0, 2.0);
        b.pop();
        b.comment("Realism camera: a lens look. Slight fisheye, less colour, film grain, a dark vignette. Toggle in game with F8").push("realism");
        REALISM = b.comment("Turn it on or off").define("enabled", true);
        FISHEYE = b.comment("How much the lens bends the edges of the picture").defineInRange("fisheye", 1.0, 0.0, 2.5);
        GRAIN = b.comment("How much film grain").defineInRange("grain", 1.0, 0.0, 3.0);
        DESATURATE = b.comment("How much colour is taken out (0 none, 1 grey)").defineInRange("desaturate", 0.35, 0.0, 1.0);
        VIGNETTE = b.comment("How dark the corners get").defineInRange("vignette", 1.0, 0.0, 2.5);
        b.pop();
        DREAD = b.comment("Dread: the world out past your base is frightening. Distant gunfire, footsteps, your heartbeat and breathing under stress, ringing ears after loud shots")
            .define("dread", true);
        TARKOV_ANIMATION = b.comment("Tarkov inventory: look down, a duffel bag comes up in your hands and the inventory opens out of it (off: it just opens)")
            .define("inventoryAnimation", true);
        PACK_DEFAULTS = b.comment("Set after the pack's default video, mouse and key settings were applied the first time (your changes stand after that)").define("packDefaultsApplied", false);
        DEPTH_ADDED = b.comment("Set after the Factory Ascension Depth pack was switched on the first time (your choice stands after that)").define("depthPackAdded", false);
        OCULUS_KEY_MOVED = b.comment("Set after the shotgun's R stopped also reloading Oculus shaders (done once)").define("oculusReloadKeyMoved", false);
        SPEC = b.build();
    }

    private FAClientConfig() {
    }
}
