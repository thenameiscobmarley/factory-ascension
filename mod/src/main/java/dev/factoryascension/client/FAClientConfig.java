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

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Shiny Reflections: lights (and the sun) glint off shiny blocks as you move. Cheap: no shaders.").push("reflections");
        REFLECTIONS = b.comment("Turn it on or off (it also turns itself off if a shader mod is installed)").define("enabled", true);
        RADIUS = b.comment("How far around you shiny blocks catch light (blocks)").defineInRange("radius", 12, 4, 32);
        MAX_GLINTS = b.comment("Most glints drawn per frame").defineInRange("maxGlints", 400, 16, 4000);
        STRENGTH = b.comment("How bright the glints are").defineInRange("strength", 1.0, 0.1, 3.0);
        b.pop();
        CONFLICT_MESSAGES = b.comment("Say in chat when another mod turned one of these features off").define("conflictMessages", true);
        SPEC = b.build();
    }

    private FAClientConfig() {
    }
}
