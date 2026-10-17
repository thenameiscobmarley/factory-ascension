package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Under fire: rounds cracking past you make you flinch (the view jolts) and blur the picture (the realism camera). */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Suppression {
    public static float level = 0;
    private static float jx = 0, jy = 0;
    private static final RandomSource RNG = RandomSource.create();

    private Suppression() {
    }

    public static void hit(float amount) {
        level = Math.min(1, level + amount * 0.45f);
        jx += (RNG.nextFloat() - 0.5f) * 3.5f * amount;
        jy += (RNG.nextFloat() - 0.5f) * 2.5f * amount;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        level = Math.max(0, level - 0.012f);
        jx *= 0.7f;
        jy *= 0.7f;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (level <= 0.001f && Math.abs(jx) < 0.01f) return;
        float t = (net.minecraft.Util.getMillis() % 100000L) / 1000f;
        float shake = level * 0.6f;
        event.setYaw(event.getYaw() + jx + (float) Math.sin(t * 31) * shake);
        event.setPitch(event.getPitch() + jy + (float) Math.sin(t * 27 + 1) * shake);
    }
}
