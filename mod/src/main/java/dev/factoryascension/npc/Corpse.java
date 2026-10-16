package dev.factoryascension.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A body where an operator fell. It stays a while, and anyone from its side who sees it raises the alarm. Sneak and
 * right-click it to hide it (it's gone, and so is the evidence).
 */
public class Corpse extends Entity {
    static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(Corpse.class, EntityDataSerializers.STRING);
    int life = 0;

    public Corpse(EntityType<? extends Corpse> type, Level level) {
        super(type, level);
    }

    public String kind() {
        return entityData.get(KIND);
    }

    public void setKind(String k) {
        entityData.set(KIND, k);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, "scav");
    }

    @Override
    public void tick() {
        super.tick();
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.6, 0.98, 0.6));
        if (!level().isClientSide && ++life > 6000) discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1, 0.6f);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(tag.getString("Kind"));
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Kind", kind());
        tag.putInt("Life", life);
    }
}
