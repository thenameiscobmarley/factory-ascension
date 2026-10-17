package dev.factoryascension.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.armor.NodsItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thermal imaging, through thermal goggles (down: N) or while aiming through a Thermal Scope. Living things are drawn
 * in a marker colour (magenta; how warm: undead cooler), full bright, so the thermal shader (shaders/post/fa_thermal)
 * shows them white-hot against a cold world. If anything fails, it turns itself off.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ThermalClient {
    @Nullable private static PostChain chain;
    private static int w = -1, h = -1;
    private static boolean broken = false, drawing = false;
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private ThermalClient() {
    }

    /** 0 off, 1 goggles, 2 scope. */
    public static int mode() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (broken || p == null || !mc.options.getCameraType().isFirstPerson()) return 0;
        NodsItem n = NodsItem.active(p);
        if (n != null && n.thermal) return 1;
        var st = dev.factoryascension.gun.GunStats.of(p.getMainHandItem());
        if (st != null && st.thermal && p.isUsingItem() && dev.factoryascension.client.gun.GunAnim.aim > 0.8f) return 2;
        return 0;
    }

    /** Living things, while thermal is on: drawn again, flat and full bright, in the marker colour. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void living(RenderLivingEvent.Pre<?, ?> event) {
        if (drawing || mode() == 0 || event.isCanceled()) return;
        LivingEntity e = event.getEntity();
        if (e == Minecraft.getInstance().player) return;
        int warm = e.getMobType() == MobType.UNDEAD ? 150 : 255;
        MultiBufferSource base = event.getMultiBufferSource();
        MultiBufferSource hot = type -> new Marker(base.getBuffer(RenderType.entityTranslucentEmissive(WHITE)), warm);
        drawing = true;
        try {
            @SuppressWarnings("unchecked")
            var r = (net.minecraft.client.renderer.entity.LivingEntityRenderer<LivingEntity, ?>) event.getRenderer();
            r.render(e, e.getYRot(), event.getPartialTick(), event.getPoseStack(), hot, 0xF000F0);
            event.setCanceled(true);
        } catch (Throwable err) {
            broken = true;
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Thermal drawing failed; thermal is off", err);
        } finally {
            drawing = false;
        }
    }

    /** Every vertex in the marker colour. */
    record Marker(VertexConsumer in, int warm) implements VertexConsumer {
        public VertexConsumer vertex(double x, double y, double z) { in.vertex(x, y, z); return this; }
        public VertexConsumer color(int r, int g, int b, int a) { in.color(warm, 0, warm, 255); return this; }
        public VertexConsumer uv(float u, float v) { in.uv(u, v); return this; }
        public VertexConsumer overlayCoords(int u, int v) { in.overlayCoords(u, v); return this; }
        public VertexConsumer uv2(int u, int v) { in.uv2(0xF0, 0xF0); return this; }
        public VertexConsumer normal(float x, float y, float z) { in.normal(x, y, z); return this; }
        public void endVertex() { in.endVertex(); }
        public void defaultColor(int r, int g, int b, int a) { in.defaultColor(warm, 0, warm, 255); }
        public void unsetDefaultColor() { in.unsetDefaultColor(); }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void draw(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        int m = mode();
        if (m == 0 || mc.level == null) {
            if (chain != null) free();
            return;
        }
        try {
            var main = mc.getMainRenderTarget();
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, new ResourceLocation(FactoryAscension.MODID, "shaders/post/fa_thermal.json"));
                w = h = -1;
            }
            if (w != main.width || h != main.height) {
                chain.resize(main.width, main.height);
                w = main.width;
                h = main.height;
            }
            List<PostPass> passes = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(PostChain.class, chain, "f_110009_");
            for (PostPass p : passes) {
                p.getEffect().safeGetUniform("Clock").set((net.minecraft.Util.getMillis() % 1_000_000L) / 1000f);
                p.getEffect().safeGetUniform("Scope").set(m == 2 ? 1f : 0f);
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(event.getPartialTick());
        } catch (Throwable err) {
            broken = true;
            free();
            FAClientConfig.failSafe("Thermal imaging", err);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    public static void free() {
        if (chain != null) chain.close();
        chain = null;
    }
}
