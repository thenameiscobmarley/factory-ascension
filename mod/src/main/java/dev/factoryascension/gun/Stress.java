package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.network.FANetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * How stressed you are, 0 (calm) to 1 (panicking). It climbs when you get hurt, when your health is low, when hostile
 * mobs are close and after you fire a lot; it settles back down when nothing is around. Stress makes reload fumbles
 * (gun/Fumble) likelier and your hands shake (the client gets the number). No time-based anything: it only follows
 * what's happening to you right now.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Stress {
    private static final Map<ServerPlayer, float[]> STATE = new WeakHashMap<>();   // {stress, last sent, ticks since hurt, danger, danger sent}

    private Stress() {
    }

    private static float[] of(ServerPlayer p) {
        return STATE.computeIfAbsent(p, k -> new float[] {0, -1, 1000, 0, -1});
    }

    public static float get(ServerPlayer p) {
        return of(p)[0];
    }

    public static void set(ServerPlayer p, float v) {
        of(p)[0] = Math.max(0, Math.min(1, v));
    }

    public static void add(ServerPlayer p, float v) {
        set(p, get(p) + v);
    }

    /** The danger level where the player is (the pack's scripts say, as they show zone titles). */
    public static void setDanger(ServerPlayer p, int danger) {
        of(p)[3] = Math.max(0, Math.min(99, danger));
    }

    public static int danger(ServerPlayer p) {
        return (int) of(p)[3];
    }

    /** Chance that a step of a reload goes wrong. */
    public static float fumbleChance(ServerPlayer p) {
        if (!FAConfig.fumblesEnabled) return 0;
        float s = get(p);
        return Math.min(0.95f, FAConfig.fumbleBase + FAConfig.fumbleStress * s * (float) Math.sqrt(s));
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !(p instanceof net.minecraftforge.common.util.FakePlayer)) {
            add(p, Math.min(0.5f, event.getAmount() / Math.max(1, p.getMaxHealth()) * 1.6f));
            of(p)[2] = 0;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p instanceof net.minecraftforge.common.util.FakePlayer) return;
        float[] st = of(p);
        st[2]++;
        if (p.tickCount % 10 == 0) {
            int near = 0;
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(14), m -> m instanceof Enemy && m.isAlive())) {
                if (m.getTarget() == p || m.distanceToSqr(p) < 36) near++;
            }
            float delta = 0;
            if (near > 0) delta += Math.min(0.06f, 0.012f * near);
            if (p.getHealth() < p.getMaxHealth() * 0.4f) delta += 0.03f;
            if (p.isSprinting()) delta += 0.004f;
            if (near == 0 && st[2] > 100) delta -= 0.05f;   // calm: settles in about 10 seconds
            else if (near == 0) delta -= 0.015f;
            add(p, delta);
        }
        if (Math.abs(st[0] - st[1]) > 0.02f || (st[0] == 0 && st[1] != 0) || st[3] != st[4]) {
            st[1] = st[0];
            st[4] = st[3];
            FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.StressSync(st[0], (int) st[3]));
        }
    }
}
