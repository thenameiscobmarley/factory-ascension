package dev.factoryascension.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A dirt bike: built from parts (vehicle/BikeParts: engine, frame, gearbox), and it rides like what's fitted to it.
 * W throttle, S brake (and reverse), A/D steer (sharper slow, gentler fast), Space hops it, Sprint pulls a wheelie,
 * Sneak gets off. It climbs a block without slowing, runs on fuel (right-click with a fuel can, racing fuel, TFMG
 * gasoline or diesel, or a blaze rod), takes a dye, and anything it hits at speed gets hurt. Sneak + right-click with a
 * part to fit it; sneak + right-click with an empty hand to pack it up.
 * Like a boat, the rider's own game moves it (smooth, no lag) and the server checks the moves.
 */
public class DirtBike extends Entity {
    static final EntityDataAccessor<String> ENGINE = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.STRING);
    static final EntityDataAccessor<String> FRAME = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.STRING);
    static final EntityDataAccessor<String> GEARBOX = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.STRING);
    static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> FUEL_POWER = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> FUEL_USE = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(DirtBike.class, EntityDataSerializers.INT);

    /** The rider's controls, read on the rider's side (client/vehicle sets it): {forward, strafe, hop, wheelie}. */
    public static java.util.function.Supplier<float[]> input = () -> null;

    // Motion and the looks (client side, for the renderer and the engine sound)
    public float speed, steer, steerO, wheel, wheelO, lean, leanO, wheelie, wheelieO, throttle, rpm;
    public int gear = 0;
    private float damage;
    @Nullable private Vec3 serverLast;
    private int lerpSteps;
    private double lx, ly, lz;
    private float lyaw;

    public DirtBike(EntityType<?> type, Level level) {
        super(type, level);
        setMaxUpStep(1.05f);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ENGINE, "engine_125");
        entityData.define(FRAME, "frame_standard");
        entityData.define(GEARBOX, "gearbox_4");
        entityData.define(FUEL, 50f);
        entityData.define(FUEL_POWER, 1f);
        entityData.define(FUEL_USE, 1f);
        entityData.define(COLOUR, 0xE8661C);
    }

    public String engine() { return entityData.get(ENGINE); }
    public String frame() { return entityData.get(FRAME); }
    public String gearbox() { return entityData.get(GEARBOX); }
    public float fuel() { return entityData.get(FUEL); }
    public int colour() { return entityData.get(COLOUR); }

    public BikeParts.Stats stats() {
        return BikeParts.stats(engine(), frame(), gearbox(), entityData.get(FUEL_POWER), entityData.get(FUEL_USE));
    }

    // ---- Saving, and packing into the item ----
    public void readBike(CompoundTag t) {
        if (t.contains("Engine")) entityData.set(ENGINE, t.getString("Engine"));
        if (t.contains("Frame")) entityData.set(FRAME, t.getString("Frame"));
        if (t.contains("Gearbox")) entityData.set(GEARBOX, t.getString("Gearbox"));
        if (t.contains("Fuel")) entityData.set(FUEL, t.getFloat("Fuel"));
        if (t.contains("FuelPower")) entityData.set(FUEL_POWER, t.getFloat("FuelPower"));
        if (t.contains("FuelUse")) entityData.set(FUEL_USE, t.getFloat("FuelUse"));
        if (t.contains("Colour")) entityData.set(COLOUR, t.getInt("Colour"));
    }

    public CompoundTag writeBike() {
        CompoundTag t = new CompoundTag();
        t.putString("Engine", engine());
        t.putString("Frame", frame());
        t.putString("Gearbox", gearbox());
        t.putFloat("Fuel", fuel());
        t.putFloat("FuelPower", entityData.get(FUEL_POWER));
        t.putFloat("FuelUse", entityData.get(FUEL_USE));
        t.putInt("Colour", colour());
        return t;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        readBike(tag.getCompound("Bike"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Bike", writeBike());
    }

    ItemStack asItem() {
        ItemStack s = new ItemStack(Vehicles.DIRT_BIKE_ITEM.get());
        s.getOrCreateTag().put("Bike", writeBike());
        return s;
    }

    // ---- Riding ----
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return BikeParts.frame(frame()).seatY() / 16.0 - 0.55;
    }

    @Override
    protected void positionRider(Entity e, MoveFunction move) {
        if (!hasPassenger(e)) return;
        float back = BikeParts.frame(frame()).seatX() / 16f;
        Vec3 off = new Vec3(0, 0, back).yRot(-getYRot() * Mth.DEG_TO_RAD);
        move.accept(e, getX() + off.x, getY() + getPassengersRidingOffset() + e.getMyRidingOffset(), getZ() + off.z);
        e.setYBodyRot(getYRot());
        float d = Mth.wrapDegrees(e.getYRot() - getYRot());
        float c = Mth.clamp(d, -100, 100);
        e.yRotO += c - d;
        e.setYRot(e.getYRot() + c - d);
        e.setYHeadRot(e.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity e) {
        Vec3 side = new Vec3(1.2, 0, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        return position().add(side);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity e) {
        return (e.canBeCollidedWith() || e.isPushable()) && !isPassengerOfSameVehicle(e);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** Fits the part in `held` (taking the old one out, into your inventory). False if it can't. */
    public boolean fitPart(Player p, ItemStack held) {
        if (!(held.getItem() instanceof BikePartItem part)) return false;
        String old = switch (part.kind) {
            case "engine" -> engine();
            case "frame" -> frame();
            default -> gearbox();
        };
        if (old.equals(part.id)) return false;
        if (part.kind.equals("engine") && BikeParts.engine(part.id).size() > BikeParts.frame(frame()).maxEngine()) {
            p.displayClientMessage(Component.literal("That engine doesn't fit this frame. Fit a Long Frame first.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (part.kind.equals("frame") && BikeParts.engine(engine()).size() > BikeParts.frame(part.id).maxEngine()) {
            p.displayClientMessage(Component.literal("The engine wouldn't fit in that frame. Take it out first (fit a smaller one).").withStyle(ChatFormatting.RED), true);
            return false;
        }
        switch (part.kind) {
            case "engine" -> entityData.set(ENGINE, part.id);
            case "frame" -> entityData.set(FRAME, part.id);
            default -> entityData.set(GEARBOX, part.id);
        }
        var oldItem = Vehicles.PARTS.get(old);
        if (!p.isCreative()) held.shrink(1);
        if (oldItem != null && !p.getInventory().add(new ItemStack(oldItem.get()))) spawnAtLocation(new ItemStack(oldItem.get()));
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.6f, 1.4f);
        return true;
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        ItemStack held = p.getItemInHand(hand);
        if (level().isClientSide) {
            if (held.getItem() instanceof Workshop.Wrench) Workshop.openClient.accept(this);
            return InteractionResult.SUCCESS;
        }
        // The wrench: the workshop screen (client/vehicle/WorkshopScreen)
        if (held.getItem() instanceof Workshop.Wrench) return InteractionResult.CONSUME;
        // Fitting a part
        if (p.isSecondaryUseActive() && held.getItem() instanceof BikePartItem) {
            fitPart(p, held);
            return InteractionResult.CONSUME;
        }
        // Packing it up
        if (p.isSecondaryUseActive() && held.isEmpty()) {
            if (!p.getInventory().add(asItem())) spawnAtLocation(asItem());
            discard();
            return InteractionResult.CONSUME;
        }
        // Fuel
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        BikeParts.Fuel f = BikeParts.FUELS.get(id);
        if (f != null) {
            if (fuel() >= 99.5f) {
                p.displayClientMessage(Component.literal("The tank's full."), true);
                return InteractionResult.CONSUME;
            }
            entityData.set(FUEL, Math.min(100, fuel() + f.amount()));
            entityData.set(FUEL_POWER, f.power());
            entityData.set(FUEL_USE, f.use());
            if (!p.isCreative()) {
                held.shrink(1);
                if (!f.leftover().isEmpty()) {
                    var left = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(f.leftover()));
                    left.ifPresent(i -> { if (!p.getInventory().add(new ItemStack(i))) spawnAtLocation(new ItemStack(i)); });
                }
            }
            level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.8f, 1.3f);
            p.displayClientMessage(Component.literal(String.format("%s: tank %.0f%%", f.name(), fuel())), true);
            return InteractionResult.CONSUME;
        }
        // Paint
        if (held.getItem() instanceof DyeItem dye) {
            entityData.set(COLOUR, dye.getDyeColor().getTextColor());
            if (!p.isCreative()) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (p.isSecondaryUseActive()) return InteractionResult.PASS;
        return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) return false;
        damage += amount;
        level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.5f, 1.6f);
        boolean creative = source.getEntity() instanceof Player p && p.isCreative();
        if (damage > 40 || creative) {
            if (!creative) spawnAtLocation(asItem());
            discard();
        }
        return true;
    }

    // ---- Each tick ----
    @Override
    public void tick() {
        super.tick();
        steerO = steer;
        wheelO = wheel;
        leanO = lean;
        wheelieO = wheelie;
        if (damage > 0) damage = Math.max(0, damage - 0.02f);
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            drive();
        } else {
            if (lerpSteps > 0) {
                double d = 1.0 / lerpSteps;
                setPos(getX() + (lx - getX()) * d, getY() + (ly - getY()) * d, getZ() + (lz - getZ()) * d);
                setYRot(getYRot() + (float) Mth.wrapDegrees(lyaw - getYRot()) * (float) d);
                lerpSteps--;
            }
            // Others' bikes: the looks from how it moves
            Vec3 v = getDeltaMovement();
            float sp = (float) v.horizontalDistance();
            float dir = new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD)).dot(v) >= 0 ? 1 : -1;
            speed = Mth.lerp(0.5f, speed, sp * dir);
            if (!level().isClientSide && getControllingPassenger() == null) idle();
        }
        if (!level().isClientSide && getControllingPassenger() != null) {
            // The rider's game moves it: speed and fuel from how far it actually went
            Vec3 now = position();
            if (serverLast != null) {
                Vec3 d = now.subtract(serverLast);
                Vec3 dir = new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                speed = (float) (d.horizontalDistance() * Math.signum(dir.dot(d) == 0 ? 1 : dir.dot(d)));
                var st = stats();
                if (Math.abs(speed) > 0.01f) entityData.set(FUEL, Math.max(0, fuel() - st.fuelUse() * Mth.clamp(Math.abs(speed) / st.top(), 0.2f, 1)));
            }
            serverLast = now;
        } else serverLast = null;
        looks();
        if (!level().isClientSide && Math.abs(speed) > 0.35f && getControllingPassenger() != null) runOver();
        checkInsideBlocks();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        lx = x;
        ly = y;
        lz = z;
        lyaw = yaw;
        lerpSteps = 10;
    }

    /** Nobody on it: it rolls to a stop and falls. */
    private void idle() {
        speed *= onGround() ? 0.9f : 0.99f;
        Vec3 dir = new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        Vec3 v = getDeltaMovement();
        setDeltaMovement(dir.x * speed, onGround() ? -0.04 : (v.y - 0.08) * 0.98, dir.z * speed);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** The rider's side: controls, engine, steering, gravity. */
    private void drive() {
        float[] in = input.get();
        float fwd = in != null ? in[0] : 0, side = in != null ? in[1] : 0;
        boolean hop = in != null && in[2] > 0, wheelieKey = in != null && in[3] > 0;
        var st = stats();
        boolean wet = isInWater();
        boolean hasFuel = fuel() > 0 && !wet;
        throttle = fwd > 0 && hasFuel ? 1 : 0;
        float v = speed;
        if (throttle > 0) {
            float k = v / Math.max(0.05f, st.top());
            v += st.accel() * Math.max(0.05f, 1 - k * k);
        } else if (fwd < 0) {
            v = v > 0.02f ? v - 0.05f * st.grip() : Math.max(-0.14f, v - 0.008f);
        } else {
            v *= onGround() ? 0.986f : 0.996f;
            if (Math.abs(v) < 0.003f) v = 0;
        }
        if (wet) v *= 0.85f;
        // Steering: sharp at walking pace, gentle at speed, none in the air
        float pace = Mth.clamp(Math.abs(v) / 0.18f, 0, 1);
        float rate = 5.5f * st.turn() * pace * (1 - 0.5f * Mth.clamp(Math.abs(v) / st.top(), 0, 1)) * (onGround() ? 1 : 0.25f);
        steer = Mth.lerp(0.35f, steer, side);
        setYRot(getYRot() - steer * rate * Math.signum(v == 0 ? 1 : v));
        // Move: along the bike, gravity, a hop
        Vec3 dir = new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        double vy = getDeltaMovement().y;
        vy = onGround() ? Math.max(vy, -0.08) : (vy - 0.08) * 0.98;
        if (hop && onGround()) vy = 0.42 + Math.abs(v) * 0.15;
        if (wheelieKey && onGround() && throttle > 0 && v > 0.15f) wheelie = Math.min(1, wheelie + 0.08f);
        else wheelie = Math.max(0, wheelie - 0.12f);
        setDeltaMovement(dir.x * v, vy, dir.z * v);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && Math.abs(v) > 0.05f) {
            if (Math.abs(v) > 0.7f && getControllingPassenger() != null) getControllingPassenger().hurt(damageSources().flyIntoWall(), (Math.abs(v) - 0.6f) * 12);
            v *= 0.3f;
        }
        speed = v;
        // Engine: which gear and how hard it's revving (for the sound and the HUD)
        int n = st.gears();
        float frac = Mth.clamp(Math.abs(v) / Math.max(0.05f, st.top()), 0, 1.2f);
        int g = Math.min(n - 1, (int) (frac * n * 0.999f));
        gear = g;
        float lo = (float) g / n, hi = (float) (g + 1) / n;
        rpm = Mth.lerp(0.4f, rpm, Mth.clamp(0.25f + 0.75f * (frac - lo) / (hi - lo) * (throttle > 0 ? 1 : 0.7f), 0.2f, 1.05f));
        if (!hasFuel) rpm = 0;
    }

    /** Wheels turn, it leans into turns, the front comes up in a wheelie. */
    private void looks() {
        wheel += speed / (7.4f / 16f);
        if (!isControlledByLocalInstance()) {
            float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
            steer = Mth.lerp(0.3f, steer, Mth.clamp(-dyaw / 4f, -1, 1));
            throttle = Math.abs(speed) > 0.05f ? 1 : 0;
            rpm = Mth.lerp(0.2f, rpm, Mth.clamp(0.25f + Math.abs(speed) * 0.8f, 0.2f, 1));
        }
        float target = -steer * Mth.clamp(Math.abs(speed) / 0.5f, 0, 1) * 32;
        lean = Mth.lerp(0.25f, lean, target);
    }

    /** Hitting things at speed hurts them (and slows you). */
    private void runOver() {
        Vec3 dir = new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3).move(dir.scale(0.6)),
            e -> e.isAlive() && !hasPassenger(e) && !e.isPassenger())) {
            float dmg = Math.abs(speed) * 14 * (stats().mass() / 175f);
            if (e.hurt(damageSources().mobAttack(getControllingPassenger()), dmg)) {
                e.push(dir.x * speed * 1.5, 0.35, dir.z * speed * 1.5);
                speed *= 0.75f;
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
            }
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        if (onGround) resetFallDistance();   // the suspension takes it
    }

    @Override
    public ItemStack getPickResult() {
        return asItem();
    }
}
