package dev.factoryascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.armor.NodsItem;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/** Night vision goggles: N flips them; down, the view goes through the tubes (shaders/post/fa_nods.json). */
public final class NodsClient {
    public static final KeyMapping TOGGLE = new KeyMapping("key.factoryascension.nods", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.factoryascension");
    @Nullable private static PostChain chain;
    private static int w = -1, h = -1;
    private static boolean broken = false;

    private NodsClient() {
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Keys {
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
            while (TOGGLE.consumeClick()) FANetwork.CHANNEL.sendToServer(new FANetwork.NodsToggle());
        }

        /** Before the realism camera (it goes on top, like a camera filming through the goggles). */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void draw(RenderGuiEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (broken || mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
            NodsItem n = NodsItem.active(mc.player);
            if (n != null && n.thermal) n = null;   // thermal goggles: client/ThermalClient
            if (n == null) {
                if (chain != null) free();
                return;
            }
            try {
                var main = mc.getMainRenderTarget();
                if (chain == null) {
                    chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, new ResourceLocation(FactoryAscension.MODID, "shaders/post/fa_nods.json"));
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
                    p.getEffect().safeGetUniform("White").set(n.white ? 1f : 0f);
                }
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.resetTextureMatrix();
                chain.process(event.getPartialTick());
            } catch (Throwable err) {
                broken = true;
                free();
                FAClientConfig.failSafe("Night vision goggles", err);
            } finally {
                mc.getMainRenderTarget().bindWrite(true);
            }
        }
    }

    public static void free() {
        if (chain != null) chain.close();
        chain = null;
    }
}
