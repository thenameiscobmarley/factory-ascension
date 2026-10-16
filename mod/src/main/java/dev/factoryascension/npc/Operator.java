package dev.factoryascension.npc;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.AmmoType;
import dev.factoryascension.gun.Attachments;
import dev.factoryascension.gun.CartridgeItem;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSounds;
import dev.factoryascension.gun.GunSpec;
import dev.factoryascension.gun.GunStats;
import dev.factoryascension.gun.Guns;
import dev.factoryascension.gun.Skins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * An armed operator (npc/OpType): a person out there with a gun, who fights like a player. It hears gunshots and
 * comes to look, calls its squad onto you, takes cover to reload and to patch itself up, strafes and crouches while it
 * shoots, flanks, throws grenades at cover you're hiding behind, keeps firing where you were last seen, and runs out of
 * ammo like you do (reloads take the gun's real times, and it fumbles them when it's hurt). It carries what it shoots:
 * kill it and its gun drops with what's left in it, its spare rounds, its armor, its medical kit and its dog tag.
 */
public class Operator extends Monster {
    static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(Operator.class, EntityDataSerializers.BOOLEAN);
    /** 0 calm (gun slung), 1 suspicious (looking around), 2 alert (gun up, hunting). */
    static final EntityDataAccessor<Integer> ALERT = SynchedEntityData.defineId(Operator.class, EntityDataSerializers.INT);
    /** 0..1: how sure it is it saw someone (the meter over its head). */
    static final EntityDataAccessor<Float> SUSPICION = SynchedEntityData.defineId(Operator.class, EntityDataSerializers.FLOAT);
    /** For the animations: 0 nothing, 1 reloading, 2 fumbling a reload, 3 healing, 4 throwing. */
    static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Operator.class, EntityDataSerializers.INT);
    int alertTimer = 0, barkCooldown = 0;
    public final OpType type;
    boolean reloadFumbled = false;
    int ammo = -1, reload = 0, fireCooldown = 0, burstLeft = 0, grenades = 0, bandages = 0, grenadeCooldown = 0, healing = 0;
    int seenTicks = 0, unseenTicks = 0, healCooldown = 0, dazzled = 0;

    /** Blinded (a 20,000 lumen light, a flashbang): it can barely see and can't aim. */
    public void dazzle(int ticks) {
        dazzled = Math.max(dazzled, ticks);
    }
    @Nullable BlockPos home;
    @Nullable Vec3 lastSeen, investigate;
    long investigateUntil = 0;
    boolean fromStructure = false;

    public Operator(EntityType<? extends Operator> entityType, Level level, OpType type) {
        super(entityType, level);
        this.type = type;
        this.xpReward = 12 + (int) (type.health / 6);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder attributes(OpType t) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, t.health).add(Attributes.MOVEMENT_SPEED, 0.27 * t.speed)
            .add(Attributes.FOLLOW_RANGE, Math.max(48, t.maxRange + 24)).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.ARMOR, t.has(OpType.Trait.HEAVY) ? 6 : 0)
            .add(Attributes.KNOCKBACK_RESISTANCE, t.has(OpType.Trait.HEAVY) ? 0.8 : 0.1);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AIMING, false);
        entityData.define(ALERT, 0);
        entityData.define(SUSPICION, 0f);
        entityData.define(ACTION, 0);
    }

    public int alertLevel() {
        return entityData.get(ALERT);
    }

    public float suspicion() {
        return entityData.get(SUSPICION);
    }

    public int action() {
        return entityData.get(ACTION);
    }

    void setAction(int a) {
        if (entityData.get(ACTION) != a) entityData.set(ACTION, a);
    }

    void setAlert(int a) {
        if (entityData.get(ALERT) != a) entityData.set(ALERT, a);
    }

    /** Start calm (structures, patrols) or already hunting (hunters, juggernauts). */
    public void startAlert(int level) {
        setAlert(level);
        alertTimer = level == 2 ? 1200 : level == 1 ? 600 : 0;
    }

    // ---- Stealth: noticing, raising the alarm, standing down ----
    /** Sound the alarm: gun up, the whole group near it too. */
    public void raiseAlert(@Nullable LivingEntity target, boolean spread) {
        boolean fresh = alertLevel() < 2;
        setAlert(2);
        alertTimer = 1200;
        entityData.set(SUSPICION, 1f);
        if (target != null && target.isAlive()) {
            if (getTarget() == null) setTarget(target);
            lastSeen = target.getEyePosition();
            investigate = target.position();
            investigateUntil = level().getGameTime() + 1200;
        }
        if (fresh) bark(true);
        if (!spread) return;
        for (Operator o : level().getEntitiesOfClass(Operator.class, getBoundingBox().inflate(48), o -> o != this && o.isAlive() && o.type.faction == type.faction)) {
            if (o.alertLevel() >= 2 && o.getTarget() != null) continue;
            o.setAlert(2);
            o.alertTimer = 1200;
            o.entityData.set(SUSPICION, 1f);
            if (target != null) {
                o.lastSeen = target.getEyePosition();
                o.investigate = target.position();
                o.investigateUntil = level().getGameTime() + 1200;
                if (o.hasLineOfSight(target)) o.setTarget(target);
            } else if (investigate != null) {
                o.investigate = investigate;
                o.investigateUntil = level().getGameTime() + 1200;
            }
        }
    }

    /** Something odd (a quiet shot, a glimpse): look into it. */
    public void suspect(Vec3 at, float amount) {
        if (alertLevel() >= 2) return;
        float s = Math.min(0.99f, suspicion() + amount);
        entityData.set(SUSPICION, s);
        if (s > 0.3f) {
            if (alertLevel() == 0) bark(false);
            setAlert(1);
            alertTimer = 600;
            investigate = at;
            investigateUntil = level().getGameTime() + 600;
        }
    }

    /** A voice: villager and pillager noises, pitched like a person. */
    void bark(boolean alarm) {
        if (barkCooldown > 0) return;
        barkCooldown = 40 + getRandom().nextInt(60);
        var s = alarm ? (type.faction == OpType.Faction.SCAV ? SoundEvents.PILLAGER_CELEBRATE : SoundEvents.VINDICATOR_CELEBRATE)
            : (getRandom().nextBoolean() ? SoundEvents.VILLAGER_NO : SoundEvents.PILLAGER_AMBIENT);
        level().playSound(null, getX(), getY() + 1.6, getZ(), s, SoundSource.HOSTILE, 1.4f, 0.65f + getRandom().nextFloat() * 0.25f);
    }

    /** Every other tick while not yet hunting: does it see you? Bodies? */
    void awareness() {
        if (!(level() instanceof ServerLevel sl)) return;
        float s = suspicion();
        boolean saw = false;
        Vec3 look = getViewVector(1);
        for (ServerPlayer p : sl.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive() || p.distanceToSqr(this) > 56 * 56) continue;
            Vec3 to = p.getEyePosition().subtract(getEyePosition());
            double d = to.length();
            boolean inView = to.normalize().dot(look) > (alertLevel() == 1 ? -0.2 : 0.35) || d < 3;
            if (!inView || !spots(p) || !hasLineOfSight(p)) continue;
            saw = true;
            float rate = (float) Math.max(0.012, 0.16 * (1 - d / 56));
            if (p.isCrouching()) rate *= 0.45f;
            if (p.isSprinting()) rate *= 1.5f;
            if (level().getMaxLocalRawBrightness(p.blockPosition()) < 7 && !type.has(OpType.Trait.NVG)) rate *= 0.5f;
            if (GunItem.spec(p.getMainHandItem()) != null) rate *= 1.3f;
            if (alertLevel() == 1) rate *= 1.7f;
            s += rate;
            lastSeen = p.getEyePosition();
            if (s >= 1) {
                entityData.set(SUSPICION, 1f);
                raiseAlert(p, true);
                return;
            }
            if (s > 0.3f) suspect(p.position(), 0);
        }
        // A body
        for (Corpse c : sl.getEntitiesOfClass(Corpse.class, getBoundingBox().inflate(20))) {
            if (hasLineOfSight(c) && c.position().subtract(position()).normalize().dot(look) > 0.0) {
                investigate = c.position();
                raiseAlert(null, true);
                return;
            }
        }
        if (!saw) s = Math.max(0, s - 0.008f);
        entityData.set(SUSPICION, s);
    }

    public boolean aiming() {
        return entityData.get(AIMING);
    }

    void setAiming(boolean on) {
        entityData.set(AIMING, on);
    }

    // ---- Goals ----
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new Combat(this));
        goalSelector.addGoal(3, new Medic(this));
        goalSelector.addGoal(4, new Guard(this));
        goalSelector.addGoal(5, new Investigate(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> alertLevel() == 2 && spots(e)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Operator.class, 10, true, false,
            e -> e instanceof Operator o && OpType.hostile(type.faction, o.type.faction) && spots(e)));
    }

    /** Whether it can make you out: darkness hides you (unless it has night vision), crouching helps, close is close. */
    boolean spots(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        double d = distanceTo(e);
        if (dazzled > 0) return d < 4;
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        if (alertLevel() == 2) range *= 1.4;     // on alert: every shadow gets looked into
        int light = level().getMaxLocalRawBrightness(e.blockPosition());
        // A weapon light on gives you away from far off
        GunStats ls = GunStats.of(e.getMainHandItem());
        if (ls != null && ls.light) light = 15;
        if (light < 6 && !type.has(OpType.Trait.NVG)) range = Math.min(range, (alertLevel() == 2 ? 22 : 14) + light * 2);
        if (e.isCrouching()) range *= 0.6;
        if (e.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) range *= 0.25;
        return d <= range;
    }

    // ---- Spawning: kit out ----
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        kitOut(getRandom());
        if (home == null) home = blockPosition();
        return out;
    }

    public void kitOut(RandomSource r) {
        String gid = type.guns[r.nextInt(type.guns.length)];
        if (gid.equals("minigun") && r.nextFloat() < 0.6f) gid = "m249";
        var reg = FactoryAscension.GUNS.get(gid);
        if (reg != null) {
            ItemStack gun = new ItemStack(reg.get());
            // Deeper out, better kit: a few attachments, now and then a skin
            int extras = r.nextInt(3) + (type.has(OpType.Trait.HEAVY) ? 1 : 0);
            java.util.List<String> ids = new java.util.ArrayList<>();
            GunSpec spec = GunItem.spec(gun);
            for (int i = 0; i < extras && spec != null; i++) {
                Attachments.Att a = Attachments.ALL.get(r.nextInt(Attachments.ALL.size()));
                if (a.fits(spec) && a.slot != Attachments.Slot.STOCK && a.slot != Attachments.Slot.GRIP) ids.add(a.id);
            }
            GunStats.setIds(gun, ids);
            if (r.nextFloat() < 0.05f) {
                var keys = new java.util.ArrayList<>(Skins.ALL.keySet());
                Skins.set(gun, keys.get(r.nextInt(keys.size())));
            }
            setItemSlot(EquipmentSlot.MAINHAND, gun);
            setDropChance(EquipmentSlot.MAINHAND, 0);   // dropped by hand in dropCustomDeathLoot (with its rounds in it)
            ammo = GunStats.of(gun).mag;
        }
        // Their armor is drawn on their skins (helmets, plate carriers); its protection is in their stats
        getAttribute(Attributes.ARMOR).setBaseValue(type.armorTier * 4 + (type.has(OpType.Trait.HEAVY) ? 6 : 0));
        getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(type.armorTier >= 3 ? type.armorTier : 0);
        grenades = type.has(OpType.Trait.GRENADES) ? 1 + r.nextInt(type.has(OpType.Trait.LOBBER) ? 4 : 2) : 0;
        bandages = 1 + r.nextInt(type.has(OpType.Trait.MEDIC) ? 6 : 2);
        if (!hasCustomName()) setCustomName(Component.literal(Names.make(type, r)));
        setCustomNameVisible(false);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.health * FAConfig.npcHealth);
        setHealth(getMaxHealth());
    }

    @Nullable
    GunSpec gun() {
        return GunItem.spec(getMainHandItem());
    }

    int magSize() {
        GunStats st = GunStats.of(getMainHandItem());
        return st == null ? 0 : st.mag;
    }

    // ---- Shooting ----
    /** One shot (or one shotgun blast) at a point, with this operator's aim. */
    void fireAt(Vec3 aim, LivingEntity intended, float extraSpread) {
        GunSpec spec = gun();
        if (spec == null || !(level() instanceof ServerLevel level)) return;
        GunStats st = GunStats.of(getMainHandItem());
        RandomSource r = getRandom();
        Vec3 eye = getEyePosition();
        Vec3 look = aim.subtract(eye).normalize();
        float moving = getDeltaMovement().horizontalDistanceSqr() > 0.002 ? 1.4f : 1;
        float settle = 1 + Math.max(0, 16 - seenTicks) / 10f;
        float targetMove = intended != null ? (float) Math.min(3, intended.getDeltaMovement().horizontalDistance() * 12) : 0;
        double spreadDeg = (st.spreadHip * 0.45 + 0.6) * type.spread * moving * settle * FAConfig.npcSpread + targetMove + extraSpread + (dazzled > 0 ? 12 : 0);
        double spread = Math.toRadians(spreadDeg);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        int pellets = Math.max(1, st.pellets);
        float dmg = Guns.BASE_DAMAGE * spec.caliber.damage * st.damage * FAConfig.npcDamage * (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
            * (pellets > 1 ? 0.55f : 1);
        double range = st.range * 1.2;
        java.util.Map<LivingEntity, Float> hits = new java.util.HashMap<>();
        for (int i = 0; i < pellets; i++) {
            double a = spread * Math.sqrt(r.nextDouble()), b = r.nextDouble() * Math.PI * 2;
            Vec3 dir = look.add(right.scale(Math.tan(a) * Math.cos(b))).add(up.scale(Math.tan(a) * Math.sin(b))).normalize();
            Vec3 end = eye.add(dir.scale(range));
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            LivingEntity hit = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, stop).inflate(1), e -> e != this && e.isAlive() && !e.isSpectator())) {
                if (e instanceof Operator o && !OpType.hostile(type.faction, o.type.faction)) continue;   // never your own side
                var clip = e.getBoundingBox().inflate(0.12).clip(eye, stop);
                if (clip.isPresent() && clip.get().distanceToSqr(eye) < best) { best = clip.get().distanceToSqr(eye); hit = e; }
            }
            Vec3 tracerEnd = hit != null ? eye.add(dir.scale(Math.sqrt(best))) : stop;
            for (double d = 2; d < eye.distanceTo(tracerEnd); d += 3) {
                Vec3 p = eye.add(dir.scale(d));
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (hit != null) {
                double dist = Math.sqrt(best);
                float falloff = (float) Mth.clampedLerp(1.0, 0.5, (dist - range * 0.3) / Math.max(1, range * 0.7));
                hits.merge(hit, dmg * falloff, Float::sum);
            } else if (block.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = block.getBlockPos();
                BlockState state = level.getBlockState(pos);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), stop.x, stop.y, stop.z, 3, 0.05, 0.05, 0.05, 0.1);
                if (i < 2) dev.factoryascension.FxEvents.impact(level, stop, pos, state);
                if (FAConfig.gunBreaksGlass && (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES))) level.destroyBlock(pos, false, this);
                // A near miss: the crack of a round going past you is frightening
                for (ServerPlayer sp : level.players()) {
                    if (sp.distanceToSqr(stop) < 9 && sp != intended) dev.factoryascension.gun.Stress.add(sp, 0.05f);
                }
            }
        }
        hits.forEach((e, d) -> {
            e.invulnerableTime = 0;
            e.hurt(damageSources().mobAttack(this), d);
            if (e instanceof ServerPlayer sp) {
                dev.factoryascension.gun.Stress.add(sp, 0.12f);
                if (r.nextFloat() < FAConfig.npcBleedChance) sp.addEffect(new MobEffectInstance(FactoryAscension.BLEEDING.get(), 20 * 30, 0));
            }
        });
        Vec3 muzzle = eye.add(look.scale(1.1)).add(0, -0.15, 0);
        if (spec.sound >= 0.5f) level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);
        GunSounds.shot(level, this, spec, st.sound, r);
        GunSounds.casing(level, this, spec, r);
        ammo--;
    }

    /** The cooldown after a shot, and work the bolt / pump if it has one. */
    int afterShot(GunSpec spec, GunStats st) {
        int cd = Math.max(1, Math.round(1200f / Math.max(10, st.rpm)));
        if (!st.auto) cd = Math.round(cd * 1.6f) + 2;
        if (spec.manual && !st.noBolt) {
            cd += spec.chamber + 6;
            level().playSound(null, getX(), getY(), getZ(), spec.kind == GunSpec.Kind.SHOTGUN ? FASounds.PUMP.get() : FASounds.BOLT.get(), SoundSource.HOSTILE, 0.8f, 1);
        }
        return cd;
    }

    void startReload() {
        GunSpec spec = gun();
        if (spec == null || reload > 0) return;
        int t = switch (spec.reload) {
            case MAG, BELT -> spec.magOut + spec.magIn + (ammo <= 0 ? spec.chamber : 0);
            default -> Math.min(80, spec.perRound * Math.max(1, magSize() - Math.max(0, ammo)) + spec.chamber);
        };
        if (getHealth() < getMaxHealth() * 0.5f && getRandom().nextFloat() < 0.3f) {
            t += 20 + getRandom().nextInt(25);   // fumbled it: hurt hands
            reloadFumbled = true;
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_FALL, SoundSource.HOSTILE, 0.8f, 1.3f);
        }
        reload = Math.max(10, t);
        if (t < 30) reloadFumbled = false;
        level().playSound(null, getX(), getY(), getZ(), spec.reload == GunSpec.Reload.MAG || spec.reload == GunSpec.Reload.BELT ? FASounds.MAG_OUT.get() : FASounds.SHELL_IN.get(),
            SoundSource.HOSTILE, 0.8f, 1);
    }

    void tickReload() {
        if (reload <= 0) return;
        reload--;
        GunSpec spec = gun();
        if (spec == null) { reload = 0; return; }
        if (reload == 8 && (spec.reload == GunSpec.Reload.MAG || spec.reload == GunSpec.Reload.BELT))
            level().playSound(null, getX(), getY(), getZ(), FASounds.MAG_IN.get(), SoundSource.HOSTILE, 0.8f, 1);
        if (reload == 0) {
            reloadFumbled = false;
            ammo = magSize();
            level().playSound(null, getX(), getY(), getZ(), spec.manual ? FASounds.BOLT.get() : FASounds.RACK.get(), SoundSource.HOSTILE, 0.8f, 1);
        }
    }

    void throwGrenade(Vec3 at) {
        if (!(level() instanceof ServerLevel level) || grenades <= 0) return;
        grenades--;
        Grenade g = new Grenade(level, this);
        // Breachers and bodyguards go in behind a flashbang; the rest throw frags, now and then a flash
        if (type.has(OpType.Trait.RUSHER) || type.has(OpType.Trait.GUARD) || getRandom().nextFloat() < 0.25f) g.flash();
        Vec3 from = getEyePosition();
        Vec3 d = at.subtract(from);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        g.setPos(from.x, from.y, from.z);
        g.shoot(d.x, d.y + flat * 0.35, d.z, (float) Math.min(1.6, 0.45 + flat * 0.045), 3);
        level.addFreshEntity(g);
        swing(net.minecraft.world.InteractionHand.OFF_HAND);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 0.8f, 0.6f);
    }

    /** Somewhere nearby the threat can't see: solid blocks between its eyes and ours (standing or crouched). */
    @Nullable
    BlockPos findCover(Vec3 threat, int radius) {
        RandomSource r = getRandom();
        BlockPos me = blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < 28; i++) {
            BlockPos p = me.offset(r.nextInt(radius * 2 + 1) - radius, r.nextInt(5) - 2, r.nextInt(radius * 2 + 1) - radius);
            // Stand on something with room to crouch
            for (int k = 0; k < 4 && !level().getBlockState(p.below()).isSolid(); k++) p = p.below();
            if (!level().getBlockState(p.below()).isSolid() || !level().getBlockState(p).isAir() || !level().getBlockState(p.above()).isAir()) continue;
            Vec3 eye = Vec3.atBottomCenterOf(p).add(0, 1.0, 0);
            if (level().clip(new ClipContext(threat, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) continue;
            double score = p.distSqr(me) - Vec3.atCenterOf(p).distanceToSqr(threat) * 0.02;
            if (score < bestScore) {
                Path path = getNavigation().createPath(p, 0);
                if (path == null || !path.canReach()) continue;
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    /** Tell nearby allies who to shoot. */
    void callOut(LivingEntity target) {
        for (Operator o : level().getEntitiesOfClass(Operator.class, getBoundingBox().inflate(32), o -> o != this && o.isAlive() && o.type.faction == type.faction)) {
            if (o.getTarget() == null) {
                o.lastSeen = target.position();
                o.investigate = target.position();
                o.investigateUntil = level().getGameTime() + 400;
                if (o.hasLineOfSight(target)) o.setTarget(target);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (fireCooldown > 0) fireCooldown--;
        if (grenadeCooldown > 0) grenadeCooldown--;
        if (healCooldown > 0) healCooldown--;
        if (dazzled > 0) dazzled--;
        if (barkCooldown > 0) barkCooldown--;
        tickReload();
        setAction(healing > 0 ? 3 : reload > 0 ? (reloadFumbled ? 2 : 1) : 0);
        // Stealth: calm or suspicious, it keeps looking; alert, it hunts until it's been quiet a long while
        if (alertLevel() < 2 && tickCount % 2 == 0) awareness();
        else if (alertLevel() == 2 && getTarget() == null && --alertTimer <= 0) {
            setAlert(1);
            alertTimer = 900;
        } else if (alertLevel() == 1 && suspicion() < 0.05f && --alertTimer <= 0) {
            setAlert(0);
        }
        if (getTarget() != null && alertLevel() < 2) raiseAlert(getTarget(), true);
        // Calm in a group, now and then they talk
        if (alertLevel() == 0 && tickCount % 200 == (getId() % 200) && getRandom().nextFloat() < 0.3f
            && !level().getEntitiesOfClass(Operator.class, getBoundingBox().inflate(6), o -> o != this).isEmpty()) {
            level().playSound(null, getX(), getY() + 1.6, getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.HOSTILE, 0.9f, 0.6f + getRandom().nextFloat() * 0.2f);
        }
        // Out of a fight with a part-empty gun: top it up
        if (getTarget() == null && reload <= 0 && ammo >= 0 && ammo < magSize() / 2) startReload();
        // Night only (cultists): at dawn they slip away
        if (type.has(OpType.Trait.NIGHT) && level().isDay() && !fromStructure && tickCount % 200 == 0 && getRandom().nextFloat() < 0.3f) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // A takedown: hit from behind, by hand, by someone it never noticed
        if (alertLevel() == 0 && source.getDirectEntity() instanceof Player p && !GunItem.firing && source.getEntity() == p) {
            Vec3 toAttacker = p.position().subtract(position()).normalize();
            if (toAttacker.dot(getViewVector(1)) < -0.2) {
                amount = Math.max(amount * 8, getHealth() + 1);
                level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1, 0.6f);
            }
        }
        boolean r = super.hurt(source, amount);
        if (r && source.getEntity() instanceof LivingEntity attacker && isAlive()) {
            boolean ally = attacker instanceof Operator o && !OpType.hostile(type.faction, o.type.faction);
            if (!ally) raiseAlert(attacker, true);
            lastSeen = attacker.position();
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        // Anyone who saw it go down raises the alarm (at the killer, if they can see them too)
        LivingEntity killer = source.getEntity() instanceof LivingEntity le ? le : null;
        for (Operator o : sl.getEntitiesOfClass(Operator.class, getBoundingBox().inflate(32), o -> o != this && o.isAlive() && o.type.faction == type.faction)) {
            if (!o.hasLineOfSight(this)) continue;
            o.investigate = position();
            o.raiseAlert(killer != null && o.hasLineOfSight(killer) ? killer : null, true);
        }
        // It stays where it fell (others may find it)
        Corpse c = new Corpse(Operators.CORPSE.get(), sl);
        c.moveTo(getX(), getY(), getZ(), getYRot(), 0);
        c.setKind(type.id);
        sl.addFreshEntity(c);
    }

    // ---- Death: what it carried ----
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        RandomSource r = getRandom();
        ItemStack gun = getMainHandItem().copy();
        GunSpec spec = GunItem.spec(gun);
        if (spec != null && r.nextFloat() < FAConfig.npcGunDrop) {
            CompoundTag t = gun.getOrCreateTag();
            t.putInt("Ammo", Math.max(0, ammo));
            t.put("Load", CartridgeItem.standard(spec.caliber, AmmoType.REGULAR, 1).getTag());
            spawnAtLocation(gun);
        }
        if (spec != null) {
            int n = 4 + r.nextInt(14);
            spawnAtLocation(CartridgeItem.standard(spec.caliber, r.nextFloat() < 0.15f ? AmmoType.values()[r.nextInt(AmmoType.values().length)] : AmmoType.REGULAR, n));
        }
        if (grenades > 0 && r.nextFloat() < 0.6f) spawnAtLocation(new ItemStack(FactoryAscension.FRAG_GRENADE.get(), Math.min(2, grenades)));
        if (bandages > 0 && r.nextFloat() < 0.6f) spawnAtLocation(new ItemStack(FactoryAscension.BANDAGE.get(), 1 + r.nextInt(Math.min(3, bandages))));
        if (r.nextFloat() < (type.has(OpType.Trait.MEDIC) ? 0.6f : 0.08f)) spawnAtLocation(new ItemStack(FactoryAscension.MEDKIT.get()));
        if (r.nextFloat() < 0.35f) spawnAtLocation(new ItemStack(r.nextBoolean() ? Items.BREAD : Items.COOKED_BEEF, 1 + r.nextInt(3)));
        if (r.nextFloat() < 0.25f) spawnAtLocation(new ItemStack(r.nextBoolean() ? Items.GUNPOWDER : Items.IRON_NUGGET, 2 + r.nextInt(6)));
        if (r.nextFloat() < 0.12f) spawnAtLocation(new ItemStack(FactoryAscension.FLARE_RED.get()));
        // A piece of what they wore, now and then
        if (type.armorTier > 0 && r.nextFloat() < 0.25f) {
            net.minecraft.world.item.Item[][] tiers = {{}, {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE}, {Items.IRON_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.IRON_LEGGINGS},
                {Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}, {Items.DIAMOND_HELMET, Items.NETHERITE_CHESTPLATE, Items.DIAMOND_LEGGINGS}};
            var set = tiers[Math.min(4, type.armorTier)];
            spawnAtLocation(new ItemStack(set[r.nextInt(set.length)]));
        }
        spawnAtLocation(DogTagItem.of(getName().getString(), type, source.getEntity() instanceof Player p ? p.getName().getString() : "", level().getGameTime()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FAAmmo", ammo);
        tag.putInt("FAGrenades", grenades);
        tag.putInt("FABandages", bandages);
        tag.putBoolean("FAStructure", fromStructure);
        tag.putInt("FAAlert", alertLevel());
        if (home != null) tag.putLong("FAHome", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ammo = tag.contains("FAAmmo") ? tag.getInt("FAAmmo") : -1;
        grenades = tag.getInt("FAGrenades");
        bandages = tag.getInt("FABandages");
        fromStructure = tag.getBoolean("FAStructure");
        startAlert(Math.min(1, tag.getInt("FAAlert")));
        if (tag.contains("FAHome")) home = BlockPos.of(tag.getLong("FAHome"));
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !fromStructure && !isPersistenceRequired() && dist > 128 * 128;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (type.has(OpType.Trait.STEALTH) && isCrouching()) return;
        super.playStepSound(pos, state);
    }

    // =============================================================================================================
    // Goals
    // =============================================================================================================

    /** The fight: keep the range it likes, strafe, crouch, shoot in bursts, reload and heal behind cover, flank, throw. */
    static final class Combat extends Goal {
        final Operator op;
        int strafeTime = 0, strafeDir = 1, peek = 0, coverTicks = 0, lostTicks = 0, flankSide = 1;
        @Nullable BlockPos cover;

        Combat(Operator op) {
            this.op = op;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = op.getTarget();
            return t != null && t.isAlive() && op.gun() != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = op.getTarget();
            return t != null && t.isAlive() && lostTicks < 500 && op.gun() != null;
        }

        @Override
        public void start() {
            op.setAggressive(true);
            op.seenTicks = 0;
            lostTicks = 0;
            flankSide = op.getRandom().nextBoolean() ? 1 : -1;
            if (op.getTarget() != null) op.callOut(op.getTarget());
            if (op.ammo < 0) op.ammo = op.magSize();
        }

        @Override
        public void stop() {
            op.setAiming(false);
            op.setAggressive(false);
            op.setPose(Pose.STANDING);
            op.getNavigation().stop();
            if (op.lastSeen != null) {
                op.investigate = op.lastSeen;
                op.investigateUntil = op.level().getGameTime() + 600;
            }
            op.setTarget(null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = op.getTarget();
            if (t == null) return;
            GunSpec spec = op.gun();
            GunStats st = GunStats.of(op.getMainHandItem());
            if (spec == null || st == null) return;
            boolean see = op.getSensing().hasLineOfSight(t);
            double dist = op.distanceTo(t);
            if (see) {
                op.lastSeen = t.getEyePosition();
                op.seenTicks++;
                lostTicks = 0;
                op.unseenTicks = 0;
            } else {
                op.seenTicks = 0;
                op.unseenTicks++;
                lostTicks++;
                // Trackers always seem to know
                if (op.type.has(OpType.Trait.TRACKER) && op.unseenTicks % 60 == 0) op.lastSeen = t.getEyePosition();
            }
            Vec3 lookAt = see ? t.getEyePosition() : (op.lastSeen != null ? op.lastSeen : t.getEyePosition());
            op.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z, 40, 40);
            boolean hurt = op.getHealth() < op.getMaxHealth() * 0.4f;
            // Running away (smugglers when hurt, anyone badly hurt with nothing left)
            if (op.type.has(OpType.Trait.FLEES) && hurt || hurt && op.bandages <= 0 && op.ammo <= 0) {
                op.setAiming(false);
                Vec3 away = op.position().subtract(t.position()).normalize().scale(16).add(op.position());
                op.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                return;
            }
            // Reloading or patching up: behind cover first
            boolean needCover = op.reload > 0 || (op.ammo <= 0) || (hurt && op.bandages > 0 && op.healCooldown <= 0) || op.healing > 0;
            if (op.ammo <= 0 && op.reload <= 0) op.startReload();
            if (needCover) {
                op.setAiming(false);
                if (cover == null || coverTicks++ > 100) {
                    cover = op.findCover(t.getEyePosition(), 9);
                    coverTicks = 0;
                }
                if (cover != null && op.blockPosition().distSqr(cover) > 2) op.getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 1.25);
                else {
                    op.getNavigation().stop();
                    op.setPose(Pose.CROUCHING);
                    if (hurt && op.bandages > 0 && op.healCooldown <= 0 && op.healing <= 0) op.healing = 50;
                }
                if (op.healing > 0 && --op.healing == 0) {
                    op.bandages--;
                    op.heal(6 + op.getMaxHealth() * 0.15f);
                    op.healCooldown = 300;
                    op.level().playSound(null, op.getX(), op.getY(), op.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.HOSTILE, 0.8f, 0.8f);
                }
                return;
            }
            cover = null;
            // Grenade: you're hiding and it knows roughly where
            if (!see && op.grenades > 0 && op.grenadeCooldown <= 0 && op.lastSeen != null && op.unseenTicks > 40 && op.unseenTicks < 300) {
                double gd = op.position().distanceTo(op.lastSeen);
                if (gd > 6 && gd < 28) {
                    op.throwGrenade(op.lastSeen);
                    op.grenadeCooldown = op.type.has(OpType.Trait.LOBBER) ? 140 : 300;
                }
            }
            // Moving: into its range band, then strafing
            boolean sniper = op.type.has(OpType.Trait.SNIPER);
            if (!see || dist > op.type.maxRange) {
                op.setPose(Pose.STANDING);
                Vec3 to = op.lastSeen != null ? op.lastSeen : t.position();
                if (op.type.has(OpType.Trait.FLANK) && dist > 10) {
                    // Come at it from the side
                    Vec3 dir = to.subtract(op.position()).normalize();
                    Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(flankSide * Math.min(14, dist * 0.5));
                    to = to.add(side);
                }
                if (!(sniper && see)) op.getNavigation().moveTo(to.x, to.y, to.z, op.type.has(OpType.Trait.STEALTH) ? 0.8 : 1.15);
                if (op.type.has(OpType.Trait.STEALTH) && dist < 24 && !see) op.setPose(Pose.CROUCHING);
            } else {
                op.getNavigation().stop();
                if (--strafeTime <= 0) {
                    strafeTime = 15 + op.getRandom().nextInt(40);
                    strafeDir = op.getRandom().nextBoolean() ? 1 : -1;
                    peek = op.getRandom().nextInt(5);
                }
                float fwd = dist < op.type.minRange && !op.type.has(OpType.Trait.RUSHER) ? -0.5f : (op.type.has(OpType.Trait.RUSHER) && dist > op.type.minRange ? 0.6f : 0);
                float sideMove = sniper ? 0 : 0.45f * strafeDir;
                op.getMoveControl().strafe(fwd, sideMove);
                op.setPose(peek == 0 || sniper && dist > 30 ? Pose.CROUCHING : Pose.STANDING);
            }
            // Shooting
            op.setAiming(see || op.type.has(OpType.Trait.SUPPRESS) && op.unseenTicks < 80);
            int reaction = sniper ? 25 : op.type.has(OpType.Trait.RUSHER) ? 6 : 12;
            boolean canShoot = op.fireCooldown <= 0 && op.ammo > 0 && op.reload <= 0;
            if (canShoot && see && op.seenTicks > reaction && dist <= st.range * 1.3) {
                Vec3 aim = t.position().add(0, t.getBbHeight() * (0.5 + op.getRandom().nextFloat() * 0.35), 0);
                op.fireAt(aim, t, 0);
                op.fireCooldown = op.afterShot(spec, st);
                if (st.auto) {
                    if (op.burstLeft <= 0) op.burstLeft = op.type.has(OpType.Trait.SUPPRESS) ? 6 + op.getRandom().nextInt(10) : 2 + op.getRandom().nextInt(6);
                    if (--op.burstLeft <= 0) op.fireCooldown += 8 + op.getRandom().nextInt(18);
                }
            } else if (canShoot && !see && op.type.has(OpType.Trait.SUPPRESS) && op.lastSeen != null && op.unseenTicks < 80) {
                // Suppressing: keep firing where you were
                op.fireAt(op.lastSeen, null, 3);
                op.fireCooldown = op.afterShot(spec, st) + 2;
            }
        }
    }

    /** Medics patch up hurt squad mates. */
    static final class Medic extends Goal {
        final Operator op;
        @Nullable Operator patient;
        int work = 0;

        Medic(Operator op) {
            this.op = op;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!op.type.has(OpType.Trait.MEDIC) || op.bandages <= 0 || op.healCooldown > 0 || op.reload > 0) return false;
            if (op.getTarget() != null && op.getSensing().hasLineOfSight(op.getTarget())) return false;
            List<Operator> hurt = op.level().getEntitiesOfClass(Operator.class, op.getBoundingBox().inflate(16),
                o -> o != op && o.isAlive() && o.type.faction == op.type.faction && o.getHealth() < o.getMaxHealth() * 0.6f);
            if (hurt.isEmpty()) return false;
            patient = hurt.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return patient != null && patient.isAlive() && op.bandages > 0 && work < 120;
        }

        @Override
        public void start() {
            work = 0;
        }

        @Override
        public void tick() {
            if (patient == null) return;
            op.getLookControl().setLookAt(patient, 30, 30);
            if (op.distanceTo(patient) > 2.2) {
                op.getNavigation().moveTo(patient, 1.2);
                return;
            }
            op.getNavigation().stop();
            op.setPose(Pose.CROUCHING);
            if (++work % 30 == 0 && op.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + 1.6, patient.getZ(), 2, 0.3, 0.2, 0.3, 0);
                level.playSound(null, op.getX(), op.getY(), op.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.HOSTILE, 0.7f, 1.1f);
                patient.heal(5);
                if (patient.getHealth() >= patient.getMaxHealth() * 0.9f) {
                    op.bandages--;
                    op.healCooldown = 200;
                    work = 999;
                }
            }
        }

        @Override
        public void stop() {
            patient = null;
            op.setPose(Pose.STANDING);
        }
    }

    /** Bodyguards stay by their warlord. */
    static final class Guard extends Goal {
        final Operator op;
        @Nullable Operator boss;

        Guard(Operator op) {
            this.op = op;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!op.type.has(OpType.Trait.GUARD) || op.getTarget() != null) return false;
            List<Operator> leaders = op.level().getEntitiesOfClass(Operator.class, op.getBoundingBox().inflate(32), o -> o.isAlive() && o.type.has(OpType.Trait.LEADER));
            if (leaders.isEmpty()) return false;
            boss = leaders.get(0);
            return op.distanceTo(boss) > 5;
        }

        @Override
        public boolean canContinueToUse() {
            return boss != null && boss.isAlive() && op.getTarget() == null && op.distanceTo(boss) > 3;
        }

        @Override
        public void tick() {
            if (boss != null) op.getNavigation().moveTo(boss, 1.0);
        }
    }

    /** Going to look: a gunshot it heard, a flare it saw, where its target was last seen. */
    static final class Investigate extends Goal {
        final Operator op;

        Investigate(Operator op) {
            this.op = op;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return op.getTarget() == null && op.investigate != null && op.level().getGameTime() < op.investigateUntil;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && op.position().distanceToSqr(op.investigate) > 9;
        }

        @Override
        public void start() {
            Vec3 v = op.investigate;
            op.getNavigation().moveTo(v.x, v.y, v.z, op.type.has(OpType.Trait.STEALTH) ? 0.8 : 1.05);
        }

        @Override
        public void tick() {
            if (op.getNavigation().isDone() && op.investigate != null) {
                Vec3 v = op.investigate;
                op.getNavigation().moveTo(v.x, v.y, v.z, 1.0);
            }
            if (op.type.has(OpType.Trait.STEALTH)) op.setPose(Pose.CROUCHING);
        }

        @Override
        public void stop() {
            op.setPose(Pose.STANDING);
            if (op.investigate != null && op.position().distanceToSqr(op.investigate) <= 9) op.investigate = null;
        }
    }
}
