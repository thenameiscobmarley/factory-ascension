package dev.factoryascension;

import net.minecraftforge.common.ForgeConfigSpec;

/** Options for both sides (config/factoryascension-common.toml). */
public final class FACommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BACKUPS;
    public static final ForgeConfigSpec.IntValue KEEP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("World backups: each time a world closes, it is zipped into backups/factoryascension/ (in the game folder),",
            "so if it's ever opened without a mod it needs (and loses that mod's blocks and items), the last files are safe.").push("backups");
        BACKUPS = b.comment("Back up the world when it closes").define("enabled", true);
        KEEP = b.comment("How many backups to keep per world (the oldest are deleted)").defineInRange("keep", 3, 1, 50);
        b.pop();
        SPEC = b.build();
    }

    private FACommonConfig() {
    }
}
