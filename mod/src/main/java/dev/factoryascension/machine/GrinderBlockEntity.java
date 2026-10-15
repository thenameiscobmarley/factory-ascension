package dev.factoryascension.machine;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * Mob Grinder: turn its shaft and it hits every mob in the box in front of it (FAConfig.grinderReach deep, 5 wide, 3
 * high) every so often, harder the faster it spins. The hits count as a player's, so mobs drop everything a player kill
 * gets (rare drops, the pack's loot rolls). It leaves players, villagers, armor stands, pets and named mobs alone. Drops in
 * the box are collected into the inventory under it (or wait inside, for hoppers and funnels).
 */
public class GrinderBlockEntity extends KineticBlockEntity implements KineticMachineBlock.Status, KineticMachineBlock.Buffered {
    private final Output out = new Output(27, this::setChanged);
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new dev.factoryascension.blockentity.Automation.ExtractOnly(out.items));
    private static final com.mojang.authlib.GameProfile PROFILE = new com.mojang.authlib.GameProfile(
        java.util.UUID.fromString("6f2a7c1e-4d3b-4a8e-9b1f-fa0000000001"), "[FA Grinder]");
    private int cooldown = 0;
    private long kills = 0;

    public GrinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static GrinderBlockEntity create(BlockPos pos, BlockState state) {
        return new GrinderBlockEntity(FactoryAscension.GRINDER_BE.get(), pos, state);
    }

    /** The box in front: reach blocks deep, 5 wide, 3 high (from the grinder's own height up). */
    public AABB box() {
        Direction f = getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        int reach = FAConfig.grinderReach;
        BlockPos a = worldPosition.relative(f, 1), b = worldPosition.relative(f, reach);
        AABB box = new AABB(a).minmax(new AABB(b));
        return f.getAxis() == Direction.Axis.X ? box.inflate(0, 0, 2).expandTowards(0, 2, 0) : box.inflate(2, 0, 0).expandTowards(0, 2, 0);
    }

    private static boolean target(LivingEntity e) {
        return e.isAlive() && !(e instanceof Player) && !(e instanceof Npc) && !(e instanceof ArmorStand) && !e.hasCustomName()
            && !(e instanceof TamableAnimal t && t.isTame());
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 10 == 0) out.push(level, worldPosition, Direction.DOWN);
        float rpm = Math.abs(getSpeed());
        if (rpm < FAConfig.grinderMinRpm || isOverStressed()) return;
        if (--cooldown > 0) return;
        cooldown = Math.max(5, Math.round(FAConfig.grinderInterval * 64 / rpm));
        AABB box = box();
        // Its own fake player (moving the shared one would move whatever else uses it)
        FakePlayer fake = FakePlayerFactory.get((ServerLevel) level, PROFILE);
        fake.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        float damage = FAConfig.grinderDamagePerRpm * rpm;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, GrinderBlockEntity::target)) {
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().playerAttack(fake), damage);
            if (!e.isAlive()) kills++;
        }
        // Collect drops a tick later (they spawn when the mob finishes dying)
        if (out.hasRoom()) for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box.inflate(1))) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;
            out.add(item.getItem().copy());
            item.discard();
            if (!out.hasRoom()) break;
        }
        setChanged();
    }

    @Override
    public Component status() {
        float rpm = Math.abs(getSpeed());
        String why = rpm < FAConfig.grinderMinRpm ? " (needs at least " + (int) FAConfig.grinderMinRpm + " RPM)" : isOverStressed() ? " (overstressed)"
            : !out.hasRoom() ? " (full: put a chest under it)" : "";
        return Component.literal("Mob Grinder: " + Math.round(FAConfig.grinderDamagePerRpm * rpm) + " damage a hit, " + kills + " mobs ground" + why);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = FAConfig.grinderStress;
        return FAConfig.grinderStress;
    }

    @Override
    public void dropBuffer(Level level, BlockPos pos) {
        out.drop(level, pos);
    }

    @Override
    public <C> LazyOptional<C> getCapability(Capability<C> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putLong("Kills", kills);
        if (!clientPacket) tag.put("Out", out.items.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        kills = tag.getLong("Kills");
        if (!clientPacket && tag.contains("Out")) out.items.deserializeNBT(tag.getCompound("Out"));
    }

    // For the self-test
    public long faKills() { return kills; }
    public IItemHandler faOutput() { return out.items; }
}
