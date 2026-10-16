package dev.factoryascension.gun;

/**
 * One gun's numbers. All guns share GunItem; everything that differs between them is here (and in its model).
 * Damage per hit = Guns.BASE_DAMAGE x the caliber's damage x `damage` (x the round's type and makings).
 * Ticks are game ticks (20 a second). Offsets are in blocks, in first-person view space (x right, y up, -z ahead).
 */
public final class GunSpec {
    public enum Kind { PISTOL, REVOLVER, SMG, RIFLE, DMR, SNIPER, SHOTGUN, LMG }
    public enum Reload { MAG, TUBE, BOLT_CLIP, CYLINDER, BREAK, BELT }

    public final String id, name;
    public final Kind kind;
    public final Caliber caliber;
    public boolean auto = false;
    public int rpm = 400, mag = 30, pellets = 1;
    public float damage = 1, spreadHip = 3.5f, spreadAim = 0.4f, range = 120, recoilUp = 2.5f, recoilSide = 0.8f, zoom = 0.2f;
    public Reload reload = Reload.MAG;
    public int magOut = 12, magIn = 14, chamber = 10, perRound = 10;   // reload phases (perRound: tube / cylinder / clip)
    public int gridW = 4, gridH = 2;
    public float weight = 3.5f;
    // First-person: where it's held at the hip, and aimed (sight line on the screen's centre, stock clear of the eye)
    public float hipX = 0.42f, hipY = -0.44f, hipZ = -0.70f, adsY = -0.30f, adsZ = -0.62f;
    public String desc = "";
    public float fpScale = 0.6f;                 // its first-person model size (GunGeometry)
    public float[] mounts = new float[36];       // x, y, z per attachment slot, model units (GunGeometry)

    public float[] mount(Attachments.Slot slot) {
        int i = slot.ordinal() * 3;
        return new float[] {mounts[i], mounts[i + 1], mounts[i + 2]};
    }

    public GunSpec(String id, String name, Kind kind, Caliber caliber) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.caliber = caliber;
        switch (kind) {
            case PISTOL, REVOLVER -> { gridW = 2; gridH = 1; weight = 1; hipX = 0.36f; hipY = -0.40f; hipZ = -0.66f; adsY = -0.205f; adsZ = -0.52f; spreadHip = 3f; spreadAim = 0.8f; range = 60; recoilUp = 3.5f; zoom = 0.1f; }
            case SMG -> { gridW = 3; gridH = 2; weight = 2.5f; range = 80; spreadHip = 3.2f; spreadAim = 0.9f; recoilUp = 1.6f; zoom = 0.15f; }
            case RIFLE -> { gridW = 4; gridH = 2; weight = 3.5f; }
            case DMR -> { gridW = 5; gridH = 2; weight = 4.5f; range = 180; spreadAim = 0.15f; zoom = 0.45f; recoilUp = 3.5f; }
            case SNIPER -> { gridW = 5; gridH = 2; weight = 5.5f; range = 256; spreadHip = 6f; spreadAim = 0.05f; zoom = 0.7f; recoilUp = 6f; }
            case SHOTGUN -> { gridW = 5; gridH = 2; weight = 3.6f; range = 40; spreadHip = 7f; spreadAim = 2.5f; pellets = 8; recoilUp = 5f; zoom = 0.15f; }
            case LMG -> { gridW = 5; gridH = 3; weight = 8f; range = 150; spreadHip = 5f; spreadAim = 1.2f; recoilUp = 1.8f; recoilSide = 1.2f; }
        }
    }

    // The numbers as written in Guns.java, so the config's scales apply to them and not to an earlier scaled copy
    private float[] base;

    public boolean hasBase() {
        return base != null;
    }

    public void saveBase() {
        base = new float[] {damage, rpm, mag, spreadHip, spreadAim, range, recoilUp, recoilSide};
    }

    public void resetToBase() {
        if (base == null) return;
        damage = base[0]; rpm = (int) base[1]; mag = (int) base[2]; spreadHip = base[3]; spreadAim = base[4];
        range = base[5]; recoilUp = base[6]; recoilSide = base[7];
    }

    public int cooldown() {
        return Math.max(1, Math.round(1200f / rpm));
    }

    // Small fluent setters, so the gun list reads like a table
    public GunSpec auto(int rpm) { this.auto = true; this.rpm = rpm; return this; }
    public GunSpec semi(int rpm) { this.auto = false; this.rpm = rpm; return this; }
    public GunSpec mag(int n) { this.mag = n; return this; }
    public GunSpec dmg(float d) { this.damage = d; return this; }
    public GunSpec pellets(int n) { this.pellets = n; return this; }
    public GunSpec spread(float hip, float aim) { this.spreadHip = hip; this.spreadAim = aim; return this; }
    public GunSpec range(float r) { this.range = r; return this; }
    public GunSpec recoil(float up, float side) { this.recoilUp = up; this.recoilSide = side; return this; }
    public GunSpec zoom(float z) { this.zoom = z; return this; }
    public GunSpec reload(Reload r, int out, int in, int chamber) { this.reload = r; this.magOut = out; this.magIn = in; this.chamber = chamber; return this; }
    public GunSpec perRound(int t) { this.perRound = t; return this; }
    public GunSpec grid(int w, int h) { this.gridW = w; this.gridH = h; return this; }
    public GunSpec weight(float kg) { this.weight = kg; return this; }
    public GunSpec hip(float x, float y, float z) { this.hipX = x; this.hipY = y; this.hipZ = z; return this; }
    public GunSpec ads(float y, float z) { this.adsY = y; this.adsZ = z; return this; }
    public GunSpec desc(String d) { this.desc = d; return this; }
}
