package dev.factoryascension.worldgen;

import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The pack's structures, built block by block as world generation features (placed by the data in
 * data/factoryascension/worldgen and forge/biome_modifier, written by tools/structure_gen.py):
 *  - Factory Ruin (Overworld surface): a collapsed Create workshop with a chest of parts.
 *  - Ascension Crypt (Overworld, underground): a crypt with an Ascendite mob spawner and two chests.
 *  - Realm Vault (Ascended Realm, inside the islands): a gold-trimmed vault with a Void Stalker spawner and the best loot.
 *  - Sky Shrine (Ascended Realm, on the islands): crystal pillars around an offering chest.
 * Each chest gets its loot table (data/factoryascension/loot_tables/chests), which the scripts add packs and caches to.
 */
public final class FAStructures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, FactoryAscension.MODID);

    public enum Kind { FACTORY_RUIN, ASCENSION_CRYPT, REALM_VAULT, SKY_SHRINE }

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FACTORY_RUIN = FEATURES.register("factory_ruin", () -> new Structure(Kind.FACTORY_RUIN));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ASCENSION_CRYPT = FEATURES.register("ascension_crypt", () -> new Structure(Kind.ASCENSION_CRYPT));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> REALM_VAULT = FEATURES.register("realm_vault", () -> new Structure(Kind.REALM_VAULT));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SKY_SHRINE = FEATURES.register("sky_shrine", () -> new Structure(Kind.SKY_SHRINE));

    private FAStructures() {}

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    /** A block by id, or the fallback if that mod isn't there. */
    static BlockState block(String id, Block fallback) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return (b == null || b == Blocks.AIR ? fallback : b).defaultBlockState();
    }

    static final class Structure extends Feature<NoneFeatureConfiguration> {
        private final Kind kind;

        Structure(Kind kind) {
            super(NoneFeatureConfiguration.CODEC);
            this.kind = kind;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            RandomSource r = ctx.random();
            return switch (kind) {
                case FACTORY_RUIN -> ruin(level, o, r);
                case ASCENSION_CRYPT -> crypt(level, o, r);
                case REALM_VAULT -> vault(level, o, r);
                case SKY_SHRINE -> shrine(level, o, r);
            };
        }

        private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
            if (!level.getBlockState(pos).is(Blocks.BEDROCK)) level.setBlock(pos, state, 2);
        }

        private static void chest(WorldGenLevel level, BlockPos pos, RandomSource r, String table) {
            set(level, pos, Blocks.CHEST.defaultBlockState());
            RandomizableContainerBlockEntity.setLootTable(level, r, pos, new ResourceLocation(FactoryAscension.MODID, "chests/" + table));
        }

        private static void spawner(WorldGenLevel level, BlockPos pos, RandomSource r, String mob) {
            set(level, pos, Blocks.SPAWNER.defaultBlockState());
            BlockEntity be = level.getBlockEntity(pos);
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mob));
            if (be instanceof SpawnerBlockEntity s && type != null) s.setEntityId(type, r);
        }

        /** Ground under a structure: fill down until it meets something solid, so it never floats. */
        private static void foot(WorldGenLevel level, BlockPos pos, BlockState state) {
            for (int i = 0; i < 6; i++) {
                BlockPos p = pos.below(i);
                if (level.getBlockState(p).canOcclude()) return;
                set(level, p, state);
            }
        }

        // ---- Factory Ruin: 11 x 11, walls broken to random heights, a chest and a few machine parts ----
        private boolean ruin(WorldGenLevel level, BlockPos o, RandomSource r) {
            if (!level.getBlockState(o.below()).canOcclude() || level.getBlockState(o).liquid()) return false;
            BlockState andesite = block("create:andesite_casing", Blocks.STONE_BRICKS);
            BlockState brass = block("create:brass_casing", Blocks.POLISHED_GRANITE);
            BlockState glass = block("factoryascension:factory_glass", Blocks.GLASS);
            BlockState floor2 = Blocks.COBBLESTONE.defaultBlockState();
            int h = 5;
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                BlockPos f = o.offset(dx, -1, dz);
                set(level, f, r.nextInt(4) == 0 ? floor2 : andesite);
                foot(level, f.below(), floor2);
                boolean edge = Math.abs(dx) == 5 || Math.abs(dz) == 5;
                boolean corner = Math.abs(dx) == 5 && Math.abs(dz) == 5;
                int wall = corner ? h : edge ? r.nextInt(h + 1) : 0;
                for (int y = 0; y <= h + 1; y++) {
                    BlockPos p = o.offset(dx, y, dz);
                    if (y < wall) set(level, p, corner ? brass : (y == 2 && r.nextInt(3) == 0 ? glass : andesite));
                    else set(level, p, Blocks.AIR.defaultBlockState());
                }
            }
            // A doorway on a random side, the chest, and some leftovers
            chest(level, o.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2), r, "factory_ruin");
            set(level, o.offset(-3, 0, 3), Blocks.BARREL.defaultBlockState());
            set(level, o.offset(3, 0, -3), block("create:andesite_casing", Blocks.STONE_BRICKS));
            set(level, o.offset(3, 1, -3), Blocks.LANTERN.defaultBlockState());
            set(level, o.offset(-3, 0, -3), Blocks.CRAFTING_TABLE.defaultBlockState());
            if (r.nextBoolean()) set(level, o.offset(2, 0, 3), Blocks.COBWEB.defaultBlockState());
            return true;
        }

        // ---- Ascension Crypt: a 9 x 5 x 9 room underground with a spawner and two chests ----
        private boolean crypt(WorldGenLevel level, BlockPos o, RandomSource r) {
            BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            BlockState cracked = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            BlockState trim = block("kubejs:ascendite_block", Blocks.WARPED_WART_BLOCK);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int y = -1; y <= 4; y++) {
                BlockPos p = o.offset(dx, y, dz);
                boolean shell = Math.abs(dx) == 4 || Math.abs(dz) == 4 || y == -1 || y == 4;
                if (shell) {
                    boolean pillar = Math.abs(dx) == 4 && Math.abs(dz) == 4;
                    set(level, p, pillar ? trim : r.nextInt(5) == 0 ? cracked : wall);
                } else set(level, p, Blocks.AIR.defaultBlockState());
            }
            for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) set(level, o.offset(c[0], 3, c[1]), Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
            spawner(level, o, r, r.nextBoolean() ? "factoryascension:ascendite_skeleton" : "factoryascension:ascendite_husk");
            chest(level, o.offset(-3, 0, 0), r, "ascension_crypt");
            chest(level, o.offset(3, 0, 0), r, "ascension_crypt");
            return true;
        }

        // ---- Realm Vault: 9 x 6 x 9 inside an island, gold trim, crystal lights, a Void Stalker spawner, two chests ----
        private boolean vault(WorldGenLevel level, BlockPos o, RandomSource r) {
            BlockState stone = FactoryAscension.REALM_STONE.get().defaultBlockState();
            if (!level.getBlockState(o).is(stone.getBlock())) return false;   // only inside the islands
            BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
            BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int y = -1; y <= 5; y++) {
                BlockPos p = o.offset(dx, y, dz);
                boolean wallX = Math.abs(dx) == 4, wallZ = Math.abs(dz) == 4;
                if (y == -1) set(level, p, (dx + dz) % 2 == 0 ? floor : stone);
                else if (y == 5) set(level, p, wallX || wallZ ? gold : stone);
                else if (wallX && wallZ) set(level, p, gold);
                else if (wallX || wallZ) set(level, p, stone);
                else set(level, p, Blocks.AIR.defaultBlockState());
            }
            BlockState crystal = FactoryAscension.REALM_CRYSTAL.get().defaultBlockState();
            for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) set(level, o.offset(c[0], 0, c[1]), crystal);
            spawner(level, o, r, r.nextInt(3) == 0 ? "factoryascension:void_archer" : "factoryascension:void_stalker");
            chest(level, o.offset(0, 0, -3), r, "realm_vault");
            chest(level, o.offset(0, 0, 3), r, "realm_vault");
            return true;
        }

        // ---- Sky Shrine: a 7 x 7 quartz platform on an island, four crystal-topped pillars, an offering chest ----
        private boolean shrine(WorldGenLevel level, BlockPos o, RandomSource r) {
            if (!level.getBlockState(o.below()).is(FactoryAscension.REALM_MOSS.get())) return false;
            BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
            BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
            BlockState stone = FactoryAscension.REALM_STONE.get().defaultBlockState();
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                set(level, o.offset(dx, -1, dz), Math.abs(dx) == 3 || Math.abs(dz) == 3 ? Blocks.GOLD_BLOCK.defaultBlockState() : quartz);
                foot(level, o.offset(dx, -2, dz), stone);
                for (int y = 0; y <= 5; y++) set(level, o.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
            }
            for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                for (int y = 0; y < 3; y++) set(level, o.offset(c[0], y, c[1]), pillar);
                set(level, o.offset(c[0], 3, c[1]), FactoryAscension.REALM_CRYSTAL.get().defaultBlockState());
            }
            chest(level, o, r, "sky_shrine");
            return true;
        }
    }
}
