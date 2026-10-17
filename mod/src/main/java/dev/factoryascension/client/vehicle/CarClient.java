package dev.factoryascension.client.vehicle;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.network.FANetwork;
import dev.factoryascension.vehicle.Car;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** The quad's and truck's engines, the gunner's trigger, and the dash (speed, fuel, condition, the belt). */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CarClient {
    private static final Map<Integer, Engine> SOUNDS = new HashMap<>();

    private CarClient() {
    }

    static boolean gunning() {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof Car c && c.gunner() == mc.player;
    }

    static final class Engine extends AbstractTickableSoundInstance {
        final Car car;

        Engine(Car c) {
            super(FASounds.ENGINE.get(), SoundSource.NEUTRAL, RandomSource.create());
            car = c;
            looping = true;
            delay = 0;
            volume = 0.01f;
            x = c.getX();
            y = c.getY();
            z = c.getZ();
            attenuation = SoundInstance.Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            if (car.isRemoved() || car.fuel() <= 0 || car.getControllingPassenger() == null) {
                volume = Math.max(0, volume - 0.08f);
                if (volume <= 0) stop();
            } else volume = Mth.lerp(0.3f, volume, 0.4f + 0.6f * Mth.clamp(car.throttle * 0.6f + car.rpm * 0.5f, 0, 1));
            float base = car.model() == Car.Model.PICKUP ? 0.55f : 1.0f;
            pitch = base * (0.55f + car.rpm * 1.1f);
            x = car.getX();
            y = car.getY() + 0.6;
            z = car.getZ();
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
        for (Car c : mc.level.getEntitiesOfClass(Car.class, mc.player.getBoundingBox().inflate(64))) {
            if (c.getControllingPassenger() == null || c.fuel() <= 0 || SOUNDS.containsKey(c.getId())) continue;
            Engine e = new Engine(c);
            SOUNDS.put(c.getId(), e);
            mc.getSoundManager().play(e);
        }
        // The trigger: held attack while on the gun
        if (gunning() && mc.screen == null && mc.options.keyAttack.isDown()) FANetwork.CHANNEL.sendToServer(new FANetwork.TurretFire());
    }

    /** On the gun, attack fires it (it doesn't punch the truck). */
    @SubscribeEvent
    public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && gunning()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Car c) || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth(), h = event.getWindow().getGuiScaledHeight();
        int x = w - 112, y = h - 52;
        g.fillGradient(x - 6, y - 6, w - 6, h - 6, 0xC01C1C1B, 0xC00B0B0B);
        int bw = 98;
        if (c.gunner() == mc.player) {
            g.drawString(mc.font, TarkovScreen.uiBold(".50 BMG"), x, y, 0xFFF2EFE6, false);
            String ammo = String.valueOf(c.ammo());
            g.drawString(mc.font, TarkovScreen.uiBold(ammo), x, y + 16, c.ammo() < 50 ? 0xFFC8452E : 0xFFF2EFE6, false);
            g.drawString(mc.font, TarkovScreen.ui("/ " + Car.GUN_MAX), x + mc.font.width(TarkovScreen.uiBold(ammo)) + 4, y + 16, 0xFF8C8A84, false);
            g.fill(x, y + 33, x + bw, y + 37, 0xFF262626);
            g.fill(x, y + 33, x + Math.round(bw * c.ammo() / (float) Car.GUN_MAX), y + 37, 0xFFB4BEC8);
            // A ring sight in the middle of the screen
            int cx = w / 2, cy = h / 2;
            for (int k = 0; k < 24; k++) {
                double a = Math.PI * 2 * k / 24;
                g.fill(cx + (int) (Math.cos(a) * 12), cy + (int) (Math.sin(a) * 12), cx + (int) (Math.cos(a) * 12) + 1, cy + (int) (Math.sin(a) * 12) + 1, 0xC0FFFFFF);
            }
            return;
        }
        int kmh = Math.round(Math.abs(c.speed) * 72);
        g.drawString(mc.font, TarkovScreen.uiBold(String.valueOf(kmh)), x, y + 4, 0xFFF2EFE6, false);
        g.drawString(mc.font, TarkovScreen.ui("km/h"), x + 4 + mc.font.width(TarkovScreen.uiBold(String.valueOf(kmh))), y + 4, 0xFF8C8A84, false);
        float cond = 1 - c.damage() / c.model().health;
        g.drawString(mc.font, TarkovScreen.ui("COND " + Math.round(cond * 100) + "%"), x + 56, y, cond < 0.4f ? 0xFFC8452E : 0xFFD6D4CC, false);
        g.fill(x, y + 22, x + bw, y + 26, 0xFF262626);
        g.fill(x, y + 22, x + Math.round(bw * Mth.clamp(c.rpm, 0, 1)), y + 26, c.rpm > 0.9f ? 0xFFC8452E : 0xFFB4BEC8);
        g.drawString(mc.font, TarkovScreen.ui("FUEL"), x, y + 31, 0xFF8C8A84, false);
        g.fill(x + 26, y + 33, x + bw, y + 37, 0xFF262626);
        g.fill(x + 26, y + 33, x + 26 + Math.round((bw - 26) * c.fuel() / 100f), y + 37, c.fuel() < 15 ? 0xFFC8452E : 0xFF6FB04A);
    }
}
