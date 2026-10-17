package dev.factoryascension.world;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.network.FANetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Weather that matters:
 *  - Fog banks: now and then a thick fog rolls in for a few minutes (more often out in the danger zones): you see a few
 *    dozen blocks, and so do they (operators spot you at fog distance at most).
 *  - Storms mask sound: rain and thunder cut how far gunfire and noise carry (gun/Hearing), both ways.
 *  - Darker nights are drawn by the lighting (client/render/AscensionLighting).
 * Everything here is in the config (weather).
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Weather {
    /** 0 clear .. 1 thickest, in the overworld (the server's; clients get it by FogSync). */
    public static float fog = 0;
    private static float target = 0;
    private static int left = 0, nextCheck = 1200;

    private Weather() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || !FAConfig.fogBanks) return;
        if (left > 0) {
            left--;
            if (left == 0) target = 0;
        } else if (--nextCheck <= 0) {
            nextCheck = 1200;
            // Each minute: a chance a bank rolls in (higher at night and in the rain)
            float chance = FAConfig.fogChancePerMinute * (level.isNight() ? 1.6f : 1) * (level.isRaining() ? 1.5f : 1);
            if (level.random.nextFloat() < chance) {
                target = 0.55f + level.random.nextFloat() * 0.45f;
                left = (int) ((FAConfig.fogMinutes * (0.6f + level.random.nextFloat() * 0.8f)) * 1200);
            }
        }
        float before = fog;
        fog += (target - fog) * 0.004f;                  // rolls in and out over half a minute or so
        if (Math.abs(fog - before) > 0.001f || level.getGameTime() % 100 == 0)
            for (ServerPlayer p : level.players()) FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.FogSync(fog));
    }

    /** For tests: a bank of this thickness now, for `minutes`. */
    public static void force(float thick, float minutes) {
        fog = target = thick;
        left = (int) (minutes * 1200);
    }

    /** How far you can see in the fog (blocks), or a big number when it's clear. */
    public static double sight() {
        return fog < 0.05f ? 9999 : 16 + (1 - fog) * 80;
    }

    /** Sound carries less in rain and thunder (and fog muffles a little). */
    public static double soundScale(Level level, net.minecraft.world.phys.Vec3 at) {
        double k = 1;
        if (level.isRainingAt(net.minecraft.core.BlockPos.containing(at).above(2)) || level.isRaining()) k *= 1 - 0.35 * level.getRainLevel(1);
        if (level.isThundering()) k *= 1 - 0.3 * level.getThunderLevel(1);
        k *= 1 - 0.15 * fog;
        return Math.max(0.3, k);
    }
}
