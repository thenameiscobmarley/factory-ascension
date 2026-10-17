package dev.factoryascension.client;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Air strafing and bunny hopping, like Source games:
 *  - Air strafe: in the air, hold A or D and turn the mouse the same way: you curve and pick up speed (Quake air
 *    acceleration: a push toward where you're steering, capped so only turning gains speed; the air doesn't slow you
 *    while you strafe).
 *  - Bunny hop: jump again within a tick or two of landing and you keep your speed (ground friction never gets you).
 * Capped at a top speed. All in the config (movement); Stamina still applies.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Movement {
    private static int airTicks = 0, groundTicks = 0;
    private static Vec3 landed = Vec3.ZERO;
    private static boolean hopping = false;

    private Movement() {
    }

    static boolean free(LocalPlayer p) {
        return FAConfig.airStrafe && !p.getAbilities().flying && !p.isFallFlying() && !p.isInWater() && !p.isInLava() && !p.onClimbable()
            && !p.isPassenger() && !p.isSpectator() && p.getVehicle() == null;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.player instanceof LocalPlayer p) || p != mc.player || !event.side.isClient()) return;
        try {
            if (event.phase == TickEvent.Phase.START) start(p);
            else end(p);
        } catch (Throwable err) {
            FAConfig.airStrafe = false;
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Air strafing failed; it's off", err);
        }
    }

    private static void start(LocalPlayer p) {
        hopping = false;
        if (!free(p)) {
            airTicks = 0;
            return;
        }
        Vec3 v = p.getDeltaMovement();
        if (p.onGround()) {
            // Just landed with jump held: this tick's jump keeps the speed we came down with
            if (airTicks > 3 && FAConfig.bhop && p.input.jumping) {
                landed = new Vec3(v.x, 0, v.z);
                if (landed.horizontalDistance() < 0.01) landed = lastAir;
                hopping = true;
            }
            airTicks = 0;
            groundTicks++;
            return;
        }
        groundTicks = 0;
        airTicks++;
        lastAir = new Vec3(v.x, 0, v.z);
        float strafe = p.input.leftImpulse, fwd = p.input.forwardImpulse;
        if (strafe == 0 && fwd == 0) return;
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        float s = Mth.sin(yaw), c = Mth.cos(yaw);
        Vec3 wish = new Vec3(strafe * c - fwd * s, 0, fwd * c + strafe * s);
        if (wish.lengthSqr() < 1e-6) return;
        wish = wish.normalize();
        // Quake air acceleration: only the part of the speed along where you steer is capped
        double cur = v.x * wish.x + v.z * wish.z;
        double add = FAConfig.airCap - cur;
        double nx = v.x, nz = v.z;
        if (add > 0) {
            double acc = Math.min(add, FAConfig.airAccel);
            nx += wish.x * acc;
            nz += wish.z * acc;
        }
        // Strafing (A or D held): the air doesn't drag you (vanilla takes 9% a tick; this hands it back first)
        if (strafe != 0) {
            nx /= 0.91;
            nz /= 0.91;
        }
        double sp = Math.hypot(nx, nz), max = FAConfig.strafeMax * dev.factoryascension.client.Stamina.speedScale();
        if (sp > max && sp > Math.hypot(v.x, v.z)) {
            double k = Math.max(Math.hypot(v.x, v.z), max) / sp;
            nx *= k;
            nz *= k;
        }
        p.setDeltaMovement(nx, v.y, nz);
    }

    private static Vec3 lastAir = Vec3.ZERO;

    private static void end(LocalPlayer p) {
        if (!hopping) return;
        Vec3 v = p.getDeltaMovement();
        if (v.y <= 0) return;                          // it didn't jump after all
        double keep = landed.horizontalDistance() * FAConfig.bhopKeep;
        double now = Math.hypot(v.x, v.z);
        if (now >= keep) return;
        keep = Math.min(keep, FAConfig.strafeMax);
        Vec3 dir = landed.normalize();
        p.setDeltaMovement(dir.x * keep, v.y, dir.z * keep);
    }
}
