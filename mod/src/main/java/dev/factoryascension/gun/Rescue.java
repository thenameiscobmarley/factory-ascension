package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A red flare is a call for rescue. Hopefully someone sees it: after a wait (FAConfig.rescueDelay), with
 * FAConfig.rescueChance, a helicopter comes. You hear it coming, it circles the flare for a few seconds, and if you're
 * still within FAConfig.rescueRadius of where you fired it, you're pulled out: taken home (your bed, or world spawn) with
 * everything you're carrying. Wander off, and it leaves without you. Or no one answers at all.
 * Either way, a flare is seen for miles: anyone armed out there (the NPC operators) comes to see who sent it.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Rescue {
    /** A rescue on its way: where, when it arrives, whether anyone's actually coming. */
    public record Call(ServerLevel level, Vec3 lz, long arrives, boolean coming, int circling) {
    }

    private static final Map<UUID, Call> CALLS = new HashMap<>();
    /** Told when a flare goes up (the operators come to look). */
    public static Consumer<Call> onFlare = c -> { };
    /** For the self-test: what the last rescue did ("extracted", "missed", "nobody"). */
    public static String last = "";

    private Rescue() {
    }

    public static boolean pending(ServerPlayer p) {
        return CALLS.containsKey(p.getUUID());
    }

    public static void call(ServerPlayer p) {
        if (!FAConfig.rescueEnabled) return;
        if (CALLS.containsKey(p.getUUID())) {
            p.displayClientMessage(Component.literal("They already have your position. Hold on.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean coming = p.getRandom().nextFloat() < FAConfig.rescueChance;
        Call c = new Call(p.serverLevel(), p.position(), p.serverLevel().getGameTime() + FAConfig.rescueDelay, coming, 0);
        CALLS.put(p.getUUID(), c);
        p.displayClientMessage(Component.literal("Red flare up. If anyone's out there, they'll come for you here. Hold this spot ("
            + Math.max(1, FAConfig.rescueDelay / 20) + "s).").withStyle(ChatFormatting.RED), true);
        try {
            onFlare.accept(c);
        } catch (Throwable ignored) {
            // The hunters are a bonus; the rescue goes on regardless
        }
    }

    /** For the self-test: arrive now. */
    public static void arriveNow(ServerPlayer p, boolean coming) {
        Call c = CALLS.get(p.getUUID());
        if (c != null) CALLS.put(p.getUUID(), new Call(c.level, c.lz, c.level.getGameTime(), coming, c.circling));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CALLS.isEmpty()) return;
        var server = event.getServer();
        CALLS.entrySet().removeIf(e -> {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Call c = e.getValue();
            if (p == null || !p.isAlive()) return true;
            long now = c.level.getGameTime();
            long left = c.arrives - now;
            // Red smoke on the spot while you wait
            if (left > 0) {
                if (left % 5 == 0) {
                    double gy = c.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(c.lz.x), (int) Math.floor(c.lz.z)) + 0.2;
                    c.level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, c.lz.x, gy, c.lz.z, 1, 0.1, 0.1, 0.1, 0.02);
                    c.level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9f, 0.1f, 0.05f), 2.0f), c.lz.x, gy + 0.8, c.lz.z, 3, 0.3, 0.6, 0.3, 0);
                }
                // The rotors, faint and growing, the last twenty seconds
                if (c.coming && left < 400 && left % 80 == 0) {
                    float k = 1 - left / 400f;
                    Vec3 from = c.lz.add(Math.cos(now * 0.01) * 60 * (1 - k), 30, Math.sin(now * 0.01) * 60 * (1 - k));
                    c.level.playSound(null, from.x, from.y, from.z, FASounds.HELICOPTER.get(), SoundSource.NEUTRAL, 2 + 4 * k, 0.95f);
                }
                return false;
            }
            if (!c.coming) {
                p.displayClientMessage(Component.literal("No one answered your flare.").withStyle(ChatFormatting.DARK_RED), true);
                last = "nobody";
                return true;
            }
            // Overhead now: circle for a few seconds
            if (c.circling < 160) {
                e.setValue(new Call(c.level, c.lz, c.arrives, true, c.circling + 1));
                double a = c.circling * 0.12;
                Vec3 heli = c.lz.add(Math.cos(a) * 10, 22, Math.sin(a) * 10);
                if (c.circling % 80 == 0) c.level.playSound(null, heli.x, heli.y, heli.z, FASounds.HELICOPTER.get(), SoundSource.NEUTRAL, 7, 1.0f);
                c.level.sendParticles(ParticleTypes.CLOUD, heli.x, heli.y - 2, heli.z, 4, 1.5, 0.3, 1.5, 0.02);
                c.level.sendParticles(ParticleTypes.POOF, c.lz.x, c.lz.y + 0.2, c.lz.z, 6, 3, 0.1, 3, 0.15);   // rotor wash on the ground
                if (c.circling == 0) p.displayClientMessage(Component.literal("Helicopter overhead. Get to the flare!").withStyle(ChatFormatting.YELLOW), true);
                return false;
            }
            if (p.level() == c.level && p.position().distanceTo(c.lz) <= FAConfig.rescueRadius) extract(p);
            else {
                p.displayClientMessage(Component.literal("The helicopter circled your flare, but you weren't there. It's gone.").withStyle(ChatFormatting.DARK_RED), true);
                last = "missed";
            }
            return true;
        });
    }

    /** Out: home with everything you're carrying. */
    public static void extract(ServerPlayer p) {
        var server = p.server;
        ServerLevel to = server.getLevel(p.getRespawnDimension());
        BlockPos bed = p.getRespawnPosition();
        Vec3 at = null;
        if (to != null && bed != null) at = Player.findRespawnPositionAndUseSpawnBlock(to, bed, p.getRespawnAngle(), p.isRespawnForced(), true).orElse(null);
        if (at == null) {
            to = server.overworld();
            BlockPos s = to.getSharedSpawnPos();
            at = new Vec3(s.getX() + 0.5, to.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ()), s.getZ() + 0.5);
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), FASounds.HELICOPTER.get(), SoundSource.NEUTRAL, 4, 1.05f);
        p.teleportTo(to, at.x, at.y, at.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        Stress.set(p, 0);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("You made it out with everything you carried").withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("EXTRACTED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
        to.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
        last = "extracted";
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        CALLS.clear();
    }
}
