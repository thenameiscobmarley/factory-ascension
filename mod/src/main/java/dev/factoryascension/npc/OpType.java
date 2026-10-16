package dev.factoryascension.npc;

import java.util.EnumSet;
import java.util.Set;

/**
 * The twenty kinds of armed operators: people out there with guns, who fight like players. Each has its faction (who
 * it shoots at besides you), its guns, armor, health, how well it aims, the distance it likes to fight at, and what it
 * does: medics heal their squad, grenadiers throw, snipers stay far back, rushers close in, machine gunners keep
 * firing where you were, ghosts creep up quietly, bodyguards stay by their warlord.
 */
public enum OpType {
    SCAV("scav", "Scav", Faction.SCAV, 20, 0, 1.6f, 6, 26, 1.0f, 0x6A6A52, 0x9A8A62,
        new String[] {"makarov", "tt33", "mp40", "sawnoff", "m870", "aks74u", "double_barrel"}, EnumSet.noneOf(Trait.class)),
    SCAV_RIFLEMAN("scav_rifleman", "Scav Rifleman", Faction.SCAV, 24, 1, 1.3f, 12, 40, 1.0f, 0x5A5E44, 0x8A6A3A,
        new String[] {"ak74", "akm", "aks74u", "galil"}, EnumSet.noneOf(Trait.class)),
    SCAV_SNIPER("scav_sniper", "Scav Sniper", Faction.SCAV, 20, 0, 0.6f, 30, 90, 0.95f, 0x4E5A3A, 0x2E2E26,
        new String[] {"mosin", "svd", "kar98k"}, EnumSet.of(Trait.SNIPER)),
    LOOTER("looter", "Looter", Faction.SCAV, 22, 0, 1.5f, 3, 14, 1.15f, 0x7A6A5A, 0x3A3A3A,
        new String[] {"mac10", "uz9", "ppsh", "thompson", "bizon"}, EnumSet.of(Trait.RUSHER)),
    RAIDER("raider", "Raider", Faction.RAIDER, 30, 2, 0.9f, 10, 40, 1.05f, 0x3A4030, 0x1E1E1E,
        new String[] {"hk416", "m4a1", "scar_l", "ak12", "mcx"}, EnumSet.of(Trait.FLANK, Trait.GRENADES)),
    RAIDER_MEDIC("raider_medic", "Raider Medic", Faction.RAIDER, 28, 2, 1.0f, 8, 30, 1.05f, 0x3A4030, 0xC02020,
        new String[] {"mp5", "ump45", "vector"}, EnumSet.of(Trait.MEDIC)),
    BREACHER("breacher", "Breacher", Faction.RAIDER, 32, 2, 1.0f, 2, 9, 1.12f, 0x2A2E26, 0x6A6A6A,
        new String[] {"m870", "spas12", "m1014", "saiga12", "aa12"}, EnumSet.of(Trait.RUSHER, Trait.GRENADES)),
    GRENADIER("grenadier", "Grenadier", Faction.RAIDER, 28, 2, 1.0f, 12, 36, 1.0f, 0x3A4030, 0xC08020,
        new String[] {"m4a1", "akm", "hk416"}, EnumSet.of(Trait.GRENADES, Trait.LOBBER)),
    MARKSMAN("marksman", "Marksman", Faction.RAIDER, 26, 1, 0.5f, 25, 70, 1.0f, 0x46503A, 0x2A3A2A,
        new String[] {"sr25", "mk14", "svd"}, EnumSet.of(Trait.SNIPER)),
    MACHINE_GUNNER("machine_gunner", "Machine Gunner", Faction.RAIDER, 36, 3, 1.1f, 14, 50, 0.85f, 0x3A3A30, 0x8A7A40,
        new String[] {"pkm", "m249", "m60", "rpk", "mg42"}, EnumSet.of(Trait.SUPPRESS)),
    ROGUE("rogue", "Rogue", Faction.ROGUE, 40, 3, 0.7f, 10, 45, 1.08f, 0x2E3436, 0x6A7A80,
        new String[] {"mk17", "scar_l", "hk416", "fal"}, EnumSet.of(Trait.FLANK, Trait.GRENADES, Trait.NVG)),
    ROGUE_SNIPER("rogue_sniper", "Rogue Sniper", Faction.ROGUE, 32, 2, 0.35f, 40, 120, 0.95f, 0x2E3436, 0x9AA2A6,
        new String[] {"ax338", "awm", "m700"}, EnumSet.of(Trait.SNIPER, Trait.NVG)),
    CULTIST("cultist", "Cultist", Faction.CULT, 30, 0, 0.8f, 5, 24, 1.05f, 0x1E1A16, 0x6A1A12,
        new String[] {"vss", "as_val"}, EnumSet.of(Trait.STEALTH, Trait.NVG, Trait.NIGHT)),
    GHOST("ghost", "Ghost", Faction.ROGUE, 30, 2, 0.8f, 8, 30, 1.1f, 0x26282A, 0x4A5A4A,
        new String[] {"as_val", "mcx", "mac10"}, EnumSet.of(Trait.STEALTH, Trait.FLANK, Trait.NVG)),
    JUGGERNAUT("juggernaut", "Juggernaut", Faction.ROGUE, 120, 4, 1.1f, 6, 30, 0.72f, 0x1A1A1A, 0x8A2A1A,
        new String[] {"m249", "m60", "minigun"}, EnumSet.of(Trait.SUPPRESS, Trait.HEAVY)),
    HUNTER("hunter", "Hunter", Faction.ROGUE, 34, 2, 0.75f, 10, 40, 1.1f, 0x3A3226, 0xA08050,
        new String[] {"hk416", "ak12", "scar_l", "m1014"}, EnumSet.of(Trait.FLANK, Trait.TRACKER, Trait.NVG)),
    WARLORD("warlord", "Warlord", Faction.BANDIT, 160, 3, 0.6f, 6, 30, 1.0f, 0xC8A040, 0x5A1A1A,
        new String[] {"hawk50", "m29", "python"}, EnumSet.of(Trait.LEADER, Trait.HEAVY)),
    BODYGUARD("bodyguard", "Bodyguard", Faction.BANDIT, 50, 3, 0.8f, 4, 24, 1.05f, 0x2A2A2E, 0xC8A040,
        new String[] {"saiga12", "ak12", "vector", "aa12"}, EnumSet.of(Trait.GUARD, Trait.GRENADES)),
    SMUGGLER("smuggler", "Smuggler", Faction.BANDIT, 24, 1, 1.0f, 6, 26, 1.2f, 0x5A4A3A, 0x2A6A6A,
        new String[] {"p90", "vector", "five7"}, EnumSet.of(Trait.FLEES)),
    DESERTER("deserter", "Deserter", Faction.SCAV, 28, 1, 0.9f, 12, 45, 1.0f, 0x5A6440, 0x3A3A2A,
        new String[] {"fal", "g36", "l85", "famas", "m1_garand"}, EnumSet.noneOf(Trait.class));

    public enum Faction { SCAV, RAIDER, ROGUE, CULT, BANDIT }

    public enum Trait { SNIPER, RUSHER, FLANK, GRENADES, LOBBER, MEDIC, SUPPRESS, STEALTH, NVG, NIGHT, HEAVY, TRACKER, LEADER, GUARD, FLEES }

    public final String id, name;
    public final Faction faction;
    public final float health, spread, speed;
    public final int armorTier, minRange, maxRange, eggA, eggB;
    public final String[] guns;
    public final Set<Trait> traits;

    OpType(String id, String name, Faction faction, float health, int armorTier, float spread, int minRange, int maxRange, float speed, int eggA, int eggB,
           String[] guns, Set<Trait> traits) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.health = health;
        this.armorTier = armorTier;
        this.spread = spread;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.speed = speed;
        this.eggA = eggA;
        this.eggB = eggB;
        this.guns = guns;
        this.traits = traits;
    }

    public boolean has(Trait t) {
        return traits.contains(t);
    }

    /** Factions that shoot at each other on sight (everyone shoots at players). */
    public static boolean hostile(Faction a, Faction b) {
        if (a == b) return false;
        if (a == Faction.CULT || b == Faction.CULT) return true;
        if (a == Faction.SCAV) return b == Faction.RAIDER || b == Faction.ROGUE;
        if (b == Faction.SCAV) return a == Faction.RAIDER || a == Faction.ROGUE;
        return a == Faction.BANDIT && b == Faction.ROGUE || a == Faction.ROGUE && b == Faction.BANDIT;
    }

    public static OpType byId(String id) {
        for (OpType t : values()) if (t.id.equals(id)) return t;
        return SCAV;
    }
}
