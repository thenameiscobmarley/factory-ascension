package dev.factoryascension.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Waves on open water: the depth pack's water shader (fa_depth: core/fa_water, include/fa_waves.glsl) moves the
 * surface and shades it (sky reflection, sun glint, crest foam); this hands it the camera and the time each frame,
 * and works out the same wave heights here so boats ride them (mixin/BoatWavesMixin): up and down, and tipping
 * with the slope. The numbers below must match fa_waves.glsl. Off with Water effects in Pause > Factory Ascension.
 */
public final class Waves {
    // (m, n, height, speed): the same four waves as FA_WAVES in fa_waves.glsl
    private static final float[][] WAVES = {{23, 11, 0.030f, 1.3f}, {-15, 29, 0.022f, 1.7f}, {41, -27, 0.012f, 2.6f}, {-61, -47, 0.007f, 3.4f}};
    private static boolean broken;

    private Waves() {
    }

    static boolean on() {
        return !broken && FAClientConfig.on(FAClientConfig.WATER) && dev.factoryascension.client.DepthShaders.enabled();
    }

    /** The wave clock in seconds (wraps every hour; the shader gets the same number). */
    static float time(float partial) {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : ((level.getGameTime() % 72000L) + partial) / 20f;
    }

    /** Height of the waves at world (x, z), and their slope along x and z: {height, dx, dz}. */
    public static float[] at(double x, double z, float t) {
        float px = (float) Mth.positiveModulo(x, 256.0), pz = (float) Mth.positiveModulo(z, 256.0);
        float h = 0, dx = 0, dz = 0;
        for (float[] w : WAVES) {
            float kx = w[0] * Mth.TWO_PI / 256f, kz = w[1] * Mth.TWO_PI / 256f;
            float a = kx * px + kz * pz + w[3] * t;
            h += w[2] * Mth.sin(a);
            float c = w[2] * Mth.cos(a);
            dx += c * kx;
            dz += c * kz;
        }
        return new float[] {h, dx, dz};
    }

    /** Is this entity floating on open water (water at its feet, air above)? */
    static boolean onWater(Entity e) {
        var level = e.level();
        BlockPos p = BlockPos.containing(e.getX(), e.getY() + 0.2, e.getZ());
        return level.getFluidState(p).is(FluidTags.WATER) || level.getFluidState(p.below()).is(FluidTags.WATER) && level.getBlockState(p).isAir();
    }

    /** BoatWavesMixin: lift and tip a boat with the waves under it (inside the dispatcher's own push/pop). */
    public static void ride(Entity boat, float partial, PoseStack pose) {
        if (!on() || !onWater(boat)) return;
        try {
            double x = Mth.lerp(partial, boat.xo, boat.getX()), z = Mth.lerp(partial, boat.zo, boat.getZ());
            float t = time(partial);
            float[] w = at(x, z, t);
            pose.translate(0, w[0], 0);
            // Tip with the slope (exaggerated a little, like the shading), about the world's x and z axes
            pose.mulPose(com.mojang.math.Axis.XP.rotation(w[2] * 3f));
            pose.mulPose(com.mojang.math.Axis.ZP.rotation(-w[1] * 3f));
        } catch (Throwable err) {
            broken = true;
            FAClientConfig.failSafe("Waves", err);
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void frame(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
            try {
                var shader = GameRenderer.getRendertypeTranslucentShader();
                if (shader == null) return;
                if (!on()) { shader.safeGetUniform("FaWater").set(0f, 0f, 0f, 0f); return; }
                var cam = event.getCamera().getPosition();
                shader.safeGetUniform("FaWater").set((float) Mth.positiveModulo(cam.x, 256.0), 0f, (float) Mth.positiveModulo(cam.z, 256.0),
                    Math.max(0.001f, time(event.getPartialTick())));
                float a = Minecraft.getInstance().level.getSunAngle(event.getPartialTick());
                shader.safeGetUniform("FaSun").set(-Mth.sin(a), Mth.cos(a), 0.1f);
            } catch (Throwable err) {
                broken = true;
                FAClientConfig.failSafe("Waves", err);
            }
        }
    }
}
