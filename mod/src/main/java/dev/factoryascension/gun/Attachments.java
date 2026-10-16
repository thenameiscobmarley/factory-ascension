package dev.factoryascension.gun;

import dev.factoryascension.gun.GunSpec.Kind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every gun attachment, made from a table: each family (a suppressor, an ACOG, a drum magazine, ...) comes from a dozen
 * makers, in several finishes and four grades, each with its own numbers. That's well over a thousand, all generated the
 * same way every time from this file (no item per attachment: an attachment is one item with its id in NBT).
 *
 * An attachment changes the gun's numbers (multipliers and bonuses, see Mod) and is drawn on the gun at its slot's mount
 * point (shape + finish colour). Normally each gun takes one per slot and only what fits its kind; a gun with the
 * Jury-Rigged enchantment takes anything anywhere, several per slot.
 */
public final class Attachments {
    public enum Slot {
        MUZZLE("Muzzle"), BARREL("Barrel"), HANDGUARD("Handguard"), FOREGRIP("Foregrip"), OPTIC("Optic"), STOCK("Stock"),
        MAGAZINE("Magazine"), UNDERBARREL("Underbarrel"), TACTICAL("Tactical"), GRIP("Grip"), TRIGGER("Trigger"), CHARM("Charm");

        public final String label;

        Slot(String label) {
            this.label = label;
        }
    }

    /** What an attachment does. Multipliers default to 1, bonuses to 0. */
    public static final class Mod {
        public float damage = 1, rpm = 1, spread = 1, recoil = 1, range = 1, reload = 1, sound = 1, ads = 1, weight = 0;
        public float magMult = 1;
        public int magAdd = 0;
        public float zoom = 0;          // added zoom (an optic's magnification); the highest optic counts
        public float sightRaise = 0;    // how much higher the sight line is (model units) with this optic
        public boolean auto = false, laser = false, light = false, noBolt = false, akimbo = false, gangster = false, blinding = false;
        public int pelletsAdd = 0;
    }

    public static final class Att {
        public final String id, name, family, shape;
        public final Slot slot;
        public final int tier, colour;
        public final Mod mod;
        public final Set<Kind> fits;
        public final int cost;          // how many "parts" it takes to make (gun/WorkbenchCost)

        Att(String id, String name, String family, String shape, Slot slot, int tier, int colour, Mod mod, Set<Kind> fits, int cost) {
            this.id = id;
            this.name = name;
            this.family = family;
            this.shape = shape;
            this.slot = slot;
            this.tier = tier;
            this.colour = colour;
            this.mod = mod;
            this.fits = fits;
            this.cost = cost;
        }

        public boolean fits(GunSpec g) {
            return fits.contains(g.kind);
        }
    }

    public static final List<Att> ALL = new ArrayList<>();
    public static final Map<String, Att> BY_ID = new LinkedHashMap<>();

    static final Set<Kind> ALL_KINDS = EnumSet.allOf(Kind.class);
    static final Set<Kind> LONG = EnumSet.of(Kind.SMG, Kind.RIFLE, Kind.DMR, Kind.SNIPER, Kind.SHOTGUN, Kind.LMG);
    static final Set<Kind> RIFLES = EnumSet.of(Kind.RIFLE, Kind.DMR, Kind.SNIPER, Kind.LMG);
    static final Set<Kind> AUTOS = EnumSet.of(Kind.SMG, Kind.RIFLE, Kind.LMG, Kind.PISTOL);
    static final Set<Kind> MAGFED = EnumSet.of(Kind.PISTOL, Kind.SMG, Kind.RIFLE, Kind.DMR, Kind.SHOTGUN);

    static final String[] MAKERS = {"Vanguard", "Kestrel", "Ironside", "Halcyon", "Black Lantern", "Taiga", "Meridian", "Corvid",
        "Steppe", "Northwind", "Atlas", "Varyag", "Saber", "Quill"};
    static final String[][] FINISHES = {{"Black", "1C1C1E"}, {"FDE", "A8916A"}, {"Olive", "5A6440"}, {"Grey", "6E7074"},
        {"Ascendite", "40C8B8"}, {"Gold", "D8A838"}};
    static final String[] GRADES = {"", " Mk II", " Mk III", " Elite"};

    /** One family: its slot, the shape drawn, which guns take it, and how it changes them (at grade 0; grades improve it). */
    interface Effect {
        void apply(Mod m, float q);   // q: 1 (basic) .. ~1.6 (elite): how strong the good side is
    }

    record Family(String key, String name, Slot slot, String shape, Set<Kind> fits, int cost, Effect effect) {
    }

    static final List<Family> FAMILIES = new ArrayList<>();

    static void fam(String key, String name, Slot slot, String shape, Set<Kind> fits, int cost, Effect e) {
        FAMILIES.add(new Family(key, name, slot, shape, fits, cost, e));
    }

    static {
        // ---- Muzzle ----
        fam("compensator", "Compensator", Slot.MUZZLE, "comp", ALL_KINDS, 2, (m, q) -> { m.recoil = 1 - 0.12f * q; m.spread = 1 - 0.03f * q; });
        fam("muzzle_brake", "Muzzle Brake", Slot.MUZZLE, "brake", ALL_KINDS, 2, (m, q) -> { m.recoil = 1 - 0.2f * q; m.sound = 1.25f; });
        fam("flash_hider", "Flash Hider", Slot.MUZZLE, "flash", ALL_KINDS, 1, (m, q) -> { m.recoil = 1 - 0.05f * q; m.spread = 1 - 0.04f * q; });
        fam("suppressor_short", "Short Suppressor", Slot.MUZZLE, "supp_short", ALL_KINDS, 3, (m, q) -> { m.sound = 0.4f; m.recoil = 1 - 0.08f * q; m.range = 0.95f; });
        fam("suppressor_long", "Long Suppressor", Slot.MUZZLE, "supp_long", ALL_KINDS, 4, (m, q) -> { m.sound = 0.25f; m.recoil = 1 - 0.14f * q; m.ads = 1.1f; m.weight = 0.5f; });
        fam("suppressor_fat", "Can Suppressor", Slot.MUZZLE, "supp_fat", ALL_KINDS, 5, (m, q) -> { m.sound = 0.15f; m.recoil = 1 - 0.18f * q; m.ads = 1.18f; m.weight = 0.8f; });
        fam("choke", "Full Choke", Slot.MUZZLE, "choke", EnumSet.of(Kind.SHOTGUN), 2, (m, q) -> m.spread = 1 - 0.35f * q);
        fam("duckbill", "Duckbill Choke", Slot.MUZZLE, "choke", EnumSet.of(Kind.SHOTGUN), 2, (m, q) -> { m.spread = 1.2f; m.pelletsAdd = Math.round(2 * q); });
        fam("booster", "Muzzle Booster", Slot.MUZZLE, "comp", EnumSet.of(Kind.SMG, Kind.PISTOL, Kind.LMG), 2, (m, q) -> { m.rpm = 1 + 0.08f * q; m.recoil = 1.05f; });
        fam("bayonet", "Bayonet", Slot.MUZZLE, "bayonet", LONG, 1, (m, q) -> { m.damage = 1 + 0.03f * q; m.ads = 1.05f; });
        fam("thread_cap", "Thread Protector", Slot.MUZZLE, "cap", ALL_KINDS, 1, (m, q) -> m.ads = 1 - 0.03f * q);
        // ---- Barrel ----
        fam("barrel_short", "Short Barrel", Slot.BARREL, "barrel_short", LONG, 3, (m, q) -> { m.range = 0.8f; m.ads = 1 - 0.1f * q; m.spread = 1.08f; m.weight = -0.4f; });
        fam("barrel_long", "Long Barrel", Slot.BARREL, "barrel_long", LONG, 4, (m, q) -> { m.range = 1 + 0.18f * q; m.damage = 1 + 0.04f * q; m.ads = 1.08f; m.weight = 0.6f; });
        fam("barrel_heavy", "Heavy Barrel", Slot.BARREL, "barrel_heavy", RIFLES, 4, (m, q) -> { m.spread = 1 - 0.15f * q; m.recoil = 1 - 0.05f * q; m.ads = 1.12f; m.weight = 1f; });
        fam("barrel_fluted", "Fluted Barrel", Slot.BARREL, "barrel_fluted", LONG, 4, (m, q) -> { m.ads = 1 - 0.06f * q; m.spread = 1 - 0.05f * q; m.weight = -0.3f; });
        fam("barrel_match", "Match Barrel", Slot.BARREL, "barrel_long", RIFLES, 5, (m, q) -> { m.spread = 1 - 0.25f * q; m.damage = 1 + 0.03f * q; });
        fam("barrel_ported", "Ported Barrel", Slot.BARREL, "barrel_fluted", ALL_KINDS, 3, (m, q) -> { m.recoil = 1 - 0.1f * q; m.sound = 1.15f; });
        fam("barrel_integral", "Integrally Suppressed Barrel", Slot.BARREL, "supp_long", EnumSet.of(Kind.SMG, Kind.RIFLE, Kind.PISTOL), 6, (m, q) -> { m.sound = 0.2f; m.range = 0.85f; });
        // ---- Handguard ----
        fam("hg_quadrail", "Quad Rail Handguard", Slot.HANDGUARD, "hg_rail", LONG, 3, (m, q) -> { m.recoil = 1 - 0.04f * q; m.weight = 0.4f; });
        fam("hg_mlok", "M-LOK Handguard", Slot.HANDGUARD, "hg_slim", LONG, 3, (m, q) -> { m.ads = 1 - 0.05f * q; m.recoil = 1 - 0.02f * q; m.weight = -0.2f; });
        fam("hg_wood", "Wooden Handguard", Slot.HANDGUARD, "hg_wood", LONG, 2, (m, q) -> { m.recoil = 1 - 0.03f * q; m.spread = 1 - 0.02f * q; });
        fam("hg_carbon", "Carbon Handguard", Slot.HANDGUARD, "hg_slim", LONG, 4, (m, q) -> { m.ads = 1 - 0.08f * q; m.weight = -0.4f; });
        // ---- Foregrip ----
        fam("grip_vertical", "Vertical Foregrip", Slot.FOREGRIP, "fg_vertical", LONG, 2, (m, q) -> m.recoil = 1 - 0.1f * q);
        fam("grip_angled", "Angled Foregrip", Slot.FOREGRIP, "fg_angled", LONG, 2, (m, q) -> { m.ads = 1 - 0.07f * q; m.recoil = 1 - 0.05f * q; });
        fam("grip_stubby", "Stubby Grip", Slot.FOREGRIP, "fg_stubby", LONG, 2, (m, q) -> { m.recoil = 1 - 0.06f * q; m.spread = 1 - 0.04f * q; });
        fam("hand_stop", "Hand Stop", Slot.FOREGRIP, "fg_stop", LONG, 1, (m, q) -> m.ads = 1 - 0.05f * q);
        fam("bipod_grip", "Bipod Grip", Slot.FOREGRIP, "fg_vertical", RIFLES, 3, (m, q) -> { m.recoil = 1 - 0.14f * q; m.ads = 1.06f; });
        // ---- Optics ----
        fam("red_dot", "Red Dot", Slot.OPTIC, "opt_dot", ALL_KINDS, 3, (m, q) -> { m.zoom = 0.12f; m.sightRaise = 1.6f; m.spread = 1 - 0.04f * q; });
        fam("holo", "Holographic Sight", Slot.OPTIC, "opt_holo", ALL_KINDS, 3, (m, q) -> { m.zoom = 0.15f; m.sightRaise = 1.8f; m.ads = 1 - 0.03f * q; });
        fam("prism", "2x Prism", Slot.OPTIC, "opt_prism", ALL_KINDS, 4, (m, q) -> { m.zoom = 0.3f; m.sightRaise = 2.0f; m.spread = 1 - 0.06f * q; });
        fam("acog", "4x ACOG", Slot.OPTIC, "opt_acog", ALL_KINDS, 5, (m, q) -> { m.zoom = 0.5f; m.sightRaise = 2.2f; m.spread = 1 - 0.08f * q; m.ads = 1.06f; });
        fam("lpvo", "1-6x Variable Scope", Slot.OPTIC, "opt_scope", ALL_KINDS, 6, (m, q) -> { m.zoom = 0.6f; m.sightRaise = 2.4f; m.spread = 1 - 0.1f * q; m.ads = 1.1f; });
        fam("sniper_scope", "8x Sniper Scope", Slot.OPTIC, "opt_scope_big", ALL_KINDS, 7, (m, q) -> { m.zoom = 0.78f; m.sightRaise = 2.8f; m.spread = 1 - 0.15f * q; m.ads = 1.2f; });
        fam("long_scope", "12x Long-Range Scope", Slot.OPTIC, "opt_scope_big", ALL_KINDS, 8, (m, q) -> { m.zoom = 0.86f; m.sightRaise = 3.0f; m.spread = 1 - 0.2f * q; m.ads = 1.3f; });
        fam("magnifier", "3x Magnifier", Slot.OPTIC, "opt_prism", ALL_KINDS, 3, (m, q) -> { m.zoom = 0.4f; m.sightRaise = 1.9f; });
        fam("canted_irons", "Canted Iron Sights", Slot.OPTIC, "opt_irons", ALL_KINDS, 1, (m, q) -> { m.ads = 1 - 0.06f * q; });
        fam("reflex", "Mini Reflex", Slot.OPTIC, "opt_dot", ALL_KINDS, 2, (m, q) -> { m.zoom = 0.08f; m.sightRaise = 1.3f; m.ads = 1 - 0.05f * q; });
        // ---- Stocks ----
        fam("stock_fixed", "Fixed Stock", Slot.STOCK, "stock_pad", LONG, 2, (m, q) -> { m.recoil = 1 - 0.12f * q; m.ads = 1.04f; });
        fam("stock_collapsible", "Collapsible Stock", Slot.STOCK, "stock_pad", LONG, 2, (m, q) -> { m.recoil = 1 - 0.08f * q; m.ads = 1 - 0.03f * q; });
        fam("stock_precision", "Precision Stock", Slot.STOCK, "stock_cheek", RIFLES, 4, (m, q) -> { m.spread = 1 - 0.1f * q; m.recoil = 1 - 0.1f * q; m.ads = 1.08f; });
        fam("stock_skeleton", "Skeleton Stock", Slot.STOCK, "stock_pad", LONG, 3, (m, q) -> { m.ads = 1 - 0.08f * q; m.recoil = 1 - 0.04f * q; m.weight = -0.4f; });
        // Akimbo: no stock, a second gun in the other hand. Both fire (twice the rate, twice the rounds), neither aims well
        fam("akimbo", "Akimbo Kit", Slot.STOCK, "none", EnumSet.of(Kind.PISTOL, Kind.REVOLVER, Kind.SMG, Kind.SHOTGUN), 6,
            (m, q) -> { m.akimbo = true; m.rpm = 1.6f + 0.1f * q; m.magMult = 2; m.spread = 1.55f - 0.1f * q; m.recoil = 1.2f; m.ads = 0.85f; m.reload = 1.35f; m.weight = 1.2f; });
        fam("stock_pistol", "Pistol Stock", Slot.STOCK, "stock_pad", EnumSet.of(Kind.PISTOL, Kind.REVOLVER), 3, (m, q) -> { m.recoil = 1 - 0.2f * q; m.spread = 1 - 0.1f * q; });
        // ---- Magazines ----
        fam("mag_extended", "Extended Magazine", Slot.MAGAZINE, "mag_ext", MAGFED, 3, (m, q) -> { m.magMult = 1.5f; m.reload = 1.1f; m.weight = 0.3f; });
        fam("mag_drum", "Drum Magazine", Slot.MAGAZINE, "mag_drum", MAGFED, 5, (m, q) -> { m.magMult = 2.5f; m.reload = 1.4f; m.ads = 1.12f; m.weight = 1f; });
        fam("mag_quick", "Quick Magazine", Slot.MAGAZINE, "mag_ext", MAGFED, 3, (m, q) -> m.reload = 1 - 0.25f * q);
        fam("mag_coupled", "Coupled Magazines", Slot.MAGAZINE, "mag_coupled", MAGFED, 4, (m, q) -> { m.reload = 1 - 0.4f * q; m.weight = 0.4f; });
        fam("mag_short", "Short Magazine", Slot.MAGAZINE, "mag_short", MAGFED, 1, (m, q) -> { m.magMult = 0.67f; m.ads = 1 - 0.08f * q; m.reload = 0.9f; });
        fam("mag_casket", "Casket Magazine", Slot.MAGAZINE, "mag_ext", MAGFED, 4, (m, q) -> { m.magMult = 2f; m.reload = 1.2f; });
        // ---- Underbarrel ----
        fam("bipod", "Bipod", Slot.UNDERBARREL, "under_bipod", RIFLES, 3, (m, q) -> { m.recoil = 1 - 0.15f * q; m.spread = 1 - 0.1f * q; m.ads = 1.1f; });
        fam("launcher", "Underbarrel Launcher", Slot.UNDERBARREL, "under_launcher", RIFLES, 7, (m, q) -> { m.damage = 1 + 0.15f * q; m.weight = 1.4f; m.ads = 1.15f; });
        fam("masterkey", "Underbarrel Shotgun", Slot.UNDERBARREL, "under_shotgun", RIFLES, 6, (m, q) -> { m.pelletsAdd = Math.round(3 * q); m.spread = 1.25f; m.weight = 1f; });
        fam("rail_cover", "Rail Covers", Slot.UNDERBARREL, "under_cover", LONG, 1, (m, q) -> m.recoil = 1 - 0.02f * q);
        // ---- Tactical ----
        fam("laser", "Laser Sight", Slot.TACTICAL, "tac_laser", ALL_KINDS, 3, (m, q) -> { m.laser = true; m.spread = 1 - 0.2f * q; });
        fam("flashlight", "Weapon Light", Slot.TACTICAL, "tac_light", ALL_KINDS, 2, (m, q) -> m.light = true);
        fam("combo_unit", "Laser-Light Combo", Slot.TACTICAL, "tac_combo", ALL_KINDS, 4, (m, q) -> { m.laser = true; m.light = true; m.spread = 1 - 0.15f * q; });
        // ---- Grips ----
        fam("grip_rubber", "Rubber Grip", Slot.GRIP, "grip_wrap", ALL_KINDS, 1, (m, q) -> m.recoil = 1 - 0.05f * q);
        fam("grip_stippled", "Stippled Grip", Slot.GRIP, "grip_wrap", ALL_KINDS, 2, (m, q) -> { m.recoil = 1 - 0.04f * q; m.ads = 1 - 0.03f * q; });
        // Gangster: held flat on its side. Quick to raise, wild to control
        fam("gangster_grip", "Gangster Grip", Slot.GRIP, "grip_wrap", ALL_KINDS, 2,
            (m, q) -> { m.gangster = true; m.ads = 0.7f - 0.05f * q; m.spread = 1.3f; m.recoil = 1.1f; m.damage = 1 + 0.02f * q; });
        fam("grip_ergo", "Ergonomic Grip", Slot.GRIP, "grip_wrap", ALL_KINDS, 2, (m, q) -> { m.ads = 1 - 0.06f * q; m.reload = 1 - 0.05f * q; });
        // ---- Triggers ----
        fam("trigger_match", "Match Trigger", Slot.TRIGGER, "none", ALL_KINDS, 3, (m, q) -> m.spread = 1 - 0.08f * q);
        fam("trigger_hair", "Hair Trigger", Slot.TRIGGER, "none", ALL_KINDS, 3, (m, q) -> m.rpm = 1 + 0.15f * q);
        fam("auto_sear", "Auto Sear", Slot.TRIGGER, "none", AUTOS, 6, (m, q) -> { m.auto = true; m.recoil = 1.15f; m.noBolt = true; });
        fam("bump_stock", "Bump Fire Stock", Slot.TRIGGER, "none", EnumSet.of(Kind.RIFLE, Kind.DMR), 5, (m, q) -> { m.auto = true; m.spread = 1.6f; m.recoil = 1.3f; });
        // ---- Charms (just for looks, and a little luck) ----
        fam("charm_dice", "Dice Charm", Slot.CHARM, "charm", ALL_KINDS, 1, (m, q) -> m.damage = 1 + 0.01f * q);
        fam("charm_skull", "Skull Charm", Slot.CHARM, "charm", ALL_KINDS, 1, (m, q) -> m.damage = 1 + 0.01f * q);
        fam("charm_core", "Ascension Core Charm", Slot.CHARM, "charm", ALL_KINDS, 2, (m, q) -> m.damage = 1 + 0.02f * q);

        // ---- Cursed: one of each, fits anything, absurd ----
        fam("cursed_belt200", "200-Round Belt Box", Slot.MAGAZINE, "cursed_box", ALL_KINDS, 12, (m, q) -> { m.magAdd = 200; m.reload = 2.2f; m.weight = 6; m.ads = 1.4f; });
        fam("cursed_barrel_of_rounds", "Barrel of Rounds (1,000)", Slot.MAGAZINE, "cursed_drum", ALL_KINDS, 20, (m, q) -> { m.magAdd = 1000; m.reload = 4; m.weight = 18; m.ads = 2.2f; });
        fam("cursed_scope20", "20x Observatory Scope", Slot.OPTIC, "cursed_scope", ALL_KINDS, 14, (m, q) -> { m.zoom = 0.95f; m.sightRaise = 4.8f; m.spread = 0.3f; m.ads = 1.9f; m.weight = 3; });
        fam("cursed_barrel50", "50-Inch Barrel", Slot.BARREL, "cursed_barrel", ALL_KINDS, 16, (m, q) -> { m.range = 3.2f; m.damage = 1.35f; m.spread = 0.5f; m.ads = 2.3f; m.weight = 7; });
        fam("cursed_light20k", "20,000 Lumen Light", Slot.TACTICAL, "cursed_light", ALL_KINDS, 10, (m, q) -> { m.light = true; m.blinding = true; m.weight = 1.5f; });
        fam("cursed_hypersear", "Hyper Sear", Slot.TRIGGER, "none", ALL_KINDS, 15, (m, q) -> { m.auto = true; m.noBolt = true; m.rpm = 3.2f; m.recoil = 2.2f; m.spread = 1.6f; });
        fam("cursed_shotgun_kit", "Shotgun Conversion Kit", Slot.BARREL, "cursed_barrel_wide", ALL_KINDS, 12, (m, q) -> { m.pelletsAdd = 11; m.spread = 4; m.range = 0.5f; });
        fam("cursed_railcoil", "Railgun Coil", Slot.BARREL, "cursed_coil", ALL_KINDS, 22, (m, q) -> { m.damage = 2.6f; m.rpm = 0.3f; m.sound = 3; m.range = 2.5f; m.weight = 5; });
        fam("cursed_jetpack", "Recoil Jetpack", Slot.STOCK, "cursed_jet", ALL_KINDS, 14, (m, q) -> { m.recoil = 0.08f; m.weight = 9; m.ads = 1.3f; });
        fam("cursed_mag_tape", "Mag Taped to a Mag Taped to a Mag", Slot.MAGAZINE, "mag_coupled", ALL_KINDS, 8, (m, q) -> { m.magMult = 1.5f; m.reload = 0.3f; m.weight = 1.2f; });
        fam("cursed_chainsaw", "Chainsaw Bayonet", Slot.MUZZLE, "cursed_saw", ALL_KINDS, 12, (m, q) -> { m.damage = 1.25f; m.ads = 1.5f; m.weight = 4; m.sound = 1.6f; });
        fam("cursed_suppressor_stack", "Six Suppressors, Taped", Slot.MUZZLE, "cursed_stack", ALL_KINDS, 18, (m, q) -> { m.sound = 0.02f; m.recoil = 0.5f; m.ads = 2.4f; m.weight = 4; });
        fam("cursed_gold", "Gold-Plated Everything", Slot.CHARM, "charm", ALL_KINDS, 25, (m, q) -> { m.damage = 1.12f; m.weight = 8; });
        fam("cursed_duck", "Rubber Duck", Slot.CHARM, "cursed_duck", ALL_KINDS, 2, (m, q) -> { m.recoil = 0.94f; m.spread = 0.95f; });
        fam("cursed_toaster", "Tactical Toaster", Slot.UNDERBARREL, "cursed_toaster", ALL_KINDS, 6, (m, q) -> { m.weight = 2; m.ads = 1.2f; m.damage = 1.02f; });
        fam("cursed_bipod_tall", "Three-Metre Bipod", Slot.UNDERBARREL, "cursed_bipod", ALL_KINDS, 8, (m, q) -> { m.spread = 0.2f; m.recoil = 0.3f; m.ads = 2.6f; m.weight = 6; });

        // Each family x makers x finishes x grades, deterministic. Not every maker makes every finish:
        // a maker makes 2-4 of the finishes, picked from its index, so names and colours vary across the catalog.
        for (Family f : FAMILIES) {
            if (f.key.startsWith("cursed_")) {
                // One of each, from no maker anyone admits to
                Mod m = new Mod();
                f.effect.apply(m, 1);
                String id = f.key;
                Att a = new Att(id, "Cursed " + f.name, f.key, f.shape, f.slot, 3, 0xB03A2E, m, f.fits, f.cost);
                ALL.add(a);
                BY_ID.put(id, a);
                continue;
            }
            for (int mk = 0; mk < MAKERS.length; mk++) {
                int finishes = 2 + (mk + f.key.length()) % 3;
                for (int fi = 0; fi < finishes; fi++) {
                    int finish = (mk * 3 + fi * 2 + f.key.length()) % FINISHES.length;
                    int grades = (mk + fi) % 2 == 0 ? 2 : 1;
                    if (f.slot == Slot.CHARM) grades = 1;
                    for (int gr = 0; gr < grades; gr++) {
                        int tier = gr == 0 ? (mk % 3 == 0 ? 0 : 1) : (mk % 2 == 0 ? 3 : 2);
                        float q = 1 + tier * 0.2f + (mk % 5) * 0.02f;
                        Mod m = new Mod();
                        f.effect.apply(m, q);
                        String id = f.key + "." + MAKERS[mk].toLowerCase().replace(' ', '_') + "." + FINISHES[finish][0].toLowerCase() + "." + tier;
                        if (BY_ID.containsKey(id)) continue;
                        String name = MAKERS[mk] + " " + f.name + GRADES[tier] + " (" + FINISHES[finish][0] + ")";
                        Att a = new Att(id, name, f.key, f.shape, f.slot, tier, Integer.parseInt(FINISHES[finish][1], 16), m, f.fits, f.cost + tier * 2);
                        ALL.add(a);
                        BY_ID.put(id, a);
                    }
                }
            }
        }
    }

    private Attachments() {
    }

    public static List<Att> forSlot(Slot s) {
        List<Att> out = new ArrayList<>();
        for (Att a : ALL) if (a.slot == s) out.add(a);
        return Collections.unmodifiableList(out);
    }
}
