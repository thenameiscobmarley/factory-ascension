package dev.factoryascension.gun;

import dev.factoryascension.FASounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Gunshots as people far apart hear them: up close, the gun's own report (crack, blast, echo off the hills); past that,
 * up to a few hundred blocks, only the low rolling boom of distant gunfire, coming from the right direction. Anyone who
 * hears a shot can be startled by it (gun/Hearing: the armed NPCs come to look).
 */
public final class GunSounds {
    private GunSounds() {
    }

    /** The near report for this gun. */
    public static SoundEvent report(GunSpec spec, float loudness) {
        if (loudness < 0.5f) return FASounds.SUPPRESSED.get();
        return switch (spec.kind) {
            case PISTOL -> spec.caliber == Caliber.P50 || spec.caliber == Caliber.R44 ? FASounds.MAGNUM.get() : FASounds.PISTOL.get();
            case REVOLVER -> spec.caliber == Caliber.G12 ? FASounds.SHOTGUN.get() : FASounds.MAGNUM.get();
            case SMG -> FASounds.SMG.get();
            case RIFLE -> spec.caliber == Caliber.R762N || spec.caliber == Caliber.R762R || spec.caliber == Caliber.R44 ? FASounds.BATTLE.get() : FASounds.RIFLE.get();
            case DMR -> FASounds.BATTLE.get();
            case SNIPER -> spec.caliber == Caliber.B50 ? FASounds.FIFTY.get() : FASounds.SNIPER.get();
            case SHOTGUN -> FASounds.SHOTGUN.get();
            case LMG -> FASounds.LMG.get();
        };
    }

    /** How far the report carries before only the distant boom is heard, by loudness (1 = an ordinary rifle). */
    public static double nearRange(float loudness) {
        return Math.max(12, 96 * loudness);
    }

    public static void shot(ServerLevel level, Entity shooter, GunSpec spec, float loudness, RandomSource r) {
        float vol = (float) (nearRange(loudness) / 16);
        float pitch = 0.94f + r.nextFloat() * 0.12f;
        level.playSound(null, shooter.getX(), shooter.getY() + 1, shooter.getZ(), report(spec, loudness), SoundSource.PLAYERS, vol, pitch);
        // Distant gunfire for everyone further off, placed a little way from them in the shot's direction (so it's
        // heard from the right side, as loud as the distance allows)
        double near = nearRange(loudness), far = near * (loudness < 0.5f ? 1.6 : 3.4);
        var holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(FASounds.DISTANT.get());
        for (ServerPlayer sp : level.players()) {
            if (sp == shooter) continue;
            double d = sp.distanceTo(shooter);
            if (d <= near * 0.8 || d > far) continue;
            Vec3 dir = shooter.position().subtract(sp.position()).normalize();
            Vec3 at = sp.getEyePosition().add(dir.scale(10));
            float k = (float) (1 - (d - near * 0.8) / (far - near * 0.8));
            sp.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, at.x, at.y, at.z, 0.35f + 1.4f * k, 0.85f + r.nextFloat() * 0.25f, r.nextLong()));
        }
        Hearing.gunshot(level, shooter, near * 1.2);
    }

    /** Spent brass bouncing at the shooter's feet. */
    public static void casing(ServerLevel level, Entity shooter, GunSpec spec, RandomSource r) {
        if (spec.reload == GunSpec.Reload.CYLINDER || spec.reload == GunSpec.Reload.BREAK) return;
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), FASounds.CASING.get(), SoundSource.PLAYERS, 0.35f, 0.9f + r.nextFloat() * 0.25f);
    }
}
