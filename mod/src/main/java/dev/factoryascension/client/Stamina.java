package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Stamina (yours, worked out on your side so it never lags): sprinting and jumping use it, holding your breath on the
 * sights (Sprint while aiming) uses it fast, and it comes back when you ease off (slower when you're thirsty). Run out
 * and you can't sprint until you've got a quarter back, and your aim wobbles. GunAnim reads holdingBreath / value.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Stamina {
    public static float value = 100;
    public static boolean exhausted = false, holdingBreath = false;
    private static boolean wasOnGround = true;

    private Stamina() {
    }

    /** Exhausted legs don't carry speed (air strafing's cap). */
    private static int groundTicks = 100;

    public static double speedScale() {
        return exhausted ? 0.6 : 1;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.isPaused()) return;
        if (p.isCreative() || p.isSpectator()) {
            value = 100;
            exhausted = holdingBreath = false;
            return;
        }
        boolean aiming = dev.factoryascension.gun.GunItem.spec(p.getMainHandItem()) != null && p.isUsingItem();
        holdingBreath = aiming && mc.options.keySprint.isDown() && !exhausted;
        float use = 0;
        if (p.isSprinting()) use += 0.55f;
        if (holdingBreath) use += 1.1f;
        // A jump costs 6; hopping on again the moment you land (a bunny hop) only 2
        if (wasOnGround && !p.onGround() && p.getDeltaMovement().y > 0.2) use += groundTicks <= 3 ? 2 : 6;
        groundTicks = p.onGround() ? groundTicks + 1 : 0;
        wasOnGround = p.onGround();
        float water = BodyHud.state != null && BodyHud.state.length >= 15 ? BodyHud.state[14] : 100;
        float regen = (p.isSprinting() || holdingBreath) ? 0 : (water < 20 ? 0.12f : 0.32f);
        value = Math.max(0, Math.min(100, value - use + regen));
        if (value <= 0) exhausted = true;
        if (exhausted && value > 25) exhausted = false;
        if (exhausted) {
            p.setSprinting(false);
            mc.options.keySprint.setDown(false);
        }
    }
}
