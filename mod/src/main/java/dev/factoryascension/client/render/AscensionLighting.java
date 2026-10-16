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
 * 1. fa_gi (half resolution): rays marched through the depth buffer give contact shadows, one bounce of coloured light,
 *    and sun shadows (a ray toward the sun: whatever's on screen in the way casts a shadow).
 * 1b. fa_ssr (half resolution): screen-space reflections: water, wet ground in the rain, the sky's sheen on flat ground.
 * 2. fa_blur twice: a depth-aware blur.
 * 3. fa_rays: light shafts from the sun or moon.
 * 4. fa_composite: shadows multiplied into the frame, bounced light and rays added.
 * Standard GLSL 150 only (no vendor extensions). If a shader fails to load or anything throws, safe mode turns it off.
 * Steps aside while an Oculus shader pack is in use.
 */
public final class AscensionLighting {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static ShaderInstance gi, blur, rays, composite, ssr, glow;
    private static RenderTarget giTarget, blurTarget, raysTarget, sceneTarget, reflTarget, bloomA, bloomB, dofA, dofB;
    private static int lastW = -1, lastH = -1;
    // For the self-test and the frame-time check: how long the last frame's passes took (ns, CPU side) and how many ran
    public static long lastNanos = 0;
    public static int framesDrawn = 0;
    /** Developer views: 0 normal, 1 the shadow term, 2 the bounced light, 5 the reflections. */
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
            gi = blur = rays = composite = ssr = glow = null;
            try {
                event.registerShader(load(event, "fa_gi"), s -> gi = s);
                event.registerShader(load(event, "fa_blur"), s -> blur = s);
                event.registerShader(load(event, "fa_rays"), s -> rays = s);
                event.registerShader(load(event, "fa_composite"), s -> composite = s);
                event.registerShader(load(event, "fa_ssr"), s -> ssr = s);
                event.registerShader(load(event, "fa_glow"), s -> glow = s);
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
            if (q <= 0 || gi == null || blur == null || rays == null || composite == null || ssr == null || glow == null || shaderPackActive()) return;
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
        reflTarget = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        bloomA = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        bloomB = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        dofA = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        dofB = new TextureTarget(hw, hh, false, Minecraft.ON_OSX);
        for (RenderTarget t : all()) {
            t.setFilterMode(com.mojang.blaze3d.platform.GlConst.GL_LINEAR);   // the half-size result stays smooth when scaled up
            t.setClearColor(0, 0, 0, 1);
        }
        lastW = w;
        lastH = h;
    }

    /** Frees the GPU targets (on resize and when the game closes); never leaks the old ones. */
    public static void free() {
        for (RenderTarget t : all()) if (t != null) t.destroyBuffers();
        giTarget = blurTarget = raysTarget = sceneTarget = reflTarget = bloomA = bloomB = dofA = dofB = null;
        lastW = lastH = -1;
    }

    private static RenderTarget[] all() {
        return new RenderTarget[] {giTarget, blurTarget, raysTarget, sceneTarget, reflTarget, bloomA, bloomB, dofA, dofB};
    }

    /** How many GPU targets it holds right now (the leak check expects 0 or 9). */
    public static int targetsHeld() {
        int n = 0;
        for (RenderTarget t : all()) if (t != null) n++;
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

        // Where the sun is (view space), and which way is up
        var cam = mc.gameRenderer.getMainCamera();
        Matrix4f view = new Matrix4f().rotateX((float) Math.toRadians(cam.getXRot())).rotateY((float) Math.toRadians(cam.getYRot() + 180f));
        float[] sunWorld = sunDirection(mc, event.getPartialTick());
        org.joml.Vector3f sunView = view.transformDirection(new org.joml.Vector3f(sunWorld[0], sunWorld[1], 0)).normalize();
        org.joml.Vector3f upView = view.transformDirection(new org.joml.Vector3f(0, 1, 0)).normalize();
        float skyOpen = 0;
        float rain = 0;
        if (mc.level != null && mc.level.dimensionType().hasSkyLight()) {
            skyOpen = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, cam.getBlockPosition()) / 15f;
            rain = mc.level.getRainLevel(event.getPartialTick());
        }
        boolean shadows = FAClientConfig.on(FAClientConfig.SUN_SHADOWS);
        // 1. Shadows and bounce
        gi.setSampler("DiffuseSampler", main.getColorTextureId());
        gi.safeGetUniform("ProjMat").set(proj);
        gi.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
        gi.safeGetUniform("SunLight").set(shadows ? sunWorld[2] * skyOpen * (1 - rain * 0.8f) * Math.min(1.5f, strength) : 0f);
        gi.safeGetUniform("ShadowSteps").set(quality == 1 ? 10 : quality == 2 ? 18 : 32);
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
        // 2b. Reflections
        boolean reflOn = FAClientConfig.on(FAClientConfig.SCREEN_REFLECTIONS);
        if (reflOn) {
            ssr.setSampler("DiffuseSampler", main.getColorTextureId());
            ssr.setSampler("DepthSampler", main.getDepthTextureId());
            ssr.safeGetUniform("InvProjMat").set(invProj);
            ssr.safeGetUniform("ProjMat").set(proj);
            ssr.safeGetUniform("OutSize").set((float) hw, (float) hh);
            ssr.safeGetUniform("ViewUp").set(upView.x, upView.y, upView.z);
            net.minecraft.world.phys.Vec3 skyCol = mc.level != null ? mc.level.getSkyColor(cam.getPosition(), event.getPartialTick()) : net.minecraft.world.phys.Vec3.ZERO;
            ssr.safeGetUniform("SkyColor").set((float) skyCol.x, (float) skyCol.y, (float) skyCol.z);
            ssr.safeGetUniform("SkyOpen").set(skyOpen);
            ssr.safeGetUniform("Rain").set(rain);
            ssr.safeGetUniform("Steps").set(quality == 1 ? 16 : quality == 2 ? 28 : 48);
            ssr.safeGetUniform("Strength").set(Math.min(1.5f, strength));
            pass(ssr, reflTarget);
        } else {
            reflTarget.setClearColor(0, 0, 0, 0);
            reflTarget.clear(Minecraft.ON_OSX);
        }
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

        // 3b. Cinematic: bloom (bright pass, blurred wide) and, while aiming, a blurred copy for depth of field
        boolean grade = FAClientConfig.on(FAClientConfig.CINEMATIC);
        float aimK = FAClientConfig.on(FAClientConfig.AIM_DOF) ? dev.factoryascension.client.gun.GunAnim.aim : 0;
        glow.safeGetUniform("OutSize").set((float) hw, (float) hh);
        if (grade) {
            glow.setSampler("DiffuseSampler", main.getColorTextureId());
            glow.safeGetUniform("Mode").set(0);
            glow.safeGetUniform("Threshold").set(0.78f);
            pass(glow, bloomA);
            for (int k = 0; k < 2; k++) {
                glow.safeGetUniform("Mode").set(1);
                glow.safeGetUniform("Radius").set(k == 0 ? 2.0f : 4.5f);
                glow.setSampler("DiffuseSampler", bloomA.getColorTextureId());
                glow.safeGetUniform("Direction").set(1f, 0f);
                pass(glow, bloomB);
                glow.setSampler("DiffuseSampler", bloomB.getColorTextureId());
                glow.safeGetUniform("Direction").set(0f, 1f);
                pass(glow, bloomA);
            }
        } else {
            bloomA.setClearColor(0, 0, 0, 1);
            bloomA.clear(Minecraft.ON_OSX);
        }
        if (aimK > 0.01f) {
            glow.setSampler("DiffuseSampler", main.getColorTextureId());
            glow.safeGetUniform("Mode").set(2);
            pass(glow, dofA);
            glow.safeGetUniform("Mode").set(1);
            glow.safeGetUniform("Radius").set(1.6f);
            glow.setSampler("DiffuseSampler", dofA.getColorTextureId());
            glow.safeGetUniform("Direction").set(1f, 0f);
            pass(glow, dofB);
            glow.setSampler("DiffuseSampler", dofB.getColorTextureId());
            glow.safeGetUniform("Direction").set(0f, 1f);
            pass(glow, dofA);
        }

        // 4. Back onto the frame, with no blending at all: copy the frame, then draw frame x shadow + light + rays over it
        composite.setSampler("DiffuseSampler", giTarget.getColorTextureId());
        composite.setSampler("RaySampler", raysTarget.getColorTextureId());
        composite.setSampler("SceneSampler", main.getColorTextureId());
        composite.setSampler("ReflSampler", reflTarget.getColorTextureId());
        composite.setSampler("BloomSampler", bloomA.getColorTextureId());
        composite.setSampler("BlurSampler", dofA.getColorTextureId());
        composite.setSampler("DepthSampler", main.getDepthTextureId());
        composite.safeGetUniform("InvProjMat").set(invProj);
        composite.safeGetUniform("Grade").set(grade ? 1f : 0f);
        composite.safeGetUniform("BloomAmount").set(0.55f);
        composite.safeGetUniform("Aim").set(aimK);
        float haze = FAClientConfig.on(FAClientConfig.HAZE) ? 0.0035f * (1 + rain * 1.5f) : 0f;
        composite.safeGetUniform("Haze").set(haze);
        net.minecraft.world.phys.Vec3 fogc = mc.level != null ? mc.level.getSkyColor(cam.getPosition(), event.getPartialTick()) : net.minecraft.world.phys.Vec3.ZERO;
        float day = sunWorld[2];
        composite.safeGetUniform("HazeColor").set((float) fogc.x * 0.85f + 0.12f * day, (float) fogc.y * 0.85f + 0.12f * day, (float) fogc.z * 0.85f + 0.13f * day);
        composite.safeGetUniform("SunDir").set(sunView.x, sunView.y, sunView.z);
        composite.safeGetUniform("SunColor").set(1.0f * day, 0.86f * day, 0.66f * day);
        composite.safeGetUniform("SkyOpen").set(skyOpen);
        // The sun's glare and ghosts only when it's on screen and nothing's in front of it (the rays pass already knows)
        float sunVis = raysOn ? Math.min(1, sun[2]) * (1 - rain) : 0;
        if (sunVis > 0 && main.getDepthTextureId() > 0) sunVis *= sunOpen(sun[0], sun[1]);
        composite.safeGetUniform("SunScreen").set(sun[0], sun[1], sunVis);
        composite.safeGetUniform("Aspect").set((float) main.width / Math.max(1, main.height));
        composite.safeGetUniform("RaysOn").set(raysOn ? 1f : 0f);
        composite.safeGetUniform("Mode").set(4);
        pass(composite, sceneTarget);
        main.bindWrite(true);
        composite.setSampler("SceneSampler", sceneTarget.getColorTextureId());
        composite.safeGetUniform("Mode").set(debugView == 5 ? 6 : debugView > 0 ? debugView + 1 : 0);
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


    /** Is the sun's spot on screen open sky (not behind a hill or a wall)? Read from the depth buffer: 1 or 0. */
    private static int sunReadIn = 0;
    private static float sunWasOpen = 0;

    private static float sunOpen(float x, float y) {
        if (x < 0 || x > 1 || y < 0 || y > 1) return 0;
        if (--sunReadIn > 0) return sunWasOpen;     // a GPU read-back stalls the frame: every fourth frame is plenty
        sunReadIn = 4;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        java.nio.FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(1);
        main.bindRead();
        int fbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        org.lwjgl.opengl.GL11.glReadPixels(Math.round(x * (main.width - 1)), Math.round(y * (main.height - 1)), 1, 1,
            org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT, org.lwjgl.opengl.GL11.GL_FLOAT, buf);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, fbo);
        main.unbindRead();
        sunWasOpen = buf.get(0) >= 0.9999f ? 1 : 0;
        return sunWasOpen;
    }

    /** The sun's direction in the world {x, y, strength of its shadows}; the moon's (weaker) at night. */
    private static float[] sunDirection(Minecraft mc, float partial) {
        if (mc.level == null || !mc.level.dimensionType().hasSkyLight() || mc.level.dimensionType().hasCeiling()) return new float[] {0, 1, 0};
        double a = mc.level.getTimeOfDay(partial) * Math.PI * 2;
        float sx = (float) -Math.sin(a), sy = (float) Math.cos(a);
        float k = Mth.clamp(sy * 3, 0, 1);
        if (sy < -0.1f) { sx = -sx; sy = -sy; k = Mth.clamp(sy * 3, 0, 1) * 0.35f; }
        return new float[] {sx, Math.max(0.08f, sy), k};
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
