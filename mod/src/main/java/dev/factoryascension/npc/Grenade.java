package dev.factoryascension.npc;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A frag grenade: it bounces and rolls (it doesn't go off when it hits something), and explodes when its fuse runs out.
 * The blast hurts and throws everyone near, and breaks no blocks.
 */
public class Grenade extends ThrowableItemProjectile {
    int fuse = 55;

    public Grenade(EntityType<? extends Grenade> type, Level level) {
        super(type, level);
    }

    public Grenade(Level level, LivingEntity thrower) {
        super(Operators.GRENADE.get(), thrower, level);
    }

    /** How long it has left (a thrower who cooked it sets less). */
    public void setFuse(int ticks) {
        fuse = Math.max(1, ticks);
    }

    @Override
    protected Item getDefaultItem() {
        return FactoryAscension.FRAG_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            return;
        }
        if (--fuse <= 0) {
            explode();
        }
    }

    void explode() {
        level().explode(this, getOwner() instanceof LivingEntity le ? damageSources().explosion(this, le) : null, null, getX(), getY() + 0.2, getZ(),
            3.2f, false, Level.ExplosionInteraction.NONE);
        level().playSound(null, getX(), getY(), getZ(), FASounds.GRENADE.get(), SoundSource.HOSTILE, 8, 0.9f + random.nextFloat() * 0.2f);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        // Bounce: reflect off the face it hit, losing most of its speed
        Vec3 v = getDeltaMovement();
        switch (hit.getDirection().getAxis()) {
            case X -> v = new Vec3(-v.x * 0.4, v.y * 0.6, v.z * 0.6);
            case Y -> v = new Vec3(v.x * 0.55, -v.y * 0.35, v.z * 0.55);
            default -> v = new Vec3(v.x * 0.6, v.y * 0.6, -v.z * 0.4);
        }
        setDeltaMovement(v);
        if (v.lengthSqr() > 0.01) level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 0.5f, 1.6f);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        setDeltaMovement(getDeltaMovement().scale(-0.2));
    }
}
