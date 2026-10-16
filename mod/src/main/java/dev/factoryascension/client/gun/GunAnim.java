package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.gun.Fumble;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * First-person gun animation, all procedural: walking, strafing, sprinting, jumping and landing, idle breathing and
 * fidgets, aiming, recoil, every reload step and every fumble. Nothing is a fixed clip: every motion is driven by random
 * targets that keep being re-drawn (each step of a walk, each breath, each fidget, each reload gets its own amounts,
 * timings and directions), so you never see exactly the same movement twice.
 */
public final class GunAnim {
    /** How stressed you are (from the server, gun/Stress): shaky hands. */
    public static float stress = 0;

    private static final Random RNG = new Random(System.nanoTime());

    /** A value that drifts smoothly toward a new random target, then picks another, forever. */
    static final class Wander {
        final float lo, hi, minT, maxT;
        float from, to, t0, dur;

        Wander(float lo, float hi, float minT, float maxT) {
            this.lo = lo;
            this.hi = hi;
            this.minT = minT;
            this.maxT = maxT;
            from = to = lo + (hi - lo) * RNG.nextFloat();
            dur = 0;
        }

        float at(float now) {
            if (now - t0 >= dur) {
                from = to;
                to = lo + (hi - lo) * RNG.nextFloat();
                t0 = now;
                dur = minT + (maxT - minT) * RNG.nextFloat();
            }
            float k = Mth.clamp((now - t0) / Math.max(1e-3f, dur), 0, 1);
            k = k * k * (3 - 2 * k);
            return from + (to - from) * k;
        }
    }

    /** A damped spring, for kicks and landings. */
    static final class Spring {
        float x, v;
        final float k, d;

        Spring(float k, float d) {
            this.k = k;
            this.d = d;
        }

        void step(float dt) {
            float a = -k * x - d * v;
            v += a * dt;
            x += v * dt;
        }
    }

    // Idle
    static final Wander breathRate = new Wander(0.16f, 0.34f, 3, 9), breathDepth = new Wander(0.5f, 1.3f, 2, 7);
    static final Wander swayX = new Wander(-1, 1, 0.8f, 2.6f), swayY = new Wander(-1, 1, 0.8f, 2.6f), swayRoll = new Wander(-1, 1, 1.5f, 4f);
    static final Wander tremX = new Wander(-1, 1, 0.04f, 0.11f), tremY = new Wander(-1, 1, 0.04f, 0.11f);
    // Walking: each step draws its own size, sway and twist
    static int stepIndex = Integer.MIN_VALUE;
    static float stepAmp = 1, stepAmpPrev = 1, stepSway = 0, stepSwayPrev = 0, stepTwist = 0, stepTwistPrev = 0;
    // Smoothed states
    static float strafeRoll = 0, strafeTarget = 0, forwardLean = 0, sprint = 0, aim = 0, onGroundO = 1;
    static float sprintCantY = 30, sprintCantZ = 18, sprintDrop = 0.12f;
    static int sprintStyle = 0;
    static boolean wasSprinting = false;
    static float lastStrafeSign = 0;
    // Kicks
    static final Spring kickBack = new Spring(180, 16), kickPitch = new Spring(150, 13), kickSide = new Spring(120, 12), land = new Spring(90, 9);
    // Fidgets
    static int fidget = -1;
    static float fidgetT0 = 0, fidgetDur = 1, fidgetAmp = 1, fidgetDir = 1, nextFidget = 6;
    // Reloads: each one gets its own way of doing it
    static int reloadSeen = 0;
    static float rlRoll = -35, rlYaw = 20, rlDrop = 0.08f, rlSpeedWobble = 1;
    static float last = 0;
    /** Where the gun points off the crosshair right now (degrees; + is right / down), for the hip-fire crosshair and the shot. */
    public static float aimYaw = 0, aimPitch = 0;
    // For skins: heat from firing (embers, flames) and how fast the gun is swinging (cloth strips, grass)
    static float heatV = 0, swingX = 0, swingY = 0, lastYaw = Float.NaN, lastPitch = 0;

    /** 0..1, up while firing, cooling after. */
    public static float heat() {
        return heatV;
    }

    /** How fast the gun is being swung, roughly -1..1 (0: sideways, 1: up/down), for things hanging off it. */
    public static float swayVelocity(int axis) {
        return axis == 0 ? swingX : swingY;
    }

    private GunAnim() {
    }

    static float now() {
        return (Util.getMillis() % 10_000_000L) / 1000f;
    }

    /** A shot: kick the gun back and up, with a random sideways twitch. */
    public static void kick(float recoilUp, float recoilSide, boolean aimed) {
        float s = aimed ? 0.6f : 1;
        heatV = Math.min(1, heatV + 0.12f);
        kickBack.v += (2.5f + recoilUp * 0.45f) * s * (0.85f + RNG.nextFloat() * 0.3f);
        kickPitch.v += recoilUp * 9 * s * (0.8f + RNG.nextFloat() * 0.4f);
        kickSide.v += (RNG.nextFloat() - 0.5f) * recoilSide * 14 * s;
    }

    /**
     * Move and turn the gun for this frame. `pose` is at the hand; afterwards the item model's own first-person transform
     * is applied, so x is right, y up and -z ahead of you, in blocks.
     */
    public static void apply(PoseStack pose, LocalPlayer p, GunSpec spec, ItemStack stack, float partial, float equip, int side) {
        float t = now();
        float dt = Mth.clamp(t - last, 0, 0.1f);
        last = t;
        for (Spring s : new Spring[] {kickBack, kickPitch, kickSide, land}) {
            for (int i = 0; i < 4; i++) s.step(dt / 4);
        }
        heatV = Math.max(0, heatV - dt * 0.35f);
        float yaw = p.getViewYRot(partial), pitch = p.getViewXRot(partial);
        if (!Float.isNaN(lastYaw) && dt > 0) {
            float vy = Mth.clamp(Mth.wrapDegrees(yaw - lastYaw) / Math.max(dt, 1e-3f) / 400f, -1, 1);
            float vp = Mth.clamp((pitch - lastPitch) / Math.max(dt, 1e-3f) / 400f, -1, 1);
            swingX += (vy - swingX) * Math.min(1, dt * 6);
            swingY += (vp - swingY) * Math.min(1, dt * 6);
        }
        lastYaw = yaw;
        lastPitch = pitch;
        boolean aiming = p.isUsingItem() && p.getUseItem() == stack;
        aim += ((aiming ? 1 : 0) - aim) * Math.min(1, dt * 12);
        float hip = 1 - aim;

        // ---- Movement ----
        float speed = Mth.lerp(partial, p.oBob, p.bob) * 10;                 // 0 standing .. ~1 walking .. more sprinting
        float walk = Mth.lerp(partial, p.walkDistO, p.walkDist);
        int idx = (int) Math.floor(walk * 1.0f);
        if (idx != stepIndex) {
            stepIndex = idx;
            stepAmpPrev = stepAmp; stepSwayPrev = stepSway; stepTwistPrev = stepTwist;
            stepAmp = 0.75f + RNG.nextFloat() * 0.6f;
            stepSway = (RNG.nextFloat() - 0.5f) * 0.5f;
            stepTwist = (RNG.nextFloat() - 0.5f) * 2.2f;
        }
        float f = walk - (float) Math.floor(walk);
        float fk = f * f * (3 - 2 * f);
        float amp = Mth.lerp(fk, stepAmpPrev, stepAmp), sway = Mth.lerp(fk, stepSwayPrev, stepSway), twist = Mth.lerp(fk, stepTwistPrev, stepTwist);
        float phase = walk * (float) Math.PI;
        float bobX = Mth.sin(phase) * 0.028f * amp * speed;
        float bobY = -Math.abs(Mth.cos(phase)) * 0.034f * amp * speed;
        float bobRoll = Mth.sin(phase) * (2.2f + twist) * speed;
        // Strafing: lean into it (each new strafe its own angle and lag)
        float strafe = Math.signum(p.xxa);
        if (strafe != lastStrafeSign) {
            lastStrafeSign = strafe;
            strafeTarget = strafe * (5 + RNG.nextFloat() * 6);
        }
        strafeRoll += (strafeTarget - strafeRoll) * Math.min(1, dt * (5 + RNG.nextFloat() * 3));
        forwardLean += (p.zza * 2.5f - forwardLean) * Math.min(1, dt * 6);
        // Sprinting: lowered and canted, a different carry each time you start running
        boolean sprinting = p.isSprinting() && !aiming;
        if (sprinting && !wasSprinting) {
            sprintStyle = RNG.nextInt(3);
            sprintCantY = 18 + RNG.nextFloat() * 22;
            sprintCantZ = 10 + RNG.nextFloat() * 18;
            sprintDrop = 0.08f + RNG.nextFloat() * 0.08f;
        }
        wasSprinting = sprinting;
        sprint += ((sprinting ? 1 : 0) - sprint) * Math.min(1, dt * 7);
        // Landing: a dip
        float ground = p.onGround() ? 1 : 0;
        if (ground > onGroundO) land.v -= 2.2f + RNG.nextFloat() * 1.5f;
        onGroundO = ground;

        // ---- Idle: breathing, drift, and now and then a fidget ----
        float still = Mth.clamp(1 - speed * 2, 0, 1) * hip * (1 - sprint);
        float breath = Mth.sin(t * breathRate.at(t) * 6.2832f) * 0.006f * breathDepth.at(t);
        float driftX = swayX.at(t) * 0.006f, driftY = swayY.at(t) * 0.005f, driftRoll = swayRoll.at(t) * 1.2f;
        float fidgetPose[] = fidget(t, still, GunItem.reloading(stack));
        // Stress: shaking hands (worse when stressed, a little even when aiming)
        float shake = stress * stress;
        if (p.hasEffect(dev.factoryascension.drugs.Drugs.TWEAKING.get())) shake = Math.max(shake, 0.55f);   // meth: jittery hands
        float trX = tremX.at(t) * 0.006f * shake, trY = tremY.at(t) * 0.006f * shake;

        // ---- Base pose: hip or aimed ----
        // Aimed through an optic attachment, the sight line is the optic's, higher than the iron sights
        float adsY = spec.adsY;
        dev.factoryascension.gun.GunStats stats = dev.factoryascension.gun.GunStats.of(stack);
        if (stats != null && stats.sightRaise > 0) {
            float opticY = spec.mount(dev.factoryascension.gun.Attachments.Slot.OPTIC)[1] + stats.sightRaise;
            adsY = -(2f / 16 + (opticY - 8) * spec.fpScale / 16);
        }
        // Akimbo: two guns can't both be on the sight line, so aiming only brings them up a little (both stay out wide).
        // Gangster grip: the sights are on their side, so aiming points it in front of you rather than at your eye.
        float aimK = 1;
        if (stats != null && stats.akimbo) aimK = 0.3f;
        else if (stats != null && stats.gangster) aimK = 0.55f;
        float am = aim * aimK, hp = 1 - am;
        float x = side * spec.hipX * hp, y = spec.hipY * hp + adsY * am, z = spec.hipZ * hp + spec.adsZ * am;
        if (stats != null && stats.gangster) x += side * 0.06f * aim;
        float moveK = hip + aim * 0.15f;
        x += (bobX + sway * 0.02f * speed) * moveK + driftX * (hip + aim * 0.2f) + trX;
        y += (bobY + breath) * moveK + driftY * (hip + aim * 0.2f) + trY + land.x * 0.03f * moveK;
        z += kickBack.x * 0.035f;
        // Sprint carry
        y -= sprint * sprintDrop;
        x -= sprint * side * (sprintStyle == 1 ? 0.12f : 0.05f);
        z += sprint * 0.06f;
        // Drawing the gun (equip goes 1 -> 0)
        y -= equip * 0.6f;
        // Reload and bolt work
        float[] rl = reload(stack, spec, t);

        pose.translate(x + fidgetPose[0] + rl[0], y + fidgetPose[1] + rl[1], z + fidgetPose[2] + rl[2]);
        // The barrel's own aim, off the crosshair (only at the hip: aimed, the sights are on it)
        float hipK = Math.max(0, 1 - aim * 1.2f);
        aimYaw = -(twist * speed * moveK + fidgetPose[4] + kickSide.x * 0.4f + strafeRoll * 0.05f) * hipK * 0.8f;
        aimPitch = -(kickPitch.x * 0.35f + fidgetPose[3] + (bobY + breath) * 40 * moveK) * hipK * 0.8f;
        pose.mulPose(Axis.YP.rotationDegrees(side * (-sprint * sprintCantY * (sprintStyle == 2 ? 1.3f : 1)) + twist * speed * moveK + fidgetPose[4] + rl[4] + kickSide.x * 0.4f));
        pose.mulPose(Axis.XP.rotationDegrees(kickPitch.x * 0.35f - forwardLean * moveK * 0.4f + fidgetPose[3] + rl[3] + sprint * (sprintStyle == 0 ? 12 : 4)));
        // Gangster grip: the gun turned flat on its side, held up and in
        if (stats != null && stats.gangster) {
            pose.translate(-side * 0.05f, 0.1f, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 82));
        }
        pose.mulPose(Axis.ZP.rotationDegrees(side * (bobRoll * moveK + strafeRoll * (hip + aim * 0.35f) + driftRoll * hip + sprint * sprintCantZ) + fidgetPose[5] + rl[5]));
    }

    /** Idle fidgets: {x, y, z, pitch, yaw, roll}. One at a time, each its own size, side and speed. */
    static float[] fidget(float t, float still, boolean reloading) {
        float[] o = new float[6];
        if (still < 0.9f || reloading) {
            fidget = -1;
            nextFidget = t + 3 + RNG.nextFloat() * 6;
            return o;
        }
        if (fidget < 0) {
            if (t < nextFidget) return o;
            fidget = RNG.nextInt(7);
            fidgetT0 = t;
            fidgetDur = 0.8f + RNG.nextFloat() * 1.4f;
            fidgetAmp = 0.6f + RNG.nextFloat() * 0.8f;
            fidgetDir = RNG.nextBoolean() ? 1 : -1;
        }
        float k = (t - fidgetT0) / fidgetDur;
        if (k >= 1) {
            fidget = -1;
            nextFidget = t + 4 + RNG.nextFloat() * 10;
            return o;
        }
        float e = Mth.sin(k * (float) Math.PI);              // out and back
        e = e * e * (3 - 2 * e);
        float a = fidgetAmp;
        switch (fidget) {
            case 0 -> o[5] = 24 * a * e * fidgetDir;                                        // cant it and look along the side
            case 1 -> { o[1] = -0.035f * a * Mth.sin(k * 12.56f) * (1 - k); }               // re-grip: a little bounce
            case 2 -> { o[4] = 32 * a * e * fidgetDir; o[3] = -10 * a * e; o[0] = -0.06f * e * fidgetDir; }   // turn it to look it over
            case 3 -> { o[1] = 0.03f * a * e; o[3] = -5 * a * e; }                          // shoulder roll
            case 4 -> { o[5] = -38 * a * e; o[1] = -0.05f * e; o[3] = 8 * e; }              // check the mag / chamber
            case 5 -> { o[2] = -0.03f * a * Mth.sin(k * 6.28f); }                            // tap forward
            default -> { o[4] = 8 * a * Mth.sin(k * 18.8f) * (1 - k) * fidgetDir; }         // restless wiggle
        }
        return o;
    }

    /** Reloads and bolt work: {x, y, z, pitch, yaw, roll}. */
    static float[] reload(ItemStack stack, GunSpec spec, float t) {
        float[] o = new float[6];
        if (!stack.hasTag()) return o;
        int bolt = stack.getTag().getInt("Bolt");
        if (bolt > 0) {
            // Working the bolt / racking: roll to the side and a sharp pull back and forward
            float k = 1 - bolt / (float) Math.max(1, spec.chamber + 4);
            float e = Mth.sin(k * (float) Math.PI);
            o[5] = -22 * e;
            o[2] = 0.05f * Mth.sin(k * 6.28f) * e;
        }
        int ph = GunItem.phase(stack);
        if (ph == GunItem.NONE) return o;
        int id = stack.getTag().getInt("RPh") * 31 + stack.getTag().getInt("RL") + stack.getTag().getInt("Ammo") * 7;
        if (id != reloadSeen) {
            reloadSeen = id;
            rlRoll = -(25 + RNG.nextFloat() * 25);
            rlYaw = 12 + RNG.nextFloat() * 18;
            rlDrop = 0.05f + RNG.nextFloat() * 0.07f;
            rlSpeedWobble = 0.8f + RNG.nextFloat() * 0.4f;
        }
        float k = GunItem.phaseProgress(stack);
        // The reload pose: tipped onto its side, turned toward you, a little lower
        o[5] += rlRoll;
        o[4] += rlYaw;
        o[1] -= rlDrop;
        o[3] += 6;
        switch (ph) {
            case GunItem.OUT -> {
                // Mag out: a pull down halfway through
                float jerk = smoothPulse(k, 0.45f, 0.18f);
                o[1] -= 0.08f * jerk;
                o[3] -= 10 * jerk;
            }
            case GunItem.IN -> {
                // Mag in: lined up, then pushed home with a jolt
                float push = smoothPulse(k, 0.7f, 0.12f);
                o[1] += 0.06f * push;
                o[3] += 6 * push;
            }
            case GunItem.CHAMBER -> {
                // Chambering: tilt up, pull back, snap forward
                o[5] -= rlRoll * 0.6f;
                o[2] += 0.07f * smoothPulse(k, 0.55f, 0.15f);
                o[3] -= 8 * smoothPulse(k, 0.55f, 0.25f);
            }
            case GunItem.ROUND -> {
                // One round in: a small dip and push
                float push = smoothPulse(k, 0.6f, 0.2f);
                o[1] -= 0.03f * push;
                o[3] += 4 * push;
            }
            case GunItem.FUMBLE -> fumbleMotion(o, GunItem.fumble(stack), k, t);
            default -> { }
        }
        return o;
    }

    static float smoothPulse(float k, float at, float width) {
        float d = Math.abs(k - at) / width;
        if (d >= 1) return 0;
        float e = 1 - d;
        return e * e * (3 - 2 * e);
    }

    /** What each fumble looks like. */
    static void fumbleMotion(float[] o, Fumble f, float k, float t) {
        if (f == null) return;
        float w = rlSpeedWobble;
        switch (f) {
            case MAG_STUCK, COVER_STUCK, CYLINDER_STUCK, HINGE_STIFF, BOLT_WONT_OPEN -> {
                // Tugging: two hard pulls with shaking in between
                o[1] -= 0.06f * (smoothPulse(k, 0.3f, 0.12f) + smoothPulse(k, 0.72f, 0.12f));
                o[0] += 0.012f * Mth.sin(t * 50 * w) * (1 - k);
                o[5] += 6 * Mth.sin(t * 38 * w) * (1 - k);
            }
            case DROPPED_MAG, MAG_SLIPPED, DROPPED_ROUND -> {
                // It drops: the hands follow it down, then come back
                float down = smoothPulse(k, 0.4f, 0.4f);
                o[1] -= 0.16f * down;
                o[3] += 18 * down;
                o[4] += 10 * down;
            }
            case MISSED_WELL -> {
                // Bounces off the well: up, knock, again
                o[1] += 0.04f * smoothPulse(k, 0.3f, 0.1f) - 0.03f * smoothPulse(k, 0.45f, 0.1f) + 0.05f * smoothPulse(k, 0.8f, 0.1f);
                o[0] += 0.02f * Mth.sin(k * 25);
            }
            case MAG_BACKWARDS, ROUND_BACKWARDS -> {
                // Turn it round
                o[5] += 50 * smoothPulse(k, 0.5f, 0.35f);
                o[4] -= 15 * smoothPulse(k, 0.5f, 0.35f);
            }
            case NOT_SEATED -> o[1] += 0.07f * smoothPulse(k, 0.6f, 0.08f);          // the slap
            case WRONG_MAG, FUMBLED_POUCH -> {
                // Looking down, rummaging
                o[1] -= 0.1f * smoothPulse(k, 0.5f, 0.45f);
                o[0] -= 0.04f * Mth.sin(k * 20 * w) * smoothPulse(k, 0.5f, 0.45f);
                o[3] += 12 * smoothPulse(k, 0.5f, 0.45f);
            }
            case BELT_TANGLED -> {
                o[0] += 0.03f * Mth.sin(t * 9 * w);
                o[5] += 14 * Mth.sin(t * 7 * w);
                o[1] -= 0.05f;
            }
            case HANDLE_SLIPPED, SLIDE_STUCK, BOLT_STICKY -> {
                // Rack, slip, rack again
                o[2] += 0.07f * (smoothPulse(k, 0.25f, 0.1f) + smoothPulse(k, 0.7f, 0.12f));
                o[5] -= 15 * smoothPulse(k, 0.5f, 0.4f);
            }
            case DOUBLE_FEED -> {
                // Rip the mag out, rack three times, shake it
                o[1] -= 0.09f * smoothPulse(k, 0.15f, 0.1f);
                for (float a = 0.35f; a < 0.9f; a += 0.17f) o[2] += 0.07f * smoothPulse(k, a, 0.06f);
                o[5] += 10 * Mth.sin(t * 30 * w) * smoothPulse(k, 0.6f, 0.4f);
            }
            case STOVEPIPE, SHELL_JAMMED -> {
                // Sweep across the port
                o[4] += 25 * smoothPulse(k, 0.5f, 0.3f);
                o[0] -= 0.05f * smoothPulse(k, 0.5f, 0.3f);
            }
            default -> o[0] += 0.004f * Mth.sin(t * 60);
        }
    }
}
