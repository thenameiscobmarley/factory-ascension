package dev.factoryascension.raid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;

/** What someone carried when they fell on a raid, where they fell. Right-click to search it; it lasts a day, or until empty. */
public class BodyBag extends Entity {
    static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(BodyBag.class, EntityDataSerializers.STRING);
    public final SimpleContainer items = new SimpleContainer(54);
    public String owner = "";
    private int age = 0;

    public BodyBag(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, "");
    }

    public String ownerName() {
        return entityData.get(OWNER);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!owner.equals(entityData.get(OWNER))) entityData.set(OWNER, owner);
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        }
        if (++age > 24000 || (age % 100 == 0 && items.isEmpty())) discard();
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.sixRows(id, inv, items),
            Component.literal(owner.isEmpty() ? "Body bag" : owner + "'s things")));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource s, float a) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        owner = t.getString("Owner");
        age = t.getInt("Age");
        items.fromTag(t.getList("Items", 10));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putString("Owner", owner);
        t.putInt("Age", age);
        t.put("Items", items.createTag());
    }
}
