package dev.factoryascension.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ascension Lighting: screen-space traced lighting drawn after the world (before the hand and HUD).
 * 1. fa_gi (half resolution): rays marched through the depth buffer give contact shadows and one bounce of coloured light.
 * 2. fa_blur twice: a depth-aware blur.
 * 3. fa_rays: light shafts from the sun or moon.
 * 4. fa_composite: shadows multiplied into the frame, bounced light and rays added.
 * Standard GLSL 150 only (no vendor extensions). If a shader fails to load or anything throws, safe mode turns it off.
 * Steps aside while an Oculus shader pack is in use.
 */
public final class AscensionLighting {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static ShaderInstance gi, blur, rays, composite;
    private static RenderTarget giTarget, blurTarget, raysTarget, sceneTarget;
    private static int lastW = -1, lastH = -1;
    // For the self-test and the frame-time check: how long the last frame's passes took (ns, CPU side) and how many ran
    public static long lastNanos = 0;
    public static int framesDrawn = 0;
    /** Developer views: 0 normal, 1 the shadow term, 2 the bounced light. */
    public static int debugView = 0;
    /** The last frame's sun on screen {x, y, strength} (for the developer test). */
    public static float[] lastSun = {0, 0, 0};

    private AscensionLighting() {
    }

    /** Is an Oculus/Iris shader pack in use? (Then it does the lighting.) */
    public static boolean shaderPackActive() {
        for (String cls : new String[] {"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
                Object api = Class.forName(cls).getMethod("getInstance").invoke(null);
                return (Boolean) api.getClass().getMethod("isShaderPackInUse").invoke(api);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    // ---- Shaders, loaded with the game's other shaders (a broken one only turns this feature off) ----
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) {
            gi = blur = rays = composite = null;
            try {
                event.registerShader(load(event, "fa_gi"), s -> gi = s);
                event.registerShader(load(event, "fa_blur"), s -> blur = s);
                event.registerShader(load(event, "fa_rays"), s -> rays = s);
                event.registerShader(load(event, "fa_composite"), s -> composite = s);
                event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(FactoryAscension.MODID, "fa_soft_particle"),
                    DefaultVertexFormat.PARTICLE), s -> FxParticles.softShader = s);
            } catch (Throwable err) {
                LOG.error("Ascension Lighting shaders failed to load; the effect stays off", err);
            }
        }

        private static ShaderInstance load(RegisterShadersEvent event, String name) throws java.io.IOException {
            return new ShaderInstance(event.getResourceProvider(), new ResourceLocation(FactoryAscension.MODID, name), DefaultVertexFormat.POSITION);
        }
    }

    // ---- Each frame ----
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Frame {
        @SubscribeEvent
        public static void afterLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
            lastNanos = 0;
            int q = FAClientConfig.lighting();
            if (q <= 0 || gi == null || blur == null || rays == null || composite == null || shaderPackActive()) return;
            long t0 = System.nanoTime();
            // Whatever GL state the game had, it gets back exactly: later drawing (the vignette, the hand, the HUD)
            // counts on it, and changing it shows up as a different picture
            GlState saved = GlState.save();
            try {
                draw(event, q);
                framesDrawn++;
            } catch (Throwable err) {
                FAClientConfig.failSafe("Ascension Lighting", err);
            } finally {
                saved.restore();
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
                lastNanos = System.nanoTime() - t0;
            }
        }
    }

    private static void ensureTargets(int w, int h) {
        if (w == lastW && h == lastH && giTarget != null) return;
        free();
        int hw = Math.max(1, w / 2), hh = Math.max(1, h / 2);
        giTarget = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        blurTarget = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        raysTarget = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        sceneTarget = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        for (RenderTarget t : new RenderTarget[] {giTarget, blurTarget, raysTarget, sceneTarget}) {
            t.setFilterMode(com.mojang.blaze3d.platform.GlConst.GL_LINEAR);   // the half-size result stays smooth when scaled up
            t.setClearColor(0, 0, 0, 1);
        }
        lastW = w;
        lastH = h;
    }

    /** Frees the GPU targets (on resize and when the game closes); never leaks the old ones. */
    public static void free() {
        for (RenderTarget t : new RenderTarget[] {giTarget, blurTarget, raysTarget, sceneTarget}) if (t != null) t.destroyBuffers();
        giTarget = blurTarget = raysTarget = sceneTarget = null;
        lastW = lastH = -1;
    }

    /** How many GPU targets it holds right now (the leak check expects 0 or 4). */
    public static int targetsHeld() {
        int n = 0;
        for (RenderTarget t : new RenderTarget[] {giTarget, blurTarget, raysTarget, sceneTarget}) if (t != null) n++;
        return n;
    }

    private static void draw(RenderLevelStageEvent event, int quality) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        int hw = giTarget.width, hh = giTarget.height;
        Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
        Matrix4f invProj = new Matrix4f(proj).invert();
        float strength = FAClientConfig.LIGHT_STRENGTH.get().floatValue();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        // 1. Shadows and bounce
        gi.setSampler("DiffuseSampler", main.getColorTextureId());
        gi.setSampler("DepthSampler", main.getDepthTextureId());
        gi.safeGetUniform("InvProjMat").set(invProj);
        gi.safeGetUniform("OutSize").set((float) hw, (float) hh);
        gi.safeGetUniform("Radius").set(quality == 3 ? 2.0f : 1.5f);
        gi.safeGetUniform("Samples").set(quality == 1 ? 6 : quality == 2 ? 10 : 16);
        gi.safeGetUniform("Strength").set(strength);
        pass(gi, giTarget);
        // 2. Blur: across, then down
        blur.setSampler("DepthSampler", main.getDepthTextureId());
        blur.safeGetUniform("OutSize").set((float) hw, (float) hh);
        blur.setSampler("DiffuseSampler", giTarget.getColorTextureId());
        blur.safeGetUniform("Direction").set(1f, 0f);
        pass(blur, blurTarget);
        blur.setSampler("DiffuseSampler", blurTarget.getColorTextureId());
        blur.safeGetUniform("Direction").set(0f, 1f);
        pass(blur, giTarget);
        // 3. Rays
        float[] sun = sunOnScreen(event, proj, mc);
        lastSun = sun;
        boolean raysOn = FAClientConfig.on(FAClientConfig.SUN_RAYS) && sun[2] > 0;
        rays.setSampler("DiffuseSampler", main.getColorTextureId());
        rays.setSampler("DepthSampler", main.getDepthTextureId());
        rays.safeGetUniform("SunPos").set(sun[0], sun[1]);
        rays.safeGetUniform("RayStrength").set(raysOn ? sun[2] * 0.9f * strength : 0f);
        rays.safeGetUniform("Steps").set(quality == 1 ? 16 : quality == 2 ? 28 : 48);
        pass(rays, raysTarget);

        // 4. Back onto the frame, with no blending at all: copy the frame, then draw frame x shadow + light + rays over it
        composite.setSampler("DiffuseSampler", giTarget.getColorTextureId());
        composite.setSampler("RaySampler", raysTarget.getColorTextureId());
        composite.setSampler("SceneSampler", main.getColorTextureId());
        composite.safeGetUniform("RaysOn").set(raysOn ? 1f : 0f);
        composite.safeGetUniform("Mode").set(4);
        pass(composite, sceneTarget);
        main.bindWrite(true);
        composite.setSampler("SceneSampler", sceneTarget.getColorTextureId());
        composite.safeGetUniform("Mode").set(debugView > 0 ? debugView + 1 : 0);
        quad(composite);
    }

    /** The bits of GL state the passes change, read before and put back after. */
    private record GlState(boolean blend, int srcRgb, int dstRgb, int srcA, int dstA, boolean depthTest, boolean depthMask, boolean cull) {
        static GlState save() {
            return new GlState(org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND),
                org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB), org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB),
                org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA), org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA),
                org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST), org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK),
                org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE));
        }

        void restore() {
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(depthMask);
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        }
    }


    /** Where the sun (or, at night, the moon) is on screen, and how strong its rays are: {x, y, strength}. */
    private static float[] sunOnScreen(RenderLevelStageEvent event, Matrix4f proj, Minecraft mc) {
        if (mc.level == null || !mc.level.dimensionType().hasSkyLight() || mc.level.dimensionType().hasCeiling()) return new float[] {0.5f, 0.5f, 0};
        float time = mc.level.getTimeOfDay(event.getPartialTick());
        double a = time * Math.PI * 2;
        // The sky renders the sun turned -90 about Y, then by the time of day about X: it sits at (-sin a, cos a, 0)
        float sx = (float) -Math.sin(a), sy = (float) Math.cos(a);
        float rain = 1 - mc.level.getRainLevel(event.getPartialTick());
        // The sun until it's well under the horizon (sunsets are when rays look best), then the moon, fainter
        float strength = Mth.clamp(sy * 4 + 0.6f, 0, 1) * rain;
        if (sy < -0.15f) { sx = -sx; sy = -sy; strength = Mth.clamp(sy * 4, 0, 1) * 0.3f * rain; }
        // Into view space with the camera's own turn (the event's pose stack doesn't carry it at this stage)
        var cam = mc.gameRenderer.getMainCamera();
        Matrix4f view = new Matrix4f().rotateX((float) Math.toRadians(cam.getXRot())).rotateY((float) Math.toRadians(cam.getYRot() + 180f));
        Vector4f p = new Vector4f(sx * 100, sy * 100, 0, 1);
        view.transform(p);
        p.w = 1;
        proj.transform(p);
        if (p.w <= 0) return new float[] {0.5f, 0.5f, 0};   // behind you
        float x = p.x / p.w * 0.5f + 0.5f, y = p.y / p.w * 0.5f + 0.5f;
        float offScreen = Math.max(Math.abs(x - 0.5f), Math.abs(y - 0.5f));
        strength *= Math.max(0, 1 - Math.max(0, offScreen - 0.5f) * 2f);   // fades out as it leaves the screen
        return new float[] {x, y, strength};
    }

    private static void pass(ShaderInstance shader, RenderTarget out) {
        out.setClearColor(0, 0, 0, 1);
        out.clear(Minecraft.ON_OSX);
        out.bindWrite(true);
        quad(shader);
    }

    private static void quad(ShaderInstance shader) {
        quad(shader, () -> RenderSystem.disableBlend());
    }

    /** A full-screen quad: bind the shader, then set the blend exactly as wanted, then draw. */
    private static void quad(ShaderInstance shader, Runnable blend) {
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        b.vertex(-1, -1, 0).endVertex();
        b.vertex(1, -1, 0).endVertex();
        b.vertex(1, 1, 0).endVertex();
        b.vertex(-1, 1, 0).endVertex();
        BufferBuilder.RenderedBuffer mesh = b.end();
        shader.apply();
        blend.run();
        BufferUploader.draw(mesh);
        shader.clear();
    }
}
