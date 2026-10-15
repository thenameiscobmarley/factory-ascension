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
    public static final ForgeConfigSpec.BooleanValue SAFE_MODE, SUN_RAYS, SMOKE, DEBRIS, WATER, OCULUS_KEY_MOVED;
    public static final ForgeConfigSpec.IntValue LIGHTING, MAX_DEBRIS;
    public static final ForgeConfigSpec.DoubleValue LIGHT_STRENGTH, SMOKE_AMOUNT, WATER_AMOUNT;

    /** An effect is on: its own switch, and not in safe mode. */
    public static boolean on(ForgeConfigSpec.BooleanValue v) {
        return v.get() && !SAFE_MODE.get();
    }

    public static int lighting() {
        return SAFE_MODE.get() ? 0 : LIGHTING.get();
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
        b.comment("Shiny Reflections: lights (and the sun) glint off shiny blocks as you move. Cheap: no shaders.").push("reflections");
        REFLECTIONS = b.comment("Turn it on or off (it also turns itself off if a shader mod is installed)").define("enabled", true);
        RADIUS = b.comment("How far around you shiny blocks catch light (blocks)").defineInRange("radius", 12, 4, 32);
        MAX_GLINTS = b.comment("Most glints drawn per frame").defineInRange("maxGlints", 400, 16, 4000);
        STRENGTH = b.comment("How bright the glints are").defineInRange("strength", 1.0, 0.1, 3.0);
        b.pop();
        CONFLICT_MESSAGES = b.comment("Say in chat when another mod turned one of these features off").define("conflictMessages", true);
        b.comment("Ascension graphics (the pause menu's Factory Ascension button changes all of these)").push("graphics");
        SAFE_MODE = b.comment("Turns every effect below off. Set by itself if an effect ever fails, so the game keeps running").define("safeMode", false);
        LIGHTING = b.comment("Ascension Lighting: screen-space traced contact shadows, bounced light and sun rays. 0 off, 1 low, 2 medium, 3 high")
            .defineInRange("lighting", 2, 0, 3);
        LIGHT_STRENGTH = b.comment("How strong the shadows and bounced light are").defineInRange("lightingStrength", 1.0, 0.1, 2.0);
        SUN_RAYS = b.comment("Light shafts from the sun and moon").define("sunRays", true);
        SMOKE = b.comment("Real smoke from explosions, gunfire and fires").define("smoke", true);
        SMOKE_AMOUNT = b.comment("How much smoke").defineInRange("smokeAmount", 1.0, 0.1, 3.0);
        DEBRIS = b.comment("Broken blocks shatter into bouncing voxel pieces").define("debris", true);
        MAX_DEBRIS = b.comment("Most voxel pieces at once").defineInRange("maxDebris", 800, 50, 5000);
        WATER = b.comment("Water foams where it flows and falls, bubbles, and splashes").define("water", true);
        WATER_AMOUNT = b.comment("How much foam and bubbles").defineInRange("waterAmount", 1.0, 0.1, 3.0);
        b.pop();
        OCULUS_KEY_MOVED = b.comment("Set after the shotgun's R stopped also reloading Oculus shaders (done once)").define("oculusReloadKeyMoved", false);
        SPEC = b.build();
    }

    private FAClientConfig() {
    }
}
