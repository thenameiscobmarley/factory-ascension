package dev.factoryascension.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.Dread;
import dev.factoryascension.client.FAClientConfig;
import dev.factoryascension.client.gun.GunAnim;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The realism camera (F8 toggles it, the Factory Ascension settings tune it): the finished picture, hand and gun
 * included but not the HUD, is drawn back through a lens: a slight fisheye, colour fringes at the edges, less colour
 * and a cold grade, moving film grain, and a vignette that tightens and pulses with your heart under stress, goes red
 * when you're hurt, and blurs and pales after a blast close by (gun/Dread rings your ears). Off with a shader pack,
 * and in safe mode; any failure turns it off for good (FAClientConfig.failSafe).
 */
public final class RealismCamera {
    private static ShaderInstance lens;
    private static RenderTarget scene;
    public static final KeyMapping TOGGLE = new KeyMapping("key.factoryascension.realism", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.factoryascension");

    private RealismCamera() {
    }

    public static boolean active() {
        return lens != null && FAClientConfig.on(FAClientConfig.REALISM) && !AscensionLighting.shaderPackActive();
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) {
            lens = null;
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(FactoryAscension.MODID, "fa_lens"), DefaultVertexFormat.POSITION),
                    s -> lens = s);
            } catch (Throwable err) {
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Realism camera shader failed to load; it stays off", err);
            }
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Frame {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (TOGGLE.consumeClick()) {
                boolean on = !FAClientConfig.REALISM.get();
                FAClientConfig.REALISM.set(on);
                FAClientConfig.REALISM.save();
                var p = Minecraft.getInstance().player;
                if (p != null) p.displayClientMessage(Component.literal("Realism camera: " + (on ? "on" : "off") + "  (F8)"), true);
            }
        }

        /** Before the HUD: the world and your hands are drawn, nothing else yet. */
        @SubscribeEvent
        public static void beforeHud(RenderGuiEvent.Pre event) {
            if (!active()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            GlState saved = GlState.save();
            try {
                draw(mc, event.getPartialTick());
            } catch (Throwable err) {
                FAClientConfig.failSafe("Realism camera", err);
            } finally {
                saved.restore();
                mc.getMainRenderTarget().bindWrite(true);
            }
        }
    }

    private static void draw(Minecraft mc, float partial) {
        RenderTarget main = mc.getMainRenderTarget();
        if (scene == null || scene.width != main.width || scene.height != main.height) {
            if (scene != null) scene.destroyBuffers();
            scene = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            scene.setFilterMode(com.mojang.blaze3d.platform.GlConst.GL_LINEAR);
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        float hp = mc.player.getHealth() / Math.max(1, mc.player.getMaxHealth());
        lens.safeGetUniform("OutSize").set((float) main.width, (float) main.height);
        lens.safeGetUniform("Time").set((net.minecraft.Util.getMillis() % 1_000_000L) / 1000f);
        lens.safeGetUniform("Fisheye").set(FAClientConfig.FISHEYE.get().floatValue());
        lens.safeGetUniform("Grain").set(FAClientConfig.GRAIN.get().floatValue());
        lens.safeGetUniform("Desat").set(FAClientConfig.DESATURATE.get().floatValue());
        lens.safeGetUniform("Vignette").set(FAClientConfig.VIGNETTE.get().floatValue());
        lens.safeGetUniform("Stress").set(Math.max(GunAnim.stress, hp < 0.4f ? (0.4f - hp) * 2 : 0));
        lens.safeGetUniform("Ring").set(Dread.ring());
        lens.safeGetUniform("Hurt").set(Math.max(0, Math.min(1, (0.45f - hp) * 2.2f)) + mc.player.hurtTime / 10f * 0.5f);
        lens.safeGetUniform("Dread").set(Dread.dreadAmount());
        // Copy the frame, then draw it back through the lens
        lens.setSampler("SceneSampler", main.getColorTextureId());
        lens.safeGetUniform("Mode").set(1);
        scene.setClearColor(0, 0, 0, 1);
        scene.clear(Minecraft.ON_OSX);
        scene.bindWrite(true);
        quad();
        main.bindWrite(true);
        lens.setSampler("SceneSampler", scene.getColorTextureId());
        lens.safeGetUniform("Mode").set(0);
        quad();
    }

    private static void quad() {
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        b.vertex(-1, -1, 0).endVertex();
        b.vertex(1, -1, 0).endVertex();
        b.vertex(1, 1, 0).endVertex();
        b.vertex(-1, 1, 0).endVertex();
        BufferBuilder.RenderedBuffer mesh = b.end();
        lens.apply();
        RenderSystem.disableBlend();
        BufferUploader.draw(mesh);
        lens.clear();
    }

    public static void free() {
        if (scene != null) scene.destroyBuffers();
        scene = null;
    }

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
}
