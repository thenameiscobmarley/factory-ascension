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
    /** This frame's numbers, for Embeddium's shader too: camera x mod 256, camera y's fraction, z mod 256, seconds; the sun. */
    static final float[] WATER = new float[4], SUN = {0, 1, 0};
    private static final java.util.Map<Integer, int[]> EMBEDDIUM_LOCATIONS = new java.util.HashMap<>();

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

    /** EmbeddiumWavesMixin: Embeddium's terrain shader is bound; give it this frame's numbers (raw GL: it's not ours). */
    public static void embeddium() {
        if (broken) return;
        try {
            int prog = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
            if (prog == 0) return;
            int[] loc = EMBEDDIUM_LOCATIONS.computeIfAbsent(prog, pr -> new int[] {
                org.lwjgl.opengl.GL20.glGetUniformLocation(pr, "u_FaWater"), org.lwjgl.opengl.GL20.glGetUniformLocation(pr, "u_FaSun")});
            if (loc[0] >= 0) org.lwjgl.opengl.GL20.glUniform4f(loc[0], WATER[0], WATER[1], WATER[2], WATER[3]);
            if (loc[1] >= 0) org.lwjgl.opengl.GL20.glUniform3f(loc[1], SUN[0], SUN[1], SUN[2]);
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
                var cam = event.getCamera().getPosition();
                boolean on = on();
                WATER[0] = on ? (float) Mth.positiveModulo(cam.x, 256.0) : 0;
                WATER[1] = on ? (float) (cam.y - Math.floor(cam.y)) : 0;
                WATER[2] = on ? (float) Mth.positiveModulo(cam.z, 256.0) : 0;
                WATER[3] = on ? Math.max(0.001f, time(event.getPartialTick())) : 0;
                float a = Minecraft.getInstance().level.getSunAngle(event.getPartialTick());
                SUN[0] = -Mth.sin(a); SUN[1] = Mth.cos(a); SUN[2] = 0.1f;
                var shader = GameRenderer.getRendertypeTranslucentShader();
                if (shader == null) return;
                shader.safeGetUniform("FaWater").set(WATER[0], 0f, WATER[2], WATER[3]);
                shader.safeGetUniform("FaSun").set(SUN[0], SUN[1], SUN[2]);
            } catch (Throwable err) {
                broken = true;
                FAClientConfig.failSafe("Waves", err);
            }
        }
    }
}
