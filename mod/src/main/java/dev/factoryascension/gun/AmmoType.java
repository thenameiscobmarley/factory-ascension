package dev.factoryascension.gun;

/**
 * What the projectile is. Pellets > 1 turns any gun into a scattergun; the rest are multipliers and effects that
 * GunItem.fire applies. Tip colour is the default colour of the round's tip (the press can recolour the case).
 */
public enum AmmoType {
    REGULAR("Regular", "Full metal jacket. Does what it says.", 1, 1, 1, 1, 0xB87333),
    HOLLOW_POINT("Hollow Point", "Opens up inside: more damage, less through armor.", 1, 1.25f, 0.5f, 1, 0x8A8A8A),
    ARMOR_PIERCING("Armor Piercing", "A hard core: ignores most armor and goes through soft targets.", 1, 0.95f, 2.5f, 1, 0x202020),
    RIP("RIP", "Radically Invasive Projectile: splits into petals, more damage, and the target bleeds.", 1, 1.45f, 0.3f, 1, 0xC02828),
    SNAKESHOT("Snakeshot", "A capsule of tiny shot: a cloud of pellets from any gun, short range.", 9, 0.22f, 0.1f, 0.35f, 0x40A040),
    SLUG("Slug", "One heavy lump of lead: big hit, big knockback.", 1, 2.2f, 1.2f, 0.8f, 0x707070),
    BUCKSHOT("Buckshot", "Nine big pellets. Close range is a bad place to be.", 9, 0.45f, 0.5f, 0.5f, 0xB02020),
    INCENDIARY("Incendiary", "Sets what it hits on fire.", 1, 0.95f, 1, 1, 0xE06010),
    TRACER("Tracer", "Leaves a glowing line, and what it hits glows.", 1, 1, 1, 1, 0x30E030),
    EXPLOSIVE("Explosive", "Pops on impact (doesn't break blocks).", 1, 1.2f, 0.6f, 0.9f, 0xE8C020),
    SUBSONIC("Subsonic", "Quiet. Mobs don't hear it; less range.", 1, 0.85f, 0.9f, 0.6f, 0x303050),
    RUBBER("Rubber", "Less lethal: tiny damage, huge knockback.", 1, 0.15f, 0, 0.5f, 0x202020),
    FLECHETTE("Flechette", "Steel darts: few pellets that go through everything.", 5, 0.5f, 3, 0.9f, 0xA0A0B0),
    DRAGONS_BREATH("Dragon's Breath", "A cone of burning metal. Pretty. Terrible.", 12, 0.18f, 0, 0.3f, 0xFF5000),
    FRANGIBLE("Frangible", "Turns to dust on walls (no ricochet, no glass); extra damage to mobs.", 1, 1.15f, 0.4f, 1, 0x9A7A5A),
    ENDER("Ender", "The target is teleported a few blocks away.", 1, 0.5f, 1, 1, 0x30A090);

    public final String label, desc;
    public final int pellets;
    public final float damage, penetration, range;
    public final int tip;

    AmmoType(String label, String desc, int pellets, float damage, float penetration, float range, int tip) {
        this.label = label;
        this.desc = desc;
        this.pellets = pellets;
        this.damage = damage;
        this.penetration = penetration;
        this.range = range;
        this.tip = tip;
    }

    public static AmmoType byName(String n) {
        try {
            return valueOf(n);
        } catch (Exception e) {
            return REGULAR;
        }
    }
}
