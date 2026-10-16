package dev.factoryascension.npc;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
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
    boolean flash = false;

    /** Make it a flashbang: a blinding flash and a bang, no damage. */
    public Grenade flash() {
        flash = true;
        fuse = Math.min(fuse, 35);
        setItem(new net.minecraft.world.item.ItemStack(FactoryAscension.FLASHBANG.get()));
        return this;
    }

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
        return flash ? FactoryAscension.FLASHBANG.get() : FactoryAscension.FRAG_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            return;
        }
        if (getItem().is(FactoryAscension.FLASHBANG.get())) flash = true;
        if (--fuse <= 0) {
            explode();
        }
    }

    void explode() {
        if (flash) {
            flashBang();
            discard();
            return;
        }
        level().explode(this, getOwner() instanceof LivingEntity le ? damageSources().explosion(this, le) : null, null, getX(), getY() + 0.2, getZ(),
            3.2f, false, Level.ExplosionInteraction.NONE);
        level().playSound(null, getX(), getY(), getZ(), FASounds.GRENADE.get(), SoundSource.HOSTILE, 8, 0.9f + random.nextFloat() * 0.2f);
        discard();
    }

    /** Everyone who can see it is blinded (more the closer and the more they face it); everyone near is deafened. */
    void flashBang() {
        var level = level();
        Vec3 at = position().add(0, 0.2, 0);
        level.playSound(null, at.x, at.y, at.z, FASounds.GRENADE.get(), SoundSource.HOSTILE, 6, 1.6f);
        if (level instanceof net.minecraft.server.level.ServerLevel sl)
            sl.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20), LivingEntity::isAlive)) {
            Vec3 eye = e.getEyePosition();
            double d = eye.distanceTo(at);
            if (d > 20) continue;
            var clip = level.clip(new net.minecraft.world.level.ClipContext(eye, at, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, e));
            boolean sees = clip.getType() == net.minecraft.world.phys.HitResult.Type.MISS || clip.getLocation().distanceTo(at) < 0.6;
            if (!sees && d > 4) continue;
            double facing = e.getViewVector(1).dot(at.subtract(eye).normalize());
            int ticks = (int) Math.max(20, (1 - d / 20) * (facing > 0.3 ? 140 : 60));
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(FactoryAscension.FLASHED.get(), ticks, 0));
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, ticks / 2, 0));
            if (e instanceof Operator o) {
                o.dazzle(ticks);
                o.setTarget(null);
            }
            if (e instanceof net.minecraft.world.entity.Mob m && !(e instanceof Operator)) m.setTarget(null);
        }
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
