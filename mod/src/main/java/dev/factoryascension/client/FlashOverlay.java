package dev.factoryascension.client;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Flashed (a flashbang): the screen burns white and fades back, with the after-image of the moment, and your ears ring. */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FlashOverlay {
    private static int peak = 0;
    private static boolean was = false;

    private FlashOverlay() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        var fx = p.getEffect(FactoryAscension.FLASHED.get());
        boolean now = fx != null;
        if (now && !was) {
            peak = fx.getDuration();
            Dread.flash();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(FASounds.RINGING.get(), 1.1f, 1.0f));
        }
        was = now;
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        var fx = p.getEffect(FactoryAscension.FLASHED.get());
        if (fx == null || peak <= 0) return;
        float k = Math.min(1, fx.getDuration() / (float) Math.max(1, peak));
        float a = k > 0.6f ? 1 : k / 0.6f;
        a = a * a;
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, w, h, (Math.round(a * 255) << 24) | 0xF4F2EA);
    }
}
