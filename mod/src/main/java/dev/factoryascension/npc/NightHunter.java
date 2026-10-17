package dev.factoryascension.npc;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FASounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Something that hunts the danger zones after dark. Tall, thin, quick. It picks one of you and stalks: keeping to the
 * edge of your sight, closing in only while you're not looking; when you look right at it, it stops dead, then slips
 * back into the dark. It clicks to itself as it hunts, whispers when it's close, and shrieks when it strikes (from
 * behind if it can). Light drives it off: standing in lamplight, or with a weapon light on it. At dawn it's gone.
 * Spawned by npc/NightHunters (config: nightHunter).
 */
public class NightHunter extends Monster {
    @Nullable private Player prey;
    private int seen = 0, retreat = 0, clickIn = 40, attackCool = 0;

    public NightHunter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 40;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 70).add(Attributes.MOVEMENT_SPEED, 0.36)
            .add(Attributes.ATTACK_DAMAGE, 9).add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        // Its own mind: below
    }

    static boolean looking(Player p, LivingEntity at) {
        Vec3 to = at.getEyePosition().subtract(p.getEyePosition());
        return to.normalize().dot(p.getViewVector(1)) > 0.93 && p.hasLineOfSight(at);
    }

    static boolean lit(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) >= 9;
    }

    void play(SoundEvent s, float vol, float pitch) {
        level().playSound(null, getX(), getEyeY(), getZ(), s, SoundSource.HOSTILE, vol, pitch);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        // Dawn: gone
        if (sl.isDay() && sl.canSeeSky(blockPosition())) {
            play(FASounds.HUNTER_WHISPER.get(), 0.6f, 0.6f);
            discard();
            return;
        }
        if (prey == null || !prey.isAlive() || prey.distanceToSqr(this) > 90 * 90 || prey.isCreative() || prey.isSpectator()) {
            prey = sl.getNearestPlayer(this, 64);
            if (prey != null && (prey.isCreative() || prey.isSpectator())) prey = null;
            if (prey == null) return;
        }
        if (attackCool > 0) attackCool--;
        double d = distanceTo(prey);
        // Light drives it off
        boolean flashed = prey.getMainHandItem().getItem() instanceof dev.factoryascension.gun.GunItem && dev.factoryascension.gun.GunStats.of(prey.getMainHandItem()) != null
            && dev.factoryascension.gun.GunStats.of(prey.getMainHandItem()).light && looking(prey, this) && d < 30;
        if (lit(sl, blockPosition()) || flashed) {
            retreat = 60;
        }
        if (--clickIn <= 0) {
            clickIn = 40 + random.nextInt(80);
            play(d < 10 ? FASounds.HUNTER_WHISPER.get() : FASounds.HUNTER_CLICK.get(), d < 10 ? 0.7f : 1.2f, 0.85f + random.nextFloat() * 0.3f);
        }
        if (looking(prey, this) && d > 4) {
            // Caught in your sight: dead still, then away into the dark
            seen++;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            getLookControl().setLookAt(prey, 30, 30);
            if (seen > 25) retreat = 40;
            if (retreat <= 0) return;
        } else seen = Math.max(0, seen - 1);
        if (retreat > 0) {
            retreat--;
            Vec3 away = position().subtract(prey.position()).normalize().scale(14);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.5);
            return;
        }
        // Stalk: hold off at the edge of sight while you can see this way; close in while your back is turned
        Vec3 toMe = position().subtract(prey.position()).normalize();
        boolean behind = toMe.dot(prey.getViewVector(1)) < 0.2;
        if (d > 18 || behind) {
            getNavigation().moveTo(prey, behind ? 1.6 : 0.9);
        } else {
            Vec3 side = new Vec3(-toMe.z, 0, toMe.x).scale(random.nextBoolean() ? 6 : -6);
            if (getNavigation().isDone()) getNavigation().moveTo(getX() + side.x, getY(), getZ() + side.z, 0.8);
        }
        if (d < 2.4 && attackCool == 0 && hasLineOfSight(prey)) {
            attackCool = 30;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            play(FASounds.HUNTER_SHRIEK.get(), 1.6f, 0.9f + random.nextFloat() * 0.2f);
            doHurtTarget(prey);
            if (prey instanceof ServerPlayer sp) dev.factoryascension.gun.Stress.add(sp, 0.5f);
            retreat = 25;   // strike and fade
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return dist > 128 * 128;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s) {
        return FASounds.HUNTER_SHRIEK.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }
}
