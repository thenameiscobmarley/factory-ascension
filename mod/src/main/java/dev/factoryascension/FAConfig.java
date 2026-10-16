package dev.factoryascension;

/**
 * Numbers for the mod's machines. The pack's KubeJS config (section `machines`) pushes its values in at startup
 * (startup_scripts/97_fa_mod.js), so balance stays in the config; these defaults only apply without the scripts.
 */
public final class FAConfig {
    // Super Smelter, per tier: items smelted at once, and stress (SU per RPM)
    public static int[] smelterLanes = {4, 8, 16, 32, 64, 128};
    public static float[] smelterStress = {4, 6, 8, 12, 16, 24};
    /** Ticks per batch at 64 RPM (faster spins smelt faster). */
    public static int smelterCookTicks = 40;
    /** Below this speed the smelter doesn't run. */
    public static float smelterMinRpm = 8;

    // Ascension Quarry: square radius, work per block (RPM adds up each tick), most blocks a tick, slowest speed, stress
    public static int quarryRadius = 8;
    public static float quarryWork = 128;
    public static int quarryMaxPerTick = 8;
    public static float quarryMinRpm = 8;
    public static float quarryStress = 16;
    // Mob Grinder: reach in front, damage per RPM, ticks between hits at 64 RPM, slowest speed, stress
    public static int grinderReach = 5;
    public static float grinderDamagePerRpm = 0.5f;
    public static int grinderInterval = 20;
    public static float grinderMinRpm = 8;
    public static float grinderStress = 12;

    public static void setQuarry(int radius, double work, int maxPerTick, double minRpm, double stress) {
        quarryRadius = Math.max(0, Math.min(64, radius));
        quarryWork = (float) Math.max(1, work);
        quarryMaxPerTick = Math.max(1, maxPerTick);
        quarryMinRpm = (float) Math.max(0, minRpm);
        quarryStress = (float) Math.max(0, stress);
    }

    public static void setGrinder(int reach, double damagePerRpm, int interval, double minRpm, double stress) {
        grinderReach = Math.max(1, Math.min(16, reach));
        grinderDamagePerRpm = (float) Math.max(0, damagePerRpm);
        grinderInterval = Math.max(1, interval);
        grinderMinRpm = (float) Math.max(0, minRpm);
        grinderStress = (float) Math.max(0, stress);
    }

    // Shotgun (item/ShotgunItem): pellets a shot, damage each, spread in degrees (hip / aimed), range, full damage
    // within, damage left at max range, ticks between shots, shells held, ticks per shell reloaded, kick, glass
    public static int gunPellets = 8;
    public static float gunPelletDamage = 5;
    public static float gunSpreadHip = 7, gunSpreadAim = 2.5f;
    public static float gunRange = 40, gunFullDamageRange = 10, gunFalloff = 0.35f;
    public static int gunCooldown = 16, gunCapacity = 6, gunReloadTicks = 10;
    public static float gunKnockback = 0.15f;
    public static boolean gunBreaksGlass = true;

    public static void setShotgun(int pellets, double damage, double spreadHip, double spreadAim, double range, double fullRange, double falloff,
                                  int cooldown, int capacity, int reloadTicks, double knockback, boolean breaksGlass) {
        gunPellets = Math.max(1, Math.min(64, pellets));
        gunPelletDamage = (float) Math.max(0, damage);
        gunSpreadHip = (float) Math.max(0, Math.min(45, spreadHip));
        gunSpreadAim = (float) Math.max(0, Math.min(45, spreadAim));
        gunRange = (float) Math.max(1, Math.min(256, range));
        gunFullDamageRange = (float) Math.max(0, fullRange);
        gunFalloff = (float) Math.max(0, Math.min(1, falloff));
        gunCooldown = Math.max(1, cooldown);
        gunCapacity = Math.max(1, Math.min(64, capacity));
        gunReloadTicks = Math.max(1, reloadTicks);
        gunKnockback = (float) Math.max(0, knockback);
        gunBreaksGlass = breaksGlass;
    }

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

    // Legendary stars, for the Ascension Workbench screen: the most stars, each star's cost ([item id, count]) and grade names
    public static int legendMaxStars = 10;
    public static final java.util.List<java.util.List<String[]>> legendCosts = new java.util.ArrayList<>();
    public static final java.util.List<String> legendGrades = new java.util.ArrayList<>();

    // Mini Crusher: ticks per item at speed x1, battery charge per item, and the motors and batteries it accepts
    public static int crusherTicks = 100;
    public static int crusherCharge = 2;
    public static final java.util.Map<String, Float> motors = new java.util.LinkedHashMap<>();
    public static final java.util.Set<String> batteries = new java.util.LinkedHashSet<>();

    // Ascension backpacks (with Sophisticated Backpacks): storage and upgrade slots per tier
    public static final int[] backpackSlots = {162, 270, 432, 648, 1080, 1728};
    public static final int[] backpackUpgrades = {7, 8, 9, 10, 12, 14};

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

    public static void setLegendary(int maxStars) {
        legendMaxStars = Math.max(1, maxStars);
        legendCosts.clear();
        legendGrades.clear();
    }

    public static void addLegendCost(int star, String item, int count) {
        while (legendCosts.size() <= star) legendCosts.add(new java.util.ArrayList<>());
        legendCosts.get(star).add(new String[]{item, String.valueOf(Math.max(1, count))});
    }

    public static void addLegendGrade(String name) {
        legendGrades.add(name);
    }

    public static void setMiniCrusher(int ticksPerItem, int chargePerItem) {
        crusherTicks = Math.max(1, ticksPerItem);
        crusherCharge = Math.max(0, chargePerItem);
        motors.clear();
        batteries.clear();
    }

    public static void addMotor(String item, double speed) {
        motors.put(item, (float) Math.max(0.05, speed));
    }

    public static void addBattery(String item) {
        batteries.add(item);
    }

    public static void setBackpack(int tier, int slots, int upgrades) {
        if (tier < 0 || tier >= backpackSlots.length) return;
        backpackSlots[tier] = Math.max(9, slots);
        backpackUpgrades[tier] = Math.max(1, Math.min(32, upgrades));
    }

    public static void setSalvager(int ticks) {
        salvagerTicks = Math.max(1, ticks);
    }

    // Tarkov inventory (tarkov/): the main inventory is a grid where items take their real size. The pack's config
    // section `inventory`. Sizes are width x height in cells; weight is per cell (a full stack of a 1x1 item = one cell).
    public static boolean tarkovEnabled = true;
    public static int tarkovWidth = 10, tarkovHeight = 6;
    public static boolean tarkovBlockPickup = true;
    public static final java.util.Map<String, int[]> tarkovSizes = new java.util.HashMap<>();
    public static boolean weightEnabled = true;
    public static float weightPerCell = 1, weightCarry = 100, weightSlowPerKg = 0.01f, weightMaxSlow = 0.5f;

    public static void setTarkov(boolean enabled, int width, int height, boolean blockPickup) {
        tarkovEnabled = enabled;
        tarkovWidth = Math.max(3, Math.min(16, width));
        tarkovHeight = Math.max(3, Math.min(12, height));
        tarkovBlockPickup = blockPickup;
    }

    public static void setTarkovSize(String item, int w, int h) {
        tarkovSizes.put(item, new int[] {Math.max(1, Math.min(6, w)), Math.max(1, Math.min(6, h))});
    }

    public static void setWeight(boolean enabled, double perCell, double carry, double slowPerKg, double maxSlow) {
        weightEnabled = enabled;
        weightPerCell = (float) Math.max(0, perCell);
        weightCarry = (float) Math.max(0, carry);
        weightSlowPerKg = (float) Math.max(0, Math.min(1, slowPerKg));
        weightMaxSlow = (float) Math.max(0, Math.min(0.9, maxSlow));
    }

    // Flare gun: shots held, ticks between shots, the airdrop's wait (ticks), how high it falls from, its loot table
    public static int flareCooldown = 30, airdropDelay = 400, airdropHeight = 60;
    public static float flareSpeed = 1.6f;
    public static String airdropLoot = "factoryascension:chests/airdrop";

    public static void setFlareGun(int cooldown, double speed, int delayTicks, int height, String lootTable) {
        flareCooldown = Math.max(1, cooldown);
        flareSpeed = (float) Math.max(0.2, Math.min(4, speed));
        airdropDelay = Math.max(0, delayTicks);
        airdropHeight = Math.max(8, Math.min(200, height));
        airdropLoot = lootTable;
    }

    // Guns (gun/): reload fumbles under stress. Chance per reload step = base + stress^1.5 x stress factor.
    public static boolean fumblesEnabled = true;
    public static float fumbleBase = 0.02f, fumbleStress = 0.55f;
    public static boolean explosiveBreaksBlocks = false;

    public static void setFumbles(boolean enabled, double base, double stress) {
        fumblesEnabled = enabled;
        fumbleBase = (float) Math.max(0, Math.min(1, base));
        fumbleStress = (float) Math.max(0, Math.min(1, stress));
    }

    /** The pack's `guns.list` entry for one gun: scales its base numbers (gun/Guns.java). */
    public static void setGun(String id, double damage, double rpmScale, int magBonus, double spreadScale, double rangeScale, double recoilScale) {
        dev.factoryascension.gun.GunSpec g = dev.factoryascension.gun.Guns.BY_ID.get(id);
        if (g == null) return;
        if (!g.hasBase()) g.saveBase();
        g.resetToBase();
        g.damage *= (float) Math.max(0, damage);
        g.rpm = Math.max(10, (int) Math.round(g.rpm * Math.max(0.05, rpmScale)));
        g.mag = Math.max(1, g.mag + magBonus);
        g.spreadHip *= (float) Math.max(0, spreadScale);
        g.spreadAim *= (float) Math.max(0, spreadScale);
        g.range = (float) Math.max(4, Math.min(512, g.range * rangeScale));
        g.recoilUp *= (float) Math.max(0, recoilScale);
        g.recoilSide *= (float) Math.max(0, recoilScale);
    }

    // Weapons Workbench: rounds per press by caliber; what an attachment costs (cost x partItem, and tier x tierItem
    // for grade 2 and up); what the Jury-Rigged book takes
    public static final java.util.Map<String, Integer> ammoPerCraft = new java.util.HashMap<>();
    public static String benchPartItem = "minecraft:iron_ingot", benchTierItem = "kubejs:ascendite_ingot";

    public static void setAmmoPerCraft(String caliber, int n) {
        ammoPerCraft.put(caliber, Math.max(1, Math.min(64, n)));
    }

    public static void setBench(String partItem, String tierItem) {
        benchPartItem = partItem;
        benchTierItem = tierItem;
    }
}
