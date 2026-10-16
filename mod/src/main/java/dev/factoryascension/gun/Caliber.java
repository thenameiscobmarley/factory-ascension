package dev.factoryascension.gun;

/** The rounds the guns take. Damage and velocity are the caliber's baseline; the round's type and makings change them. */
public enum Caliber {
    P9("9x19mm", 1.0f, 1.0f, 64),
    P45(".45 ACP", 1.25f, 0.85f, 64),
    P50(".50 AE", 2.2f, 1.1f, 32),
    R357(".357 Magnum", 1.7f, 1.15f, 48),
    P46("4.6x30mm", 0.9f, 1.4f, 64),
    R545("5.45x39mm", 1.5f, 1.6f, 64),
    R556("5.56x45mm", 1.55f, 1.7f, 64),
    R762S("7.62x39mm", 1.8f, 1.45f, 64),
    R762N("7.62x51mm", 2.4f, 1.8f, 48),
    R762R("7.62x54mmR", 2.5f, 1.8f, 48),
    R338(".338 Lapua", 4.0f, 2.2f, 32),
    G12("12 gauge", 1.0f, 0.8f, 32),
    P57("5.7x28mm", 1.0f, 1.5f, 64),
    P762T("7.62x25mm", 1.05f, 1.35f, 64),
    R44(".44 Magnum", 1.9f, 1.15f, 48),
    R9X39("9x39mm", 1.9f, 0.9f, 48),
    R300(".300 Blackout", 1.7f, 1.2f, 64),
    B50(".50 BMG", 5.5f, 2.4f, 16);

    public final String label;
    public final float damage, velocity;
    public final int stack;

    Caliber(String label, float damage, float velocity, int stack) {
        this.label = label;
        this.damage = damage;
        this.velocity = velocity;
        this.stack = stack;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Caliber byId(String id) {
        for (Caliber c : values()) if (c.id().equals(id)) return c;
        return P9;
    }
}
