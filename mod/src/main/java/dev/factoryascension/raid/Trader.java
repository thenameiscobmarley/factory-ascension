package dev.factoryascension.raid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A trader at your base (raid/Traders): stays put, looks at you, can't be hurt. Right-click to trade. */
public class Trader extends PathfinderMob {
    static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(Trader.class, EntityDataSerializers.INT);

    public Trader(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, 0);
    }

    public Traders.Kind kind() {
        return Traders.Kind.values()[Math.max(0, Math.min(Traders.Kind.values().length - 1, entityData.get(KIND)))];
    }

    public void setKind(Traders.Kind k) {
        entityData.set(KIND, k.ordinal());
        setCustomName(net.minecraft.network.chat.Component.literal(k.name));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (!level().isClientSide && p instanceof ServerPlayer sp) Traders.open(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource s, float a) {
        if (s.getEntity() instanceof Player p && p.isCreative() && p.isShiftKeyDown()) return super.hurt(s, 1000);
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(KIND, t.getInt("Kind"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Kind", entityData.get(KIND));
    }
}
