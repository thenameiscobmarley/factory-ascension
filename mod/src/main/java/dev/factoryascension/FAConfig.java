package dev.factoryascension;

/**
 * Numbers for the mod's machines. The pack's KubeJS config (section `machines`) pushes its values in at startup
 * (startup_scripts/97_fa_mod.js), so balance stays in the config; these defaults only apply without the scripts.
 */
public final class FAConfig {
    // Super Smelter, per tier: items smelted at once, and stress (SU per RPM)
    public static int[] smelterLanes = {4, 8, 16, 32};
    public static float[] smelterStress = {4, 6, 8, 12};
    /** Ticks per batch at 64 RPM (faster spins smelt faster). */
    public static int smelterCookTicks = 40;
    /** Below this speed the smelter doesn't run. */
    public static float smelterMinRpm = 8;

    // Modular Engine
    public static float engineSuPerCoil = 256;
    public static int engineCoilsPerBurner = 4;
    public static int engineMaxModules = 32;
    /** RPM with 0, 1, 2 ... Gearbox Modules. */
    public static int[] engineRpm = {16, 32, 64, 128, 256};
    /** Fuel lasts this many times its furnace burn time. */
    public static float engineFuelMultiplier = 1;

    // Universal Storage Terminal: how far its linked chests may be (0 = any distance, same dimension) and how many
    public static int storageRange = 128;
    public static int storageMaxLinks = 128;

    /** How often (ticks) the Salvager asks the scripts to salvage. */
    public static int salvagerTicks = 20;

    private FAConfig() {
    }

    // ---- Setters for KubeJS (one value at a time: Rhino arrays don't convert to Java arrays cleanly) ----
    public static void setSmelterTier(int tier, int lanes, double stress) {
        if (tier < 0 || tier >= smelterLanes.length) return;
        smelterLanes[tier] = Math.max(1, lanes);
        smelterStress[tier] = (float) Math.max(0, stress);
    }

    public static void setSmelter(int cookTicks, double minRpm) {
        smelterCookTicks = Math.max(1, cookTicks);
        smelterMinRpm = (float) Math.max(0, minRpm);
    }

    public static void setEngine(double suPerCoil, int coilsPerBurner, int maxModules, double fuelMultiplier) {
        engineSuPerCoil = (float) Math.max(0, suPerCoil);
        engineCoilsPerBurner = Math.max(1, coilsPerBurner);
        engineMaxModules = Math.max(1, maxModules);
        engineFuelMultiplier = (float) Math.max(0.01, fuelMultiplier);
    }

    public static void setEngineRpm(int gears, int rpm) {
        if (gears < 0 || gears >= engineRpm.length) return;
        engineRpm[gears] = Math.max(1, Math.min(256, rpm));
    }

    public static void setStorage(int range, int maxLinks) {
        storageRange = Math.max(0, range);
        storageMaxLinks = Math.max(1, maxLinks);
    }

    public static void setSalvager(int ticks) {
        salvagerTicks = Math.max(1, ticks);
    }
}
