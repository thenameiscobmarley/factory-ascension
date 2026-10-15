package dev.factoryascension.mob;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

import java.util.List;

/** The mod's four bosses: vanilla bodies scaled up (see MobRenderers), each with a BossBrain. They never despawn. */
public final class FABosses {
    private FABosses() {
    }

    public static class BossZombie extends FAMobClasses.FAZombie {
        private final BossBrain brain;

        public BossZombie(EntityType<? extends Zombie> type, Level level, MobSpec spec, BossEvent.BossBarColor color, List<String> minions) {
            super(type, level, spec);
            brain = new BossBrain(this, color, BossBrain.Attack.SLAM, minions);
            setPersistenceRequired();
        }

        @Override protected void customServerAiStep() { super.customServerAiStep(); brain.tick(); }
        @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); brain.seen(p); }
        @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); brain.unseen(p); }
        @Override public void setCustomName(Component name) { super.setCustomName(name); brain.rename(getDisplayName()); }
        @Override public boolean removeWhenFarAway(double d) { return false; }
        @Override protected boolean canReplaceCurrentItem(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b) { return false; }
        @Override public boolean isBaby() { return false; }
        @Override public void setBaby(boolean baby) { }
    }

    public static class BossSpider extends FAMobClasses.FASpider {
        private final BossBrain brain;

        public BossSpider(EntityType<? extends Spider> type, Level level, MobSpec spec, BossEvent.BossBarColor color, List<String> minions) {
            super(type, level, spec);
            brain = new BossBrain(this, color, BossBrain.Attack.SLAM, minions);
            setPersistenceRequired();
        }

        @Override protected void customServerAiStep() { super.customServerAiStep(); brain.tick(); }
        @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); brain.seen(p); }
        @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); brain.unseen(p); }
        @Override public void setCustomName(Component name) { super.setCustomName(name); brain.rename(getDisplayName()); }
        @Override public boolean removeWhenFarAway(double d) { return false; }
    }

    public static class BossSkeleton extends FAMobClasses.FASkeleton {
        private final BossBrain brain;

        public BossSkeleton(EntityType<? extends Skeleton> type, Level level, MobSpec spec, BossEvent.BossBarColor color, List<String> minions) {
            super(type, level, spec);
            brain = new BossBrain(this, color, BossBrain.Attack.VOLLEY, minions);
            setPersistenceRequired();
        }

        @Override protected void customServerAiStep() { super.customServerAiStep(); brain.tick(); }
        @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); brain.seen(p); }
        @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); brain.unseen(p); }
        @Override public void setCustomName(Component name) { super.setCustomName(name); brain.rename(getDisplayName()); }
        @Override public boolean removeWhenFarAway(double d) { return false; }
    }

    public static class BossBlaze extends FAMobClasses.FABlaze {
        private final BossBrain brain;

        public BossBlaze(EntityType<? extends Blaze> type, Level level, MobSpec spec, BossEvent.BossBarColor color, List<String> minions) {
            super(type, level, spec);
            brain = new BossBrain(this, color, BossBrain.Attack.FIREBALLS, minions);
            setPersistenceRequired();
        }

        @Override protected void customServerAiStep() { super.customServerAiStep(); brain.tick(); }
        @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); brain.seen(p); }
        @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); brain.unseen(p); }
        @Override public void setCustomName(Component name) { super.setCustomName(name); brain.rename(getDisplayName()); }
        @Override public boolean removeWhenFarAway(double d) { return false; }
    }
}
