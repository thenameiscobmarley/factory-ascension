package dev.factoryascension.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * What makes a mod mob a boss: a boss bar, minions at 2/3 and 1/3 health, an enrage below 1/4, and its attack:
 * a ground slam (Rust Colossus, Broodmother), an arrow volley (Hollow King) or a ring of fireballs (Wisp Tyrant).
 */
public final class BossBrain {
    public enum Attack { SLAM, VOLLEY, FIREBALLS }

    private final Mob boss;
    private final Attack attack;
    private final List<String> minions;
    private final ServerBossEvent bar;
    private int phase = 0, cooldown = 80;
    private boolean enraged = false;

    public BossBrain(Mob boss, BossEvent.BossBarColor color, Attack attack, List<String> minions) {
        this.boss = boss;
        this.attack = attack;
        this.minions = minions;
        this.bar = new ServerBossEvent(boss.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bar.setDarkenScreen(true);
    }

    public void seen(ServerPlayer p) { bar.addPlayer(p); }
    public void unseen(ServerPlayer p) { bar.removePlayer(p); }
    public void rename(Component name) { bar.setName(name); }

    /** Every server tick (from the boss's customServerAiStep). */
    public void tick() {
        if (!(boss.level() instanceof ServerLevel level)) return;
        float frac = boss.getHealth() / boss.getMaxHealth();
        bar.setProgress(frac);
        bar.setName(boss.getDisplayName());
        // Minions at 2/3 and 1/3 health
        if ((phase == 0 && frac < 0.66f) || (phase == 1 && frac < 0.33f)) {
            phase++;
            summon(level, 2 + phase);
        }
        if (!enraged && frac < 0.25f) {
            enraged = true;
            boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 1));
            level.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.6f);
        }
        LivingEntity target = boss.getTarget();
        if (--cooldown > 0 || target == null || !target.isAlive()) return;
        double d = boss.distanceTo(target);
        switch (attack) {
            case SLAM -> {
                if (d > 9) { cooldown = 20; return; }
                slam(level);
                cooldown = enraged ? 90 : 150;
            }
            case VOLLEY -> {
                if (d > 32) { cooldown = 20; return; }
                volley(level, target);
                cooldown = enraged ? 60 : 100;
            }
            case FIREBALLS -> {
                if (d > 32) { cooldown = 20; return; }
                fireRing(level);
                cooldown = enraged ? 80 : 140;
            }
        }
    }

    private void summon(ServerLevel level, int count) {
        level.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3f, 0.7f);
        for (int i = 0; i < count; i++) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(minions.get(i % minions.size())));
            if (type == null) continue;
            double a = Math.PI * 2 * i / count;
            var e = type.create(level);
            if (!(e instanceof Mob m)) continue;
            m.moveTo(boss.getX() + Math.cos(a) * 3, boss.getY(), boss.getZ() + Math.sin(a) * 3, boss.getYRot(), 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            scriptData(m).putBoolean("fa_minion", true);   // so the pack's scripts never give a minion a horde of its own
            m.setTarget(boss.getTarget());
            level.addFreshEntity(m);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
    }

    /** The data the pack's KubeJS scripts read (KubeJS keeps its own, apart from Forge's); Forge's if KubeJS isn't there. */
    static net.minecraft.nbt.CompoundTag scriptData(net.minecraft.world.entity.Entity e) {
        try {
            Object tag = e.getClass().getMethod("kjs$getPersistentData").invoke(e);
            if (tag instanceof net.minecraft.nbt.CompoundTag t) return t;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return e.getPersistentData();
    }

    private void slam(ServerLevel level) {
        double dmg = boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5;
        level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.2, boss.getZ(), 10, 3, 0.2, 3, 0);
        level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, 0.5f);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(boss.blockPosition()).inflate(7))) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.hurt(boss.damageSources().mobAttack(boss), (float) dmg);
            Vec3 away = p.position().subtract(boss.position()).normalize().scale(1.4);
            p.push(away.x, 0.8, away.z);
            p.hurtMarked = true;
        }
    }

    private void volley(ServerLevel level, LivingEntity target) {
        level.playSound(null, boss.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 2f, 0.6f);
        for (int i = -2; i <= 2; i++) {
            Arrow arrow = new Arrow(level, boss);
            double dx = target.getX() - boss.getX(), dz = target.getZ() - boss.getZ();
            double dy = target.getY(0.33) - arrow.getY();
            double spread = i * 0.12;
            double cos = Math.cos(spread), sin = Math.sin(spread);
            arrow.setBaseDamage(boss.getAttributeValue(Attributes.ATTACK_DAMAGE));
            arrow.shoot(dx * cos - dz * sin, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dx * sin + dz * cos, 1.8f, 4f);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            level.addFreshEntity(arrow);
        }
    }

    private void fireRing(ServerLevel level) {
        level.playSound(null, boss.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3f, 0.6f);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            SmallFireball ball = new SmallFireball(level, boss, Math.cos(a), -0.1, Math.sin(a));
            ball.setPos(boss.getX() + Math.cos(a) * 1.5, boss.getY(0.6), boss.getZ() + Math.sin(a) * 1.5);
            level.addFreshEntity(ball);
        }
    }
}
