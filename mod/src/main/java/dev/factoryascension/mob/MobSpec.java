package dev.factoryascension.mob;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * What makes one of the mod's mobs different from the vanilla mob it's built on: what its hits do, what its arrows
 * carry, how it explodes, its stats. Each mob class reads its spec.
 */
public final class MobSpec {
    MobEffect hitEffect;
    int hitSeconds, hitAmp;
    int igniteSeconds;
    boolean lightning;
    MobEffect arrowEffect;
    int arrowSeconds, arrowAmp;
    String explosion = "";      // "", "shards" (breaks no blocks, scatters shards) or "frost" (breaks no blocks, snow and slowness)
    double health, damage, speed, armor;
    boolean miner, lays, sunProof, anyLight;
    int maxY = 10000;   // spawns only below this height (cave mobs)

    public static MobSpec of() {
        return new MobSpec();
    }

    MobSpec hit(MobEffect effect, int seconds, int amp) { hitEffect = effect; hitSeconds = seconds; hitAmp = amp; return this; }
    MobSpec ignite(int seconds) { igniteSeconds = seconds; return this; }
    MobSpec lightning() { lightning = true; return this; }
    MobSpec arrows(MobEffect effect, int seconds, int amp) { arrowEffect = effect; arrowSeconds = seconds; arrowAmp = amp; return this; }
    MobSpec explosion(String kind) { explosion = kind; return this; }
    MobSpec stats(double health, double damage, double speed) { this.health = health; this.damage = damage; this.speed = speed; return this; }
    MobSpec armor(double armor) { this.armor = armor; return this; }
    MobSpec miner() { miner = true; return this; }
    MobSpec lays() { lays = true; return this; }
    MobSpec sunProof() { sunProof = true; return this; }
    MobSpec anyLight() { anyLight = true; return this; }
    MobSpec below(int y) { maxY = y; return this; }

    /** Call after a successful melee hit. */
    void onHit(Mob attacker, Entity target) {
        if (!(target instanceof LivingEntity living) || attacker.level().isClientSide) return;
        if (hitEffect != null) living.addEffect(new MobEffectInstance(hitEffect, hitSeconds * 20, hitAmp), attacker);
        if (igniteSeconds > 0) living.setSecondsOnFire(igniteSeconds);
        if (lightning && attacker.getRandom().nextFloat() < 0.25f) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(attacker.level());
            if (bolt != null) {
                bolt.moveTo(living.getX(), living.getY(), living.getZ());
                bolt.setVisualOnly(true);
                attacker.level().addFreshEntity(bolt);
            }
        }
    }
}
