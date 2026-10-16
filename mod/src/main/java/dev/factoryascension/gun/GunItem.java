package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunSpec.Kind;
import dev.factoryascension.gun.GunSpec.Reload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Every gun but the original shotgun: one class, the differences in its GunSpec. Hold right-click to aim, left-click to
 * fire (hold it on automatics), R to reload. Rounds are cartridges (gun/CartridgeItem) of the gun's caliber; whatever
 * type and making they are changes the shot.
 *
 * Reloading is a series of steps kept in the gun's NBT, which the client reads to animate it:
 *   magazine / belt: OUT (old mag out, its rounds go back to you), IN (new mag in), CHAMBER (only from empty)
 *   tube: ROUND per shell, then CHAMBER from empty;  cylinder / break-action: OUT, ROUND each, IN (close)
 *   bolt with clips: OUT (bolt open), ROUND each, CHAMBER (bolt closed)
 * Before each step a fumble may happen (gun/Fumble, likelier the more stressed you are): it costs time and says so.
 */
public class GunItem extends Item {
    public static final int NONE = 0, OUT = 1, IN = 2, CHAMBER = 3, ROUND = 4, FUMBLE = 5;
    public final GunSpec spec;

    public GunItem(GunSpec spec, Properties props) {
        super(props);
        this.spec = spec;
    }

    public static GunSpec spec(ItemStack s) {
        return s.getItem() instanceof GunItem g ? g.spec : null;
    }

    // ---- State ----
    private static CompoundTag tag(ItemStack s) {
        return s.getOrCreateTag();
    }

    public static int ammo(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("Ammo") : 0;
    }

    public static int phase(ItemStack s) {
        return s.hasTag() ? s.getTag().getInt("RPh") : NONE;
    }

    public static boolean reloading(ItemStack s) {
        return phase(s) != NONE;
    }

    /** 0..1 through the current reload step (for the animation). */
    public static float phaseProgress(ItemStack s) {
        if (!s.hasTag()) return 0;
        int len = Math.max(1, s.getTag().getInt("RL"));
        return 1 - s.getTag().getInt("RT") / (float) len;
    }

    @Nullable
    public static Fumble fumble(ItemStack s) {
        if (phase(s) != FUMBLE) return null;
        int i = s.getTag().getInt("RF");
        return i >= 0 && i < Fumble.values().length ? Fumble.values()[i] : null;
    }

    /** The round loaded (a cartridge's NBT), for its type. */
    public static AmmoType loadedType(ItemStack s) {
        return s.hasTag() && s.getTag().contains("Load") ? AmmoType.byName(s.getTag().getCompound("Load").getString("Type")) : AmmoType.REGULAR;
    }

    // ---- Aiming: right-click and hold ----
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public static boolean aiming(LivingEntity e) {
        return e.isUsingItem() && e.getUseItem().getItem() instanceof GunItem;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    // ---- Ammo in your inventory ----
    static boolean fits(GunSpec spec, ItemStack s) {
        if (s.getItem() instanceof CartridgeItem) return CartridgeItem.caliber(s) == spec.caliber;
        return spec.caliber == Caliber.G12 && s.is(FactoryAscension.SHOTGUN_SHELL.get());
    }

    /** The round tag a stack would load (shotgun shells count as plain buckshot). */
    static CompoundTag roundTag(GunSpec spec, ItemStack s) {
        if (s.getItem() instanceof CartridgeItem) return s.getTag() == null ? new CompoundTag() : s.getTag().copy();
        return CartridgeItem.standard(Caliber.G12, AmmoType.REGULAR, 1).getTag().copy();
    }

    static boolean same(CompoundTag a, CompoundTag b) {
        return a.getString("Cal").equals(b.getString("Cal")) && a.getString("Type").equals(b.getString("Type")) && a.getString("Prop").equals(b.getString("Prop"))
            && a.getString("Case").equals(b.getString("Case")) && a.getInt("Color") == b.getInt("Color");
    }

    /** The first stack of rounds this gun takes; with `like`, only rounds the same as that. */
    @Nullable
    static ItemStack findRounds(ServerPlayer p, GunSpec spec, @Nullable CompoundTag like) {
        for (ItemStack s : p.getInventory().items) if (!s.isEmpty() && fits(spec, s) && (like == null || same(roundTag(spec, s), like))) return s;
        return null;
    }

    static void giveBack(ServerPlayer p, CompoundTag round, int n) {
        if (n <= 0 || round.isEmpty()) return;
        ItemStack back = new ItemStack(FactoryAscension.CARTRIDGE.get(), n);
        back.setTag(round.copy());
        if (!p.getInventory().add(back)) p.drop(back, false);
    }

    // ---- Firing ----
    /** For the self-test: what the last shot did. */
    public static int lastHits = 0, lastPellets = 0;
    /** The shot being fired: how far the player leans (-1..1) and where the gun points off the crosshair (degrees). */
    public static float shotLean = 0, shotYaw = 0, shotPitch = 0;
    /** True while a shot's damage is being dealt (so a gunshot is never taken for a punch: npc takedowns). */
    public static boolean firing = false;
    public static float lastDamage = 0;
    public static String lastResult = "";

    public static void fire(ServerPlayer player, boolean aimingNow) {
        ItemStack gun = player.getMainHandItem();
        GunSpec spec = spec(gun);
        if (spec == null || player.getCooldowns().isOnCooldown(gun.getItem())) return;
        GunStats st = GunStats.of(gun);
        ServerLevel level = player.serverLevel();
        CompoundTag t = tag(gun);
        int ph = phase(gun);
        if (ph != NONE) {
            // One-round-at-a-time reloads can be cut short to shoot; a magazine reload can't
            boolean perRound = spec.reload == Reload.TUBE || spec.reload == Reload.BOLT_CLIP;
            if (!perRound || ammo(gun) <= 0 || ph == OUT) return;
            clearReload(t);
        }
        if (t.getInt("Bolt") > 0) return;
        if (t.getBoolean("NoCh") && ammo(gun) > 0) {
            // A reload that forgot to chamber: the trigger clicks, then you rack it
            t.putBoolean("NoCh", false);
            t.putInt("Bolt", spec.chamber);
            sound(level, player, SoundEvents.DISPENSER_FAIL, 0.7f, 1.8f);
            sound(level, player, SoundEvents.PISTON_CONTRACT, 0.6f, 1.7f);
            player.displayClientMessage(Component.literal("Click. No round chambered: racking it").withStyle(ChatFormatting.YELLOW), true);
            player.getCooldowns().addCooldown(gun.getItem(), spec.chamber);
            lastResult = "racked";
            return;
        }
        if (ammo(gun) <= 0 && !player.isCreative()) {
            sound(level, player, dev.factoryascension.FASounds.DRY.get(), 0.8f, 1.0f);
            startReload(player, gun);
            lastResult = "empty";
            return;
        }
        CompoundTag load = t.getCompound("Load");
        AmmoType type = load.isEmpty() ? AmmoType.REGULAR : AmmoType.byName(load.getString("Type"));
        float power = load.contains("Power") ? load.getFloat("Power") : 1, rel = load.contains("Rel") ? load.getFloat("Rel") : 1;
        RandomSource r = player.getRandom();
        boolean bolt = spec.manual && !st.noBolt;
        player.getCooldowns().addCooldown(gun.getItem(), Math.max(st.cooldown(), bolt ? spec.chamber + 6 : 0));
        if (!player.isCreative()) t.putInt("Ammo", ammo(gun) - 1);
        // A bad round: the primer clicks and nothing happens (cheap cases, weird propellant)
        if (r.nextFloat() < (1 - rel) * 0.5f) {
            sound(level, player, SoundEvents.DISPENSER_FAIL, 0.8f, 1.3f);
            player.displayClientMessage(Component.literal("Misfire").withStyle(ChatFormatting.YELLOW), true);
            t.putInt("Bolt", spec.chamber);
            lastResult = "misfire";
            return;
        }
        if (bolt) t.putInt("Bolt", spec.chamber + 4);   // work the bolt after every shot (unless an auto sear says otherwise)
        t.putInt("Shots", t.getInt("Shots") + 1);
        Stress.add(player, 0.004f);

        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1);
        // Leaning: the round leaves from where you peek (not through the wall beside you)
        if (shotLean != 0) {
            Vec3 side = look.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() > 1e-4) {
                Vec3 to = eye.add(side.normalize().scale(shotLean * Lean.DISTANCE)).add(0, -Math.abs(shotLean) * 0.08, 0);
                BlockHitResult wall = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                eye = wall.getType() == HitResult.Type.MISS ? to : eye.add(wall.getLocation().subtract(eye).scale(0.8));
            }
        }
        // From the hip, the round goes where the gun points (the crosshair shows it), not dead centre
        if (shotYaw != 0 || shotPitch != 0) look = Vec3.directionFromRotation(player.getXRot() + shotPitch, player.getYRot() + shotYaw);
        boolean aim = aimingNow && aiming(player);
        // Pellets: the round's (snakeshot, buckshot, ...) if it has several, else the gun's (a shotgun's plain shell);
        // a single projectile from a shotgun (slug, AP, ...) carries the weight of the whole shell
        float base = Guns.BASE_DAMAGE * spec.caliber.damage * st.damage * type.damage * power;
        int pellets;
        float perPellet;
        int gunPellets = st.pellets;   // the gun's own (a shotgun's), plus any an attachment adds
        if (gunPellets > 1 && (type == AmmoType.REGULAR || type == AmmoType.BUCKSHOT)) { pellets = gunPellets; perPellet = Guns.BASE_DAMAGE * spec.caliber.damage * st.damage * power; }
        else if (type.pellets > 1) { pellets = type.pellets + Math.max(0, gunPellets - spec.pellets); perPellet = base; }
        else if (spec.pellets > 1) { pellets = 1; perPellet = base * spec.pellets * 0.6f; }
        else { pellets = Math.max(1, gunPellets); perPellet = gunPellets > 1 ? base * 0.5f : base; }
        double spread = Math.toRadians((aim ? st.spreadAim : st.spreadHip) + (pellets > 1 && spec.pellets == 1 ? 4 : 0));
        double range = st.range * type.range * Math.min(1.5, Math.max(0.3, power));
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        Map<Entity, Float> hits = new HashMap<>();
        int impacts = 0;
        int pierce = type.penetration >= 2 ? 3 : 1;
        for (int i = 0; i < pellets; i++) {
            double a = spread * Math.sqrt(r.nextDouble()), b = r.nextDouble() * Math.PI * 2;
            Vec3 dir = look.add(right.scale(Math.tan(a) * Math.cos(b))).add(up.scale(Math.tan(a) * Math.sin(b))).normalize();
            Vec3 end = eye.add(dir.scale(range));
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            List<EntityHitResult> along = entitiesAlong(player, eye, stop);
            int n = 0;
            Vec3 lastHit = null;
            for (EntityHitResult eh : along) {
                if (n >= pierce) break;
                double dist = eye.distanceTo(eh.getLocation());
                float falloff = (float) Mth.clampedLerp(1.0, 0.45, (dist - range * 0.3) / Math.max(1, range * 0.7));
                hits.merge(eh.getEntity(), perPellet * falloff, Float::sum);
                lastHit = eh.getLocation();
                n++;
            }
            Vec3 tracerEnd = lastHit != null && pierce == 1 ? lastHit : stop;
            tracer(level, eye, dir, eye.distanceTo(tracerEnd), type);
            if (lastHit != null && pierce == 1) continue;
            if (block.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = block.getBlockPos();
                BlockState state = level.getBlockState(pos);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), stop.x, stop.y, stop.z, 3, 0.05, 0.05, 0.05, 0.1);
                if (impacts++ < 3) dev.factoryascension.FxEvents.impact(level, stop, pos, state);
                if (type == AmmoType.EXPLOSIVE && impacts <= 2) pop(level, player, stop);
                if (type == AmmoType.INCENDIARY || type == AmmoType.DRAGONS_BREATH) level.sendParticles(ParticleTypes.FLAME, stop.x, stop.y, stop.z, 3, 0.1, 0.1, 0.1, 0.01);
                if (FAConfig.gunBreaksGlass && type != AmmoType.FRANGIBLE && type != AmmoType.RUBBER && (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)))
                    level.destroyBlock(pos, false, player);
            }
        }
        // All of a target's pellets land as one hit
        lastHits = hits.size();
        lastPellets = pellets;
        lastDamage = 0;
        final AmmoType ft = type;
        final Vec3 shotDir = look;
        hits.forEach((e, dmg) -> {
            float d = dmg;
            if (e instanceof LivingEntity le) {
                int armor = le.getArmorValue();
                if (ft == AmmoType.HOLLOW_POINT && armor > 4) d *= 0.7f;
                if (ft.penetration >= 2) d *= 1 + armor * 0.04f;
                if (ft == AmmoType.FRANGIBLE) d *= 1.15f;
            }
            e.invulnerableTime = 0;
            lastDamage += d;
            firing = true;
            try {
                e.hurt(player.damageSources().playerAttack(player), d);
            } finally {
                firing = false;
            }
            if (e instanceof LivingEntity le && le.isAlive()) effects(level, player, le, ft, shotDir, d, spec);
            if (ft == AmmoType.EXPLOSIVE) pop(level, player, e.position().add(0, e.getBbHeight() / 2, 0));
        });
        // Muzzle flash, smoke and the bang
        Vec3 muzzle = eye.add(look.scale(1.3)).add(right.scale(aim ? 0 : 0.3)).add(up.scale(aim ? -0.08 : -0.22));
        if (type != AmmoType.SUBSONIC) level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.02);
        if (type == AmmoType.DRAGONS_BREATH) level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 30, 0.2, 0.2, 0.2, 0.15);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);
        if (r.nextInt(3) == 0) dev.factoryascension.FxEvents.smoke(level, muzzle, 0.25f);
        bang(level, player, spec, type, r, st.sound);
        lastResult = "fired";
    }

    private static List<EntityHitResult> entitiesAlong(ServerPlayer player, Vec3 from, Vec3 to) {
        List<EntityHitResult> out = new ArrayList<>();
        AABB box = new AABB(from, to).inflate(1);
        for (Entity e : player.level().getEntities(player, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable())) {
            var hit = e.getBoundingBox().inflate(0.15).clip(from, to);
            hit.ifPresent(v -> out.add(new EntityHitResult(e, v)));
        }
        out.sort(Comparator.comparingDouble(h -> h.getLocation().distanceToSqr(from)));
        return out;
    }

    private static void tracer(ServerLevel level, Vec3 eye, Vec3 dir, double len, AmmoType type) {
        var particle = type == AmmoType.TRACER ? ParticleTypes.END_ROD : (type == AmmoType.INCENDIARY || type == AmmoType.DRAGONS_BREATH ? ParticleTypes.SMALL_FLAME : ParticleTypes.CRIT);
        double stepLen = type == AmmoType.TRACER ? 1.2 : 3.0;
        for (double d = 1.5; d < len; d += stepLen) {
            Vec3 p = eye.add(dir.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void pop(ServerLevel level, ServerPlayer player, Vec3 at) {
        level.explode(player, at.x, at.y, at.z, 1.1f, false, FAConfig.explosiveBreaksBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
    }

    private static void effects(ServerLevel level, ServerPlayer player, LivingEntity le, AmmoType t, Vec3 look, float dmg, GunSpec spec) {
        switch (t) {
            case RIP -> le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));            // bleeding
            case INCENDIARY, DRAGONS_BREATH -> le.setSecondsOnFire(t == AmmoType.DRAGONS_BREATH ? 6 : 4);
            case TRACER -> le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            case RUBBER -> le.knockback(2.2, -look.x, -look.z);
            case SLUG -> le.knockback(1.2, -look.x, -look.z);
            case ENDER -> {
                for (int i = 0; i < 16; i++) {
                    double x = le.getX() + (le.getRandom().nextDouble() - 0.5) * 16, z = le.getZ() + (le.getRandom().nextDouble() - 0.5) * 16;
                    if (le.randomTeleport(x, le.getY() + le.getRandom().nextInt(8) - 4, z, true)) break;
                }
            }
            default -> {
                if (spec.pellets > 1) le.knockback(Math.min(1.2, dmg / 40.0), -look.x, -look.z);
            }
        }
    }

    private static void bang(ServerLevel level, ServerPlayer p, GunSpec spec, AmmoType type, RandomSource r, float soundMult) {
        float loud = (type == AmmoType.SUBSONIC ? 0.3f : 1) * Math.max(0.05f, soundMult);
        GunSounds.shot(level, p, spec, loud, r);
        GunSounds.casing(level, p, spec, r);
    }

    static void sound(ServerLevel level, Player p, SoundEvent s, float vol, float pitch) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    // ---- Reloading ----
    /** For the self-test: the fumbles the last reloads had. */
    public static final List<Fumble> lastFumbles = new ArrayList<>();
    /** For the self-test: force the fumble chance (negative = use stress). */
    public static float forceFumbleChance = -1;

    public static void startReload(ServerPlayer player, ItemStack gun) {
        GunSpec spec = spec(gun);
        if (spec == null || reloading(gun)) return;
        CompoundTag t = tag(gun);
        if (ammo(gun) >= magOf(gun) && !t.getBoolean("NoCh")) return;
        if (!player.isCreative() && findRounds(player, spec, null) == null) {
            player.displayClientMessage(Component.literal("No " + spec.caliber.label + " rounds").withStyle(ChatFormatting.RED), true);
            return;
        }
        t.putBoolean("Dry", ammo(gun) <= 0);
        t.putFloat("Shake", 1);
        lastFumbles.clear();
        List<Fumble> start = Fumble.at(Fumble.Step.START, spec);
        if (!start.isEmpty() && player.getRandom().nextFloat() < chance(player) * 0.6f) {
            t.putFloat("Shake", 1.35f);
            lastFumbles.add(Fumble.SHAKY_HANDS);
            player.displayClientMessage(Component.literal(Fumble.SHAKY_HANDS.message).withStyle(ChatFormatting.YELLOW), true);
        }
        enter(player, gun, spec, spec.reload == Reload.TUBE ? ROUND : OUT, true);
    }

    private static float chance(ServerPlayer p) {
        return forceFumbleChance >= 0 ? forceFumbleChance : Stress.fumbleChance(p);
    }

    private static Fumble.Step step(int phase) {
        return switch (phase) {
            case OUT -> Fumble.Step.OUT;
            case IN -> Fumble.Step.IN;
            case CHAMBER -> Fumble.Step.CHAMBER;
            default -> Fumble.Step.ROUND;
        };
    }

    private static int length(GunSpec spec, int phase) {
        return switch (phase) {
            case OUT -> spec.magOut;
            case IN -> spec.magIn;
            case CHAMBER -> spec.chamber;
            default -> spec.perRound;
        };
    }

    /** Go to a reload step, maybe fumbling first. */
    private static void enter(ServerPlayer player, ItemStack gun, GunSpec spec, int phase, boolean mayFumble) {
        CompoundTag t = tag(gun);
        float shake = t.contains("Shake") ? t.getFloat("Shake") : 1;
        RandomSource r = player.getRandom();
        if (mayFumble) {
            List<Fumble> options = Fumble.at(step(phase), spec);
            float c = chance(player) * (phase == ROUND ? 0.5f : 1);
            if (!options.isEmpty() && r.nextFloat() < c) {
                Fumble f = options.get(r.nextInt(options.size()));
                lastFumbles.add(f);
                player.displayClientMessage(Component.literal(f.message).withStyle(ChatFormatting.YELLOW), true);
                fumbleSound(player, f);
                if (f == Fumble.FORGOT_CHAMBER) {   // the reload "finishes", but the first pull will click
                    t.putBoolean("NoCh", true);
                    clearReload(t);
                    return;
                }
                if (f == Fumble.DROPPED_ROUND) dropOneRound(player, spec, t);
                int len = Math.max(1, Math.round(f.ticks * shake));
                t.putInt("RPh", FUMBLE);
                t.putInt("RF", f.ordinal());
                t.putInt("RT", len);
                t.putInt("RL", len);
                t.putInt("RNext", phase);
                return;
            }
        }
        GunStats st = GunStats.of(gun);
        int len = Math.max(1, Math.round(length(spec, phase) * shake * (st != null ? st.reload : 1)));
        t.putInt("RPh", phase);
        t.putInt("RF", -1);
        t.putInt("RT", len);
        t.putInt("RL", len);
        phaseSound(player, spec, phase);
    }

    private static void dropOneRound(ServerPlayer player, GunSpec spec, CompoundTag t) {
        ItemStack s = findRounds(player, spec, t.getInt("Ammo") > 0 ? t.getCompound("Load") : null);
        if (s == null || player.isCreative()) return;
        ItemStack one = s.split(1);
        ItemEntity e = new ItemEntity(player.level(), player.getX(), player.getY() + 1.1, player.getZ(), one);
        e.setDeltaMovement((player.getRandom().nextDouble() - 0.5) * 0.15, 0.1, (player.getRandom().nextDouble() - 0.5) * 0.15);
        e.setPickUpDelay(30);
        player.level().addFreshEntity(e);
    }

    static void clearReload(CompoundTag t) {
        for (String k : new String[] {"RPh", "RT", "RL", "RF", "RNext", "Shake", "Dry"}) t.remove(k);
    }

    /** One tick of reloading (the gun must be in hand). Public for the self-test, which has no ticking player. */
    public static void tickReload(ServerPlayer player, ItemStack gun) {
        GunSpec spec = spec(gun);
        if (spec == null) return;
        CompoundTag t = tag(gun);
        int bolt = t.getInt("Bolt");
        if (bolt > 0) {
            t.putInt("Bolt", bolt - 1);
            if (bolt == Math.max(2, spec.chamber - 2) && spec.manual)
                sound(player.serverLevel(), player, spec.kind == Kind.SHOTGUN ? dev.factoryascension.FASounds.PUMP.get() : spec.reload == Reload.TUBE ? dev.factoryascension.FASounds.RACK.get() : dev.factoryascension.FASounds.BOLT.get(), 0.8f, 1.0f);
        }
        int ph = phase(gun);
        if (ph == NONE) return;
        int left = t.getInt("RT") - 1;
        if (left > 0) {
            t.putInt("RT", left);
            return;
        }
        if (ph == FUMBLE) {
            // Over it: on to the step it interrupted (it may still go wrong, though less likely)
            int next = t.getInt("RNext");
            float keep = forceFumbleChance;
            if (forceFumbleChance < 0 || forceFumbleChance > 0.3f) forceFumbleChance = Math.min(0.3f, chance(player) * 0.4f);
            enter(player, gun, spec, next, true);
            forceFumbleChance = keep;
            return;
        }
        complete(player, gun, spec, ph);
    }

    private static void complete(ServerPlayer player, ItemStack gun, GunSpec spec, int ph) {
        CompoundTag t = tag(gun);
        boolean creative = player.isCreative();
        switch (ph) {
            case OUT -> {
                if (spec.reload == Reload.MAG || spec.reload == Reload.BELT || spec.reload == Reload.CYLINDER || spec.reload == Reload.BREAK) {
                    // The rounds left in the old mag (or the unfired ones in the cylinder) go back to you
                    if (!creative) giveBack(player, t.getCompound("Load"), ammo(gun));
                    t.putInt("Ammo", 0);
                }
                enter(player, gun, spec, spec.reload == Reload.MAG || spec.reload == Reload.BELT ? IN : ROUND, true);
            }
            case IN -> {
                if (spec.reload == Reload.MAG || spec.reload == Reload.BELT) {
                    int cap = magOf(gun);
                    int got = creative ? cap : 0;
                    CompoundTag load = null;
                    if (!creative) {
                        ItemStack first = findRounds(player, spec, null);
                        if (first != null) {
                            load = roundTag(spec, first);
                            while (got < cap) {
                                ItemStack s = findRounds(player, spec, load);
                                if (s == null) break;
                                int take = Math.min(cap - got, s.getCount());
                                s.shrink(take);
                                got += take;
                            }
                        }
                    } else if (!t.contains("Load")) {
                        load = CartridgeItem.standard(spec.caliber, AmmoType.REGULAR, 1).getTag();
                    }
                    if (load != null) t.put("Load", load);
                    t.putInt("Ammo", got);
                    if (t.getBoolean("Dry") && got > 0) { enter(player, gun, spec, CHAMBER, true); return; }
                }
                finish(player, gun, spec);
            }
            case ROUND -> {
                boolean empty = ammo(gun) <= 0;
                ItemStack s = creative ? null : findRounds(player, spec, empty ? null : t.getCompound("Load"));
                if (s != null || creative) {
                    if (s != null) {
                        if (empty) t.put("Load", roundTag(spec, s));
                        s.shrink(1);
                    } else if (!t.contains("Load")) {
                        t.put("Load", CartridgeItem.standard(spec.caliber, AmmoType.REGULAR, 1).getTag());
                    }
                    t.putInt("Ammo", ammo(gun) + 1);
                }
                boolean more = ammo(gun) < magOf(gun) && (creative || findRounds(player, spec, t.getCompound("Load")) != null) && (s != null || creative);
                if (more) { enter(player, gun, spec, ROUND, true); return; }
                switch (spec.reload) {
                    case CYLINDER, BREAK -> enter(player, gun, spec, IN, false);
                    case BOLT_CLIP -> enter(player, gun, spec, CHAMBER, true);
                    default -> {
                        if (t.getBoolean("Dry") && ammo(gun) > 0) enter(player, gun, spec, CHAMBER, true);
                        else finish(player, gun, spec);
                    }
                }
            }
            default -> finish(player, gun, spec);
        }
    }

    /** Rounds it holds with its attachments (an extended magazine, a drum, ...). */
    public static int magOf(ItemStack gun) {
        GunStats st = GunStats.of(gun);
        return st != null ? st.mag : 1;
    }

    private static void finish(ServerPlayer player, ItemStack gun, GunSpec spec) {
        clearReload(tag(gun));
        sound(player.serverLevel(), player, SoundEvents.CROSSBOW_LOADING_END, 0.6f, 1.5f);
    }

    private static void phaseSound(ServerPlayer p, GunSpec spec, int phase) {
        ServerLevel l = p.serverLevel();
        switch (phase) {
            case OUT -> {
                if (spec.reload == Reload.BOLT_CLIP) sound(l, p, dev.factoryascension.FASounds.BOLT.get(), 0.7f, 1.1f);
                else if (spec.reload == Reload.CYLINDER || spec.reload == Reload.BREAK) sound(l, p, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.6f);
                else sound(l, p, dev.factoryascension.FASounds.MAG_OUT.get(), 0.8f, 1.0f);
            }
            case IN -> {
                if (spec.reload == Reload.CYLINDER || spec.reload == Reload.BREAK) sound(l, p, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.7f);
                else sound(l, p, dev.factoryascension.FASounds.MAG_IN.get(), 0.8f, 1.0f);
            }
            case CHAMBER -> sound(l, p, spec.kind == Kind.SHOTGUN && spec.manual ? dev.factoryascension.FASounds.PUMP.get()
                : spec.reload == Reload.BOLT_CLIP || spec.manual ? dev.factoryascension.FASounds.BOLT.get() : dev.factoryascension.FASounds.RACK.get(), 0.8f, 1.0f);
            case ROUND -> sound(l, p, dev.factoryascension.FASounds.SHELL_IN.get(), 0.7f, 0.95f + p.getRandom().nextFloat() * 0.1f);
            default -> { }
        }
    }

    private static void fumbleSound(ServerPlayer p, Fumble f) {
        ServerLevel l = p.serverLevel();
        switch (f) {
            case DROPPED_MAG, MAG_SLIPPED, DROPPED_ROUND -> sound(l, p, SoundEvents.CHAIN_FALL, 0.9f, 1.4f);
            case MAG_STUCK, CYLINDER_STUCK, HINGE_STIFF, BOLT_WONT_OPEN, COVER_STUCK, BOLT_STICKY, SLIDE_STUCK -> sound(l, p, SoundEvents.CHAIN_HIT, 0.8f, 0.8f);
            case DOUBLE_FEED, STOVEPIPE, SHELL_JAMMED -> sound(l, p, SoundEvents.ANVIL_LAND, 0.25f, 1.9f);
            default -> sound(l, p, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 0.9f);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !stack.hasTag()) return;
        int cap = magOf(stack);
        if (ammo(stack) > cap) {   // a smaller magazine went on: the extra rounds come back to you
            giveBack(player, stack.getTag().getCompound("Load"), ammo(stack) - cap);
            stack.getTag().putInt("Ammo", cap);
        }
        if (!selected) {
            if (reloading(stack)) clearReload(stack.getTag());   // put away mid-reload: stops it
            return;
        }
        tickReload(player, stack);
        // A 20,000 lumen light: everything in front of it is blinded (mobs, operators, other players)
        if (player.tickCount % 10 == 0) {
            GunStats bs = GunStats.of(stack);
            if (bs != null && bs.blinding) {
                Vec3 eye = player.getEyePosition(), look = player.getViewVector(1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e != player && e.isAlive())) {
                    Vec3 to = e.getEyePosition().subtract(eye);
                    if (to.length() > 16 || to.normalize().dot(look) < 0.85 || !player.hasLineOfSight(e)) continue;
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    if (e instanceof dev.factoryascension.npc.Operator o) o.dazzle(30);
                }
            }
        }
        // A weapon light: you see in the dark while it's in your hand
        if (player.tickCount % 20 == 0) {
            GunStats st = GunStats.of(stack);
            if (st != null && st.light) {
                var nv = player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                if (nv == null || nv.getDuration() < 260)
                    player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 320, 0, true, false, true));
            }
        }
    }

    // ---- Shown in the bar and tooltip ----
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * ammo(stack) / Math.max(1, magOf(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ammo(stack) > 0 ? 0xE8B03A : 0x8C2A1E;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(spec.desc).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        GunStats st = GunStats.of(stack);
        tip.add(Component.literal(ammo(stack) + " / " + st.mag + " " + spec.caliber.label + (ammo(stack) > 0 ? " (" + loadedType(stack).label + ")" : "")).withStyle(ChatFormatting.GOLD));
        float dmg = Guns.BASE_DAMAGE * spec.caliber.damage * st.damage;
        String action = spec.kind == Kind.SHOTGUN ? "Pump action, " : spec.reload == Reload.TUBE ? "Lever action, " : "Bolt action, ";
        tip.add(Component.literal((st.auto ? (spec.spinup > 0 ? "Spins up, then full auto, " : "Full auto, ") : spec.manual && !st.noBolt ? action : "Semi-auto, ") + st.rpm + " rpm, "
            + (st.pellets > 1 ? st.pellets + " pellets x " + String.format("%.1f", dmg) : String.format("%.1f", dmg)) + " damage, " + Math.round(st.range) + " blocks").withStyle(ChatFormatting.GRAY));
        for (Attachments.Att a : st.atts) tip.add(Component.literal(" + " + a.name).withStyle(ChatFormatting.DARK_AQUA));
        if (net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(dev.factoryascension.FactoryAscension.JURY_RIGGED.get(), stack) > 0)
            tip.add(Component.literal("Jury-Rigged: takes any attachment, anywhere").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.literal("Hold right-click: aim. Left-click: fire. R: reload.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(dev.factoryascension.client.GunClient.EXTENSIONS);
    }
}
