package dev.factoryascension.worldgen;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.npc.OpType;
import dev.factoryascension.npc.Operators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where the armed operators live: twelve kinds of place, every one built fresh from its own random roll (size, layout,
 * materials, how wrecked it is, where the loot is, who guards it), so no two are the same. The further from world
 * spawn, the bigger and better-held they are: scav camps and ruins near home; checkpoints, trenches and sniper towers
 * further out; military outposts, bunkers, crash sites and radio towers beyond; and far out, warlords' forts and
 * cult shrines. Each fits within the 3 x 3 chunks around the one being generated (worldgen can write there).
 */
public final class Outposts {
    public enum Kind {
        SCAV_CAMP("scav_camp", 0), RUINS("ruins", 0), GAS_STATION("gas_station", 0), CHECKPOINT("checkpoint", 600), TRENCH("trench", 700),
        SNIPER_TOWER("sniper_tower", 600), OUTPOST("military_outpost", 1100), BUNKER("bunker", 1000), CRASH_SITE("crash_site", 1000),
        RADIO_TOWER("radio_tower", 900), FORT("warlord_fort", 1600), CULT_SHRINE("cult_shrine", 1400);

        public final String id;
        public final int minDistance;

        Kind(String id, int minDistance) {
            this.id = id;
            this.minDistance = minDistance;
        }
    }

    public static final Map<Kind, RegistryObject<Feature<NoneFeatureConfiguration>>> FEATURES = new EnumMap<>(Kind.class);

    static {
        for (Kind k : Kind.values()) FEATURES.put(k, FAStructures.FEATURES.register(k.id, () -> new Place(k)));
    }

    private Outposts() {
    }

    /** Loaded with FAStructures (registers the features). */
    static void init() {
    }

    static final class Place extends Feature<NoneFeatureConfiguration> {
        final Kind kind;

        Place(Kind kind) {
            super(NoneFeatureConfiguration.CODEC);
            this.kind = kind;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int cx = (o.getX() & ~15) + 8, cz = (o.getZ() & ~15) + 8;
            var spawn = level.getLevel().getSharedSpawnPos();
            double dist = Math.sqrt(Math.pow(cx - spawn.getX(), 2) + Math.pow(cz - spawn.getZ(), 2));
            // In world generation only so far out; placed by command (/place feature) anywhere
            if (level instanceof net.minecraft.server.level.WorldGenRegion && dist < kind.minDistance) return false;
            try {
                Site s = new Site(level, r, cx, cz, dist);
                return switch (kind) {
                    case SCAV_CAMP -> s.scavCamp();
                    case RUINS -> s.ruins();
                    case GAS_STATION -> s.gasStation();
                    case CHECKPOINT -> s.checkpoint();
                    case TRENCH -> s.trench();
                    case SNIPER_TOWER -> s.sniperTower();
                    case OUTPOST -> s.outpost();
                    case BUNKER -> s.bunker();
                    case CRASH_SITE -> s.crashSite();
                    case RADIO_TOWER -> s.radioTower();
                    case FORT -> s.fort();
                    case CULT_SHRINE -> s.cultShrine();
                };
            } catch (Throwable err) {
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Structure {} skipped: {}", kind.id, err.toString());
                return false;
            }
        }
    }

    /**
     * One site being built: a centre on the ground, a random facing (everything is laid out in local x / z and turned),
     * the material palette rolled for it, and the people to put in it once it's built.
     */
    static final class Site {
        final WorldGenLevel level;
        final RandomSource r;
        final int cx, cz;
        int gy;
        final double dist;
        final int rot;
        final List<Object[]> people = new ArrayList<>();

        Site(WorldGenLevel level, RandomSource r, int cx, int cz, double dist) {
            this.level = level;
            this.r = r;
            this.cx = cx;
            this.cz = cz;
            this.dist = dist;
            this.rot = r.nextInt(4);
        }

        int tier() {
            return dist < 900 ? 0 : dist < 1600 ? 1 : dist < 2500 ? 2 : 3;
        }

        // ---- Coordinates: local (x right, z forward) turned by rot, around the centre ----
        BlockPos at(int x, int y, int z) {
            int wx, wz;
            switch (rot) {
                case 1 -> { wx = -z; wz = x; }
                case 2 -> { wx = -x; wz = -z; }
                case 3 -> { wx = z; wz = -x; }
                default -> { wx = x; wz = z; }
            }
            return new BlockPos(cx + wx, gy + y, cz + wz);
        }

        Direction face(Direction local) {
            Direction d = local;
            for (int i = 0; i < rot; i++) d = d.getClockWise();
            return d;
        }

        int surface(int wx, int wz) {
            return level.getHeight(level instanceof net.minecraft.server.level.WorldGenRegion ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE, wx, wz);
        }

        /** Ground under a w x d footprint: the middle height, if it's dry land and not a cliff. */
        boolean ground(int halfW, int halfD, int maxStep) {
            int[] ys = new int[9];
            int i = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = at(dx * halfW, 0, dz * halfD);
                int y = surface(p.getX(), p.getZ());
                BlockState g = level.getBlockState(new BlockPos(p.getX(), y - 1, p.getZ()));
                if (!g.getFluidState().isEmpty() || g.is(BlockTags.LEAVES) && dx == 0 && dz == 0) return false;
                ys[i++] = y;
            }
            java.util.Arrays.sort(ys);
            if (ys[8] - ys[0] > maxStep) return false;
            gy = ys[4];
            if (gy <= level.getMinBuildHeight() + 2 || gy >= level.getMaxBuildHeight() - 40) return false;
            return true;
        }

        // ---- Blocks ----
        void set(BlockPos p, BlockState s) {
            if (!level.getBlockState(p).is(Blocks.BEDROCK)) level.setBlock(p, s, 2);
        }

        void set(int x, int y, int z, BlockState s) {
            set(at(x, y, z), s);
        }

        void set(int x, int y, int z, Block b) {
            set(at(x, y, z), b.defaultBlockState());
        }

        void air(int x, int y, int z) {
            set(at(x, y, z), Blocks.AIR.defaultBlockState());
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
            fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
        }

        /** Level a footprint: solid down to the ground, clear above to `clear`. */
        void pad(int x0, int z0, int x1, int z1, BlockState top, int clear) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    BlockPos p = at(x, -1, z);
                    set(p, top);
                    for (int k = 1; k < 10; k++) {
                        BlockPos q = p.below(k);
                        BlockState s = level.getBlockState(q);
                        if (s.canOcclude() && s.getFluidState().isEmpty()) break;
                        set(q, k < 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
                    }
                    for (int y = 0; y <= clear; y++) {
                        BlockState s = level.getBlockState(at(x, y, z));
                        if (!s.isAir()) air(x, y, z);
                    }
                }
        }

        /** Natural ground (grass, dirt, sand...) as the top of a pad: whatever is there. */
        BlockState groundTop() {
            BlockState s = level.getBlockState(at(0, -1, 0));
            return s.canOcclude() ? s : Blocks.GRASS_BLOCK.defaultBlockState();
        }

        /** Walls of a box (and a floor / roof), with a chance per block of being broken (decay). */
        void room(int x0, int z0, int x1, int z1, int h, BlockState wall, BlockState floor, BlockState roof, float decay) {
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                if (floor != null) set(x, -1, z, floor);
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 0; y < h; y++) {
                    if (edge) { if (r.nextFloat() >= decay * (0.3f + y * 0.25f)) set(x, y, z, wall); else air(x, y, z); }
                    else air(x, y, z);
                }
                if (roof != null && r.nextFloat() >= decay * 1.5f) set(x, h, z, roof);
            }
        }

        void chest(int x, int y, int z, String table, Direction facing) {
            BlockPos p = at(x, y, z);
            set(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face(facing)));
            RandomizableContainerBlockEntity.setLootTable(level, r, p, new ResourceLocation(FactoryAscension.MODID, "chests/" + table));
        }

        void barrel(int x, int y, int z, String table) {
            BlockPos p = at(x, y, z);
            set(p, Blocks.BARREL.defaultBlockState());
            RandomizableContainerBlockEntity.setLootTable(level, r, p, new ResourceLocation(FactoryAscension.MODID, "chests/" + table));
        }

        void lantern(int x, int y, int z, boolean hanging, boolean soul) {
            set(x, y, z, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, hanging));
        }

        void campfire(int x, int y, int z, boolean soul) {
            set(x, y, z, (soul ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState().setValue(CampfireBlock.LIT, true));
        }

        /** Sandbags: a low wall of slabs, a second layer on some. */
        void sandbags(int x0, int z0, int x1, int z1) {
            BlockState low = Blocks.SANDSTONE_SLAB.defaultBlockState(), full = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, 0, z, r.nextFloat() < 0.75f ? full : low);
                    if (r.nextFloat() < 0.45f) set(x, 1, z, low);
                }
        }

        /** Someone to put here once it's built. */
        void person(OpType t, int x, int y, int z) {
            people.add(new Object[] {t, at(x, y, z)});
        }

        boolean finish() {
            for (Object[] p : people) Operators.spawn(level, (OpType) p[0], (BlockPos) p[1], MobSpawnType.STRUCTURE, true);
            return true;
        }

        // Who holds a place: weaker near home, harder out deep
        OpType grunt() {
            return switch (tier()) {
                case 0 -> r.nextFloat() < 0.6f ? OpType.SCAV : OpType.SCAV_RIFLEMAN;
                case 1 -> new OpType[] {OpType.SCAV_RIFLEMAN, OpType.DESERTER, OpType.RAIDER, OpType.LOOTER}[r.nextInt(4)];
                case 2 -> new OpType[] {OpType.RAIDER, OpType.RAIDER, OpType.BREACHER, OpType.GRENADIER, OpType.RAIDER_MEDIC}[r.nextInt(5)];
                default -> new OpType[] {OpType.ROGUE, OpType.ROGUE, OpType.GHOST, OpType.RAIDER, OpType.BREACHER}[r.nextInt(5)];
            };
        }

        OpType shooter() {
            return switch (tier()) {
                case 0 -> OpType.SCAV_SNIPER;
                case 1 -> r.nextBoolean() ? OpType.SCAV_SNIPER : OpType.MARKSMAN;
                case 2 -> OpType.MARKSMAN;
                default -> OpType.ROGUE_SNIPER;
            };
        }

        String loot(String base) {
            return base + "_" + Math.min(2, tier());
        }

        // =========================================================================================================
        // Scav camp: tents round a fire, crates, a sandbag ring, three to five scavs
        // =========================================================================================================
        boolean scavCamp() {
            int rad = 6 + r.nextInt(4);
            if (!ground(rad, rad, 3)) return false;
            pad(-rad, -rad, rad, rad, groundTop(), 4);
            campfire(0, 0, 0, false);
            for (int[] c : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) if (r.nextBoolean()) set(c[0], 0, c[1], Blocks.SPRUCE_SLAB);
            int tents = 2 + r.nextInt(3);
            BlockState[] cloth = {Blocks.GREEN_WOOL.defaultBlockState(), Blocks.BROWN_WOOL.defaultBlockState(), Blocks.GRAY_WOOL.defaultBlockState(), Blocks.LIME_WOOL.defaultBlockState()};
            for (int t = 0; t < tents; t++) {
                double a = Math.PI * 2 * t / tents + r.nextDouble() * 0.5;
                int tx = (int) Math.round(Math.cos(a) * (rad - 3)), tz = (int) Math.round(Math.sin(a) * (rad - 3));
                BlockState c = cloth[r.nextInt(cloth.length)];
                // An A-frame: two sloped sides of stairs over a 3 x 4 floor
                for (int dz = -1; dz <= 2; dz++) {
                    set(tx - 1, 0, tz + dz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face(Direction.EAST)));
                    set(tx + 1, 0, tz + dz, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face(Direction.WEST)));
                    set(tx, 1, tz + dz, c);
                    set(tx, 0, tz + dz, Blocks.AIR.defaultBlockState());
                }
                set(tx, 0, tz + 2, Blocks.BROWN_CARPET);
                if (r.nextBoolean()) barrel(tx, 0, tz - 1, loot("scav"));
            }
            chest(r.nextInt(3) - 1, 0, rad - 2, loot("scav"), Direction.NORTH);
            for (int i = 0; i < 3; i++) set(r.nextInt(rad * 2 - 2) - rad + 1, 0, r.nextInt(rad * 2 - 2) - rad + 1, r.nextBoolean() ? Blocks.BARREL : Blocks.COMPOSTER);
            // A broken sandbag ring
            for (int i = 0; i < 40; i++) {
                double a = Math.PI * 2 * i / 40;
                if (r.nextFloat() < 0.3f) continue;
                int x = (int) Math.round(Math.cos(a) * rad), z = (int) Math.round(Math.sin(a) * rad);
                sandbags(x, z, x, z);
            }
            int n = 3 + r.nextInt(3);
            for (int i = 0; i < n; i++) person(i == 0 && tier() > 0 ? OpType.SCAV_RIFLEMAN : r.nextInt(4) == 0 ? OpType.LOOTER : OpType.SCAV, r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
            return finish();
        }

        // =========================================================================================================
        // Ruins: two to four broken houses, overgrown
        // =========================================================================================================
        boolean ruins() {
            if (!ground(11, 11, 4)) return false;
            BlockState[][] palettes = {
                {Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState()},
                {Blocks.BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState()},
                {Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState()},
                {Blocks.WHITE_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(), Blocks.BIRCH_PLANKS.defaultBlockState()}};
            BlockState[] pal = palettes[r.nextInt(palettes.length)];
            int houses = 2 + r.nextInt(3);
            for (int h = 0; h < houses; h++) {
                int w = 3 + r.nextInt(3), d = 3 + r.nextInt(3);
                int hx = (h % 2 == 0 ? -1 : 1) * (2 + r.nextInt(5)) - w / 2, hz = (h < 2 ? -1 : 1) * (2 + r.nextInt(5)) - d / 2;
                pad(hx - 1, hz - 1, hx + w + 1, hz + d + 1, groundTop(), 6);
                int height = 3 + r.nextInt(2);
                for (int x = hx; x <= hx + w; x++) for (int z = hz; z <= hz + d; z++) {
                    set(x, -1, z, pal[2]);
                    boolean edge = x == hx || x == hx + w || z == hz || z == hz + d;
                    if (!edge) continue;
                    int top = r.nextFloat() < 0.35f ? r.nextInt(height) : height;
                    for (int y = 0; y < top; y++) set(x, y, z, r.nextFloat() < 0.35f ? pal[1] : pal[0]);
                    if (top >= 2 && r.nextFloat() < 0.15f) set(x, 1, z, Blocks.GLASS_PANE);
                }
                set(hx + w / 2, 0, hz, Blocks.AIR.defaultBlockState());
                set(hx + w / 2, 1, hz, Blocks.AIR.defaultBlockState());
                if (r.nextBoolean()) barrel(hx + 1, 0, hz + d - 1, loot("scav"));
                if (r.nextFloat() < 0.4f) chest(hx + w - 1, 0, hz + d - 1, loot("scav"), Direction.WEST);
                for (int k = 0; k < 4; k++) {
                    int vx = hx + r.nextInt(w + 1), vz = hz + r.nextInt(d + 1);
                    if (level.getBlockState(at(vx, 0, vz)).isAir()) set(vx, 0, vz, r.nextBoolean() ? Blocks.TALL_GRASS : Blocks.FERN);
                }
                if (r.nextFloat() < 0.5f) person(r.nextInt(3) == 0 ? OpType.DESERTER : grunt(), hx + w / 2, 0, hz + d / 2);
            }
            person(grunt(), 0, 0, 0);
            if (r.nextBoolean()) person(OpType.SCAV, 2, 0, 1);
            return finish();
        }

        // =========================================================================================================
        // Gas station: forecourt, pumps, a canopy, the shop
        // =========================================================================================================
        boolean gasStation() {
            if (!ground(10, 9, 3)) return false;
            pad(-10, -9, 10, 9, Blocks.GRAY_CONCRETE_POWDER.defaultBlockState(), 7);
            fill(-10, -1, -9, 10, -1, 1, Blocks.BLACK_CONCRETE);
            for (int x = -10; x <= 10; x += 2) if (r.nextFloat() < 0.6f) set(x, -1, -4, Blocks.WHITE_CONCRETE);
            // Pumps under a canopy
            for (int px : new int[] {-4, 4}) {
                set(px, 0, -4, Blocks.RED_CONCRETE);
                set(px, 1, -4, Blocks.RED_CONCRETE);
                set(px, 2, -4, Blocks.IRON_BARS);
                for (int y = 0; y < 5; y++) set(px, y, -6, Blocks.WHITE_CONCRETE);
            }
            for (int x = -7; x <= 7; x++) for (int z = -7; z <= -1; z++) if (r.nextFloat() > 0.08f) set(x, 5, z, x % 3 == 0 ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
            // The shop
            room(-6, 2, 6, 8, 4, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 0.06f);
            for (int x = -5; x <= 5; x++) if (x % 2 != 0) set(x, 1, 2, r.nextFloat() < 0.4f ? Blocks.AIR.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
            air(0, 0, 2);
            air(0, 1, 2);
            for (int x = -4; x <= 4; x += 2) { barrel(x, 0, 7, loot("scav")); set(x, 1, 7, Blocks.BOOKSHELF); }
            chest(5, 0, 4, loot("scav"), Direction.WEST);
            set(-5, 0, 4, Blocks.SMOKER);
            lantern(0, 3, 5, true, false);
            // A burnt-out car on the forecourt
            int carX = r.nextInt(7) - 3;
            fill(carX - 1, 0, -2, carX + 1, 0, 2, Blocks.BLACK_CONCRETE);
            fill(carX - 1, 1, -1, carX + 1, 1, 1, Blocks.GRAY_STAINED_GLASS);
            set(carX, 1, 0, Blocks.COAL_BLOCK);
            for (int i = 0; i < 3 + r.nextInt(2); i++) person(r.nextInt(3) == 0 ? OpType.LOOTER : grunt(), r.nextInt(9) - 4, 0, r.nextInt(3) + 4);
            return finish();
        }

        // =========================================================================================================
        // Checkpoint: a road with a barrier, a booth, sandbags, a small tower
        // =========================================================================================================
        boolean checkpoint() {
            if (!ground(9, 9, 3)) return false;
            pad(-9, -9, 9, 9, groundTop(), 8);
            fill(-2, -1, -9, 2, -1, 9, Blocks.GRAVEL);
            for (int z = -9; z <= 9; z += 3) set(0, -1, z, Blocks.COARSE_DIRT);
            // The barrier arm across the road
            set(-3, 0, 0, Blocks.STRIPPED_SPRUCE_LOG);
            for (int x = -2; x <= 2; x++) set(x, 1, 0, x % 2 == 0 ? Blocks.RED_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState());
            // The booth
            room(4, -2, 7, 1, 3, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_SLAB.defaultBlockState(), 0.02f);
            air(4, 0, -1);
            air(4, 1, -1);
            set(4, 1, 0, Blocks.GLASS_PANE);
            set(7, 1, -1, Blocks.GLASS_PANE);
            chest(6, 0, 0, loot("military"), Direction.WEST);
            lantern(5, 2, -1, true, false);
            // Sandbag nests on both sides
            sandbags(-7, -4, -4, -4);
            sandbags(-7, 4, -4, 4);
            sandbags(4, 5, 7, 5);
            // A small tower
            int tx = -6, tz = 6;
            for (int y = 0; y < 5; y++) for (int[] c : new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}}) set(tx + c[0], y, tz + c[1], Blocks.SPRUCE_FENCE);
            fill(tx, 5, tz, tx + 2, 5, tz + 2, Blocks.SPRUCE_PLANKS);
            for (int y = 0; y <= 5; y++) set(tx + 1, y, tz + 1, Blocks.SCAFFOLDING);
            for (int[] c : new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}}) set(tx + c[0], 6, tz + c[1], Blocks.SPRUCE_FENCE);
            person(r.nextBoolean() ? shooter() : grunt(), tx, 6, tz + 1);
            int n = 2 + r.nextInt(3);
            for (int i = 0; i < n; i++) person(grunt(), r.nextInt(5) - 2, 0, r.nextInt(7) - 3);
            return finish();
        }

        // =========================================================================================================
        // Trench line: a zig-zag trench dug in, sandbagged, with a dugout
        // =========================================================================================================
        boolean trench() {
            if (!ground(12, 8, 4)) return false;
            pad(-12, -8, 12, 8, groundTop(), 3);
            int z = 0;
            for (int x = -12; x <= 12; x++) {
                if (x % 4 == 0) z += r.nextBoolean() ? 1 : -1;
                z = Math.max(-4, Math.min(4, z));
                for (int dz = -1; dz <= 1; dz++) {
                    for (int y = -2; y <= 0; y++) air(x, y, z + dz);
                    set(x, -3, z + dz, Blocks.COARSE_DIRT);
                }
                for (int y = -2; y <= -1; y++) {
                    set(x, y, z - 2, x % 3 == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.MUD.defaultBlockState());
                    set(x, y, z + 2, Blocks.PACKED_MUD);
                }
                if (r.nextFloat() < 0.7f) set(x, 0, z + 2, r.nextBoolean() ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE_SLAB.defaultBlockState());
                if (x % 7 == 0) set(x, 0, z - 3, Blocks.COBWEB);   // wire in front
            }
            // The dugout
            room(-3, 4, 3, 8, 3, Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), 0.0f);
            fill(-3, 3, 4, 3, 3, 8, Blocks.DIRT);
            air(0, 0, 4);
            air(0, 1, 4);
            chest(-2, 0, 7, loot("military"), Direction.EAST);
            barrel(2, 0, 7, loot("military"));
            lantern(0, 2, 6, true, false);
            person(tier() >= 1 ? OpType.MACHINE_GUNNER : OpType.SCAV_RIFLEMAN, 0, -2, 0);
            int n = 2 + r.nextInt(3);
            for (int i = 0; i < n; i++) person(grunt(), -8 + i * 5, -2, 0);
            return finish();
        }

        // =========================================================================================================
        // Sniper tower: a tall log-and-scaffold tower with a covered platform, a sniper up top, guards below
        // =========================================================================================================
        boolean sniperTower() {
            if (!ground(4, 4, 3)) return false;
            pad(-4, -4, 4, 4, groundTop(), 4);
            int h = 12 + r.nextInt(9);
            Block post = r.nextBoolean() ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.STRIPPED_DARK_OAK_LOG;
            for (int y = 0; y < h; y++) {
                for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) set(c[0], y, c[1], post);
                if (y % 4 == 3) for (int i = -1; i <= 1; i++) {
                    set(i, y, -2, Blocks.SPRUCE_FENCE);
                    set(i, y, 2, Blocks.SPRUCE_FENCE);
                }
                set(0, y, 0, Blocks.SCAFFOLDING);
            }
            fill(-3, h, -3, 3, h, 3, Blocks.SPRUCE_PLANKS);
            set(0, h, 0, Blocks.SCAFFOLDING);
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (edge) set(x, h + 1, z, r.nextFloat() < 0.2f ? Blocks.AIR.defaultBlockState() : Blocks.SPRUCE_FENCE.defaultBlockState());
                if (Math.abs(x) == 3 && Math.abs(z) == 3) { set(x, h + 2, z, Blocks.SPRUCE_FENCE); set(x, h + 3, z, Blocks.SPRUCE_FENCE); }
                set(x, h + 4, z, Blocks.SPRUCE_SLAB);
            }
            chest(2, h + 1, 2, loot("military"), Direction.WEST);
            lantern(0, h + 3, 0, true, false);
            person(shooter(), 1, h + 1, -1);
            if (tier() >= 2) person(shooter(), -1, h + 1, 1);
            for (int i = 0; i < 1 + r.nextInt(2); i++) person(grunt(), r.nextInt(3) - 1 + 4, 0, r.nextInt(3) - 1);
            return finish();
        }

        // =========================================================================================================
        // Military outpost: a fenced compound, two watchtowers, barracks, containers, a squad
        // =========================================================================================================
        boolean outpost() {
            int half = 12 + r.nextInt(3);
            if (!ground(half, half, 4)) return false;
            pad(-half, -half, half, half, Blocks.GRAVEL.defaultBlockState(), 7);
            for (int x = -half + 2; x <= half - 2; x += 2) for (int z = -half + 2; z <= half - 2; z += 2) if (r.nextFloat() < 0.35f) set(x, -1, z, Blocks.COARSE_DIRT);
            // The fence, with a gate
            for (int i = -half; i <= half; i++) {
                for (int[] c : new int[][] {{i, -half}, {i, half}, {-half, i}, {half, i}}) {
                    if (Math.abs(c[0]) <= 2 && c[1] == -half) continue;     // the gate
                    set(c[0], 0, c[1], Blocks.IRON_BARS);
                    set(c[0], 1, c[1], Blocks.IRON_BARS);
                    if (r.nextFloat() < 0.5f) set(c[0], 2, c[1], Blocks.COBWEB);   // wire on top
                }
            }
            // Watchtowers at two corners
            for (int[] c : new int[][] {{-half + 1, -half + 1}, {half - 3, half - 3}}) {
                for (int y = 0; y < 7; y++) for (int[] k : new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}}) set(c[0] + k[0], y, c[1] + k[1], Blocks.SPRUCE_LOG);
                fill(c[0], 7, c[1], c[0] + 2, 7, c[1] + 2, Blocks.SPRUCE_PLANKS);
                for (int y = 0; y < 7; y++) set(c[0] + 1, y, c[1] + 1, Blocks.SCAFFOLDING);
                set(c[0] + 1, 7, c[1] + 1, Blocks.SCAFFOLDING);
                for (int[] k : new int[][] {{0, 0}, {2, 0}, {0, 2}, {2, 2}}) { set(c[0] + k[0], 8, c[1] + k[1], Blocks.SPRUCE_FENCE); set(c[0] + k[0], 9, c[1] + k[1], Blocks.SPRUCE_FENCE); }
                fill(c[0], 10, c[1], c[0] + 2, 10, c[1] + 2, Blocks.SPRUCE_SLAB);
                person(shooter(), c[0] + 1, 8, c[1]);
            }
            // Barracks
            int bx = -half + 3, bz = 1;
            room(bx, bz, bx + 9, bz + 6, 4, Blocks.GREEN_TERRACOTTA.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), 0.0f);
            for (int x = bx + 1; x <= bx + 8; x += 2) set(x, 2, bz, Blocks.GLASS_PANE);
            air(bx + 5, 0, bz);
            air(bx + 5, 1, bz);
            for (int x = bx + 1; x <= bx + 8; x += 3) { set(x, 0, bz + 5, Blocks.WHITE_WOOL); set(x, 0, bz + 4, Blocks.GREEN_CARPET); }   // cots
            chest(bx + 8, 0, bz + 1, loot("military"), Direction.WEST);
            lantern(bx + 4, 3, bz + 3, true, false);
            // Shipping containers
            BlockState[] cont = {Blocks.BLUE_CONCRETE.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.GREEN_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState()};
            for (int k = 0; k < 2 + r.nextInt(2); k++) {
                int kx = 2 + r.nextInt(half - 8), kz = -half + 3 + k * 4;
                BlockState c = cont[r.nextInt(cont.length)];
                for (int x = kx; x <= kx + 5; x++) for (int z = kz; z <= kz + 2; z++) for (int y = 0; y < 3; y++) {
                    boolean shell = x == kx || x == kx + 5 || z == kz || z == kz + 2 || y == 0 || y == 2;
                    if (shell) set(x, y, z, c); else air(x, y, z);
                }
                air(kx + 5, 1, kz + 1);
                set(kx + 5, 1, kz + 1, Blocks.IRON_TRAPDOOR);
                if (r.nextBoolean()) barrel(kx + 1, 1, kz + 1, loot("military"));
            }
            // A generator and fuel
            set(half - 4, 0, 0, Blocks.BLAST_FURNACE);
            set(half - 4, 0, 1, Blocks.BARREL);
            set(half - 3, 0, 0, Blocks.LIGHTNING_ROD);
            // The squad
            person(tier() >= 2 ? OpType.MACHINE_GUNNER : grunt(), 0, 0, -half + 2);
            person(OpType.RAIDER_MEDIC, bx + 3, 0, bz + 2);
            int n = 3 + r.nextInt(3);
            for (int i = 0; i < n; i++) person(grunt(), r.nextInt(half) - half / 2, 0, r.nextInt(half) - half / 2);
            return finish();
        }

        // =========================================================================================================
        // Bunker: a concrete blockhouse over a stair down to rooms and corridors in the dark
        // =========================================================================================================
        boolean bunker() {
            if (!ground(6, 6, 3)) return false;
            pad(-6, -6, 6, 6, groundTop(), 5);
            BlockState concrete = Blocks.GRAY_CONCRETE.defaultBlockState(), dark = Blocks.CYAN_TERRACOTTA.defaultBlockState();
            // The blockhouse with its slit and door
            room(-3, -3, 3, 3, 3, concrete, concrete, concrete, 0.03f);
            air(0, 0, -3);
            air(0, 1, -3);
            for (int x = -2; x <= 2; x++) if (x != 0) set(x, 1, 3, Blocks.IRON_BARS);
            // The stair down
            int depth = 7 + r.nextInt(4);
            for (int i = 0; i < depth; i++) {
                int z = -1 + i, y = -1 - i;
                if (z > 20) break;
                for (int x = -1; x <= 1; x++) {
                    for (int yy = y; yy <= y + 3; yy++) air(x, yy, z);
                    set(x, y - 1, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face(Direction.NORTH)).setValue(StairBlock.HALF, Half.BOTTOM));
                    set(x, y + 4, z, concrete);
                }
                set(-2, y, z, concrete); set(-2, y + 1, z, concrete); set(-2, y + 2, z, concrete); set(-2, y + 3, z, concrete);
                set(2, y, z, concrete); set(2, y + 1, z, concrete); set(2, y + 2, z, concrete); set(2, y + 3, z, concrete);
                if (i % 4 == 2) lantern(0, y + 3, z, true, true);
            }
            int by = -depth - 1, bz = depth - 1;
            // A central hall and rooms off it
            int rooms = 2 + r.nextInt(3);
            // The room under the stair's end (shift everything by `by`)
            shiftDown(by, () -> {
                room(-5, bz, 5, bz + 8, 4, dark, concrete, concrete, 0f);
                for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) air(x, y, bz);
                lantern(0, 3, bz + 4, true, true);
                int[][] doors = {{-5, bz + 4}, {5, bz + 4}, {0, bz + 8}};
                for (int k = 0; k < Math.min(rooms, doors.length); k++) {
                    int dx = doors[k][0], dz = doors[k][1];
                    int rx0 = dx == -5 ? -11 : dx == 5 ? 5 : -3, rz0 = dz == bz + 8 ? bz + 8 : bz + 1, rx1 = rx0 + 6, rz1 = Math.min(23, rz0 + 6);
                    room(rx0, rz0, rx1, rz1, 4, concrete, concrete, concrete, 0f);
                    air(dx, 0, dz);
                    air(dx, 1, dz);
                    chest(rx0 + 1, 0, rz0 + 1, loot("bunker"), Direction.SOUTH);
                    if (r.nextBoolean()) barrel(rx1 - 1, 0, rz1 - 1, loot("military"));
                    if (r.nextFloat() < 0.6f) lantern((rx0 + rx1) / 2, 3, (rz0 + rz1) / 2, true, true);
                    person(tier() >= 2 ? (r.nextBoolean() ? OpType.ROGUE : OpType.GHOST) : grunt(), (rx0 + rx1) / 2, 0, (rz0 + rz1) / 2);
                }
                chest(4, 0, bz + 8, loot("bunker"), Direction.WEST);
                person(grunt(), 0, 0, bz + 5);
                person(grunt(), -3, 0, bz + 7);
            });
            person(grunt(), 1, 0, 0);
            return finish();
        }

        /** Build with gy lowered by dy (for the rooms underground), then put it back. */
        void shiftDown(int dy, Runnable build) {
            int keep = gy;
            gy += dy;
            try {
                build.run();
            } finally {
                gy = keep;
            }
        }

        // =========================================================================================================
        // Crash site: a downed helicopter in a scorched crater, burning, crates thrown about
        // =========================================================================================================
        boolean crashSite() {
            if (!ground(9, 9, 4)) return false;
            pad(-9, -9, 9, 9, groundTop(), 6);
            // The crater
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 6) continue;
                int dig = d < 3 ? 2 : d < 5 ? 1 : 0;
                for (int y = -dig; y <= 0; y++) air(x, y, z);
                set(x, -dig - 1, z, r.nextFloat() < 0.5f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
            }
            // The fuselage on its side, the tail boom, a broken rotor
            BlockState hull = Blocks.GRAY_CONCRETE.defaultBlockState(), dark = Blocks.BLACK_CONCRETE.defaultBlockState();
            for (int x = -3; x <= 3; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                boolean shell = Math.abs(y) == 1 || Math.abs(z) == 1 || Math.abs(x) == 3;
                if (shell && r.nextFloat() > 0.12f) set(x, y, z, x == 3 && y == 0 ? Blocks.GRAY_STAINED_GLASS.defaultBlockState() : hull);
                else air(x, y, z);
            }
            for (int x = -9; x < -3; x++) set(x, 0, 0, x % 2 == 0 ? hull : dark);
            set(-9, 1, 0, hull);
            set(-9, 2, 0, hull);
            for (int x = -2; x <= 5; x++) set(x, 2, x - 1, Blocks.IRON_BARS);
            for (int i = 0; i < 6; i++) { int fx = r.nextInt(11) - 5, fz = r.nextInt(11) - 5; if (level.getBlockState(at(fx, 0, fz)).isAir() && level.getBlockState(at(fx, -1, fz)).isSolid()) campfire(fx, 0, fz, false); }
            chest(0, 0, 0, loot("bunker"), Direction.EAST);
            for (int i = 0; i < 2; i++) barrel(r.nextInt(9) - 4, 0, 4 + r.nextInt(3), loot("military"));
            OpType crew = tier() >= 2 ? OpType.ROGUE : OpType.RAIDER;
            for (int i = 0; i < 3 + r.nextInt(2); i++) person(i == 0 && tier() >= 2 ? OpType.GHOST : crew, r.nextInt(11) - 5, 0, r.nextInt(5) + 3);
            return finish();
        }

        // =========================================================================================================
        // Radio tower: a tall lattice mast with an antenna, a shack at its foot
        // =========================================================================================================
        boolean radioTower() {
            if (!ground(5, 5, 3)) return false;
            pad(-5, -5, 5, 5, Blocks.GRAVEL.defaultBlockState(), 5);
            int h = 20 + r.nextInt(10);
            for (int y = 0; y < h; y++) {
                int w = y < h / 2 ? 2 : 1;
                for (int[] c : new int[][] {{-w, -w}, {w, -w}, {-w, w}, {w, w}}) set(c[0], y, c[1], Blocks.IRON_BARS);
                if (y % 3 == 0) for (int i = -w; i <= w; i++) { set(i, y, -w, Blocks.IRON_BARS); set(i, y, w, Blocks.IRON_BARS); set(-w, y, i, Blocks.IRON_BARS); set(w, y, i, Blocks.IRON_BARS); }
                set(0, y, 0, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, face(Direction.NORTH)));
                set(0, y, 1, Blocks.IRON_BLOCK);
            }
            fill(-1, h, -1, 1, h, 1, Blocks.IRON_TRAPDOOR);
            set(0, h, 0, Blocks.IRON_BARS);
            for (int y = h + 1; y < h + 5; y++) set(0, y, 0, Blocks.LIGHTNING_ROD);
            set(0, h + 5, 0, Blocks.REDSTONE_LAMP);
            room(2, -4, 5, -1, 3, Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(), 0.02f);
            air(2, 0, -2);
            air(2, 1, -2);
            chest(4, 0, -3, loot("military"), Direction.WEST);
            set(4, 0, -2, Blocks.CRAFTING_TABLE);
            set(4, 1, -2, Blocks.REPEATER);
            person(shooter(), 1, h + 1, -1);
            for (int i = 0; i < 2; i++) person(tier() >= 2 ? OpType.GHOST : grunt(), 3 + i, 0, 3);
            return finish();
        }

        // =========================================================================================================
        // Warlord's fort: high stone walls with towers, a keep in the middle, the warlord and his guards
        // =========================================================================================================
        boolean fort() {
            int half = 12;
            if (!ground(half, half, 5)) return false;
            pad(-half, -half, half, half, Blocks.COBBLESTONE.defaultBlockState(), 10);
            BlockState wall = Blocks.STONE_BRICKS.defaultBlockState(), mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), crack = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            for (int i = -half; i <= half; i++) for (int y = 0; y < 6; y++) {
                for (int[] c : new int[][] {{i, -half}, {i, half}, {-half, i}, {half, i}}) {
                    if (c[1] == -half && Math.abs(c[0]) <= 1 && y < 3) { air(c[0], y, c[1]); continue; }   // gate
                    set(c[0], y, c[1], r.nextFloat() < 0.2f ? mossy : r.nextFloat() < 0.1f ? crack : wall);
                }
            }
            for (int i = -half; i <= half; i += 2) for (int[] c : new int[][] {{i, -half}, {i, half}, {-half, i}, {half, i}}) set(c[0], 6, c[1], wall);
            // Corner towers
            for (int[] c : new int[][] {{-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < 9; y++) set(c[0] + x, y, c[1] + z, wall);
                set(c[0], 9, c[1], Blocks.STONE_BRICK_WALL);
            }
            // Walkway inside the walls
            for (int i = -half + 1; i <= half - 1; i++) for (int[] c : new int[][] {{i, -half + 1}, {i, half - 1}, {-half + 1, i}, {half - 1, i}})
                set(c[0], 4, c[1], Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            // The keep
            room(-5, -3, 5, 7, 7, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(), 0f);
            air(0, 0, -3);
            air(0, 1, -3);
            air(0, 2, -3);
            for (int x = -3; x <= 3; x += 2) set(x, 3, -3, Blocks.IRON_BARS);
            set(0, 0, 5, Blocks.GOLD_BLOCK);
            chest(-3, 0, 6, "fort_treasure", Direction.SOUTH);
            chest(3, 0, 6, "fort_treasure", Direction.SOUTH);
            barrel(-4, 0, 0, loot("military"));
            barrel(4, 0, 0, loot("military"));
            lantern(0, 5, 2, true, false);
            set(-4, 0, 3, Blocks.RED_CARPET);
            set(4, 0, 3, Blocks.RED_CARPET);
            person(OpType.WARLORD, 0, 0, 3);
            for (int i = 0; i < 3 + r.nextInt(2); i++) person(OpType.BODYGUARD, -2 + i, 0, 1);
            person(OpType.SMUGGLER, 6, 0, -6);
            person(shooter(), -half + 1, 5, 0);
            person(shooter(), half - 1, 5, 0);
            return finish();
        }

        // =========================================================================================================
        // Cult shrine: a ring of stones and soul fire, an altar, the robed ones waiting
        // =========================================================================================================
        boolean cultShrine() {
            if (!ground(8, 8, 3)) return false;
            pad(-8, -8, 8, 8, Blocks.PODZOL.defaultBlockState(), 6);
            int stones = 7 + r.nextInt(5);
            for (int i = 0; i < stones; i++) {
                double a = Math.PI * 2 * i / stones;
                int x = (int) Math.round(Math.cos(a) * 6), z = (int) Math.round(Math.sin(a) * 6);
                int h = 2 + r.nextInt(3);
                for (int y = 0; y < h; y++) set(x, y, z, r.nextFloat() < 0.3f ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState());
                if (r.nextBoolean()) set(x, h, z, Blocks.SOUL_LANTERN);
            }
            fill(-1, 0, -1, 1, 0, 1, Blocks.POLISHED_BLACKSTONE);
            set(0, 1, 0, Blocks.CRYING_OBSIDIAN);
            campfire(2, 0, 0, true);
            campfire(-2, 0, 0, true);
            chest(0, 0, 2, "cult", Direction.NORTH);
            for (int i = 0; i < 6; i++) {
                int x = r.nextInt(13) - 6, z = r.nextInt(13) - 6;
                if (level.getBlockState(at(x, 0, z)).isAir()) set(x, 0, z, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true)
                    .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 1 + r.nextInt(4)));
            }
            for (int i = 0; i < 3 + r.nextInt(3); i++) person(OpType.CULTIST, r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
            return finish();
        }
    }
}
