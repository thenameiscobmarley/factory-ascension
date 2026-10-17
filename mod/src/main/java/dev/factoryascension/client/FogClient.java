package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.world.Weather;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fog banks (world/Weather): the view closes in, grey and soft, while one's over you (only in the overworld, outdoors). */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FogClient {
    private FogClient() {
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        var mc = Minecraft.getInstance();
        if (Weather.fog < 0.02f || mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        if (event.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
        float k = Weather.fog;
        float far = (float) Weather.sight();
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.05f * (1 - k) + 1));
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void colour(ViewportEvent.ComputeFogColor event) {
        if (Weather.fog < 0.02f) return;
        float k = Math.min(1, Weather.fog * 1.2f);
        float g = 0.62f * (event.getRed() + event.getGreen() + event.getBlue()) / 3f + 0.2f;
        event.setRed(event.getRed() + (g - event.getRed()) * k);
        event.setGreen(event.getGreen() + (g - event.getGreen()) * k);
        event.setBlue(event.getBlue() + (g * 1.03f - event.getBlue()) * k);
    }
}
