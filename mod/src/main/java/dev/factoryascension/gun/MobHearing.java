package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Gunfire draws monsters: every hostile mob that hears a shot (a hundred blocks for a rifle, a few for a suppressed
 * pistol: gun/GunSounds.nearRange) heads for it, and the ones close enough to see the shooter go for them. The
 * hordes come running when you shoot; a suppressor keeps them away. (The armed operators have their own hearing.)
 */
public final class MobHearing {
    private MobHearing() {
    }

    public static void heard(ServerLevel level, Entity shooter, double range) {
        if (!FAConfig.mobsHearShots || !(shooter instanceof Player p) || p.isCreative() || p.isSpectator()) return;
        double r = range * FAConfig.mobHearingScale;
        int moved = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, shooter.getBoundingBox().inflate(r), m -> m instanceof Enemy && m.isAlive()
            && !(m instanceof dev.factoryascension.npc.Operator) && m.getTarget() == null)) {
            if (m.distanceToSqr(shooter) > r * r || moved++ > 60) continue;
            if (m.distanceTo(shooter) < 32 && m.hasLineOfSight(shooter)) {
                m.setTarget((LivingEntity) shooter);
            } else {
                double err = m.distanceTo(shooter) * 0.1;
                m.getNavigation().moveTo(shooter.getX() + (m.getRandom().nextDouble() - 0.5) * err, shooter.getY(),
                    shooter.getZ() + (m.getRandom().nextDouble() - 0.5) * err, 1.1);
            }
        }
    }
}
