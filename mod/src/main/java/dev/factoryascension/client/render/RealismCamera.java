package dev.factoryascension.client.render;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.Dread;
import dev.factoryascension.client.FAClientConfig;
import dev.factoryascension.client.gun.GunAnim;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

/**
 * The realism camera (F8 toggles it, the Factory Ascension settings tune it): the finished picture, your hands and gun
 * included but not the HUD, drawn back through a lens (shaders/post/fa_lens.json, program fa_lens): a slight fisheye,
 * colour fringes at the edges, less colour and a cold grade, moving film grain, and a vignette that tightens and pulses
 * with your heart under stress, goes red when you're hurt, and blurs and pales after a blast close by (client/Dread rings
 * your ears). It's a post-processing chain like the game's own (the one it uses when you spectate a creeper), run at the
 * same moment the game runs those. Off with a shader pack and in safe mode; if it ever fails, it turns itself off.
 */
public final class RealismCamera {
    @Nullable private static PostChain chain;
    private static int chainW = -1, chainH = -1;
    private static boolean broken = false;
    public static final KeyMapping TOGGLE = new KeyMapping("key.factoryascension.realism", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.factoryascension");

    private RealismCamera() {
    }

    public static boolean active() {
        return !broken && FAClientConfig.on(FAClientConfig.REALISM) && !AscensionLighting.shaderPackActive();
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
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
                broken = false;
                var p = Minecraft.getInstance().player;
                if (p != null) p.displayClientMessage(Component.literal("Realism camera: " + (on ? "on" : "off") + "  (F8)"), true);
            }
            if (!active() && chain != null) free();
        }

        /** Before the HUD: the world and your hands are drawn, nothing else yet (where the game runs its own post effects). */
        @SubscribeEvent
        public static void beforeHud(RenderGuiEvent.Pre event) {
            if (!active()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            try {
                draw(mc, event.getPartialTick());
            } catch (Throwable err) {
                broken = true;
                free();
                FAClientConfig.failSafe("Realism camera", err);
            } finally {
                mc.getMainRenderTarget().bindWrite(true);
            }
        }
    }

    private static void draw(Minecraft mc, float partial) throws java.io.IOException {
        var main = mc.getMainRenderTarget();
        if (chain == null) {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, new ResourceLocation(FactoryAscension.MODID, "shaders/post/fa_lens.json"));
            chainW = chainH = -1;
        }
        if (chainW != main.width || chainH != main.height) {
            chain.resize(main.width, main.height);
            chainW = main.width;
            chainH = main.height;
        }
        java.util.List<net.minecraft.client.renderer.PostPass> passes = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(PostChain.class, chain, "f_110009_");
        java.util.function.BiConsumer<String, Float> set = (n, v) -> { for (var p : passes) p.getEffect().safeGetUniform(n).set(v); };
        float hp = mc.player.getHealth() / Math.max(1, mc.player.getMaxHealth());
        set.accept("Fisheye", FAClientConfig.FISHEYE.get().floatValue());
        set.accept("Grain", FAClientConfig.GRAIN.get().floatValue());
        set.accept("Desat", FAClientConfig.DESATURATE.get().floatValue());
        set.accept("Vignette", FAClientConfig.VIGNETTE.get().floatValue());
        set.accept("Stress", Math.max(GunAnim.stress, hp < 0.4f ? (0.4f - hp) * 2 : 0));
        set.accept("Ring", Math.max(Dread.ring(), dev.factoryascension.client.Suppression.level * 0.6f));
        set.accept("Hurt", Math.min(1.5f, Math.max(0, (0.45f - hp) * 2.2f) + mc.player.hurtTime / 10f * 0.5f));
        set.accept("Dread", Dread.dreadAmount());
        set.accept("Clock", (net.minecraft.Util.getMillis() % 1_000_000L) / 1000f);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(partial);
    }

    public static void free() {
        if (chain != null) chain.close();
        chain = null;
        chainW = chainH = -1;
    }
}
