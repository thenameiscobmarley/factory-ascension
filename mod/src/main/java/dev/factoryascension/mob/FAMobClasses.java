package dev.factoryascension.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** The mod's mobs: each is a vanilla mob (its AI and model) with a MobSpec on top. */
public final class FAMobClasses {
    private FAMobClasses() {
    }

    public static class FAZombie extends Zombie {
        final MobSpec spec;

        public FAZombie(EntityType<? extends Zombie> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }

        @Override
        protected boolean convertsInWater() {
            return false;
        }

        @Override
        protected boolean isSunSensitive() {
            return !spec.sunProof && super.isSunSensitive();
        }

        @Override
        protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
            super.populateDefaultEquipmentSlots(random, difficulty);
            if (spec.miner) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        }
    }

    public static class FASkeleton extends Skeleton {
        final MobSpec spec;

        public FASkeleton(EntityType<? extends Skeleton> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        protected AbstractArrow getArrow(ItemStack stack, float power) {
            AbstractArrow arrow = super.getArrow(stack, power);
            if (spec.arrowEffect != null && arrow instanceof Arrow a) a.addEffect(new MobEffectInstance(spec.arrowEffect, spec.arrowSeconds * 20, spec.arrowAmp));
            return arrow;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FASpider extends Spider {
        final MobSpec spec;

        public FASpider(EntityType<? extends Spider> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FACreeper extends Creeper {
        final MobSpec spec;

        public FACreeper(EntityType<? extends Creeper> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        public String explosionKind() {
            return spec.explosion;
        }
    }

    public static class FASlime extends Slime {
        final MobSpec spec;

        public FASlime(EntityType<? extends Slime> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }
    }

    public static class FASilverfish extends Silverfish {
        final MobSpec spec;

        public FASilverfish(EntityType<? extends Silverfish> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FABlaze extends Blaze {
        final MobSpec spec;

        public FABlaze(EntityType<? extends Blaze> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FAEnderman extends EnderMan {
        final MobSpec spec;

        public FAEnderman(EntityType<? extends EnderMan> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FAVindicator extends Vindicator {
        final MobSpec spec;

        public FAVindicator(EntityType<? extends Vindicator> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) spec.onHit(this, target);
            return hit;
        }
    }

    public static class FAChicken extends Chicken {
        final MobSpec spec;
        private int powderTime;

        public FAChicken(EntityType<? extends Chicken> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
            this.powderTime = this.random.nextInt(6000) + 6000;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            // An Ember Hen lays blaze powder now and then, as well as eggs
            if (spec.lays && !level().isClientSide && isAlive() && !isBaby() && --powderTime <= 0) {
                spawnAtLocation(Items.BLAZE_POWDER);
                powderTime = this.random.nextInt(6000) + 6000;
            }
        }

        @Override
        public Chicken getBreedOffspring(ServerLevel level, AgeableMob mate) {
            return (Chicken) getType().create(level);
        }
    }

    public static class FACow extends Cow {
        final MobSpec spec;

        public FACow(EntityType<? extends Cow> type, Level level, MobSpec spec) {
            super(type, level);
            this.spec = spec;
        }

        @Override
        public Cow getBreedOffspring(ServerLevel level, AgeableMob mate) {
            return (Cow) getType().create(level);
        }
    }
}
