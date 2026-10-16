package dev.factoryascension.armor;

/**
 * Armor plates for a plate carrier. Each has a protection class (what it can stop: 2 pistol rounds, 3 rifle rounds,
 * 4 and up armor-piercing and full-power rounds), how much a hit wears it, its weight, and what's left of a stopped hit
 * (blunt trauma). Polyethylene is light but only stops pistols reliably; steel stops rifles and lasts but is heavy;
 * ceramic stops almost anything but cracks fast; hybrid (ceramic over polyethylene) cracks slower; titanium is a
 * lighter steel; Ascendite is the pack's own, better than any of them.
 */
public enum Plate {
    POLYETHYLENE("polyethylene_plate", "Polyethylene Plate", 2.6f, 140, 1.4f, 0.35f),
    STEEL("steel_plate", "Steel Plate", 3.2f, 400, 4.0f, 0.3f),
    TITANIUM("titanium_plate", "Titanium Plate", 3.6f, 340, 2.6f, 0.28f),
    CERAMIC("ceramic_plate", "Ceramic Plate", 4.3f, 110, 2.8f, 0.22f),
    HYBRID("hybrid_plate", "Hybrid Plate", 4.4f, 220, 3.0f, 0.2f),
    ASCENDITE("ascendite_plate", "Ascendite Plate", 5.6f, 600, 2.2f, 0.12f);

    public final String id, name;
    public final float protection, weight, blunt;
    public final int durability;

    Plate(String id, String name, float protection, int durability, float weight, float blunt) {
        this.id = id;
        this.name = name;
        this.protection = protection;
        this.durability = durability;
        this.weight = weight;
        this.blunt = blunt;
    }

    public static Plate byId(String id) {
        for (Plate p : values()) if (p.id.equals(id)) return p;
        return null;
    }
}
