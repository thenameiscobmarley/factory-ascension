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
    /** The grid (items take their real size). Off: the game's own inventory layout in the Tarkov look. */
    public static boolean tarkovGrid = false;

    public static void setTarkovGrid(boolean grid) {
        tarkovGrid = grid;
    }
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

    // Gunsmith (the bench's GUNSMITH tab): what each gun costs to build, [item id, count] pairs; guns with none listed
    // use a cost by kind (gun/Bench.defaultCost). Skins: what putting one on costs.
    public static final java.util.Map<String, java.util.List<String[]>> gunCost = new java.util.HashMap<>();
    public static String skinItem = "minecraft:amethyst_shard";
    public static int skinCost = 2;

    public static void clearGunCost(String id) {
        gunCost.remove(id);
    }

    public static void addGunCost(String id, String item, int count) {
        gunCost.computeIfAbsent(id, k -> new java.util.ArrayList<>()).add(new String[] {item, String.valueOf(Math.max(1, Math.min(999, count)))});
    }

    public static void setSkins(String item, int cost) {
        skinItem = item;
        skinCost = Math.max(0, Math.min(64, cost));
    }

    // Rescue: a red flare may bring a helicopter (gun/Rescue)
    public static boolean rescueEnabled = true;
    public static float rescueChance = 0.7f;
    public static int rescueDelay = 1200;
    public static double rescueRadius = 12;

    public static void setRescue(boolean enabled, double chance, int delayTicks, double radius) {
        rescueEnabled = enabled;
        rescueChance = (float) Math.max(0, Math.min(1, chance));
        rescueDelay = Math.max(20, delayTicks);
        rescueRadius = Math.max(2, radius);
    }

    // The armed operators (npc/): how tough and deadly, where and how often they turn up
    public static float npcHealth = 1, npcDamage = 0.45f, npcSpread = 1, npcBleedChance = 0.2f, npcGunDrop = 0.85f, npcPatrolChance = 0.06f;
    public static boolean npcPatrols = true, npcHunters = true;
    public static int npcPatrolMinDanger = 1, npcMaxNearby = 14;

    public static void setNpcs(double health, double damage, double spread, double bleed, double gunDrop, boolean patrols, double patrolChance, int minDanger, int maxNearby, boolean hunters) {
        npcHealth = (float) Math.max(0.1, health);
        npcDamage = (float) Math.max(0, damage);
        npcSpread = (float) Math.max(0.05, spread);
        npcBleedChance = (float) Math.max(0, Math.min(1, bleed));
        npcGunDrop = (float) Math.max(0, Math.min(1, gunDrop));
        npcPatrols = patrols;
        npcPatrolChance = (float) Math.max(0, Math.min(1, patrolChance));
        npcPatrolMinDanger = Math.max(0, minDanger);
        npcMaxNearby = Math.max(0, maxNearby);
        npcHunters = hunters;
    }

    // Monsters hear gunfire (gun/MobHearing): on or off, and how far they hear compared with the sound's reach
    public static boolean mobsHearShots = true;
    public static double mobHearingScale = 0.8;

    public static void setMobHearing(boolean on, double scale) {
        mobsHearShots = on;
        mobHearingScale = Math.max(0, scale);
    }

    // The Chem Lab's recipes (drugs/DrugBlocks): inputs [item, count], an output and how many
    public record ChemRecipe(java.util.List<java.util.Map.Entry<String, Integer>> inputs, String output, int count) {
    }

    /** Chance breaking grass drops a cannabis or coca seed. */
    public static float drugSeedChance = 0.03f;

    public static void setDrugSeeds(double chance) {
        drugSeedChance = (float) Math.max(0, Math.min(1, chance));
    }

    public static final java.util.List<ChemRecipe> chemRecipes = new java.util.ArrayList<>();
    private static java.util.List<java.util.Map.Entry<String, Integer>> pending = new java.util.ArrayList<>();

    public static void clearChem() {
        chemRecipes.clear();
        pending = new java.util.ArrayList<>();
    }

    public static void chemInput(String item, int count) {
        pending.add(java.util.Map.entry(item, Math.max(1, count)));
    }

    public static void chemOutput(String item, int count) {
        chemRecipes.add(new ChemRecipe(pending, item, Math.max(1, count)));
        pending = new java.util.ArrayList<>();
    }

    // The Chem Lab's recipes without the pack (the pack's config, drugs.lab, replaces these)
    static {
        chemInput("factoryascension:coca_leaf", 4); chemInput("minecraft:gunpowder", 1); chemOutput("factoryascension:cocaine", 2);
        chemInput("minecraft:redstone", 2); chemInput("minecraft:gunpowder", 2); chemInput("minecraft:sugar", 1); chemInput("minecraft:glowstone_dust", 1); chemOutput("factoryascension:methamphetamine", 2);
        chemInput("minecraft:poppy", 3); chemInput("minecraft:glass_bottle", 1); chemOutput("factoryascension:morphine", 2);
        chemInput("minecraft:glass_bottle", 1); chemInput("minecraft:sugar", 2); chemInput("minecraft:redstone", 1); chemOutput("factoryascension:adrenaline_shot", 2);
        chemInput("minecraft:red_mushroom", 2); chemInput("minecraft:brown_mushroom", 2); chemInput("minecraft:fermented_spider_eye", 1); chemOutput("factoryascension:magic_mushrooms", 3);
        chemInput("minecraft:wheat", 3); chemInput("minecraft:brown_mushroom", 1); chemInput("minecraft:paper", 1); chemInput("minecraft:glass_bottle", 1); chemOutput("factoryascension:lsd_tab", 4);
        chemInput("minecraft:sugar", 2); chemInput("minecraft:glowstone_dust", 1); chemInput("minecraft:pink_dye", 1); chemOutput("factoryascension:ecstasy", 3);
        chemInput("minecraft:bone_meal", 2); chemInput("minecraft:sugar", 1); chemInput("minecraft:white_dye", 1); chemOutput("factoryascension:xanax", 4);
        chemInput("minecraft:sugar", 1); chemInput("minecraft:quartz", 2); chemInput("minecraft:glass_bottle", 1); chemOutput("factoryascension:ketamine", 2);
        chemInput("minecraft:potato", 6); chemInput("minecraft:sugar", 1); chemInput("minecraft:glass_bottle", 2); chemOutput("factoryascension:vodka", 2);
        chemInput("minecraft:sugar", 2); chemInput("minecraft:cocoa_beans", 1); chemInput("minecraft:iron_nugget", 3); chemOutput("factoryascension:energy_drink", 3);
    }

    // Raids (npc/Raids): killing a structure's operators marks you; marked, raid parties come
    public static boolean raids = true, raidBreaksDoors = true;
    public static int raidMarkTicks = 72000, raidKillsPerLevel = 8, raidFirstDelay = 3600, raidInterval = 9600, raidSize = 3;
    // Bullets through thin things (wooden doors, planks, glass, leaves...), weaker each time
    public static boolean bulletsPenetrate = true;

    public static void setRaids(boolean on, int markTicks, int killsPerLevel, int firstDelay, int interval, int size, boolean breakDoors, boolean penetrate) {
        raids = on;
        raidMarkTicks = Math.max(200, markTicks);
        raidKillsPerLevel = Math.max(1, killsPerLevel);
        raidFirstDelay = Math.max(200, firstDelay);
        raidInterval = Math.max(600, interval);
        raidSize = Math.max(1, size);
        raidBreaksDoors = breakDoors;
        bulletsPenetrate = penetrate;
    }

    // Structures (worldgen/SiteGrid): at least this many blocks between any two of the mod's overworld structures, and the
    // chance each 2x-spacing square cell gets one
    public static int structureSpacing = 500;
    public static float structureCellChance = 0.8f;

    public static void setStructureSpacing(int spacing, double cellChance) {
        structureSpacing = Math.max(64, spacing);
        structureCellChance = (float) Math.max(0, Math.min(1, cellChance));
    }

    // Gun wear and malfunctions (gun/GunItem): wear per shot (x caliber damage), barrel heat per shot and cooling per
    // tick, and jam chances (base, at full wear, and with a very hot barrel)
    public static boolean gunWear = true;
    public static float wearPerShot = 0.035f, heatPerShot = 2.2f, heatCooling = 0.35f, jamBase = 0.0005f, jamWorn = 0.08f, jamHot = 0.03f;
    public static int cleaningKitRestore = 35, cleaningKitUses = 6;

    public static void setGunWear(boolean on, double perShot, double heat, double cooling, double base, double worn, double hot, int restore, int uses) {
        gunWear = on;
        wearPerShot = (float) perShot;
        heatPerShot = (float) heat;
        heatCooling = (float) cooling;
        jamBase = (float) base;
        jamWorn = (float) worn;
        jamHot = (float) hot;
        cleaningKitRestore = restore;
        cleaningKitUses = Math.max(1, uses);
    }

    // The body (body/Body): limb damage scale (vanilla damage -> part damage), fractures, heavy bleeding, water, mending
    public static boolean bodyEnabled = true;
    public static float bodyDamageScale = 4.5f, fractureFall = 6f, fractureHit = 18f, heavyBleedHit = 6f, heavyBleedChance = 0.25f;
    public static float waterPerTick = 0.0014f, mendPerSecond = 0.15f;

    public static void setBody(boolean on, double scale, double fall, double hit, double bleedHit, double bleedChance, double waterHours, double mend) {
        bodyEnabled = on;
        bodyDamageScale = (float) scale;
        fractureFall = (float) fall;
        fractureHit = (float) hit;
        heavyBleedHit = (float) bleedHit;
        heavyBleedChance = (float) bleedChance;
        waterPerTick = (float) (100.0 / Math.max(0.05, waterHours) / 72000.0);
        mendPerSecond = (float) mend;
    }

    // Weather (world/Weather): fog banks (chance per minute, minutes they last), darker nights (how much darker)
    public static boolean fogBanks = true;
    public static float fogChancePerMinute = 0.015f, fogMinutes = 5f, nightDarkness = 0.45f;

    public static void setWeather(boolean fog, double chance, double minutes, double night) {
        fogBanks = fog;
        fogChancePerMinute = (float) chance;
        fogMinutes = (float) minutes;
        nightDarkness = (float) Math.max(0, Math.min(0.9, night));
    }

    // The night hunter (npc/NightHunters): chance per minute per player at night, out at this danger level or deeper
    public static boolean nightHunter = true;
    public static float nightHunterChance = 0.08f;
    public static int nightHunterMinDanger = 2;

    public static void setNightHunter(boolean on, double chance, int minDanger) {
        nightHunter = on;
        nightHunterChance = (float) chance;
        nightHunterMinDanger = minDanger;
    }

    // Extraction raids (raid/Extraction): length, how far out the extraction points are, how close and how long to extract,
    // XP for getting out, body bags for the fallen
    public static int raidMinutes = 45, raidMinDist = 250, raidMaxDist = 700, raidExtractRadius = 5, raidExtractSeconds = 10, raidExtractXp = 250;
    public static boolean raidBodyBags = true;

    public static void setExtraction(int minutes, int minDist, int maxDist, int radius, int seconds, int xp, boolean bags) {
        raidMinutes = Math.max(1, minutes);
        raidMinDist = minDist;
        raidMaxDist = Math.max(minDist + 10, maxDist);
        raidExtractRadius = Math.max(1, radius);
        raidExtractSeconds = Math.max(1, seconds);
        raidExtractXp = xp;
        raidBodyBags = bags;
    }

    // Traders (raid/Traders): what a cash bundle puts in your wallet
    public static int cashBundleValue = 1000;
    public static float npcCashChance = 0.55f;

    public static void setCash(int value, double npcChance) {
        cashBundleValue = Math.max(1, value);
        npcCashChance = (float) npcChance;
    }

    // Cars (vehicle/Car): the truck's mounted gun (ticks between rounds, spread in degrees, damage multiplier), what a
    // wrench repairs; the quad's and truck's driving is setCar
    public static int turretCooldown = 2;
    public static float turretSpread = 0.8f, turretDamage = 0.85f, wrenchRepair = 12f;

    public static void setTurret(int cooldown, double spread, double damage, double repair) {
        turretCooldown = Math.max(1, cooldown);
        turretSpread = (float) spread;
        turretDamage = (float) damage;
        wrenchRepair = (float) repair;
    }

    public static void setCar(String model, double accel, double top, double turn, double mass, double health, double fuelUse) {
        dev.factoryascension.vehicle.Car.Model.valueOf(model).set((float) accel, (float) top, (float) turn, (float) mass, (float) health, (float) fuelUse);
    }

    // Light in your hands (world/HeldLight): on/off, and how much dimmer than the placed block (0 = as bright)
    public static boolean heldLights = true;
    public static int heldLightDim = 1;

    public static void setHeldLights(boolean on, int dim) {
        heldLights = on;
        heldLightDim = Math.max(0, dim);
    }

    // Movement (client/Movement): air strafing (air cap and acceleration, blocks/tick), bunny hopping (speed kept), top speed
    public static boolean airStrafe = true, bhop = true;
    public static double airCap = 0.035, airAccel = 0.035, strafeMax = 0.75, bhopKeep = 0.98;

    public static void setMovement(boolean strafe, boolean hop, double cap, double accel, double max, double keep) {
        airStrafe = strafe;
        bhop = hop;
        airCap = cap;
        airAccel = accel;
        strafeMax = max;
        bhopKeep = Math.min(1, keep);
    }
}
