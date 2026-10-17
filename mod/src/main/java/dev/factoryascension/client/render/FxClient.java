package dev.factoryascension.client.render;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of the effects: what the server reports (debris and smoke, via FANetwork.Fx), and the water around the
 * player, sampled a few dozen spots a tick: foam where water falls into water or flows fast, bubbles rising underwater,
 * and a splash with a ring of foam when something drops into water.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxClient {
    private FxClient() {
    }

    // ---- From the server ----
    public static void handle(int kind, double x, double y, double z, float size, int[] states, long[] positions) {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            Vec3 at = new Vec3(x, y, z);
            if (kind == 0 || kind == 2 || kind == 3) {
                // Broken blocks: more pieces when a player breaks one, fewer each in an explosion (there are many),
                // a couple of chips where a bullet hits
                int per = kind == 0 ? 10 : kind == 3 ? 2 : Math.max(2, 12 - states.length / 4);
                double speed = kind == 0 ? 0.12 : kind == 3 ? 0.18 : 0.25 + size * 0.05;
                for (int i = 0; i < states.length && i < positions.length; i++)
                    if (kind != 0 || !BreakChunks.justShattered(BlockPos.of(positions[i])))   // it already fell apart here
                        FxParticles.debris(level, BlockPos.of(positions[i]), Block.stateById(states[i]), kind == 0 || kind == 3 ? Vec3.atCenterOf(BlockPos.of(positions[i])).subtract(0, 0.6, 0) : at, per, speed);
            }
            if (kind == 1 || kind == 2) FxParticles.smoke(level, x, y, z, size, Vec3.ZERO);
        } catch (Throwable err) {
            FAClientConfig.failSafe("Smoke and debris", err);
        }
    }

    // ---- New world, empty particle engine: the live counts start again ----
    @SubscribeEvent
    public static void levelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            FxParticles.liveDebris = FxParticles.liveSmoke = FxParticles.liveFoam = 0;
            wet.clear();
        }
    }

    // ---- Water ----
    private static final Map<Entity, Boolean> wet = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || !FAClientConfig.on(FAClientConfig.WATER)) return;
        try {
            water(mc.level, mc.player.blockPosition(), mc.level.random);
            splashes(mc);
        } catch (Throwable err) {
            FAClientConfig.failSafe("Water effects", err);
        }
    }

    private static void water(ClientLevel level, BlockPos center, RandomSource r) {
        double amount = FAClientConfig.WATER_AMOUNT.get();
        int tries = (int) (80 * amount);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries; i++) {
            // Half the samples close by (where you'd notice), half out to 16 blocks
            int rad = i % 2 == 0 ? 7 : 16;
            p.set(center.getX() + r.nextInt(rad * 2 + 1) - rad, center.getY() + r.nextInt(13) - 6, center.getZ() + r.nextInt(rad * 2 + 1) - rad);
            FluidState f = level.getFluidState(p);
            if (!f.is(FluidTags.WATER)) continue;
            BlockPos up = p.above();
            boolean surface = !level.getFluidState(up).is(FluidTags.WATER) && level.getBlockState(up).isAir();
            if (!f.isSource() && f.getValue(FlowingFluid.FALLING)) {
                // A waterfall: foam and spray where it lands, in water or on the ground
                BlockPos below = p.below();
                FluidState fb = level.getFluidState(below);
                boolean lands = (fb.is(FluidTags.WATER) && !fb.getValue(FlowingFluid.FALLING)) || (!fb.is(FluidTags.WATER) && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP));
                if (lands) {
                    for (int k = 0; k < 5; k++)
                        FxParticles.foam(level, p.getX() + r.nextDouble(), p.getY() + 0.1, p.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2);
                    if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SPLASH, p.getX() + r.nextDouble(), p.getY() + 0.3, p.getZ() + r.nextDouble(), 0, 0.1, 0);
                }
            } else if (surface) {
                Vec3 flow = f.getFlow(level, p);
                if (flow.lengthSqr() > 0.01) {
                    // Moving water: foam streaks carried along the current
                    double h = p.getY() + f.getHeight(level, p) + 0.01;
                    FxParticles.foam(level, p.getX() + r.nextDouble(), h, p.getZ() + r.nextDouble(), flow.x * 0.08, flow.z * 0.08);
                }
            } else if (r.nextInt(4) == 0) {
                // Underwater: a few bubbles rise
                level.addParticle(ParticleTypes.BUBBLE, p.getX() + r.nextDouble(), p.getY() + r.nextDouble(), p.getZ() + r.nextDouble(), 0, 0.05, 0);
            }
        }
    }

    /** Anything dropping into water nearby throws up a splash and a ring of foam. */
    private static void splashes(Minecraft mc) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.distanceToSqr(mc.player) > 48 * 48) continue;
            boolean inWater = e.isInWater();
            Boolean before = wet.put(e, inWater);
            if (before == null || before || !inWater || e.getDeltaMovement().y > -0.25) continue;
            double speed = -e.getDeltaMovement().y;
            RandomSource r = mc.level.random;
            double y = Math.floor(e.getY()) + 1.0;
            int n = (int) Math.min(40, 10 + speed * 30);
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2, rad = e.getBbWidth() * 0.6 + r.nextDouble() * 0.4;
                mc.level.addParticle(ParticleTypes.SPLASH, e.getX() + Math.cos(a) * rad, y, e.getZ() + Math.sin(a) * rad, Math.cos(a) * 0.2, 0.3 + speed * 0.4, Math.sin(a) * 0.2);
                FxParticles.foam(mc.level, e.getX() + Math.cos(a) * rad, y - 0.05, e.getZ() + Math.sin(a) * rad, Math.cos(a) * 0.08, Math.sin(a) * 0.08);
            }
        }
    }
}
