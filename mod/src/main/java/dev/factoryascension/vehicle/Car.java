package dev.factoryascension.vehicle;

import dev.factoryascension.FAConfig;
import dev.factoryascension.gun.AmmoType;
import dev.factoryascension.gun.Caliber;
import dev.factoryascension.gun.CartridgeItem;
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
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Four wheels: the quad bike (two seats, quick and twitchy) and the pickup truck (driver, passenger, and a gunner on
 * the bed behind a mounted heavy machine gun). Drives like the dirt bike (W/S/A/D, the rider's own game moves it), runs
 * on the same fuels, takes a dye, and hurts what it hits. Right-click the bed of the truck to take the gun: aim with
 * the mouse, hold attack to fire (.50 BMG rounds: right-click the truck with them to load the belt). A wrench repairs
 * it. Sneak + right-click with an empty hand to pack it up. Speeds, weights, health: the config (vehicles.cars).
 */
public class Car extends Entity {
    public enum Model {
        QUAD("quad", 2, 12.5f, -12.0f, 6.2f, 11.0f),
        PICKUP("pickup", 3, 27.0f, -24.0f, 7.6f, 13.5f);
        public final String id;
        public final int seats;
        /** front and rear axle x, wheel radius, track half-width (model units, as tools/vehicles/car_mesh.py) */
        public final float front, rear, wheelR, track;
        // Driving (FAConfig.setCar): accel, top speed (blocks/tick), turn rate, mass (kg), health, fuel use per tick at full throttle
        public float accel, top, turn, mass, health, fuelUse;

        Model(String id, int seats, float front, float rear, float wheelR, float track) {
            this.id = id;
            this.seats = seats;
            this.front = front;
            this.rear = rear;
            this.wheelR = wheelR;
            this.track = track;
        }

        static {
            QUAD.set(0.032f, 1.05f, 1.15f, 290, 50, 0.007f);
            PICKUP.set(0.022f, 1.25f, 0.7f, 2100, 160, 0.012f);
        }

        public void set(float accel, float top, float turn, float mass, float health, float fuelUse) {
            this.accel = accel;
            this.top = top;
            this.turn = turn;
            this.mass = mass;
            this.health = health;
            this.fuelUse = fuelUse;
        }
    }

    /** Seats: x (along), y (up), z (across), model units. The gunner's is the pickup's third. */
    static final float[][][] SEATS = {
        {{-3.0f, 16.5f, 0}, {-10.0f, 17.5f, 0}},
        {{6.0f, 13.0f, -6.5f}, {6.0f, 13.0f, 6.5f}, {-27.0f, 23.0f, 0}},
    };
    /** The gun's pivot on the pickup (model units). */
    public static final float MOUNT_X = -14.0f, MOUNT_Y = 26.0f;
    public static final int GUN_MAX = 400;

    static final EntityDataAccessor<Integer> MODEL = SynchedEntityData.defineId(Car.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(Car.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(Car.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(Car.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> GUNNER = SynchedEntityData.defineId(Car.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(Car.class, EntityDataSerializers.FLOAT);

    public float speed, steer, steerO, wheel, wheelO, pitch, pitchO, roll, rollO, throttle, rpm, recoil, recoilO;
    @Nullable private Vec3 serverLast;
    private int lerpSteps, gunCool;
    private double lx, ly, lz;
    private float lyaw;
    private boolean wantsGun;

    public Car(EntityType<?> type, Level level) {
        super(type, level);
        setMaxUpStep(1.05f);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MODEL, 0);
        entityData.define(FUEL, 60f);
        entityData.define(COLOUR, 0x6B7A4F);
        entityData.define(AMMO, 0);
        entityData.define(GUNNER, -1);
        entityData.define(DAMAGE, 0f);
    }

    public Model model() {
        return Model.values()[Mth.clamp(entityData.get(MODEL), 0, Model.values().length - 1)];
    }

    public float fuel() { return entityData.get(FUEL); }
    public int colour() { return entityData.get(COLOUR); }
    public int ammo() { return entityData.get(AMMO); }
    public float damage() { return entityData.get(DAMAGE); }

    @Nullable
    public Player gunner() {
        return level().getEntity(entityData.get(GUNNER)) instanceof Player p && hasPassenger(p) ? p : null;
    }

    /** Puts `p` (already riding) on the gun. */
    public void setGunner(Player p) {
        if (hasGun() && hasPassenger(p)) entityData.set(GUNNER, p.getId());
    }

    public boolean hasGun() {
        return model() == Model.PICKUP;
    }

    // ---- Saving, packing ----
    public void readCar(CompoundTag t) {
        if (t.contains("Model")) entityData.set(MODEL, t.getInt("Model"));
        if (t.contains("Fuel")) entityData.set(FUEL, t.getFloat("Fuel"));
        if (t.contains("Colour")) entityData.set(COLOUR, t.getInt("Colour"));
        if (t.contains("Ammo")) entityData.set(AMMO, t.getInt("Ammo"));
        if (t.contains("Damage")) entityData.set(DAMAGE, t.getFloat("Damage"));
    }

    public CompoundTag writeCar() {
        CompoundTag t = new CompoundTag();
        t.putInt("Model", model().ordinal());
        t.putFloat("Fuel", fuel());
        t.putInt("Colour", colour());
        t.putInt("Ammo", ammo());
        t.putFloat("Damage", damage());
        return t;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        readCar(tag.getCompound("Car"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Car", writeCar());
    }

    ItemStack asItem() {
        ItemStack s = new ItemStack((model() == Model.QUAD ? Vehicles.QUAD_ITEM : Vehicles.PICKUP_ITEM).get());
        s.getOrCreateTag().put("Car", writeCar());
        return s;
    }

    @Override
    public ItemStack getPickResult() {
        return asItem();
    }

    // ---- Seats ----
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) if (e instanceof Player p && p.getId() != entityData.get(GUNNER)) return p;
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().size() < model().seats;
    }

    int seatOf(Entity e) {
        if (hasGun() && e.getId() == entityData.get(GUNNER)) return 2;
        int i = 0;
        for (Entity p : getPassengers()) {
            if (p == e) return Math.min(i, hasGun() ? 1 : model().seats - 1);
            if (!(hasGun() && p.getId() == entityData.get(GUNNER))) i++;
        }
        return 0;
    }

    @Override
    protected void positionRider(Entity e, MoveFunction move) {
        if (!hasPassenger(e)) return;
        float[] s = SEATS[model().ordinal()][seatOf(e)];
        Vec3 off = new Vec3(s[0] / 16f, 0, s[2] / 16f).yRot(-(getYRot() + 90) * Mth.DEG_TO_RAD);
        move.accept(e, getX() + off.x, getY() + s[1] / 16f - 0.6 + e.getMyRidingOffset(), getZ() + off.z);
        if (seatOf(e) == 2) return;   // the gunner turns freely
        e.setYBodyRot(getYRot());
        float d = Mth.wrapDegrees(e.getYRot() - getYRot());
        float c = Mth.clamp(d, -110, 110);
        e.yRotO += c - d;
        e.setYRot(e.getYRot() + c - d);
        e.setYHeadRot(e.getYRot());
    }

    @Override
    protected void removePassenger(Entity e) {
        super.removePassenger(e);
        if (e.getId() == entityData.get(GUNNER)) entityData.set(GUNNER, -1);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity e) {
        Vec3 side = new Vec3(0, 0, (model().track + 10) / 16f).yRot(-(getYRot() + 90) * Mth.DEG_TO_RAD);
        return position().add(seatOf(e) == 1 ? side : side.reverse());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity e) {
        return (e.canBeCollidedWith() || e.isPushable()) && !isPassengerOfSameVehicle(e);
    }

    /** Where on it you clicked: the truck's bed means the gun. */
    @Override
    public InteractionResult interactAt(Player p, Vec3 at, InteractionHand hand) {
        Vec3 fwd = forward();
        wantsGun = hasGun() && at.dot(fwd) < -0.6;
        return InteractionResult.PASS;
    }

    public Vec3 forward() {
        return new Vec3(-Mth.sin(getYRot() * Mth.DEG_TO_RAD), 0, Mth.cos(getYRot() * Mth.DEG_TO_RAD));
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        ItemStack held = p.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (p.isSecondaryUseActive() && held.isEmpty()) {
            if (!getPassengers().isEmpty()) return InteractionResult.PASS;
            if (!p.getInventory().add(asItem())) spawnAtLocation(asItem());
            discard();
            return InteractionResult.CONSUME;
        }
        // Repairs
        if (held.getItem() instanceof Workshop.Wrench) {
            if (damage() <= 0) {
                p.displayClientMessage(Component.literal("Nothing to fix."), true);
                return InteractionResult.CONSUME;
            }
            entityData.set(DAMAGE, Math.max(0, damage() - FAConfig.wrenchRepair));
            held.hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(hand));
            level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.5f, 1.5f);
            p.displayClientMessage(Component.literal(String.format("Condition %.0f%%", 100 * (1 - damage() / model().health))), true);
            return InteractionResult.CONSUME;
        }
        // The gun's belt
        if (hasGun() && held.getItem() instanceof CartridgeItem && CartridgeItem.caliber(held) == Caliber.B50) {
            int room = GUN_MAX - ammo();
            int n = Math.min(room, held.getCount());
            if (n <= 0) {
                p.displayClientMessage(Component.literal("The belt's full."), true);
                return InteractionResult.CONSUME;
            }
            entityData.set(AMMO, ammo() + n);
            if (!p.isCreative()) held.shrink(n);
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.NEUTRAL, 0.8f, 0.8f);
            p.displayClientMessage(Component.literal("Belt: " + ammo() + " rounds"), true);
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
            entityData.set(FUEL, Math.min(100, fuel() + f.amount() * (model() == Model.PICKUP ? 0.5f : 1f)));
            if (!p.isCreative()) {
                held.shrink(1);
                if (!f.leftover().isEmpty())
                    BuiltInRegistries.ITEM.getOptional(new ResourceLocation(f.leftover())).ifPresent(i -> { if (!p.getInventory().add(new ItemStack(i))) spawnAtLocation(new ItemStack(i)); });
            }
            level().playSound(null, blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.8f, 1.2f);
            p.displayClientMessage(Component.literal(String.format("%s: tank %.0f%%", f.name(), fuel())), true);
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof DyeItem dye) {
            entityData.set(COLOUR, dye.getDyeColor().getTextColor());
            if (!p.isCreative()) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (p.isSecondaryUseActive()) return InteractionResult.PASS;
        boolean gun = wantsGun && gunner() == null;
        wantsGun = false;
        if (!p.startRiding(this)) return InteractionResult.PASS;
        if (gun) entityData.set(GUNNER, p.getId());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) return false;
        boolean creative = source.getEntity() instanceof Player p && p.isCreative();
        if (creative && source.getEntity().isShiftKeyDown()) {
            discard();
            return true;
        }
        float d = damage() + amount;
        entityData.set(DAMAGE, d);
        level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.6f, 0.7f);
        if (d >= model().health) wreck();
        return true;
    }

    /** Too much damage: it burns out (a bang, smoke, the riders thrown off); what's left is scrap. */
    void wreck() {
        ejectPassengers();
        if (level() instanceof ServerLevel sl) {
            sl.explode(this, getX(), getY() + 0.8, getZ(), model() == Model.PICKUP ? 2.5f : 1.5f, false, Level.ExplosionInteraction.NONE);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 1, 0.6, 1, 0.05);
            spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, model() == Model.PICKUP ? 12 : 5));
        }
        discard();
    }

    // ---- Each tick ----
    @Override
    public void tick() {
        super.tick();
        steerO = steer;
        wheelO = wheel;
        pitchO = pitch;
        rollO = roll;
        recoilO = recoil;
        recoil *= 0.6f;
        if (gunCool > 0) gunCool--;
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
            Vec3 v = getDeltaMovement();
            float dir = forward().dot(v) >= 0 ? 1 : -1;
            speed = Mth.lerp(0.5f, speed, (float) v.horizontalDistance() * dir);
            if (!level().isClientSide && getControllingPassenger() == null) idle();
        }
        if (!level().isClientSide && getControllingPassenger() != null) {
            Vec3 now = position();
            if (serverLast != null) {
                Vec3 d = now.subtract(serverLast);
                float dot = (float) forward().dot(d);
                speed = (float) d.horizontalDistance() * (dot < 0 ? -1 : 1);
                if (Math.abs(speed) > 0.01f) entityData.set(FUEL, Math.max(0, fuel() - model().fuelUse * Mth.clamp(Math.abs(speed) / model().top, 0.2f, 1)));
            }
            serverLast = now;
        } else serverLast = null;
        looks();
        if (!level().isClientSide && Math.abs(speed) > 0.3f && getControllingPassenger() != null) runOver();
        // Badly damaged: it smokes
        if (level().isClientSide && damage() > model().health * 0.6f && random.nextInt(4) == 0)
            level().addParticle(ParticleTypes.SMOKE, getX() + forward().x * model().front / 16, getY() + 1.2, getZ() + forward().z * model().front / 16, 0, 0.05, 0);
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

    private void idle() {
        speed *= onGround() ? 0.88f : 0.99f;
        Vec3 dir = forward();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(dir.x * speed, onGround() ? -0.04 : (v.y - 0.08) * 0.98, dir.z * speed);
        move(MoverType.SELF, getDeltaMovement());
    }

    private void drive() {
        float[] in = DirtBike.input.get();
        float fwd = in != null ? in[0] : 0, side = in != null ? in[1] : 0;
        Model m = model();
        boolean wet = isInWater();
        boolean hasFuel = fuel() > 0 && !wet && damage() < m.health * 0.95f;
        throttle = fwd > 0 && hasFuel ? 1 : 0;
        float top = m.top * (1 - 0.4f * Mth.clamp(damage() / m.health - 0.5f, 0, 1));
        float v = speed;
        if (throttle > 0) {
            float k = v / Math.max(0.05f, top);
            v += m.accel * Math.max(0.05f, 1 - k * k);
        } else if (fwd < 0) {
            v = v > 0.02f ? v - 0.045f : (hasFuel ? Math.max(-0.2f, v - 0.01f) : v * 0.9f);
        } else {
            v *= onGround() ? 0.982f : 0.996f;
            if (Math.abs(v) < 0.003f) v = 0;
        }
        if (wet) v *= 0.8f;
        float pace = Mth.clamp(Math.abs(v) / 0.12f, 0, 1);
        float rate = 4.5f * m.turn * pace * (1 - 0.45f * Mth.clamp(Math.abs(v) / m.top, 0, 1)) * (onGround() ? 1 : 0.15f);
        steer = Mth.lerp(0.3f, steer, side);
        setYRot(getYRot() - steer * rate * Math.signum(v == 0 ? 1 : v));
        Vec3 dir = forward();
        double vy = getDeltaMovement().y;
        vy = onGround() ? Math.max(vy, -0.08) : (vy - 0.08) * 0.98;
        setDeltaMovement(dir.x * v, vy, dir.z * v);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && Math.abs(v) > 0.05f) {
            if (Math.abs(v) > 0.6f) for (Entity e : getPassengers()) e.hurt(damageSources().flyIntoWall(), (Math.abs(v) - 0.5f) * 10);
            v *= 0.25f;
        }
        speed = v;
        float frac = Mth.clamp(Math.abs(v) / Math.max(0.05f, m.top), 0, 1.2f);
        int n = 5;
        int g = Math.min(n - 1, (int) (frac * n * 0.999f));
        float lo = (float) g / n, hi = (float) (g + 1) / n;
        rpm = Mth.lerp(0.35f, rpm, Mth.clamp(0.25f + 0.75f * (frac - lo) / (hi - lo) * (throttle > 0 ? 1 : 0.7f), 0.2f, 1.05f));
        if (!hasFuel) rpm = 0;
    }

    /** Wheels turn; the body pitches with slopes and braking and rolls into turns. */
    private void looks() {
        wheel += speed / (model().wheelR / 16f);
        if (!isControlledByLocalInstance()) {
            float dyaw = Mth.wrapDegrees(getYRot() - yRotO);
            steer = Mth.lerp(0.3f, steer, Mth.clamp(-dyaw / 4f, -1, 1));
            throttle = Math.abs(speed) > 0.05f ? 1 : 0;
            rpm = Mth.lerp(0.2f, rpm, Mth.clamp(0.25f + Math.abs(speed) * 0.7f, 0.2f, 1));
        }
        float slope = (float) Math.toDegrees(Math.atan2(getDeltaMovement().y, Math.max(0.15, Math.abs(speed)))) * (speed < 0 ? -1 : 1);
        pitch = Mth.lerp(0.2f, pitch, onGround() ? Mth.clamp(slope, -25, 25) : pitch * 0.95f);
        float body = model() == Model.QUAD ? 7 : 4;
        roll = Mth.lerp(0.15f, roll, steer * Mth.clamp(Math.abs(speed) / 0.6f, 0, 1) * body);
    }

    private void runOver() {
        Vec3 dir = forward();
        float len = (model().front - model().rear) / 32f;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3).move(dir.scale(len * 0.6)),
            e -> e.isAlive() && !hasPassenger(e) && !e.isPassenger())) {
            float dmg = Math.abs(speed) * 14 * (float) Math.sqrt(model().mass / 175f);
            if (e.hurt(damageSources().mobAttack(getControllingPassenger()), dmg)) {
                e.push(dir.x * speed * 1.8, 0.4, dir.z * speed * 1.8);
                speed *= model() == Model.PICKUP ? 0.92f : 0.7f;
            }
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        if (onGround) resetFallDistance();
    }

    // ---- The mounted gun ----
    /** Where the gun pivots, in the world. */
    public Vec3 mount(float partial) {
        Vec3 off = new Vec3(MOUNT_X / 16f, MOUNT_Y / 16f, 0).yRot(-(Mth.rotLerp(partial, yRotO, getYRot()) + 90) * Mth.DEG_TO_RAD);
        return getPosition(partial).add(off);
    }

    /** The gunner pulls the trigger (FANetwork.TurretFire, every tick it's held): one round per cooldown. */
    public void fire(ServerPlayer gunner) {
        if (gunner != gunner() || gunCool > 0) return;
        ServerLevel level = gunner.serverLevel();
        if (ammo() <= 0 && !gunner.isCreative()) {
            gunCool = 10;
            level.playSound(null, gunner.blockPosition(), dev.factoryascension.FASounds.DRY.get(), SoundSource.PLAYERS, 0.8f, 0.8f);
            gunner.displayClientMessage(Component.literal("The belt's empty: load .50 BMG").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        gunCool = FAConfig.turretCooldown;
        if (!gunner.isCreative()) entityData.set(AMMO, ammo() - 1);
        Vec3 look = gunner.getViewVector(1);
        var r = gunner.getRandom();
        double spread = Math.toRadians(FAConfig.turretSpread * (1 + Math.abs(speed) * 2));
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        double a = spread * Math.sqrt(r.nextDouble()), b = r.nextDouble() * Math.PI * 2;
        Vec3 dir = look.add(right.scale(Math.tan(a) * Math.cos(b))).add(up.scale(Math.tan(a) * Math.sin(b))).normalize();
        Vec3 from = mount(1).add(0, 3.8 / 16, 0).add(dir.scale(28 / 16.0));
        Vec3 vel = dir.scale(880 / 20.0);
        float dmg = dev.factoryascension.gun.Guns.BASE_DAMAGE * Caliber.B50.damage * FAConfig.turretDamage;
        for (int tick = 0; tick < 60; tick++) {
            Vec3 to = from.add(vel);
            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, gunner));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            Entity hit = null;
            Vec3 at = null;
            double best = Double.MAX_VALUE;
            for (Entity e : level.getEntities(this, new AABB(from, stop).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && !hasPassenger(e) && e.isPickable())) {
                var h = e.getBoundingBox().inflate(0.15).clip(from, stop);
                if (h.isPresent() && h.get().distanceToSqr(from) < best) {
                    best = h.get().distanceToSqr(from);
                    hit = e;
                    at = h.get();
                }
            }
            if (hit != null) {
                final Entity victim = hit;
                final int when = tick;
                dev.factoryascension.gun.Delayed.after(Math.max(0, when - 1), () -> {
                    if (!victim.isAlive()) return;
                    float d = victim instanceof LivingEntity le ? dev.factoryascension.armor.Ballistics.hit(le, gunner, dmg, Caliber.B50, AmmoType.REGULAR) : dmg;
                    victim.invulnerableTime = 0;
                    victim.hurt(gunner.damageSources().playerAttack(gunner), d);
                });
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 2, 0.1, 0.1, 0.1, 0.1);
                break;
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(block.getBlockPos())),
                    stop.x, stop.y, stop.z, 6, 0.1, 0.1, 0.1, 0.15);
                break;
            }
            // Every fifth round a tracer, like a real belt
            if (ammo() % 5 == 0 && tick < 12) level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 1, 0, 0, 0, 0);
            from = to;
            vel = vel.scale(0.99).add(0, -0.0245, 0);
        }
        // Pass the operators it went by: pinned down
        for (var o : level.getEntitiesOfClass(dev.factoryascension.npc.Operator.class, new AABB(from, mount(1)).inflate(3))) o.suppress();
        Vec3 muzzle = mount(1).add(0, 3.8 / 16, 0).add(look.scale(1.8));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
        var spec = dev.factoryascension.gun.Guns.BY_ID.get("m82");
        if (spec != null) dev.factoryascension.gun.GunSounds.shot(level, gunner, spec, 1.0f, r);
        dev.factoryascension.gun.Hearing.gunshot(level, gunner, 160);
        recoil = 1;
    }

    public List<Entity> riders() {
        return getPassengers();
    }
}
