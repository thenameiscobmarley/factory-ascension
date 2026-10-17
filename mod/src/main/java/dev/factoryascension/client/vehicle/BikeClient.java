package dev.factoryascension.client.vehicle;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.vehicle.BikeParts;
import dev.factoryascension.vehicle.DirtBike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** The rider's controls for the bike, every running bike's engine note, and the speedo while you ride. */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BikeClient {
    private static final Map<Integer, Engine> SOUNDS = new HashMap<>();

    private BikeClient() {
    }

    /** Called once on the client: hands the bike your controls (forward, strafe, hop, wheelie). */
    public static void init() {
        DirtBike.input = () -> {
            var mc = Minecraft.getInstance();
            var p = mc.player;
            if (p == null) return null;
            return new float[] {p.input.forwardImpulse, p.input.leftImpulse, p.input.jumping ? 1 : 0, mc.options.keySprint.isDown() ? 1 : 0};
        };
    }

    /** One bike's engine: loops, pitch with the revs, louder on the throttle. */
    static final class Engine extends AbstractTickableSoundInstance {
        final DirtBike bike;
        final float base;

        Engine(DirtBike b) {
            super(BikeParts.engine(b.engine()).twelve() ? FASounds.ENGINE_V12.get() : FASounds.ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
            bike = b;
            base = BikeParts.engine(b.engine()).pitch();
            looping = true;
            delay = 0;
            volume = 0.01f;
            x = b.getX();
            y = b.getY();
            z = b.getZ();
            attenuation = SoundInstance.Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            if (bike.isRemoved() || bike.fuel() <= 0 || bike.getPassengers().isEmpty()) {
                volume = Math.max(0, volume - 0.08f);
                if (volume <= 0) stop();
            } else {
                float target = 0.35f + 0.65f * Mth.clamp(bike.throttle * 0.6f + bike.rpm * 0.5f, 0, 1);
                volume = Mth.lerp(0.3f, volume, target);
            }
            pitch = base * (0.55f + bike.rpm * 1.25f);
            x = bike.getX();
            y = bike.getY() + 0.5;
            z = bike.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        SOUNDS.values().removeIf(Engine::isStopped);
        for (DirtBike b : mc.level.getEntitiesOfClass(DirtBike.class, mc.player.getBoundingBox().inflate(48))) {
            if (b.getPassengers().isEmpty() || b.fuel() <= 0 || SOUNDS.containsKey(b.getId())) continue;
            Engine e = new Engine(b);
            SOUNDS.put(b.getId(), e);
            mc.getSoundManager().play(e);
        }
    }

    // ---- The speedo: speed, gear, revs and fuel, bottom right ----
    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof DirtBike b) || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        int x = w - 112, y = h - 52;
        g.fillGradient(x - 6, y - 6, w - 6, h - 6, 0xC01C1C1B, 0xC00B0B0B);
        int kmh = Math.round(Math.abs(b.speed) * 72);
        g.drawString(mc.font, TarkovScreen.uiBold(String.valueOf(kmh)), x, y + 4, 0xFFF2EFE6, false);
        g.drawString(mc.font, TarkovScreen.ui("km/h"), x + 4 + mc.font.width(TarkovScreen.uiBold(String.valueOf(kmh))), y + 4, 0xFF8C8A84, false);
        var st = b.stats();
        String gear = b.speed < -0.01f ? "R" : String.valueOf(b.gear + 1);
        g.drawString(mc.font, TarkovScreen.uiBold("GEAR " + gear + "/" + st.gears()), x + 56, y, 0xFFD6D4CC, false);
        // Revs
        int bw = 98;
        g.fill(x, y + 22, x + bw, y + 26, 0xFF262626);
        int rw = Math.round(bw * Mth.clamp(b.rpm, 0, 1));
        g.fill(x, y + 22, x + rw, y + 26, b.rpm > 0.9f ? 0xFFC8452E : 0xFFC9A55A);
        // Fuel
        g.drawString(mc.font, TarkovScreen.ui("FUEL"), x, y + 31, 0xFF8C8A84, false);
        g.fill(x + 26, y + 33, x + bw, y + 37, 0xFF262626);
        g.fill(x + 26, y + 33, x + 26 + Math.round((bw - 26) * b.fuel() / 100f), y + 37, b.fuel() < 15 ? 0xFFC8452E : 0xFF6FB04A);
    }
}
