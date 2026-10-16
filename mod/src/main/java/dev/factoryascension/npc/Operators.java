package dev.factoryascension.npc;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.Hearing;
import dev.factoryascension.gun.Rescue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The operators' entity types, spawn eggs and spawning: patrols out in the danger zones (the deeper, the more, the
 * worse), the squads in the structures (worldgen/FAStructures), hunters coming for a red flare, and everyone within
 * earshot of a gunshot coming to look.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Operators {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FactoryAscension.MODID);
    public static final Map<OpType, RegistryObject<EntityType<Operator>>> TYPES = new EnumMap<>(OpType.class);
    public static final Map<OpType, RegistryObject<Item>> EGGS = new EnumMap<>(OpType.class);
    public static final RegistryObject<EntityType<Grenade>> GRENADE = ENTITIES.register("frag_grenade",
        () -> EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(5).build(FactoryAscension.MODID + ":frag_grenade"));

    static {
        for (OpType t : OpType.values()) {
            RegistryObject<EntityType<Operator>> type = ENTITIES.register(t.id, () -> EntityType.Builder.<Operator>of((et, l) -> new Operator(et, l, t), MobCategory.MONSTER)
                .sized(0.6f, 1.8f).clientTrackingRange(10).build(FactoryAscension.MODID + ":" + t.id));
            TYPES.put(t, type);
            EGGS.put(t, FactoryAscension.ITEMS.register(t.id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, t.eggA, t.eggB, new Item.Properties())));
        }
    }

    private Operators() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(Operators::attributes);
        Hearing.listen(Operators::heard);
        Rescue.onFlare = c -> flare(c.level(), c.lz());
    }

    static void attributes(EntityAttributeCreationEvent event) {
        for (OpType t : OpType.values()) event.put(TYPES.get(t).get(), Operator.attributes(t).build());
    }

    /** Spawn one, kitted out, at a spot (null if it couldn't be made). */
    @Nullable
    public static Operator spawn(ServerLevelAccessor level, OpType t, BlockPos at, MobSpawnType why, boolean structure) {
        Operator o = TYPES.get(t).get().create(level.getLevel());
        if (o == null) return null;
        o.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        o.fromStructure = structure;
        o.home = at;
        if (structure) o.setPersistenceRequired();    // patrols may despawn once you're far away
        o.finalizeSpawn(level, level.getCurrentDifficultyAt(at), why, null, null);
        if (structure) o.restrictTo(at, 14);
        level.addFreshEntityWithPassengers(o);
        return o;
    }

    // ---- Hearing a shot: come and look ----
    static void heard(ServerLevel level, Entity shooter, double range) {
        double r2 = range * range;
        for (Operator o : level.getEntitiesOfClass(Operator.class, shooter.getBoundingBox().inflate(range), o -> o.isAlive() && o != shooter)) {
            if (o.getTarget() != null || o.distanceToSqr(shooter) > r2) continue;
            if (shooter instanceof Operator so && !OpType.hostile(o.type.faction, so.type.faction)) {
                // An ally shooting: go help
            } else if (shooter instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            double d = Math.sqrt(o.distanceToSqr(shooter));
            RandomSource r = o.getRandom();
            double err = d * 0.15;
            o.investigate = shooter.position().add((r.nextDouble() - 0.5) * err, 0, (r.nextDouble() - 0.5) * err);
            o.investigateUntil = level.getGameTime() + 600;
            if (d < 28 && shooter instanceof net.minecraft.world.entity.LivingEntity le && !(shooter instanceof Operator so2 && !OpType.hostile(o.type.faction, so2.type.faction))
                && o.hasLineOfSight(le)) o.setTarget(le);
        }
    }

    // ---- A flare: everyone near comes to see, and out in the zones, hunters are sent ----
    static void flare(ServerLevel level, Vec3 at) {
        for (Operator o : level.getEntitiesOfClass(Operator.class, new net.minecraft.world.phys.AABB(at, at).inflate(160), o -> o.isAlive() && o.getTarget() == null)) {
            o.investigate = at;
            o.investigateUntil = level.getGameTime() + 2400;
        }
        if (!FAConfig.npcHunters) return;
        ServerPlayer p = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer sp : level.players()) if (sp.position().distanceToSqr(at) < best) { best = sp.position().distanceToSqr(at); p = sp; }
        if (p == null || dev.factoryascension.gun.Stress.danger(p) < 1) return;
        RandomSource r = level.getRandom();
        int n = 2 + r.nextInt(2) + Math.min(3, dev.factoryascension.gun.Stress.danger(p) / 3);
        double a = r.nextDouble() * Math.PI * 2;
        double dist = 64 + r.nextDouble() * 30;
        squad(level, at.x + Math.cos(a) * dist, at.z + Math.sin(a) * dist, OpType.HUNTER, n, at);
    }

    /** A squad of a kind at x, z (on the surface), sent toward `goal` (or null). */
    public static int squad(ServerLevel level, double x, double z, OpType lead, int n, @Nullable Vec3 goal) {
        int made = 0;
        RandomSource r = level.getRandom();
        for (int i = 0; i < n; i++) {
            int bx = (int) Math.floor(x + r.nextInt(7) - 3), bz = (int) Math.floor(z + r.nextInt(7) - 3);
            if (!level.hasChunkAt(new BlockPos(bx, 0, bz))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            OpType t = i == 0 ? lead : squadmate(lead, r);
            Operator o = spawn(level, t, new BlockPos(bx, y, bz), MobSpawnType.EVENT, false);
            if (o == null) continue;
            if (goal != null) {
                o.investigate = goal;
                o.investigateUntil = level.getGameTime() + 3000;
            }
            made++;
        }
        return made;
    }

    static OpType squadmate(OpType lead, RandomSource r) {
        return switch (lead.faction) {
            case SCAV -> new OpType[] {OpType.SCAV, OpType.SCAV, OpType.SCAV_RIFLEMAN, OpType.LOOTER}[r.nextInt(4)];
            case RAIDER -> new OpType[] {OpType.RAIDER, OpType.RAIDER, OpType.RAIDER_MEDIC, OpType.BREACHER, OpType.GRENADIER, OpType.MACHINE_GUNNER}[r.nextInt(6)];
            case ROGUE -> lead == OpType.HUNTER ? OpType.HUNTER : new OpType[] {OpType.ROGUE, OpType.ROGUE, OpType.GHOST, OpType.ROGUE_SNIPER}[r.nextInt(4)];
            case CULT -> OpType.CULTIST;
            case BANDIT -> new OpType[] {OpType.BODYGUARD, OpType.SMUGGLER}[r.nextInt(2)];
        };
    }

    /** What roams at a danger level: scavs near home, raiders and rogues further out, cultists at night. */
    public static OpType patrolLeader(int danger, boolean night, RandomSource r) {
        if (night && r.nextFloat() < 0.25f) return OpType.CULTIST;
        if (danger <= 2) return r.nextFloat() < 0.75f ? OpType.SCAV : OpType.SCAV_RIFLEMAN;
        if (danger <= 5) return new OpType[] {OpType.SCAV_RIFLEMAN, OpType.LOOTER, OpType.DESERTER, OpType.RAIDER, OpType.SCAV_SNIPER}[r.nextInt(5)];
        if (danger <= 9) return new OpType[] {OpType.RAIDER, OpType.RAIDER, OpType.MARKSMAN, OpType.ROGUE, OpType.GHOST, OpType.SMUGGLER}[r.nextInt(6)];
        return new OpType[] {OpType.ROGUE, OpType.ROGUE, OpType.GHOST, OpType.ROGUE_SNIPER, OpType.JUGGERNAUT, OpType.HUNTER}[r.nextInt(6)];
    }

    // ---- Patrols: out in the zones, now and then, somewhere you can't see yet ----
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 200 != 37) return;
        if (!FAConfig.npcPatrols || p.isCreative() || p.isSpectator() || !(p.level() instanceof ServerLevel level)) return;
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        int danger = dev.factoryascension.gun.Stress.danger(p);
        if (danger < FAConfig.npcPatrolMinDanger) return;
        RandomSource r = p.getRandom();
        boolean night = level.isNight();
        float chance = Math.min(0.35f, FAConfig.npcPatrolChance * (1 + danger * 0.15f) * (night ? 1.5f : 1));
        if (r.nextFloat() >= chance) return;
        int near = level.getEntitiesOfClass(Operator.class, p.getBoundingBox().inflate(96)).size();
        if (near >= FAConfig.npcMaxNearby) return;
        // Somewhere 48-80 blocks away, out of your sight
        for (int tries = 0; tries < 8; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 48 + r.nextDouble() * 32;
            double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            if (!level.hasChunkAt(new BlockPos(bx, 0, bz))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            BlockPos at = new BlockPos(bx, y, bz);
            if (!level.getFluidState(at.below()).isEmpty()) continue;
            Vec3 eye = new Vec3(x, y + 1.6, z);
            if (level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), eye, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, p)).getType() == net.minecraft.world.phys.HitResult.Type.MISS && tries < 6) continue;
            OpType lead = patrolLeader(danger, night, r);
            int n = 1 + r.nextInt(2) + Math.min(3, danger / 3);
            Vec3 goal = r.nextFloat() < 0.35f ? p.position() : null;     // sometimes they're already heading your way
            squad(level, x, z, lead, n, goal);
            return;
        }
    }
}
