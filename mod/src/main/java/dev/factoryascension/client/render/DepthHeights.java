package dev.factoryascension.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Real heights for the depth pack: every fully opaque block texture drawn in the solid layer gets a height map that
 * knows what it is, carried in its alpha (255 = the surface, down to 191 = the deepest; the depth shaders read it).
 * Rules by material: planks get boards with deep seams and shallow grain; bricks and tiles raised blocks with bevelled
 * edges and sunk mortar; cobble, gravel and stone domed stones between cracks; ores gems standing out of the stone;
 * grass a mat of blades; dirt, sand and wool a fine grain; log bark deep furrows, log ends their rings; the crafting
 * table a raised rim round a grid of sunk cells, its sides tools standing off the boards. Anything else: domes between
 * the texture's edges plus its dark detail. Done once each time textures load (only with the depth pack on and no
 * shader pack); animated and see-through textures are left alone.
 */
public final class DepthHeights {
    private static boolean pending = false, applied = false;
    public static int lastCount = 0;
    private static Field imgField, mipsField;

    private DepthHeights() {
    }

    /** Textures just (re)loaded: do it before the next frame. */
    public static void reloaded() {
        pending = true;
        applied = false;
    }

    public static boolean applied() {
        return applied;
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = dev.factoryascension.FactoryAscension.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Ticker {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            DepthHeights.tick();
            // A shader pack switched on reads alpha its own way: give it the textures as they were
            if (applied && AscensionLighting.shaderPackActive()) {
                applied = false;
                Minecraft.getInstance().reloadResourcePacks();
            }
        }
    }

    /** Each tick (cheap unless pending). */
    public static void tick() {
        if (!pending) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null && mc.screen == null) return;
        pending = false;
        if (!dev.factoryascension.client.DepthShaders.enabled() || AscensionLighting.shaderPackActive()) return;
        try {
            long t0 = System.nanoTime();
            lastCount = apply(mc);
            applied = lastCount > 0;
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").info("Depth heights: {} block textures in {} ms", lastCount, (System.nanoTime() - t0) / 1_000_000);
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Depth heights failed; the depth pack falls back to flat", err);
        }
    }

    static void fields() throws ReflectiveOperationException {
        if (imgField != null) return;
        for (Field f : SpriteContents.class.getDeclaredFields()) {
            if (f.getType() == NativeImage.class && imgField == null) { f.setAccessible(true); imgField = f; }
            if (f.getType() == NativeImage[].class && mipsField == null) { f.setAccessible(true); mipsField = f; }
        }
        if (imgField == null || mipsField == null) throw new NoSuchFieldException("SpriteContents images");
    }

    static int apply(Minecraft mc) throws ReflectiveOperationException {
        fields();
        // Which sprites the solid layer draws (and none that anything see-through draws too)
        Set<TextureAtlasSprite> solid = new HashSet<>(), not = new HashSet<>();
        RandomSource r = RandomSource.create(42);
        var shaper = mc.getBlockRenderer();
        for (Block b : BuiltInRegistries.BLOCK) {
            var states = b.getStateDefinition().getPossibleStates();
            for (int i = 0; i < Math.min(16, states.size()); i++) {
                BlockState st = states.get(i);
                var model = shaper.getBlockModel(st);
                var types = model.getRenderTypes(st, r, ModelData.EMPTY);
                boolean isSolid = types.contains(RenderType.solid()) && types.asList().size() == 1;
                for (Direction d : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    for (var q : model.getQuads(st, d, r, ModelData.EMPTY, null)) (isSolid ? solid : not).add(q.getSprite());
                }
            }
        }
        solid.removeAll(not);
        TextureAtlas atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        atlas.bind();
        int n = 0;
        for (TextureAtlasSprite s : solid) {
            SpriteContents c = s.contents();
            NativeImage orig = (NativeImage) imgField.get(c);
            NativeImage[] mips = (NativeImage[]) mipsField.get(c);
            if (orig == null || mips == null || mips.length == 0) continue;
            int w = c.width(), h = c.height();
            if (orig.getWidth() != w || orig.getHeight() != h) continue;          // animated
            if (!opaque(mips[0], w, h)) continue;
            float[] depth = heights(c.name().getPath(), mips[0], w, h);
            write(mips, depth, w, h);
            s.uploadFirstFrame();
            n++;
        }
        return n;
    }

    static boolean opaque(NativeImage im, int w, int h) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((im.getPixelRGBA(x, y) >>> 24) != 255) return false;
        return true;
    }

    /** Depth (0 = the surface, 1 = the deepest) into alpha, at every mip level (averaged down). */
    static void write(NativeImage[] mips, float[] depth, int w, int h) {
        float[] d = depth;
        int cw = w, ch = h;
        for (int l = 0; l < mips.length; l++) {
            NativeImage im = mips[l];
            if (im == null) break;
            int iw = Math.min(cw, im.getWidth()), ih = Math.min(ch, im.getHeight());
            for (int y = 0; y < ih; y++)
                for (int x = 0; x < iw; x++) {
                    int px = im.getPixelRGBA(x, y);
                    int a = 255 - Math.round(Math.max(0, Math.min(1, d[y * cw + x])) * 64);
                    im.setPixelRGBA(x, y, (px & 0x00FFFFFF) | (a << 24));
                }
            if (cw <= 1 || ch <= 1) break;
            int nw = Math.max(1, cw / 2), nh = Math.max(1, ch / 2);
            float[] nd = new float[nw * nh];
            for (int y = 0; y < nh; y++)
                for (int x = 0; x < nw; x++)
                    nd[y * nw + x] = (d[(2 * y) * cw + 2 * x] + d[(2 * y) * cw + Math.min(cw - 1, 2 * x + 1)] + d[Math.min(ch - 1, 2 * y + 1) * cw + 2 * x]
                        + d[Math.min(ch - 1, 2 * y + 1) * cw + Math.min(cw - 1, 2 * x + 1)]) * 0.25f;
            d = nd;
            cw = nw;
            ch = nh;
        }
    }

    // ---------------- The heights ----------------
    static float lum(int abgr) {
        int r = abgr & 255, g = abgr >> 8 & 255, b = abgr >> 16 & 255;
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
    }

    static float sat(int abgr) {
        int r = abgr & 255, g = abgr >> 8 & 255, b = abgr >> 16 & 255;
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return mx == 0 ? 0 : (mx - mn) / (float) mx;
    }

    static float ss(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    /** Distance (texels, wrapping round like the tiling texture) from each texel to the nearest `edge` one. */
    static float[] distance(boolean[] edge, int w, int h) {
        float[] d = new float[w * h];
        float big = w + h;
        for (int i = 0; i < d.length; i++) d[i] = edge[i] ? 0 : big;
        // A few passes of a wrapped chamfer transform
        for (int pass = 0; pass < 3; pass++) {
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    float v = d[y * w + x];
                    v = Math.min(v, d[y * w + Math.floorMod(x - 1, w)] + 1);
                    v = Math.min(v, d[Math.floorMod(y - 1, h) * w + x] + 1);
                    v = Math.min(v, d[Math.floorMod(y - 1, h) * w + Math.floorMod(x - 1, w)] + 1.414f);
                    v = Math.min(v, d[Math.floorMod(y - 1, h) * w + Math.floorMod(x + 1, w)] + 1.414f);
                    d[y * w + x] = v;
                }
            for (int y = h - 1; y >= 0; y--)
                for (int x = w - 1; x >= 0; x--) {
                    float v = d[y * w + x];
                    v = Math.min(v, d[y * w + Math.floorMod(x + 1, w)] + 1);
                    v = Math.min(v, d[Math.floorMod(y + 1, h) * w + x] + 1);
                    v = Math.min(v, d[Math.floorMod(y + 1, h) * w + Math.floorMod(x + 1, w)] + 1.414f);
                    v = Math.min(v, d[Math.floorMod(y + 1, h) * w + Math.floorMod(x - 1, w)] + 1.414f);
                    d[y * w + x] = v;
                }
        }
        return d;
    }

    static float[] blur(float[] v, int w, int h) {
        float[] o = new float[v.length];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float s = v[y * w + x] * 4;
                s += v[y * w + Math.floorMod(x - 1, w)] + v[y * w + Math.floorMod(x + 1, w)] + v[Math.floorMod(y - 1, h) * w + x] + v[Math.floorMod(y + 1, h) * w + x];
                o[y * w + x] = s / 8;
            }
        return o;
    }

    static boolean has(String n, String... keys) {
        for (String k : keys) if (n.contains(k)) return true;
        return false;
    }

    static float[] heights(String name, NativeImage im, int w, int h) {
        int N = w * h;
        float[] L = new float[N], S = new float[N];
        float avg = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int p = im.getPixelRGBA(x, y);
                L[y * w + x] = lum(p);
                S[y * w + x] = sat(p);
                avg += L[y * w + x];
            }
        avg /= N;
        float k = w / 16f;                                    // texels per vanilla texel (HD packs)
        float[] D = new float[N];
        String n = name.substring(name.lastIndexOf('/') + 1);

        // Local darkness: how much darker than its 5x5 neighbourhood (seams, cracks, mortar, gaps)
        float[] local = new float[N];
        int R = Math.max(1, Math.round(2 * k));
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float s = 0;
                int c = 0;
                for (int dy = -R; dy <= R; dy++) for (int dx = -R; dx <= R; dx++) { s += L[Math.floorMod(y + dy, h) * w + Math.floorMod(x + dx, w)]; c++; }
                local[y * w + x] = s / c - L[y * w + x];
            }

        if (n.equals("crafting_table_top")) {
            // A table: the wooden rim is the top, the grid's cells sit a little down, the grooves between them deep
            int rim = Math.max(1, Math.round(1.5f * k));
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    boolean edge = x < rim || y < rim || x >= w - rim || y >= h - rim;
                    if (edge) { D[i] = 0.05f * ss(0, 0.2f, local[i]); continue; }
                    D[i] = 0.32f + 0.6f * ss(0.02f, 0.12f, local[i]);
                }
            return blur(D, w, h);
        }
        if (has(n, "crafting_table", "fletching_table", "smithing_table", "cartography_table")) {
            // The tools stand off the boards: bright metal and saturated handles up, wood behind with its seams
            for (int i = 0; i < N; i++) {
                boolean tool = (S[i] < 0.25f && L[i] > avg + 0.12f) || S[i] > 0.55f;
                D[i] = tool ? 0f : 0.35f + 0.5f * ss(0.03f, 0.14f, local[i]);
            }
            return blur(D, w, h);
        }
        if (has(n, "_ore")) {
            // Gems and nuggets stand out of the stone: the stone sits back, cracked; the ore bits are the surface
            for (int i = 0; i < N; i++) {
                boolean gem = S[i] > 0.28f || Math.abs(L[i] - avg) > 0.25f && S[i] > 0.15f;
                D[i] = gem ? 0f : 0.4f + 0.45f * ss(0.02f, 0.15f, local[i]);
            }
            return blur(D, w, h);
        }
        if (has(n, "grass_block_top", "moss", "grass", "mycelium_top", "podzol_top", "nylium", "warped_wart", "nether_wart_block")) {
            // Blades: bright tips stand up, the dark between them is deep; sharp, not smoothed
            float lo = 1, hi = 0;
            for (float v : L) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
            for (int i = 0; i < N; i++) {
                float t = (L[i] - lo) / Math.max(0.01f, hi - lo);
                D[i] = (float) Math.pow(1 - t, 0.8) * 0.9f;
            }
            return D;
        }
        if (has(n, "dirt", "sand", "soul_soil", "clay", "snow", "wool", "concrete_powder", "mud", "farmland")) {
            // A fine grain, shallow
            for (int i = 0; i < N; i++) D[i] = 0.15f + 0.6f * ss(-0.02f, 0.2f, local[i]) * 0.6f + Math.max(0, avg - L[i]) * 0.5f;
            return blur(D, w, h);
        }
        if (has(n, "log", "stem", "_wood", "hyphae") && !has(n, "_top")) {
            // Bark: deep vertical furrows where it's dark
            for (int i = 0; i < N; i++) D[i] = 0.1f + 0.85f * ss(-0.01f, 0.16f, local[i] + (avg - L[i]) * 0.5f);
            return blur(D, w, h);
        }
        if (has(n, "log_top", "stem_top", "_top") && has(n, "log", "stem")) {
            // Rings: shallow grooves; the bark round the edge stands proud
            for (int i = 0; i < N; i++) D[i] = 0.15f + 0.4f * ss(0.0f, 0.12f, local[i]);
            return blur(D, w, h);
        }
        // Everything else: what's bounded by edges (boards, bricks, stones, tiles, panels) is domed and bevelled down
        // to its seams, and dark seams sink deep
        boolean[] seam = new boolean[N];
        int seams = 0;
        float thr = has(n, "planks") ? 0.07f : 0.09f;
        for (int i = 0; i < N; i++) {
            seam[i] = local[i] > thr || L[i] < avg - 0.22f;
            if (has(n, "brick", "tiles", "quartz_pillar", "chiseled") && L[i] > avg + 0.18f && S[i] < 0.2f) seam[i] = true;   // light mortar
            if (seam[i]) seams++;
        }
        float frac = seams / (float) N;
        if (frac > 0.55f || frac < 0.02f) {
            // No clear pieces (a noisy or a plain texture): a gentle relief from its dark detail
            for (int i = 0; i < N; i++) D[i] = 0.2f + 0.7f * ss(-0.02f, 0.18f, local[i] + (avg - L[i]) * 0.4f) * 0.8f;
            return blur(D, w, h);
        }
        float[] dist = distance(seam, w, h);
        float bevel = (has(n, "planks") ? 1.2f : has(n, "cobble", "gravel", "stone", "deepslate", "blackstone", "basalt") ? 2.4f : 1.8f) * k;
        for (int i = 0; i < N; i++) {
            float dome = 1 - ss(0, bevel, dist[i]);                         // 1 at the seam .. 0 a bevel in
            float grain = Math.max(0, avg - L[i]) * 0.5f + Math.max(0, local[i]) * 0.6f;
            D[i] = seam[i] ? 0.95f : 0.08f + 0.6f * dome + Math.min(0.25f, grain);
        }
        return blur(D, w, h);
    }
}
