package dev.factoryascension.npc;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Bleeding: half a heart every three seconds until it runs out or you bandage it. */
public class Bleeding extends MobEffect {
    public Bleeding() {
        super(MobEffectCategory.HARMFUL, 0x8A0A0A);
    }

    @Override
    public void applyEffectTick(LivingEntity e, int amp) {
        if (e.getHealth() > 1 || amp > 0) e.hurt(e.damageSources().generic(), 1);
        if (e.level() instanceof net.minecraft.server.level.ServerLevel level)
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.55f, 0.02f, 0.02f), 1.0f),
                e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 4, 0.2, 0.25, 0.2, 0);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amp) {
        int every = Math.max(20, 60 >> amp);
        return duration % every == 0;
    }
}
