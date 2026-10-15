package dev.factoryascension.mob;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.mob.FAMobClasses.FABlaze;
import dev.factoryascension.mob.FAMobClasses.FAChicken;
import dev.factoryascension.mob.FAMobClasses.FACow;
import dev.factoryascension.mob.FAMobClasses.FACreeper;
import dev.factoryascension.mob.FAMobClasses.FAEnderman;
import dev.factoryascension.mob.FAMobClasses.FASilverfish;
import dev.factoryascension.mob.FAMobClasses.FASkeleton;
import dev.factoryascension.mob.FAMobClasses.FASlime;
import dev.factoryascension.mob.FAMobClasses.FASpider;
import dev.factoryascension.mob.FAMobClasses.FAVindicator;
import dev.factoryascension.mob.FAMobClasses.FAZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.BossEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The mod's 20 mobs. Each one is a vanilla mob (AI, model, sounds) with its own skin (tools/mob_art.py, which also
 * writes their loot tables and biome spawns) and a MobSpec. The pack's scripts scale them with the danger zones like
 * any hostile mob, decide how deep out they start appearing, and drop their body parts.
 */
public final class FAMobs {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FactoryAscension.MODID);
    public static final List<Entry> ALL = new ArrayList<>();

    /** One mob: its id, the vanilla mob it's built on, its spec, its type and spawn egg. */
    public record Entry(String id, String base, MobSpec spec, RegistryObject<? extends EntityType<? extends Mob>> type, RegistryObject<Item> egg, float scale) {
    }

    private FAMobs() {
    }

    private static <T extends Mob> void add(String id, String base, MobSpec spec, int c1, int c2, EntityType.Builder<T> builder) {
        add(id, base, spec, c1, c2, builder, 1f);
    }

    private static <T extends Mob> void add(String id, String base, MobSpec spec, int c1, int c2, EntityType.Builder<T> builder, float scale) {
        RegistryObject<EntityType<T>> type = ENTITIES.register(id, () -> builder.clientTrackingRange(scale > 1 ? 12 : 8).build(FactoryAscension.MODID + ":" + id));
        RegistryObject<Item> egg = FactoryAscension.ITEMS.register(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, c1, c2, new Item.Properties()));
        ALL.add(new Entry(id, base, spec, type, egg, scale));
    }

    private static void zombie(String id, MobSpec spec, int c1, int c2, boolean fire) {
        EntityType.Builder<FAZombie> b = EntityType.Builder.<FAZombie>of((t, l) -> new FAZombie(t, l, spec), MobCategory.MONSTER).sized(0.6f, 1.95f);
        if (fire) b.fireImmune();
        add(id, "zombie", spec, c1, c2, b);
    }

    private static void skeleton(String id, MobSpec spec, int c1, int c2) {
        add(id, "skeleton", spec, c1, c2, EntityType.Builder.<FASkeleton>of((t, l) -> new FASkeleton(t, l, spec), MobCategory.MONSTER).sized(0.6f, 1.99f));
    }

    private static void spider(String id, MobSpec spec, int c1, int c2, boolean fire) {
        EntityType.Builder<FASpider> b = EntityType.Builder.<FASpider>of((t, l) -> new FASpider(t, l, spec), MobCategory.MONSTER).sized(1.4f, 0.9f);
        if (fire) b.fireImmune();
        add(id, "spider", spec, c1, c2, b);
    }

    private static void creeper(String id, MobSpec spec, int c1, int c2) {
        add(id, "creeper", spec, c1, c2, EntityType.Builder.<FACreeper>of((t, l) -> new FACreeper(t, l, spec), MobCategory.MONSTER).sized(0.6f, 1.7f));
    }

    private static void slime(String id, MobSpec spec, int c1, int c2) {
        add(id, "slime", spec, c1, c2, EntityType.Builder.<FASlime>of((t, l) -> new FASlime(t, l, spec), MobCategory.MONSTER).sized(2.04f, 2.04f));
    }

    static {
        zombie("rust_walker", MobSpec.of().hit(MobEffects.WEAKNESS, 5, 0).stats(24, 4, 0.23).armor(2), 0x7A5230, 0xC86A28, false);
        zombie("ascendite_husk", MobSpec.of().hit(MobEffects.GLOWING, 8, 0).stats(30, 5, 0.25).armor(4).sunProof(), 0x2E3A44, 0x40E8D4, false);
        zombie("ember_ghoul", MobSpec.of().ignite(4).stats(26, 5, 0.24).sunProof(), 0x3A2018, 0xFF9628, true);
        zombie("frost_revenant", MobSpec.of().hit(MobEffects.MOVEMENT_SLOWDOWN, 4, 1).stats(26, 4, 0.22).sunProof(), 0x96BAD2, 0xEAF6FF, false);
        zombie("deep_miner", MobSpec.of().hit(MobEffects.DIG_SLOWDOWN, 6, 1).stats(28, 5, 0.22).miner().below(0), 0x464658, 0xE6BE3C, false);
        skeleton("void_archer", MobSpec.of().arrows(MobEffects.LEVITATION, 2, 0).stats(24, 3, 0.25), 0x342846, 0xBE64FF);
        skeleton("ascendite_skeleton", MobSpec.of().arrows(MobEffects.GLOWING, 6, 0).stats(26, 3, 0.25).armor(3), 0xC8D6D2, 0x40E8D4);
        spider("crystal_spider", MobSpec.of().hit(MobEffects.MOVEMENT_SLOWDOWN, 3, 0).stats(20, 3, 0.3).armor(4).below(40), 0x6E46A0, 0xFFBEFF, false);
        spider("spark_spider", MobSpec.of().lightning().hit(MobEffects.WEAKNESS, 3, 0).stats(18, 3, 0.32), 0x383840, 0xFFE650, false);
        spider("cinder_spider", MobSpec.of().ignite(3).stats(20, 3, 0.3), 0x3C221E, 0xFF781E, true);
        creeper("ascendite_creeper", MobSpec.of().explosion("shards").stats(24, 0, 0.25), 0x2E6868, 0x40E8D4);
        creeper("frost_creeper", MobSpec.of().explosion("frost").stats(22, 0, 0.25), 0xAAD2E6, 0x1E3C5A);
        slime("essence_slime", MobSpec.of().below(40), 0xAA60FF, 0xF0DCFF);
        slime("rust_slime", MobSpec.of().below(40), 0x5AAA82, 0xAA6432);
        MobSpec mite = MobSpec.of().stats(10, 2, 0.28).below(20);
        add("ore_mite", "silverfish", mite, 0x78787E, 0xE6BE3C, EntityType.Builder.<FASilverfish>of((t, l) -> new FASilverfish(t, l, mite), MobCategory.MONSTER).sized(0.4f, 0.3f));
        MobSpec wisp = MobSpec.of().stats(24, 6, 0.25).anyLight();
        add("teal_wisp", "blaze", wisp, 0x28A0A0, 0xC8FFFA, EntityType.Builder.<FABlaze>of((t, l) -> new FABlaze(t, l, wisp), MobCategory.MONSTER).sized(0.6f, 1.8f).fireImmune());
        MobSpec stalker = MobSpec.of().hit(MobEffects.BLINDNESS, 4, 0).stats(44, 8, 0.3);
        add("void_stalker", "enderman", stalker, 0x1A1224, 0x963CDC, EntityType.Builder.<FAEnderman>of((t, l) -> new FAEnderman(t, l, stalker), MobCategory.MONSTER).sized(0.6f, 2.9f));
        MobSpec raider = MobSpec.of().hit(MobEffects.WEAKNESS, 4, 0).stats(26, 5, 0.35).armor(2);
        add("factory_raider", "vindicator", raider, 0x8C8C88, 0x966E32, EntityType.Builder.<FAVindicator>of((t, l) -> new FAVindicator(t, l, raider), MobCategory.MONSTER).sized(0.6f, 1.95f));
        MobSpec hen = MobSpec.of().lays();
        add("ember_hen", "chicken", hen, 0xE67828, 0xFFDC5A, EntityType.Builder.<FAChicken>of((t, l) -> new FAChicken(t, l, hen), MobCategory.CREATURE).sized(0.4f, 0.7f).fireImmune());
        // ---- More mobs (1.6) ----
        zombie("bog_lurker", MobSpec.of().hit(MobEffects.POISON, 4, 0).stats(26, 4, 0.23), 0x3E4A2A, 0x7C9A3C, false);
        zombie("sand_wraith", MobSpec.of().hit(MobEffects.BLINDNESS, 3, 0).stats(24, 4, 0.26).sunProof(), 0xC8B078, 0x6E5A32, false);
        creeper("thunder_creeper", MobSpec.of().explosion("storm").stats(22, 0, 0.25), 0x383848, 0xF0E050);
        skeleton("brass_sentinel", MobSpec.of().arrows(MobEffects.MOVEMENT_SLOWDOWN, 3, 1).stats(30, 4, 0.24).armor(6), 0xB48C3C, 0x5A4628);
        skeleton("shadow_skeleton", MobSpec.of().arrows(MobEffects.DARKNESS, 4, 0).stats(24, 3, 0.27), 0x1C1C24, 0x8C50DC);
        spider("cave_weaver", MobSpec.of().webs().stats(18, 3, 0.3).below(30), 0x8C8C96, 0xE6E6F0, false);
        slime("moss_slime", MobSpec.of().below(64), 0x4E8C32, 0xA0D25A);
        MobSpec glowMite = MobSpec.of().hit(MobEffects.GLOWING, 6, 0).stats(10, 2, 0.3).below(40);
        add("glow_mite", "silverfish", glowMite, 0x6E6450, 0xFFDC5A, EntityType.Builder.<FASilverfish>of((t, l) -> new FASilverfish(t, l, glowMite), MobCategory.MONSTER).sized(0.4f, 0.3f));
        MobSpec stormWisp = MobSpec.of().lightning().stats(26, 6, 0.25).anyLight();
        add("storm_wisp", "blaze", stormWisp, 0xD2D2E6, 0xFFF064, EntityType.Builder.<FABlaze>of((t, l) -> new FABlaze(t, l, stormWisp), MobCategory.MONSTER).sized(0.6f, 1.8f).fireImmune());
        MobSpec treasure = MobSpec.of().hit(MobEffects.WEAKNESS, 3, 0).stats(30, 5, 0.36).armor(4);
        add("treasure_raider", "vindicator", treasure, 0xC8A032, 0x32A050, EntityType.Builder.<FAVindicator>of((t, l) -> new FAVindicator(t, l, treasure), MobCategory.MONSTER).sized(0.6f, 1.95f));

        // ---- Bosses: rare, huge, a boss bar, minions and a signature attack (BossBrain) ----
        MobSpec colossus = MobSpec.of().boss().hit(MobEffects.WEAKNESS, 6, 1).stats(400, 14, 0.24).armor(10);
        add("rust_colossus", "zombie", colossus, 0x5A3C28, 0xFF6A1E, EntityType.Builder.<FABosses.BossZombie>of((t, l) ->
            new FABosses.BossZombie(t, l, colossus, BossEvent.BossBarColor.RED, List.of("factoryascension:rust_walker", "factoryascension:deep_miner")), MobCategory.MONSTER)
            .sized(1.56f, 5.07f), 2.6f);
        MobSpec brood = MobSpec.of().boss().webs().hit(MobEffects.POISON, 5, 1).stats(320, 10, 0.32).armor(8);
        add("broodmother", "spider", brood, 0x46285A, 0xDC64FF, EntityType.Builder.<FABosses.BossSpider>of((t, l) ->
            new FABosses.BossSpider(t, l, brood, BossEvent.BossBarColor.PURPLE, List.of("factoryascension:crystal_spider", "factoryascension:cave_weaver")), MobCategory.MONSTER)
            .sized(3.08f, 1.98f), 2.2f);
        MobSpec king = MobSpec.of().boss().arrows(MobEffects.WITHER, 4, 1).stats(350, 8, 0.27).armor(12);
        add("hollow_king", "skeleton", king, 0xE6E6D2, 0x8C50DC, EntityType.Builder.<FABosses.BossSkeleton>of((t, l) ->
            new FABosses.BossSkeleton(t, l, king, BossEvent.BossBarColor.WHITE, List.of("factoryascension:shadow_skeleton", "factoryascension:ascendite_skeleton")), MobCategory.MONSTER)
            .sized(1.32f, 4.38f), 2.2f);
        MobSpec tyrant = MobSpec.of().boss().ignite(6).stats(360, 10, 0.25).armor(8).anyLight();
        add("wisp_tyrant", "blaze", tyrant, 0x147878, 0x40E8D4, EntityType.Builder.<FABosses.BossBlaze>of((t, l) ->
            new FABosses.BossBlaze(t, l, tyrant, BossEvent.BossBarColor.BLUE, List.of("factoryascension:teal_wisp", "factoryascension:storm_wisp")), MobCategory.MONSTER)
            .sized(1.32f, 3.96f).fireImmune(), 2.2f);

        // ---- The Ascended Realm (1.17): two mobs and two bosses, spawning only there (#factoryascension:is_realm) ----
        MobSpec astralWisp = MobSpec.of().stats(40, 8, 0.27).anyLight();
        add("astral_wisp", "blaze", astralWisp, 0xDCC878, 0xFFF0A0, EntityType.Builder.<FABlaze>of((t, l) -> new FABlaze(t, l, astralWisp), MobCategory.MONSTER).sized(0.6f, 1.8f).fireImmune());
        zombie("void_knight", MobSpec.of().hit(MobEffects.WITHER, 3, 0).stats(60, 10, 0.25).armor(10).sunProof(), 0x221830, 0xAA60FF, false);
        MobSpec sovereign = MobSpec.of().boss().arrows(MobEffects.LEVITATION, 3, 1).stats(700, 12, 0.27).armor(14).anyLight();
        add("astral_sovereign", "skeleton", sovereign, 0xF0E8C8, 0xFFE68C, EntityType.Builder.<FABosses.BossSkeleton>of((t, l) ->
            new FABosses.BossSkeleton(t, l, sovereign, BossEvent.BossBarColor.YELLOW, List.of("factoryascension:astral_wisp", "factoryascension:void_archer")), MobCategory.MONSTER)
            .sized(1.32f, 4.38f).fireImmune(), 2.2f);
        MobSpec behemoth = MobSpec.of().boss().hit(MobEffects.WITHER, 6, 1).stats(800, 18, 0.24).armor(14).anyLight();
        add("void_behemoth", "zombie", behemoth, 0x1C1228, 0xBE64FF, EntityType.Builder.<FABosses.BossZombie>of((t, l) ->
            new FABosses.BossZombie(t, l, behemoth, BossEvent.BossBarColor.PURPLE, List.of("factoryascension:void_knight", "factoryascension:void_stalker")), MobCategory.MONSTER)
            .sized(1.56f, 5.07f).fireImmune(), 2.6f);

        MobSpec cow = MobSpec.of();
        add("copper_cow", "cow", cow, 0xB4643C, 0x50AA82, EntityType.Builder.<FACow>of((t, l) -> new FACow(t, l, cow), MobCategory.CREATURE).sized(0.9f, 1.4f));
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(FAMobs::attributes);
        modBus.addListener(FAMobs::placements);
    }

    private static AttributeSupplier.Builder baseAttributes(String base) {
        return switch (base) {
            case "zombie" -> Zombie.createAttributes();
            case "skeleton" -> AbstractSkeleton.createAttributes();
            case "spider" -> Spider.createAttributes();
            case "creeper" -> Creeper.createAttributes();
            case "slime" -> Monster.createMonsterAttributes();
            case "silverfish" -> Silverfish.createAttributes();
            case "blaze" -> Blaze.createAttributes();
            case "enderman" -> EnderMan.createAttributes();
            case "vindicator" -> Vindicator.createAttributes();
            case "chicken" -> Chicken.createAttributes();
            default -> Cow.createAttributes();
        };
    }

    @SuppressWarnings("unchecked")
    private static void attributes(EntityAttributeCreationEvent event) {
        for (Entry e : ALL) {
            AttributeSupplier.Builder b = baseAttributes(e.base());
            MobSpec s = e.spec();
            if (s.health > 0) b.add(Attributes.MAX_HEALTH, s.health);
            if (s.damage > 0) b.add(Attributes.ATTACK_DAMAGE, s.damage);
            if (s.speed > 0) b.add(Attributes.MOVEMENT_SPEED, s.speed);
            if (s.armor > 0) b.add(Attributes.ARMOR, s.armor);
            if (s.boss) b.add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 48);
            event.put((EntityType<? extends LivingEntity>) e.type().get(), b.build());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void placements(SpawnPlacementRegisterEvent event) {
        for (Entry e : ALL) {
            MobSpec s = e.spec();
            boolean animal = e.base().equals("chicken") || e.base().equals("cow");
            SpawnPlacements.SpawnPredicate rule = animal
                ? (type, level, reason, pos, random) -> Animal.checkAnimalSpawnRules((EntityType) type, level, reason, pos, random)
                : (type, level, reason, pos, random) -> pos.getY() < s.maxY && level.getDifficulty() != Difficulty.PEACEFUL
                    && (s.anyLight || Monster.isDarkEnoughToSpawn(level, pos, random)) && Mob.checkMobSpawnRules((EntityType) type, level, reason, pos, random);
            event.register((EntityType) e.type().get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rule,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    /** Ascendite and Frost Creepers break no blocks: one scatters Ascension Shards, the other snow and slowness. */
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getExploder();
        if (!(source instanceof FACreeper creeper) || creeper.explosionKind().isEmpty() || !(event.getLevel() instanceof ServerLevel level)) return;
        event.getAffectedBlocks().clear();
        BlockPos at = creeper.blockPosition();
        if (creeper.explosionKind().equals("shards")) {
            Item shard = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", "ascension_shard"));
            int n = 1 + level.random.nextInt(3);
            for (int i = 0; shard != null && i < n; i++)
                level.addFreshEntity(new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, new ItemStack(shard)));
        } else if (creeper.explosionKind().equals("storm")) {
            // Lightning (for show) all around, and a jolt for everything nearby
            for (int i = 0; i < 4; i++) {
                net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt == null) continue;
                bolt.moveTo(at.getX() + level.random.nextInt(9) - 4, at.getY(), at.getZ() + level.random.nextInt(9) - 4);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(5)))
                if (e != creeper) e.hurt(level.damageSources().lightningBolt(), 8f);
        } else if (creeper.explosionKind().equals("frost")) {
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-3, -2, -3), at.offset(3, 2, 3))) {
                if (p.distSqr(at) <= 10 && level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP))
                    level.setBlockAndUpdate(p, Blocks.SNOW.defaultBlockState());
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(5)))
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
        }
    }
}
