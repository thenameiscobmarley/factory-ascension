package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * A smooth gun (or attachment) mesh made by tools/guns/mesh.py: rounded edges, round barrels, curved stocks. Drawn
 * up close (first person, your own gun in third person, the bench preview); the cuboid model is the icon and the far
 * model. Vertex colours carry the material colour, edge wear and a baked highlight; a grey detail texture per surface
 * class (metal, polymer, wood, ...) carries the fine surface, mapped from the side each face looks toward.
 *
 * Anything wrong with a file (missing, cut short) just means no mesh: the cuboid model is drawn instead.
 */
public final class GunMesh {
    public static final String[] CLASSES = {"metal", "polymer", "wood", "rubber", "plain", "glass", "glow", "knurl", "check", "fabric", "rail"};
    public static final int GLASS = 5, GLOW = 6;
    static final int FLAG_TINT = 1, FLAG_SHINY = 2, FLAG_NOWEAR = 4;
    private static final RenderType[] TYPES = new RenderType[CLASSES.length];

    static RenderType type(int cls) {
        RenderType t = TYPES[cls];
        if (t == null) {
            ResourceLocation tex = new ResourceLocation(FactoryAscension.MODID, "textures/gunmesh/" + CLASSES[cls] + ".png");
            t = switch (cls) {
                case GLASS -> RenderType.entityTranslucentCull(tex);
                case GLOW -> RenderType.eyes(tex);
                default -> RenderType.entityCutout(tex);
            };
            TYPES[cls] = t;
        }
        return t;
    }

    // Per quad corner (4 per quad), grouped by class: model units
    public final float[] pos, nrm, uv;
    public final int[] col;          // baked ARGB
    public final byte[] flags;       // per corner: tint / glow / class
    public final byte[] cls;         // per corner: surface class
    public final int[] classStart = new int[CLASSES.length + 1];   // quad index where each class starts
    public final int quads;
    public final float minX, maxX, minY, maxY, minZ, maxZ;

    private GunMesh(float[] pos, float[] nrm, float[] uv, int[] col, byte[] flags, byte[] cls, int quads, float[] bounds) {
        this.pos = pos;
        this.nrm = nrm;
        this.uv = uv;
        this.col = col;
        this.flags = flags;
        this.cls = cls;
        this.quads = quads;
        minX = bounds[0]; minY = bounds[1]; minZ = bounds[2]; maxX = bounds[3]; maxY = bounds[4]; maxZ = bounds[5];
    }

    // ---- Loading ----
    private static final Map<String, GunMesh> CACHE = new HashMap<>();
    private static final GunMesh NONE = new GunMesh(new float[0], new float[0], new float[0], new int[0], new byte[0], new byte[0], 0, new float[6]);

    /** Forget every mesh (resources reloaded). */
    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    /** The mesh at assets/factoryascension/gunmesh/<path>.fam, or null if there isn't a good one. */
    @Nullable
    public static GunMesh get(String path) {
        GunMesh m;
        synchronized (CACHE) {
            m = CACHE.get(path);
        }
        if (m == null) {
            try {
                m = load(new ResourceLocation(FactoryAscension.MODID, "gunmesh/" + path + ".fam"));
            } catch (Throwable t) {
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Gun mesh {} not loaded: {}", path, t.toString());
                m = NONE;
            }
            if (m == null) m = NONE;
            synchronized (CACHE) {
                CACHE.put(path, m);
            }
        }
        return m == NONE ? null : m;
    }

    @Nullable
    private static GunMesh load(ResourceLocation rl) throws IOException {
        var res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) return null;
        byte[] raw;
        try (InputStream in = new GZIPInputStream(res.get().open())) {
            raw = in.readAllBytes();
        }
        return parse(raw);
    }

    static GunMesh parse(byte[] raw) {
        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        if (b.get() != 'F' || b.get() != 'A' || b.get() != 'M') throw new IllegalArgumentException("not a gun mesh");
        int version = b.get();
        if (version != '2' && version != '3') throw new IllegalArgumentException("not a gun mesh");
        boolean hasAo = version == '3';     // FAM3: baked ambient occlusion per vertex
        int nm = b.getShort() & 0xFFFF;
        int[] mCls = new int[nm], mRgb = new int[nm], mFlags = new int[nm];
        for (int i = 0; i < nm; i++) {
            int len = b.get() & 0xFF;
            b.position(b.position() + len);
            mCls[i] = Math.min(CLASSES.length - 1, b.get() & 0xFF);
            mRgb[i] = (b.get() & 0xFF) << 16 | (b.get() & 0xFF) << 8 | (b.get() & 0xFF);
            mFlags[i] = b.get() & 0xFF;
        }
        int nv = b.getInt();
        float[] vx = new float[nv * 3], vn = new float[nv * 3];
        int[] vm = new int[nv];
        float[] vw = new float[nv], vao = new float[nv];
        for (int i = 0; i < nv; i++) {
            vx[i * 3] = b.getShort() / 500f;
            vx[i * 3 + 1] = b.getShort() / 500f;
            vx[i * 3 + 2] = b.getShort() / 500f;
            float nx = b.get() / 127f, ny = b.get() / 127f, nz = b.get() / 127f;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-4f) { nx = 0; ny = 1; nz = 0; l = 1; }
            vn[i * 3] = nx / l;
            vn[i * 3 + 1] = ny / l;
            vn[i * 3 + 2] = nz / l;
            vm[i] = Math.min(nm - 1, b.get() & 0xFF);
            vw[i] = (b.get() & 0xFF) / 255f;
            vao[i] = hasAo ? (b.get() & 0xFF) / 255f : 1f;
        }
        int nq = b.getInt();
        boolean big = b.get() != 0;
        int[] q = new int[nq * 4];
        for (int i = 0; i < nq * 4; i++) q[i] = big ? b.getInt() : (b.getShort() & 0xFFFF);
        // Sort quads by class (by their first corner's material)
        int[] count = new int[CLASSES.length];
        for (int i = 0; i < nq; i++) count[mCls[vm[q[i * 4]]]]++;
        int[] start = new int[CLASSES.length + 1];
        for (int c = 0; c < CLASSES.length; c++) start[c + 1] = start[c] + count[c];
        int[] fill = start.clone();
        float[] pos = new float[nq * 12], nrm = new float[nq * 12], uv = new float[nq * 8];
        int[] col = new int[nq * 4];
        byte[] flags = new byte[nq * 4], cls = new byte[nq * 4];
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < nq; i++) {
            int c = mCls[vm[q[i * 4]]];
            int o = fill[c]++;
            // The quad's facing picks how its texture is laid on
            int a = q[i * 4], bb = q[i * 4 + 1], cc = q[i * 4 + 2], d = q[i * 4 + 3];
            float e1x = vx[cc * 3] - vx[a * 3], e1y = vx[cc * 3 + 1] - vx[a * 3 + 1], e1z = vx[cc * 3 + 2] - vx[a * 3 + 2];
            float e2x = vx[d * 3] - vx[bb * 3], e2y = vx[d * 3 + 1] - vx[bb * 3 + 1], e2z = vx[d * 3 + 2] - vx[bb * 3 + 2];
            float fx = Math.abs(e1y * e2z - e1z * e2y), fy = Math.abs(e1z * e2x - e1x * e2z), fz = Math.abs(e1x * e2y - e1y * e2x);
            int axis = fx >= fy && fx >= fz ? 0 : (fy >= fz ? 1 : 2);
            for (int k = 0; k < 4; k++) {
                int v = q[i * 4 + k];
                int dst = o * 4 + k;
                float x = vx[v * 3], y = vx[v * 3 + 1], z = vx[v * 3 + 2];
                pos[dst * 3] = x; pos[dst * 3 + 1] = y; pos[dst * 3 + 2] = z;
                float nx = vn[v * 3], ny = vn[v * 3 + 1], nz = vn[v * 3 + 2];
                nrm[dst * 3] = nx; nrm[dst * 3 + 1] = ny; nrm[dst * 3 + 2] = nz;
                bounds[0] = Math.min(bounds[0], x); bounds[1] = Math.min(bounds[1], y); bounds[2] = Math.min(bounds[2], z);
                bounds[3] = Math.max(bounds[3], x); bounds[4] = Math.max(bounds[4], y); bounds[5] = Math.max(bounds[5], z);
                int m = vm[v];
                float off = m * 0.371f;
                float u, w;
                switch (axis) {
                    case 0 -> { u = z / 8f; w = -y / 8f; }
                    case 1 -> { u = x / 8f; w = z / 8f; }
                    default -> { u = x / 8f; w = -y / 8f; }
                }
                uv[dst * 2] = u + off;
                uv[dst * 2 + 1] = w + off * 0.61f;
                col[dst] = bake(mRgb[m], mCls[m], mFlags[m], vw[v], nx, ny, nz, vao[v]);
                flags[dst] = (byte) mFlags[m];
                cls[dst] = (byte) mCls[m];
            }
        }
        GunMesh mesh = new GunMesh(pos, nrm, uv, col, flags, cls, nq, nq == 0 ? new float[6] : bounds);
        System.arraycopy(start, 0, mesh.classStart, 0, start.length);
        return mesh;
    }

    /** Colour + edge wear + a soft studio highlight, scaled for the detail texture's average (0.88). */
    static int bake(int rgb, int cls, int flags, float wear, float nx, float ny, float nz, float ao) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        if (cls == GLOW || cls == GLASS) return 0xFF000000 | rgb;
        boolean metal = cls == 0 || cls == 7 || cls == 10;
        if ((flags & FLAG_NOWEAR) == 0 && wear > 0) {
            // Worn edges: bare steel shows on metal, polymer and wood go lighter
            float k = wear * (metal ? 0.55f : 0.22f);
            float tr = metal ? 0.62f : r * 1.35f + 0.04f, tg = metal ? 0.63f : g * 1.35f + 0.04f, tb = metal ? 0.65f : b * 1.35f + 0.04f;
            r += (tr - r) * k; g += (tg - g) * k; b += (tb - b) * k;
        }
        // A light from above-front-right, as in a studio: a broad sheen everywhere, a tighter one on polished metal
        float hx = 0.25f, hy = 0.88f, hz = 0.4f;
        float h = Math.max(0, nx * hx + ny * hy + nz * hz);
        float sheen = (float) Math.pow(h, (flags & FLAG_SHINY) != 0 ? 22 : 8) * ((flags & FLAG_SHINY) != 0 ? 0.32f : (metal ? 0.14f : 0.06f));
        float top = 0.94f + 0.06f * ny;
        // Baked occlusion: enclosed spots darker, and no sheen down in the crevices
        float occ = 0.38f + 0.62f * ao * ao;
        sheen *= ao;
        r = r * top * occ + sheen; g = g * top * occ + sheen; b = b * top * occ + sheen;
        float s = 1 / 0.88f;
        return 0xFF000000 | clamp(r * s) << 16 | clamp(g * s) << 8 | clamp(b * s);
    }

    static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255)));
    }

    // ---- Drawing ----
    /** What a skin does to the mesh as it's drawn (client/gun/GunSkins). */
    public interface Shader {
        /** The colour of corner i (ARGB in, ARGB out); return with alpha 0 to keep it. */
        int colour(GunMesh m, int i, int base, float time);

        /** How bright corner i glows by itself, 0..1. */
        default float glow(GunMesh m, int i, float time) {
            return 0;
        }

        /** Move corner i (model units; out[0..2] start at its position). */
        default void move(GunMesh m, int i, float time, float[] out) {
        }

        default boolean moves() {
            return false;
        }

        /** Scroll the detail texture (u, v per second). */
        default float scrollU() {
            return 0;
        }
    }

    /**
     * Draw it. The pose is the item's (0..1 = one block, the model's units / 16). `tint` (RGB, or -1) colours the
     * attachment-finish parts; `shader` (or null) is a skin.
     */
    /** A skin's colours, glow and moved corners for this mesh, worked out ahead (skins cost a lot per corner). */
    static final class SkinCache {
        int[] col;
        float[] glow;
        float[] moved;
        float t = -1;
        boolean still;         // the same at any time: worked out once
        boolean checked;
    }

    private final java.util.IdentityHashMap<Shader, SkinCache> skinCache = new java.util.IdentityHashMap<>();
    /** Animated skins are worked out this many times a second (smooth enough, and a fraction of the cost). */
    static final float SKIN_RATE = 20f;

    private SkinCache skin(Shader shader, float time) {
        SkinCache c = skinCache.computeIfAbsent(shader, k -> new SkinCache());
        int n = pos.length / 3;
        boolean stale = c.col == null || (!c.still && (time - c.t >= 1 / SKIN_RATE || time < c.t));
        if (!stale) return c;
        if (c.col == null) {
            c.col = new int[n];
            c.glow = new float[n];
        }
        boolean moves = shader.moves();
        if (moves && c.moved == null) c.moved = new float[n * 3];
        float[] p = new float[3];
        for (int i = 0; i < n; i++) {
            int ci = cls[i];
            if (ci != GLASS && ci != GLOW && ci != 4) {
                c.col[i] = shader.colour(this, i, col[i], time);
                c.glow[i] = shader.glow(this, i, time);
            }
            if (moves) {
                p[0] = pos[i * 3]; p[1] = pos[i * 3 + 1]; p[2] = pos[i * 3 + 2];
                shader.move(this, i, time, p);
                c.moved[i * 3] = p[0]; c.moved[i * 3 + 1] = p[1]; c.moved[i * 3 + 2] = p[2];
            }
        }
        // Once: is this skin the same at another time? Then it never needs working out again
        if (!c.checked) {
            c.checked = true;
            boolean same = !moves && shader.scrollU() == 0;
            for (int i = 0; same && i < n; i += Math.max(1, n / 400)) {
                int ci = cls[i];
                if (ci == GLASS || ci == GLOW || ci == 4) continue;
                if (shader.colour(this, i, col[i], time + 1.7f) != c.col[i] || shader.glow(this, i, time + 1.7f) != c.glow[i]) same = false;
            }
            c.still = same;
        }
        c.t = time;
        return c;
    }

    public void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int tint, @Nullable Shader shader, float time) {
        SkinCache sc = shader != null ? skin(shader, time) : null;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[] p = new float[3];
        float tr = 1, tg = 1, tb = 1;
        if (tint >= 0) {
            tr = (tint >> 16 & 255) / 255f;
            tg = (tint >> 8 & 255) / 255f;
            tb = (tint & 255) / 255f;
        }
        float scroll = shader != null ? shader.scrollU() * time : 0;
        boolean moves = shader != null && shader.moves();
        for (int c = 0; c < CLASSES.length; c++) {
            int q0 = classStart[c], q1 = classStart[c + 1];
            if (q0 == q1) continue;
            VertexConsumer vc = buffers.getBuffer(type(c));
            for (int i = q0 * 4; i < q1 * 4; i++) {
                float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
                if (moves && sc.moved != null) {
                    x = sc.moved[i * 3]; y = sc.moved[i * 3 + 1]; z = sc.moved[i * 3 + 2];
                }
                x /= 16f; y /= 16f; z /= 16f;
                float wx = m.m00() * x + m.m10() * y + m.m20() * z + m.m30();
                float wy = m.m01() * x + m.m11() * y + m.m21() * z + m.m31();
                float wz = m.m02() * x + m.m12() * y + m.m22() * z + m.m32();
                float nx = nrm[i * 3], ny = nrm[i * 3 + 1], nz = nrm[i * 3 + 2];
                float vx = n.m00() * nx + n.m10() * ny + n.m20() * nz;
                float vy = n.m01() * nx + n.m11() * ny + n.m21() * nz;
                float vz = n.m02() * nx + n.m12() * ny + n.m22() * nz;
                int argb = col[i];
                int li = light;
                if (sc != null && c != GLASS && c != GLOW && c != 4) {
                    int s = sc.col[i];
                    if ((s >>> 24) != 0) argb = s;
                    float gl = sc.glow[i];
                    if (gl > 0) {
                        int block = Math.max(light & 0xFFFF, Math.round(gl * 15) << 4);
                        li = (light & 0xFFFF0000) | block;
                    }
                }
                int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                if ((flags[i] & FLAG_TINT) != 0 && tint >= 0) {
                    r = Math.round(r * tr);
                    g = Math.round(g * tg);
                    b = Math.round(b * tb);
                }
                int a = c == GLASS ? 55 : 255;   // coated glass: you see through it
                vc.vertex(wx, wy, wz).color(r, g, b, a).uv(uv[i * 2] + scroll, uv[i * 2 + 1]).overlayCoords(overlay)
                    .uv2(c == GLOW ? 0xF000F0 : li).normal(vx, vy, vz).endVertex();
            }
        }
    }
}
