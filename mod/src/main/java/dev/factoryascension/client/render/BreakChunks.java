package dev.factoryascension.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mining a block breaks real pieces off it: the block is cut into 4x4x4 cubes, and as you dig, the cubes on the face
 * you're hitting come loose and fall to the ground (FxParticles.chunk: physics debris with the block's own texture),
 * layer by layer. Stop digging and the pieces grow back one by one. When the block finally goes, what's left of it
 * falls apart. While a block is being carved, the chunk mesh leaves it out (mixin/BreakHideMixin and its Embeddium
 * twin) and this draws it, cube by cube. Full cubes only; off in Pause > Factory Ascension (Breaking chunks), and off
 * by itself if it ever fails.
 */
public final class BreakChunks {
    static final int N = 4, CELLS = N * N * N;
    static final float CELL = 1f / N;
    /** At most this much of the block comes away before it breaks (the rest falls apart when it does). */
    static final float MAX_GONE = 0.6f;
    /** Ticks with no digging before it starts to grow back, and ticks between cubes coming back. */
    static final int REGROW_DELAY = 10, REGROW_EVERY = 2;
    static final float[] SHADE = {0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f};   // down, up, north, south, west, east

    private static final Map<BlockPos, Carve> CARVES = new HashMap<>();
    /** What the chunk-mesh builders (worker threads) leave out: BlockPos.asLong of every carved block. */
    private static final Set<Long> HIDDEN = ConcurrentHashMap.newKeySet();
    /** Blocks that just fell apart here (pos -> game time), so the server's own break debris doesn't double it. */
    private static final Map<Long, Long> SHATTERED = new HashMap<>();
    private static boolean broken = false;
    public static boolean devHold = false;
    public static int drawn = 0;
    public static String why = "no frame yet";

    private BreakChunks() {
    }

    /** A block being carved: which cubes are gone, in the order they go, and how far each is grown back. */
    static final class Carve {
        final BlockPos pos;
        final BlockState state;
        final Direction face;
        final int[] order = new int[CELLS];
        final boolean[] gone = new boolean[CELLS];
        final float[] grow = new float[CELLS];
        /** The cubes broken off, in order (they grow back last first). */
        final java.util.ArrayList<Integer> taken = new java.util.ArrayList<>();
        int next, idle;

        Carve(BlockPos pos, BlockState state, Direction face) {
            this.pos = pos;
            this.state = state;
            this.face = face;
            java.util.Arrays.fill(grow, 1);
            // Front layer first (the face being hit), the middle of it a little sooner, with some randomness
            RandomSource r = RandomSource.create(pos.asLong() * 31 + face.ordinal());
            float[] key = new float[CELLS];
            Integer[] idx = new Integer[CELLS];
            for (int i = 0; i < CELLS; i++) {
                int x = i % N, y = i / N % N, z = i / (N * N);
                float cx = (x + 0.5f) * CELL, cy = (y + 0.5f) * CELL, cz = (z + 0.5f) * CELL;
                float depth = switch (face) {
                    case UP -> 1 - cy; case DOWN -> cy; case NORTH -> cz; case SOUTH -> 1 - cz; case WEST -> cx; default -> 1 - cx;
                };
                float off = (float) Math.sqrt((cx - 0.5f) * (cx - 0.5f) + (cy - 0.5f) * (cy - 0.5f) + (cz - 0.5f) * (cz - 0.5f));
                key[i] = depth * 4 + off * 0.8f + r.nextFloat() * 0.9f;
                idx[i] = i;
            }
            java.util.Arrays.sort(idx, (a, b) -> Float.compare(key[a], key[b]));
            for (int i = 0; i < CELLS; i++) order[i] = idx[i];
        }

        boolean present(int x, int y, int z) {
            return x < 0 || y < 0 || z < 0 || x >= N || y >= N || z >= N || !gone[x + N * (y + N * z)];
        }
    }

    // ---- Called by the mixins (chunk-mesh worker threads, so only the thread-safe set) ----

    public static boolean hidden(BlockPos pos) {
        return !HIDDEN.isEmpty() && pos != null && HIDDEN.contains(pos.asLong());
    }

    private static MethodHandle embeddiumPos;
    private static boolean embeddiumFailed;

    /** Embeddium hands its block render context; its pos() is the block (read through a cached handle). */
    public static boolean hiddenEmbeddium(Object ctx) {
        if (HIDDEN.isEmpty() || embeddiumFailed) return false;
        try {
            if (embeddiumPos == null)
                embeddiumPos = MethodHandles.publicLookup().findVirtual(ctx.getClass(), "pos", MethodType.methodType(BlockPos.class));
            return hidden((BlockPos) embeddiumPos.invoke(ctx));
        } catch (Throwable err) {
            embeddiumFailed = true;
            return false;
        }
    }

    /** FxClient: did this block just fall apart here (so the server's break debris would be a second helping)? */
    public static boolean justShattered(BlockPos pos) {
        Long t = SHATTERED.remove(pos.asLong());
        var level = Minecraft.getInstance().level;
        return t != null && level != null && level.getGameTime() - t < 40;
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) return;
            try {
                step(mc);
            } catch (Throwable err) {
                fail(err);
            }
        }

        @SubscribeEvent
        public static void draw(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;
            why = CARVES.isEmpty() ? "nothing" : broken ? "broken" : "drawing";
            if (CARVES.isEmpty() || broken) return;
            try {
                drawAll(event.getPoseStack());
            } catch (Throwable err) {
                fail(err);
            }
        }

        @SubscribeEvent
        public static void unload(LevelEvent.Unload event) {
            if (event.getLevel().isClientSide()) { CARVES.clear(); HIDDEN.clear(); SHATTERED.clear(); }
        }
    }

    private static void fail(Throwable err) {
        broken = true;
        for (BlockPos p : new ArrayList<>(CARVES.keySet())) unhide(p);
        CARVES.clear();
        FAClientConfig.failSafe("Breaking chunks", err);
    }

    /** For tests: carve `p` as if it had been dug this far (it grows back unless devHold). */
    public static void devSet(BlockPos p, float progress) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Carve c = start(level, p.immutable(), Direction.SOUTH);
        if (c != null) dig(level, c, progress);
    }

    // ---- Digging, falling apart, growing back ----

    static void step(Minecraft mc) {
        ClientLevel level = mc.level;
        BlockPos at = null;
        float progress = 0;
        Direction face = Direction.UP;
        if (!broken && FAClientConfig.on(FAClientConfig.BREAK_CHUNKS) && mc.gameMode != null && mc.gameMode.isDestroying()
            && mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            at = bh.getBlockPos().immutable();
            face = bh.getDirection();
            progress = (mc.gameMode.getDestroyStage() + 1) / 10f;
        }
        if (at != null && !CARVES.containsKey(at)) start(level, at, face);
        for (Iterator<Carve> it = CARVES.values().iterator(); it.hasNext(); ) {
            Carve c = it.next();
            BlockState now = level.getBlockState(c.pos);
            if (now != c.state) {
                if (now.isAir()) shatter(level, c);
                it.remove();
                unhide(c.pos);
                continue;
            }
            for (int i = 0; i < CELLS; i++) if (!c.gone[i] && c.grow[i] < 1) c.grow[i] = Math.min(1, c.grow[i] + 0.2f);
            if (c.pos.equals(at)) { c.idle = 0; dig(level, c, progress); continue; }
            if (devHold || ++c.idle < REGROW_DELAY) continue;
            if (!c.taken.isEmpty()) {
                if (c.idle % REGROW_EVERY == 0) {
                    int cell = c.taken.remove(c.taken.size() - 1);
                    c.gone[cell] = false;
                    c.grow[cell] = 0;
                    c.next = 0;   // the order is walked again from the front next time it's dug
                }
            } else if (allGrown(c)) {
                it.remove();
                unhide(c.pos);
            }
        }
        if (SHATTERED.size() > 64) SHATTERED.clear();
    }

    static boolean allGrown(Carve c) {
        for (float g : c.grow) if (g < 1) return false;
        return true;
    }

    static Carve start(ClientLevel level, BlockPos pos, Direction face) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || !st.isSolidRender(level, pos) || st.hasBlockEntity()) return null;
        Carve c = new Carve(pos, st, face);
        CARVES.put(pos, c);
        HIDDEN.add(pos.asLong());
        Minecraft.getInstance().levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        return c;
    }

    static void unhide(BlockPos pos) {
        if (HIDDEN.remove(pos.asLong()))
            Minecraft.getInstance().levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Break off pieces until as many cubes are gone as this much digging takes. A piece is one cube, or two or three
     * neighbours that come away together, so the bits aren't all the same.
     */
    static void dig(ClientLevel level, Carve c, float progress) {
        int want = Math.round(progress * MAX_GONE * CELLS);
        RandomSource r = level.random;
        while (c.taken.size() < want) {
            while (c.next < CELLS && c.gone[c.order[c.next]]) c.next++;
            if (c.next >= CELLS) return;
            java.util.List<Integer> piece = new java.util.ArrayList<>();
            piece.add(c.order[c.next]);
            int extra = r.nextFloat() < 0.45f ? (r.nextFloat() < 0.4f ? 2 : 1) : 0;
            for (int k = 0; k < extra && c.taken.size() + piece.size() < want + 2; k++) {
                int n = neighbour(c, piece.get(piece.size() - 1), piece, r);
                if (n < 0) break;
                piece.add(n);
            }
            for (int cell : piece) { c.gone[cell] = true; c.taken.add(cell); }
            drop(level, c, piece, false);
        }
    }

    /** A cube next to `cell` still on the block (not in `piece`), or -1. */
    static int neighbour(Carve c, int cell, java.util.List<Integer> piece, RandomSource r) {
        int x = cell % N, y = cell / N % N, z = cell / (N * N);
        int[][] d = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        int start = r.nextInt(6);
        for (int k = 0; k < 6; k++) {
            int[] o = d[(start + k) % 6];
            int nx = x + o[0], ny = y + o[1], nz = z + o[2];
            if (nx < 0 || ny < 0 || nz < 0 || nx >= N || ny >= N || nz >= N) continue;
            int n = nx + N * (ny + N * nz);
            if (!c.gone[n] && !piece.contains(n)) return n;
        }
        return -1;
    }

    /** The block's gone: what's left of it falls apart, in pieces of one to three cubes. */
    static void shatter(ClientLevel level, Carve c) {
        RandomSource r = level.random;
        for (int i = 0; i < CELLS; i++) {
            if (c.gone[i]) continue;
            java.util.List<Integer> piece = new java.util.ArrayList<>();
            piece.add(i);
            c.gone[i] = true;
            if (r.nextFloat() < 0.5f) {
                int n = neighbour(c, i, piece, r);
                if (n >= 0) { piece.add(n); c.gone[n] = true; }
            }
            drop(level, c, piece, true);
        }
        SHATTERED.put(c.pos.asLong(), level.getGameTime());
    }

    /**
     * A piece comes loose: a box over its cubes, wearing the matching bit of the face it was nearest the outside of.
     * Dug off, it's knocked out of the face you're hitting; shattered, it falls outward from the middle. Crumbs too.
     */
    static void drop(ClientLevel level, Carve c, java.util.List<Integer> cells, boolean shatter) {
        int x0 = N, y0 = N, z0 = N, x1 = -1, y1 = -1, z1 = -1;
        for (int cell : cells) {
            int x = cell % N, y = cell / N % N, z = cell / (N * N);
            x0 = Math.min(x0, x); y0 = Math.min(y0, y); z0 = Math.min(z0, z);
            x1 = Math.max(x1, x); y1 = Math.max(y1, y); z1 = Math.max(z1, z);
        }
        double cx = (x0 + x1 + 1) * 0.5 * CELL, cy = (y0 + y1 + 1) * 0.5 * CELL, cz = (z0 + z1 + 1) * 0.5 * CELL;
        float sx = (x1 - x0 + 1) * CELL / 2 * 0.96f, sy = (y1 - y0 + 1) * CELL / 2 * 0.96f, sz = (z1 - z0 + 1) * CELL / 2 * 0.96f;
        // Its texture: the matching piece of the face it's closest to the outside of
        Direction d = Math.abs(cy - 0.5) >= Math.max(Math.abs(cx - 0.5), Math.abs(cz - 0.5)) ? (cy > 0.5 ? Direction.UP : Direction.DOWN)
            : Math.abs(cx - 0.5) > Math.abs(cz - 0.5) ? (cx > 0.5 ? Direction.EAST : Direction.WEST) : (cz > 0.5 ? Direction.SOUTH : Direction.NORTH);
        BakedQuad q = firstQuad(c.state, d);
        TextureAtlasSprite s = q != null ? q.getSprite() : Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(c.state);
        float[] uv0 = faceUV(d, (float) (x0 * CELL), (float) (y0 * CELL), (float) (z0 * CELL));
        float[] uv1 = faceUV(d, (float) ((x1 + 1) * CELL), (float) ((y1 + 1) * CELL), (float) ((z1 + 1) * CELL));
        float pu = Math.min(uv0[0], uv1[0]), pv = Math.min(uv0[1], uv1[1]);
        float uw = Math.max(CELL, Math.abs(uv1[0] - uv0[0])), vh = Math.max(CELL, Math.abs(uv1[1] - uv0[1]));
        int tint = q != null && q.isTinted() ? Minecraft.getInstance().getBlockColors().getColor(c.state, level, c.pos, q.getTintIndex()) : -1;
        RandomSource r = level.random;
        double vx, vy, vz;
        if (shatter) {
            vx = (cx - 0.5) * 0.25 + r.nextGaussian() * 0.03;
            vy = 0.04 + r.nextDouble() * 0.08;
            vz = (cz - 0.5) * 0.25 + r.nextGaussian() * 0.03;
        } else {
            // Out of the face being hit, a little up, a little to the side
            var n = c.face.getNormal();
            double push = 0.05 + r.nextDouble() * 0.06;
            vx = n.getX() * push + r.nextGaussian() * 0.02;
            vy = Math.max(0, n.getY()) * push + 0.03 + r.nextDouble() * 0.05;
            vz = n.getZ() * push + r.nextGaussian() * 0.02;
        }
        double wx = c.pos.getX() + cx, wy = c.pos.getY() + cy, wz = c.pos.getZ() + cz;
        FxParticles.chunk(level, c.state, c.pos, wx, wy, wz, vx, vy, vz, s, pu, pv, uw, vh, sx, sy, sz, tint);
        FxParticles.crumbs(level, c.state, wx, wy, wz, shatter ? 1 : 3, 0.3);
    }

    // ---- Drawing what's left of each carved block ----

    private static final RandomSource QUAD_RAND = RandomSource.create();

    static List<BakedQuad> quads(BlockState st, Direction d) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(st);
        QUAD_RAND.setSeed(42);
        List<BakedQuad> q = model.getQuads(st, d, QUAD_RAND, ModelData.EMPTY, null);
        if (!q.isEmpty()) return q;
        List<BakedQuad> loose = new ArrayList<>();
        QUAD_RAND.setSeed(42);
        for (BakedQuad b : model.getQuads(st, null, QUAD_RAND, ModelData.EMPTY, null)) if (b.getDirection() == d) loose.add(b);
        return loose;
    }

    static BakedQuad firstQuad(BlockState st, Direction d) {
        List<BakedQuad> q = quads(st, d);
        return q.isEmpty() ? null : q.get(0);
    }

    /** Where a point on a face sits on that face's texture (0..1), the way the game lays block faces out. */
    static float[] faceUV(Direction d, float x, float y, float z) {
        return switch (d) {
            case UP -> new float[] {x, z};
            case DOWN -> new float[] {x, 1 - z};
            case NORTH -> new float[] {1 - x, 1 - y};
            case SOUTH -> new float[] {x, 1 - y};
            case WEST -> new float[] {z, 1 - y};
            default -> new float[] {1 - z, 1 - y};
        };
    }

    // Each face's two edge directions, picked so the corners go round anticlockwise seen from outside
    private static final float[][][] EDGES = {
        {{1, 0, 0}, {0, 0, 1}},   // down
        {{0, 0, 1}, {1, 0, 0}},   // up
        {{0, 1, 0}, {1, 0, 0}},   // north
        {{1, 0, 0}, {0, 1, 0}},   // south
        {{0, 0, 1}, {0, 1, 0}},   // west
        {{0, 1, 0}, {0, 0, 1}}};  // east

    static void drawAll(PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(RenderType.cutout());
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        drawn = 0;
        for (Carve c : CARVES.values()) {
            BlockPos p = c.pos;
            List<List<BakedQuad>> faceQuads = new ArrayList<>(6);
            int[] tints = new int[6];
            int[] light = new int[6];
            int inner = 0;
            for (Direction d : Direction.values()) {
                List<BakedQuad> q = quads(c.state, d);
                faceQuads.add(q);
                light[d.ordinal()] = LevelRenderer.getLightColor(mc.level, p.relative(d));
                inner = Math.max(inner, light[d.ordinal()]);
            }
            float ox = (float) (p.getX() - cam.x), oy = (float) (p.getY() - cam.y), oz = (float) (p.getZ() - cam.z);
            for (int i = 0; i < CELLS; i++) {
                if (c.gone[i]) {
                    backing(mc, out, m, nm, c, i, ox, oy, oz, inner);
                    continue;
                }
                int x = i % N, y = i / N % N, z = i / (N * N);
                float g = c.grow[i] < 1 ? 0.25f + 0.75f * c.grow[i] : 1;
                float h = CELL / 2 * g;
                float cx = (x + 0.5f) * CELL, cy = (y + 0.5f) * CELL, cz = (z + 0.5f) * CELL;
                for (Direction d : Direction.values()) {
                    var n = d.getNormal();
                    boolean outside = !(x + n.getX() >= 0 && x + n.getX() < N && y + n.getY() >= 0 && y + n.getY() < N && z + n.getZ() >= 0 && z + n.getZ() < N);
                    if (g == 1 && !outside && c.present(x + n.getX(), y + n.getY(), z + n.getZ())) continue;   // inside, unseen
                    if (g == 1 && outside) {
                        BlockPos side = p.relative(d);
                        if (mc.level.getBlockState(side).isSolidRender(mc.level, side)) continue;            // against a wall
                    }
                    List<BakedQuad> qs = faceQuads.get(d.ordinal());
                    int lt = outside ? light[d.ordinal()] : inner;
                    if (qs.isEmpty()) continue;
                    // The block's real face (and overlays like grass sides) outside; just the base texture inside
                    for (int k = 0; k < (outside ? qs.size() : 1); k++) {
                        BakedQuad q = qs.get(k);
                        int tint = q.isTinted() ? mc.getBlockColors().getColor(c.state, mc.level, p, q.getTintIndex()) : -1;
                        face(out, m, nm, d, q.getSprite(), tint, lt, ox, oy, oz, cx, cy, cz, h, k * 0.0005f);
                        drawn++;
                    }
                }
            }
        }
        buffers.endBatch(RenderType.cutout());
    }

    /**
     * A gone cube at the block's edge: the block next to it there had its face left out of the chunk mesh (it was
     * covered), so through the hole you'd see into the void. Draw that piece of the neighbour's face instead.
     */
    static void backing(Minecraft mc, VertexConsumer out, Matrix4f m, Matrix3f nm, Carve c, int i, float ox, float oy, float oz, int light) {
        int x = i % N, y = i / N % N, z = i / (N * N);
        for (Direction d : Direction.values()) {
            var n = d.getNormal();
            int nx = x + n.getX(), ny = y + n.getY(), nz = z + n.getZ();
            if (nx >= 0 && ny >= 0 && nz >= 0 && nx < N && ny < N && nz < N) continue;
            BlockPos side = c.pos.relative(d);
            BlockState ns = mc.level.getBlockState(side);
            if (!ns.isSolidRender(mc.level, side)) continue;
            Direction back = d.getOpposite();
            BakedQuad q = firstQuad(ns, back);
            if (q == null) continue;
            int tint = q.isTinted() ? mc.getBlockColors().getColor(ns, mc.level, side, q.getTintIndex()) : -1;
            // The neighbour's face, seen from inside our block: a cell-sized square on the shared side
            float cx = (x + 0.5f) * CELL + n.getX() * CELL, cy = (y + 0.5f) * CELL + n.getY() * CELL, cz = (z + 0.5f) * CELL + n.getZ() * CELL;
            face(out, m, nm, back, q.getSprite(), tint, light, ox, oy, oz, cx, cy, cz, CELL / 2, 0);
            drawn++;
        }
    }

    /** One square face of a cube centred at (cx, cy, cz) in the block, `h` half its size. */
    static void face(VertexConsumer out, Matrix4f m, Matrix3f nm, Direction d, TextureAtlasSprite s, int tint, int light,
                     float ox, float oy, float oz, float cx, float cy, float cz, float h, float lift) {
        var n = d.getNormal();
        float[] a = EDGES[d.ordinal()][0], b = EDGES[d.ordinal()][1];
        float sh = SHADE[d.ordinal()];
        float r = sh, g = sh, bl = sh;
        if (tint != -1) { r *= (tint >> 16 & 255) / 255f; g *= (tint >> 8 & 255) / 255f; bl *= (tint & 255) / 255f; }
        float[][] sign = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (float[] sg : sign) {
            float px = cx + n.getX() * (h + lift) + (a[0] * sg[0] + b[0] * sg[1]) * h;
            float py = cy + n.getY() * (h + lift) + (a[1] * sg[0] + b[1] * sg[1]) * h;
            float pz = cz + n.getZ() * (h + lift) + (a[2] * sg[0] + b[2] * sg[1]) * h;
            // (A face's texture ignores its own axis, so a neighbour's face just outside the block maps the same way)
            float[] uv = faceUV(d, Math.max(0, Math.min(1, px)), Math.max(0, Math.min(1, py)), Math.max(0, Math.min(1, pz)));
            out.vertex(m, ox + px, oy + py, oz + pz).color(r, g, bl, 1f).uv(s.getU(uv[0] * 16), s.getV(uv[1] * 16))
                .uv2(light).normal(nm, n.getX(), n.getY(), n.getZ()).endVertex();
        }
    }
}
