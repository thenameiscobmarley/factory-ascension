package dev.factoryascension.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.drugs.Drugs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What the drugs (drugs/Drugs) do to your eyes: shaders/post/fa_trip.json, before the realism camera (it films you high).
 * Each effect fades in over a few seconds and out over its last few. Off in safe mode; if it fails, it turns itself off.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TripClient {
    @Nullable private static PostChain chain;
    private static int w = -1, h = -1;
    private static boolean broken = false;

    private TripClient() {
    }

    /** 0..1: how hard an effect is hitting right now (fading out over its last 5 s; ramp() fades it in). */
    public static float strength(Player p, RegistryObject<MobEffect> effect) {
        MobEffectInstance i = p.getEffect(effect.get());
        if (i == null) return 0;
        float out = i.isInfiniteDuration() ? 1 : Math.min(1, i.getDuration() / 100f);
        float amp = 0.75f + 0.25f * Math.min(3, i.getAmplifier());
        return Math.min(1, out * amp);
    }

    private static float ramp(String key, float target) {
        Float v = RAMP.get(key);
        float cur = v == null ? 0 : v;
        cur += (target - cur) * 0.02f;          // per frame: a few seconds to come on
        if (Math.abs(target - cur) < 0.002f) cur = target;
        RAMP.put(key, cur);
        return cur;
    }

    private static final java.util.Map<String, Float> RAMP = new java.util.HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void draw(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (broken || mc.player == null || mc.level == null || FAClientConfig.SAFE_MODE.get()) return;
        Player p = mc.player;
        float stoned = ramp("stoned", strength(p, Drugs.STONED)), wired = ramp("wired", strength(p, Drugs.WIRED)),
            tweak = ramp("tweak", strength(p, Drugs.TWEAKING)), trip = ramp("trip", strength(p, Drugs.TRIPPING)),
            psych = ramp("psych", strength(p, Drugs.PSYCHOSIS)), crash = ramp("crash", strength(p, Drugs.CRASH)),
            numb = ramp("numb", strength(p, Drugs.NUMB));
        if (stoned + wired + tweak + trip + psych + crash + numb < 0.003f) {
            if (chain != null) free();
            return;
        }
        try {
            var main = mc.getMainRenderTarget();
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, new ResourceLocation(FactoryAscension.MODID, "shaders/post/fa_trip.json"));
                w = h = -1;
            }
            if (w != main.width || h != main.height) {
                chain.resize(main.width, main.height);
                w = main.width;
                h = main.height;
            }
            List<PostPass> passes = net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(PostChain.class, chain, "f_110009_");
            float clock = (net.minecraft.Util.getMillis() % 1_000_000L) / 1000f;
            for (PostPass pass : passes) {
                var e = pass.getEffect();
                e.safeGetUniform("Clock").set(clock);
                e.safeGetUniform("Stoned").set(stoned);
                e.safeGetUniform("Wired").set(wired);
                e.safeGetUniform("Tweak").set(tweak);
                e.safeGetUniform("Trip").set(trip);
                e.safeGetUniform("Psych").set(psych);
                e.safeGetUniform("Crash").set(crash);
                e.safeGetUniform("Numb").set(numb);
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(event.getPartialTick());
        } catch (Throwable err) {
            broken = true;
            free();
            FAClientConfig.failSafe("Drug effects", err);
        } finally {
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    public static void free() {
        if (chain != null) chain.close();
        chain = null;
    }
}
