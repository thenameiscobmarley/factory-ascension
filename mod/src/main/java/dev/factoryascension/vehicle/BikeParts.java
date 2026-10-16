package dev.factoryascension.vehicle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The dirt bike's parts and what they do. Every number here is a default: the pack's config (vehicles.parts) sets them
 * (FAConfig bridge: setEngine / setFrame / setGearbox / setFuel). Speeds are blocks per tick (1 = 72 km/h).
 * Frame anchors (axles, steering head, engine mount, seat) match tools/vehicles/bike_mesh.py's FRAMES, in model units
 * (16 = one block), bike facing +x, ground at y = 0.
 */
public final class BikeParts {
    private BikeParts() {
    }

    /** power: acceleration at low speed; top: top speed; weight: kg; size: how big a frame it needs; fuelUse: per tick at full throttle (of 100). */
    public record Engine(String id, String name, float power, float top, float weight, int size, float fuelUse, float pitch, boolean twelve) {
    }

    public record Frame(String id, String name, float weight, int maxEngine, float grip, float turn,
                        float[] front, float[] rear, float[] headBottom, float[] headTop, float[] engine, float seatX, float seatY) {
    }

    /** gears: how many; topMul / accelMul: what the ratios do to top speed and pull. */
    public record Gearbox(String id, String name, int gears, float topMul, float accelMul) {
    }

    /** item: what you fill up with; amount: how much of the tank one fills; power / use: what it does to the engine. */
    public record Fuel(String item, String name, float amount, float power, float use, String leftover) {
    }

    public static final Map<String, Engine> ENGINES = new LinkedHashMap<>();
    public static final Map<String, Frame> FRAMES = new LinkedHashMap<>();
    public static final Map<String, Gearbox> GEARBOXES = new LinkedHashMap<>();
    public static final Map<String, Fuel> FUELS = new LinkedHashMap<>();

    static {
        engine("engine_125", "125cc Two-Stroke", 0.020f, 0.75f, 20, 1, 0.004f, 1.25f, false);
        engine("engine_250", "250cc Two-Stroke", 0.028f, 0.95f, 25, 2, 0.006f, 1.1f, false);
        engine("engine_450", "450cc Four-Stroke", 0.036f, 1.15f, 32, 3, 0.008f, 0.9f, false);
        engine("engine_v12", "V12", 0.070f, 1.8f, 160, 4, 0.030f, 0.75f, true);
        frame("frame_standard", "Standard Frame", 70, 3, 1.0f, 1.0f, new float[] {14, 7.4f}, new float[] {-13, 7.4f}, new float[] {8, 15.5f}, new float[] {9.6f, 21},
            new float[] {0.5f, 9.5f}, -5, 17);
        frame("frame_long", "Long Frame", 95, 4, 1.1f, 0.75f, new float[] {19, 7.4f}, new float[] {-18, 7.4f}, new float[] {13, 16.5f}, new float[] {14.6f, 22},
            new float[] {0.5f, 10}, -6, 18.5f);
        gearbox("gearbox_4", "4-Speed Gearbox", 4, 1.0f, 1.0f);
        gearbox("gearbox_6", "6-Speed Gearbox", 6, 1.15f, 0.95f);
        gearbox("gearbox_race", "Close-Ratio Race Gearbox", 5, 1.05f, 1.22f);
        fuel("factoryascension:fuel_can", "Petrol", 100, 1.0f, 1.0f, "");
        fuel("factoryascension:racing_fuel", "Racing Fuel", 100, 1.22f, 1.6f, "");
        fuel("tfmg:gasoline_bucket", "Gasoline", 100, 1.0f, 1.0f, "minecraft:bucket");
        fuel("tfmg:diesel_bucket", "Diesel", 100, 0.9f, 0.7f, "minecraft:bucket");
        fuel("minecraft:blaze_rod", "Blaze Rod", 15, 1.1f, 1.2f, "");
    }

    public static void engine(String id, String name, float power, float top, float weight, int size, float fuelUse, float pitch, boolean twelve) {
        ENGINES.put(id, new Engine(id, name, power, top, weight, size, fuelUse, pitch, twelve));
    }

    static void frame(String id, String name, float weight, int maxEngine, float grip, float turn, float[] f, float[] r, float[] hb, float[] ht, float[] e, float sx, float sy) {
        FRAMES.put(id, new Frame(id, name, weight, maxEngine, grip, turn, f, r, hb, ht, e, sx, sy));
    }

    /** From the config: a frame's numbers (its anchors stay: they're the model's). */
    public static void frameStats(String id, String name, float weight, int maxEngine, float grip, float turn) {
        Frame f = FRAMES.get(id);
        if (f != null) FRAMES.put(id, new Frame(id, name, weight, maxEngine, grip, turn, f.front(), f.rear(), f.headBottom(), f.headTop(), f.engine(), f.seatX(), f.seatY()));
    }

    public static void gearbox(String id, String name, int gears, float topMul, float accelMul) {
        GEARBOXES.put(id, new Gearbox(id, name, Math.max(1, gears), topMul, accelMul));
    }

    public static void clearFuels() {
        FUELS.clear();
    }

    public static void fuel(String item, String name, float amount, float power, float use, String leftover) {
        FUELS.put(item, new Fuel(item, name, amount, power, use, leftover));
    }

    public static Engine engine(String id) {
        return ENGINES.getOrDefault(id, ENGINES.get("engine_125"));
    }

    public static Frame frame(String id) {
        return FRAMES.getOrDefault(id, FRAMES.get("frame_standard"));
    }

    public static Gearbox gearbox(String id) {
        return GEARBOXES.getOrDefault(id, GEARBOXES.get("gearbox_4"));
    }

    /** What a build does, all together. */
    public record Stats(float accel, float top, float turn, float grip, float mass, float fuelUse, int gears) {
    }

    public static Stats stats(String engine, String frame, String gearbox, float fuelPower, float fuelUse) {
        Engine e = engine(engine);
        Frame f = frame(frame);
        Gearbox g = gearbox(gearbox);
        float mass = e.weight() + f.weight() + 80;          // and a rider
        float accel = e.power() * g.accelMul() * fuelPower * 175f / mass;
        float top = e.top() * g.topMul() * (float) Math.sqrt(fuelPower);
        float turn = f.turn() * 175f / Math.max(140f, mass) ;
        return new Stats(accel, top, turn, f.grip(), mass, e.fuelUse() * fuelUse, g.gears());
    }
}
