package dev.factoryascension.client.hd;

/**
 * The HD texture pack's painter (no game classes, so tools/hd/Preview can run it too): a 16x texture becomes a 32x one
 * that still looks like itself, only finer.
 *  1. Edges: each pixel splits into four by EPX/Scale2x, so diagonals become smooth steps instead of fat blocks.
 *  2. Form: the texture's own light and dark shapes are read as a height (blurred brightness), and lit from the top
 *     left, so stones look rounded, seams look sunk and planks get edges.
 *  3. Grain: a fine, fixed noise (more on rough, busy textures, almost none on flat ones), so the larger pixels don't
 *     read as flat paint.
 *  4. A touch more colour and contrast.
 * Alpha is kept as it was (cut-outs stay cut-outs; opaque stays fully opaque, which the depth pack needs).
 */
public final class Upscale {
    private Upscale() {
    }

    /** `src` is w x h ARGB (frames stacked for animations); returns 2w x 2h ARGB. `seed` varies the grain per texture. */
    public static int[] x2(int[] src, int w, int h, long seed, Style style) {
        int W = w * 2, H = h * 2;
        int[] out = new int[W * H];
        // 1. EPX, only on clean pixel art (few colours): on noisy textures (stone, gravel) it melts the noise into
        // blobs, so those are doubled as they are and get their detail from the grain instead
        java.util.Set<Integer> colours = new java.util.HashSet<>();
        for (int c : src) if (c >>> 24 != 0) colours.add(c);
        boolean clean = colours.size() <= 20;
        // (EPX on each frame separately, so animation frames don't bleed: square frames of w x w when h is a multiple)
        int fh = h % w == 0 ? w : h;
        for (int y = 0; y < h; y++) {
            int f0 = y / fh * fh, f1 = f0 + fh - 1;
            for (int x = 0; x < w; x++) {
                int p = src[y * w + x];
                int a = src[Math.max(y - 1, f0) * w + x], b = src[y * w + Math.min(x + 1, w - 1)];
                int c = src[y * w + Math.max(x - 1, 0)], d = src[Math.min(y + 1, f1) * w + x];
                int o0 = p, o1 = p, o2 = p, o3 = p;
                if (clean) {
                    if (c == a && c != d && a != b) o0 = a;
                    if (a == b && a != c && b != d) o1 = b;
                    if (d == c && d != b && c != a) o2 = c;
                    if (b == d && b != a && d != c) o3 = d;
                }
                int ox = x * 2, oy = y * 2;
                out[oy * W + ox] = o0;
                out[oy * W + ox + 1] = o1;
                out[(oy + 1) * W + ox] = o2;
                out[(oy + 1) * W + ox + 1] = o3;
            }
        }
        if (style == Style.PLAIN) return out;
        // 2. Height from brightness, softened (a 3x3 blur, twice), per frame
        int FH = fh * 2;
        float[] lum = new float[W * H];
        for (int i = 0; i < out.length; i++) lum[i] = luma(out[i]);
        float[] ht = blur(blur(lum, W, H, FH), W, H, FH);
        // How busy the texture is (its spread of brightness): busier ones get more grain and form
        float mean = 0, var = 0;
        for (float v : lum) mean += v;
        mean /= lum.length;
        for (float v : lum) var += (v - mean) * (v - mean);
        float busy = (float) Math.min(1, Math.sqrt(var / lum.length) * 5);
        // Busy textures (cobble, ore, leaves) already show their shapes: they get less form, not more
        // Gentle: the depth pack already gives blocks their relief, this only adds a hint of it
        float form = style == Style.ITEM ? 0.5f : 0.7f - busy * 0.4f, grain = style == Style.ITEM ? 0.01f : 0.03f + busy * 0.02f;
        int[] res = new int[out.length];
        for (int y = 0; y < H; y++) {
            int f0 = y / FH * FH, f1 = f0 + FH - 1;
            for (int x = 0; x < W; x++) {
                int i = y * W + x, argb = out[i];
                int alpha = argb >>> 24;
                if (alpha == 0) { res[i] = argb; continue; }
                // Slope of the height, lit from the top left
                float hl = ht[y * W + Math.max(x - 1, 0)], hr = ht[y * W + Math.min(x + 1, W - 1)];
                float hu = ht[Math.max(y - 1, f0) * W + x], hd = ht[Math.min(y + 1, f1) * W + x];
                float light = Math.max(-0.07f, Math.min(0.07f, ((hl - hr) + (hu - hd)) * form));
                // Edges of cut-outs (leaves, plants): a little darker, so they read as solid
                if (style != Style.ITEM && alpha == 255 && hasClearNeighbour(out, W, H, x, y)) light -= 0.12f;
                float n = (hash(seed, x, y) - 0.5f) * 2 * grain;
                float k = 1 + light + n;
                int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                // A touch more colour: push away from grey
                float grey = (r + g + b) / 3f;
                float sat = 1.04f;
                float rf = (grey + (r - grey) * sat) * k, gf = (grey + (g - grey) * sat) * k, bf = (grey + (b - grey) * sat) * k;
                res[i] = alpha << 24 | clamp(rf) << 16 | clamp(gf) << 8 | clamp(bf);
            }
        }
        return res;
    }

    public enum Style {
        /** Blocks: form, grain and colour. */
        BLOCK,
        /** Items: smoother, less grain. */
        ITEM,
        /** Only the edge smoothing. */
        PLAIN
    }

    static boolean hasClearNeighbour(int[] img, int W, int H, int x, int y) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                if (img[ny * W + nx] >>> 24 == 0) return true;
            }
        return false;
    }

    static float luma(int argb) {
        return ((argb >> 16 & 255) * 0.299f + (argb >> 8 & 255) * 0.587f + (argb & 255) * 0.114f) / 255f;
    }

    static float[] blur(float[] v, int W, int H, int FH) {
        float[] o = new float[v.length];
        for (int y = 0; y < H; y++) {
            int f0 = y / FH * FH, f1 = f0 + FH - 1;
            for (int x = 0; x < W; x++) {
                float s = 0;
                int n = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        // Wraps sideways (block textures tile), stays inside its frame up and down
                        int nx = (x + dx + W) % W, ny = Math.min(Math.max(y + dy, f0), f1);
                        s += v[ny * W + nx];
                        n++;
                    }
                o[y * W + x] = s / n;
            }
        }
        return o;
    }

    static float hash(long seed, int x, int y) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + y * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    static int clamp(float v) {
        return v < 0 ? 0 : v > 255 ? 255 : Math.round(v);
    }
}
