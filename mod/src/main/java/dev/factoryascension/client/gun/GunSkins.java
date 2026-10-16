package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.gun.Skins;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * How each skin (gun/Skins) looks. A skin is two things:
 *  - a Shader over the gun's mesh: every corner's colour worked out each frame from where it is on the gun (3D noise,
 *    so patterns wrap around the real shape instead of being pasted on), how it faces, and the time, so they move:
 *    magma creeping through obsidian, waves rolling down Black Water, a shine running along gold, pulses on circuits.
 *    Some also move the surface itself (water ripples, rust flaking up).
 *  - extra 3D parts for some: snow lumps on every ledge and flakes drifting off, torn cloth strips and ghillie strands
 *    that swing as the gun moves, ice crystals, grass tufts, slime and blood dripping, flames, falling petals.
 * Everything here is drawing only; a skin changes nothing about how the gun shoots.
 */
public final class GunSkins {
    private GunSkins() {
    }

    public static final class Skin {
        public final String id;
        public final GunMesh.Shader shader;
        public final int iconTint;               // the far / icon model is tinted this
        public final boolean coversAttachments;
        final Extra extra;

        Skin(String id, GunMesh.Shader shader, int iconTint, boolean coversAttachments, Extra extra) {
            this.id = id;
            this.shader = shader;
            this.iconTint = iconTint;
            this.coversAttachments = coversAttachments;
            this.extra = extra;
        }
    }

    enum Extra { NONE, SNOW, TORN, FROST, MOSS, SLIME, BLOOD, PETALS, FLAMES, GHILLIE, EMBERS }

    static final Map<String, Skin> SKINS = new HashMap<>();

    @Nullable
    public static Skin of(ItemStack gun) {
        String id = Skins.of(gun);
        return id.isEmpty() ? null : SKINS.get(id);
    }

    @Nullable
    public static Skin byId(String id) {
        return SKINS.get(id);
    }

    // ---- Noise and colour helpers ----
    static float hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / 16777215f;
    }

    static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    /** Value noise, 0..1. */
    static float noise(float x, float y, float z) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        float tx = smooth(x - x0), ty = smooth(y - y0), tz = smooth(z - z0);
        float c000 = hash(x0, y0, z0), c100 = hash(x0 + 1, y0, z0), c010 = hash(x0, y0 + 1, z0), c110 = hash(x0 + 1, y0 + 1, z0);
        float c001 = hash(x0, y0, z0 + 1), c101 = hash(x0 + 1, y0, z0 + 1), c011 = hash(x0, y0 + 1, z0 + 1), c111 = hash(x0 + 1, y0 + 1, z0 + 1);
        float a = c000 + (c100 - c000) * tx, b = c010 + (c110 - c010) * tx;
        float c = c001 + (c101 - c001) * tx, d = c011 + (c111 - c011) * tx;
        float e = a + (b - a) * ty, f = c + (d - c) * ty;
        return e + (f - e) * tz;
    }

    static float fbm(float x, float y, float z, int oct) {
        float s = 0, amp = 0.5f, tot = 0;
        for (int i = 0; i < oct; i++) {
            s += noise(x, y, z) * amp;
            tot += amp;
            x *= 2.03f; y *= 2.03f; z *= 2.03f;
            amp *= 0.5f;
        }
        // Stretched to fill 0..1 (an average of octaves bunches up around the middle)
        return Math.max(0, Math.min(1, 0.5f + (s / tot - 0.5f) * 2.6f));
    }

    /** Ridges: bright thin lines where the noise crosses its middle (veins, cracks). */
    static float ridge(float x, float y, float z, float width) {
        float n = Math.abs(fbm(x, y, z, 3) - 0.5f) * 2;   // 0 on the middle line
        return Math.max(0, 1 - n / width);
    }

    static int rgb(float r, float g, float b) {
        return 0xFF000000 | GunMesh.clamp(r) << 16 | GunMesh.clamp(g) << 8 | GunMesh.clamp(b);
    }

    static int hex(int c) {
        return 0xFF000000 | c;
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    static int scale(int c, float k) {
        return rgb((c >> 16 & 255) / 255f * k, (c >> 8 & 255) / 255f * k, (c & 255) / 255f * k);
    }

    static int add(int c, float k) {
        return rgb((c >> 16 & 255) / 255f + k, (c >> 8 & 255) / 255f + k, (c & 255) / 255f + k);
    }

    /** The studio highlight, as the mesh bakes it, for skins that replace the colour. */
    static float sheen(GunMesh m, int i, float power, float k) {
        float h = Math.max(0, m.nrm[i * 3] * 0.25f + m.nrm[i * 3 + 1] * 0.88f + m.nrm[i * 3 + 2] * 0.4f);
        return (float) Math.pow(h, power) * k;
    }

    static float px(GunMesh m, int i) {
        return m.pos[i * 3];
    }

    static float py(GunMesh m, int i) {
        return m.pos[i * 3 + 1];
    }

    static float pz(GunMesh m, int i) {
        return m.pos[i * 3 + 2];
    }

    static float ny(GunMesh m, int i) {
        return m.nrm[i * 3 + 1];
    }

    /** How lit the base is (keeps the gun's own light and dark parts under a skin). */
    static float lum(int c) {
        return ((c >> 16 & 255) * 0.3f + (c >> 8 & 255) * 0.59f + (c & 255) * 0.11f) / 255f;
    }

    interface Colour {
        int at(GunMesh m, int i, int base, float t);
    }

    interface Glow {
        float at(GunMesh m, int i, float t);
    }

    static GunMesh.Shader shader(Colour c, Glow g, float scroll) {
        return new GunMesh.Shader() {
            @Override
            public int colour(GunMesh m, int i, int base, float time) {
                return c.at(m, i, base, time);
            }

            @Override
            public float glow(GunMesh m, int i, float time) {
                return g == null ? 0 : g.at(m, i, time);
            }

            @Override
            public float scrollU() {
                return scroll;
            }
        };
    }

    static void skin(String id, int iconTint, boolean atts, Extra extra, GunMesh.Shader s) {
        SKINS.put(id, new Skin(id, s, iconTint, atts, extra));
    }

    static {
        // Obsidian: black glass with a cold sheen; magma veins that slowly creep and pulse
        skin("obsidian", 0x1A1614, true, Extra.EMBERS, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.22f, y = py(m, i) * 0.22f, z = pz(m, i) * 0.22f;
            float v = ridge(x + t * 0.05f, y, z - t * 0.03f, 0.12f);
            float pulse = 0.75f + 0.25f * (float) Math.sin(t * 1.7f + x * 3);
            int glass = rgb(0.07f + sheen(m, i, 30, 0.55f), 0.065f + sheen(m, i, 30, 0.55f), 0.07f + sheen(m, i, 30, 0.6f));
            return mix(glass, rgb(1.0f, 0.42f * pulse, 0.08f), v * pulse);
        }, (m, i, t) -> ridge(px(m, i) * 0.22f + t * 0.05f, py(m, i) * 0.22f, pz(m, i) * 0.22f - t * 0.03f, 0.12f) * 0.9f, 0));
        // Snow: white on top, blue-grey in the shade, glittering
        skin("snow", 0xE8EEF4, true, Extra.SNOW, shader((m, i, b, t) -> {
            float up = Math.max(0, ny(m, i));
            float n = fbm(px(m, i) * 0.5f, py(m, i) * 0.5f, pz(m, i) * 0.5f, 3);
            int under = mix(b, hex(0x9AAABB), 0.55f + 0.2f * n);
            int c = mix(under, hex(0xF4F8FC), smooth(Math.min(1, up * 1.4f + n * 0.3f)));
            float glint = hash(i, (int) (t * 3), 7) > 0.985f ? 0.5f : 0;
            return add(c, glint);
        }, null, 0));
        // Black Water: near-black blue, wave crests rolling from muzzle to stock, foam on the crests
        skin("black_water", 0x0C1820, true, Extra.NONE, new GunMesh.Shader() {
            float wave(GunMesh m, int i, float t) {
                float x = px(m, i), y = py(m, i), z = pz(m, i);
                float w = (float) Math.sin(x * 0.9f + t * 2.6f + fbm(x * 0.2f, y * 0.3f, z * 0.3f, 2) * 4);
                return w * 0.5f + 0.5f;
            }

            @Override
            public int colour(GunMesh m, int i, int base, float t) {
                float w = wave(m, i, t);
                float foam = smooth(Math.max(0, (w - 0.86f) / 0.14f)) * (0.6f + 0.4f * noise(px(m, i) * 2, py(m, i) * 2, t));
                int deep = mix(hex(0x05090E), hex(0x0E2A36), w);
                return add(mix(deep, hex(0xB8D8DC), foam), sheen(m, i, 40, 0.5f));
            }

            @Override
            public boolean moves() {
                return true;
            }

            @Override
            public void move(GunMesh m, int i, float t, float[] p) {
                float d = (wave(m, i, t) - 0.5f) * 0.09f;
                p[0] += m.nrm[i * 3] * d;
                p[1] += m.nrm[i * 3 + 1] * d;
                p[2] += m.nrm[i * 3 + 2] * d;
            }
        });
        // Torn: olive / tan / brown camo wrap with rips showing the gun underneath
        skin("torn", 0x6A6448, false, Extra.TORN, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.35f, y = py(m, i) * 0.35f, z = pz(m, i) * 0.35f;
            float n = fbm(x, y, z, 3);
            int camo = n < 0.42f ? hex(0x4E5434) : n < 0.55f ? hex(0x8A7A52) : hex(0x3A3022);
            float rip = fbm(x * 2.5f + 11, y * 2.5f, z * 2.5f, 3);
            if (rip > 0.64f) return b;                                          // torn through: the gun shows
            if (rip > 0.6f) return scale(camo, 0.55f);                          // frayed edge
            return scale(camo, 0.85f + 0.3f * noise(x * 9, y * 9, z * 9));
        }, null, 0));
        // Gold leaf: gold, with a bright band of shine sliding along it
        skin("gold_leaf", 0xD8A838, true, Extra.NONE, shader((m, i, b, t) -> {
            float band = (float) Math.pow(Math.max(0, Math.sin(px(m, i) * 0.25f - t * 1.6f)), 24);
            float n = noise(px(m, i) * 1.5f, py(m, i) * 1.5f, pz(m, i) * 1.5f);
            int gold = mix(hex(0x9A6A1A), hex(0xF0C450), 0.45f + 0.35f * n + 0.4f * lum(b));
            return add(gold, band * 0.5f + sheen(m, i, 18, 0.35f));
        }, null, 0));
        // Lava: a dark crust breaking into glowing rivers that flow back along the gun
        skin("lava", 0xC04010, true, Extra.EMBERS, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.3f - t * 0.25f, y = py(m, i) * 0.3f, z = pz(m, i) * 0.3f;
            float n = fbm(x, y, z, 4);
            float hot = smooth(Math.max(0, Math.min(1, (n - 0.45f) * 4)));
            int crust = mix(hex(0x1E0E08), hex(0x3A1A0E), noise(x * 6, y * 6, z * 6));
            return mix(crust, mix(hex(0xE04808), hex(0xFFD040), hot * hot), hot);
        }, (m, i, t) -> {
            float n = fbm(px(m, i) * 0.3f - t * 0.25f, py(m, i) * 0.3f, pz(m, i) * 0.3f, 4);
            return smooth(Math.max(0, Math.min(1, (n - 0.45f) * 4)));
        }, 0));
        // Frost: pale ice, cracked, shimmering
        skin("frost", 0xBFE4F0, true, Extra.FROST, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.4f, y = py(m, i) * 0.4f, z = pz(m, i) * 0.4f;
            float crack = ridge(x, y, z, 0.08f);
            float shimmer = 0.06f * (float) Math.sin(t * 2 + x * 4 + z * 3);
            int ice = mix(hex(0x8CC8DC), hex(0xE6F6FC), fbm(x, y, z, 3) + shimmer);
            return add(mix(ice, hex(0xFFFFFF), crack), sheen(m, i, 30, 0.4f));
        }, null, 0));
        // Neon grid: dark, with glowing cyan and lime lines scrolling along it
        skin("neon_grid", 0x182024, true, Extra.NONE, shader((m, i, b, t) -> {
            float gx = Math.abs(((px(m, i) - t * 2) % 2 + 2) % 2 - 1), gy = Math.abs(((py(m, i) + pz(m, i)) % 2 + 2) % 2 - 1);
            float line = Math.max(smooth(Math.max(0, (gx - 0.88f) / 0.12f)), smooth(Math.max(0, (gy - 0.9f) / 0.1f)));
            int lc = mix(hex(0x30F0FF), hex(0x9CFF40), 0.5f + 0.5f * (float) Math.sin(px(m, i) * 0.2f + t));
            return mix(rgb(0.08f + sheen(m, i, 20, 0.2f), 0.09f, 0.1f), lc, line);
        }, (m, i, t) -> {
            float gx = Math.abs(((px(m, i) - t * 2) % 2 + 2) % 2 - 1), gy = Math.abs(((py(m, i) + pz(m, i)) % 2 + 2) % 2 - 1);
            return Math.max(smooth(Math.max(0, (gx - 0.88f) / 0.12f)), smooth(Math.max(0, (gy - 0.9f) / 0.1f)));
        }, 0));
        // Digital woodland: square pixels of four greens and browns
        skin("digital", 0x4A5236, false, Extra.NONE, shader((m, i, b, t) -> {
            int qx = (int) Math.floor(px(m, i) / 0.6f), qy = (int) Math.floor(py(m, i) / 0.6f), qz = (int) Math.floor(pz(m, i) / 0.6f);
            float n = fbm(qx * 0.35f, qy * 0.35f, qz * 0.35f, 2) + (hash(qx, qy, qz) - 0.5f) * 0.25f;
            int[] pal = {0x2E3A22, 0x4E5A34, 0x6E6A44, 0x8A7C56};
            return scale(hex(pal[Math.max(0, Math.min(3, (int) (n * 4.4f - 0.4f)))]), 0.9f + 0.25f * lum(b));
        }, null, 0));
        skin("arctic", 0xC8CCD0, false, Extra.NONE, shader((m, i, b, t) -> {
            float n = fbm(px(m, i) * 0.3f, py(m, i) * 0.3f, pz(m, i) * 0.3f, 3);
            int c = n < 0.42f ? hex(0xE8ECEE) : n < 0.56f ? hex(0xA8AEB2) : hex(0x6A7074);
            return add(c, sheen(m, i, 12, 0.1f));
        }, null, 0));
        skin("tiger", 0x6A6030, false, Extra.NONE, shader((m, i, b, t) -> {
            float s = (float) Math.sin(px(m, i) * 1.1f + fbm(px(m, i) * 0.2f, py(m, i) * 0.4f, pz(m, i) * 0.4f, 3) * 7);
            return s > 0.45f ? hex(0x1E1E16) : s > 0.1f ? hex(0x4C5228) : hex(0x7A7038);
        }, null, 0));
        // Carbon fibre: a weave whose sheen shifts with the light
        skin("carbon", 0x262628, true, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i) * 3, y = (py(m, i) + pz(m, i)) * 3;
            boolean warp = (((int) Math.floor(x) + (int) Math.floor(y)) & 1) == 0;
            float f = warp ? (float) Math.pow(Math.sin((x % 1) * Math.PI), 2) : (float) Math.pow(Math.sin((y % 1) * Math.PI), 2);
            float sh = sheen(m, i, 10, 0.6f) * (warp ? 1 : 0.4f) * (0.7f + 0.3f * (float) Math.sin(t * 0.8f));
            return rgb(0.09f + 0.06f * f + sh, 0.09f + 0.06f * f + sh, 0.1f + 0.06f * f + sh);
        }, null, 0));
        // Damascus: folded steel layers
        skin("damascus", 0x8A8C90, true, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i), y = py(m, i), z = pz(m, i);
            float w = (float) Math.sin((y + z * 0.4f) * 6 + fbm(x * 0.3f, y * 0.3f, z * 0.3f, 3) * 14);
            return add(mix(hex(0x3A3C40), hex(0xB4B8BC), w * 0.5f + 0.5f), sheen(m, i, 24, 0.35f));
        }, null, 0));
        // Rust: orange-brown patches, pitted, flaking up off the surface
        skin("rust", 0x8A4A24, true, Extra.NONE, new GunMesh.Shader() {
            float r(GunMesh m, int i) {
                return fbm(px(m, i) * 0.45f, py(m, i) * 0.45f, pz(m, i) * 0.45f, 4);
            }

            @Override
            public int colour(GunMesh m, int i, int base, float t) {
                float n = r(m, i);
                if (n < 0.4f) return 0;                                               // clean metal there
                int rust = mix(hex(0x6A2E12), hex(0xB4602A), noise(px(m, i) * 4, py(m, i) * 4, pz(m, i) * 4));
                return mix(base, rust, smooth(Math.min(1, (n - 0.4f) * 6)));
            }

            @Override
            public boolean moves() {
                return true;
            }

            @Override
            public void move(GunMesh m, int i, float t, float[] p) {
                float n = r(m, i);
                if (n > 0.62f) {
                    float d = (n - 0.62f) * 0.25f;
                    p[0] += m.nrm[i * 3] * d;
                    p[1] += m.nrm[i * 3 + 1] * d;
                    p[2] += m.nrm[i * 3 + 2] * d;
                }
            }
        });
        // Overgrown: moss on top, damp dark below
        skin("moss", 0x4A6A2A, false, Extra.MOSS, shader((m, i, b, t) -> {
            float up = Math.max(0, ny(m, i));
            float n = fbm(px(m, i) * 0.6f, py(m, i) * 0.6f, pz(m, i) * 0.6f, 3);
            int moss = mix(hex(0x2E4A18), hex(0x6A8A2E), n);
            return mix(scale(b, 0.8f), moss, smooth(Math.min(1, up * 1.2f + n * 0.6f - 0.15f)));
        }, null, 0));
        skin("bone", 0xD8CCB0, false, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.5f, y = py(m, i) * 0.5f, z = pz(m, i) * 0.5f;
            int ivory = mix(hex(0xB8A888), hex(0xEEE4CC), fbm(x, y, z, 3));
            return mix(add(ivory, sheen(m, i, 14, 0.15f)), hex(0x4A3E2A), ridge(x * 1.5f, y * 1.5f, z * 1.5f, 0.05f));
        }, null, 0));
        // Galaxy: deep blue space, teal and amber nebulae drifting, stars twinkling
        skin("galaxy", 0x0E1838, true, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.18f, y = py(m, i) * 0.18f, z = pz(m, i) * 0.18f;
            float n1 = fbm(x + t * 0.03f, y, z, 4), n2 = fbm(x + 7, y - t * 0.02f, z + 3, 4);
            int c = mix(hex(0x060A1A), hex(0x1C3C6A), n1);
            c = mix(c, hex(0x2EA8A0), smooth(Math.max(0, (n2 - 0.55f) * 3)));
            c = mix(c, hex(0xE89A48), smooth(Math.max(0, (n1 - 0.68f) * 4)) * 0.7f);
            float star = hash(i * 31, 3, 5) > 0.975f ? 0.6f + 0.4f * (float) Math.sin(t * 4 + i) : 0;
            return add(c, star);
        }, (m, i, t) -> hash(i * 31, 3, 5) > 0.975f ? 0.8f : 0.15f, 0));
        // Toxic: acid green slime, glowing, oozing
        skin("toxic", 0x5AC028, true, Extra.SLIME, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.4f, y = py(m, i) * 0.4f + t * 0.3f, z = pz(m, i) * 0.4f;
            float n = fbm(x, y, z, 3);
            float bub = noise(x * 5, y * 5, z * 5) > 0.82f ? 0.3f : 0;
            return add(mix(hex(0x1E3A0C), hex(0x8CF02E), n), bub);
        }, (m, i, t) -> 0.25f + 0.35f * fbm(px(m, i) * 0.4f, py(m, i) * 0.4f + t * 0.3f, pz(m, i) * 0.4f, 3), 0));
        skin("blood", 0x5A0C0C, true, Extra.BLOOD, shader((m, i, b, t) -> {
            float n = fbm(px(m, i) * 0.5f, py(m, i) * 0.5f, pz(m, i) * 0.5f, 3);
            return add(mix(hex(0x2A0404), hex(0x8A1010), n), sheen(m, i, 30, 0.35f));
        }, null, 0));
        // Chrome: reflects a sky above and the ground below, with a highlight that drifts
        skin("chrome", 0xC8CCD0, true, Extra.NONE, shader((m, i, b, t) -> {
            float ny = ny(m, i), nz = m.nrm[i * 3 + 2];
            int sky = mix(hex(0x8AB0D8), hex(0xF0F4FA), Math.max(0, ny));
            int ground = mix(hex(0x5A5048), hex(0x2A2622), Math.max(0, -ny));
            int c = ny >= 0 ? sky : ground;
            float hl = (float) Math.pow(Math.max(0, Math.sin(nz * 2.5f + t * 0.4f)), 30);
            return add(mix(c, hex(0x9A9EA4), 0.25f), hl * 0.6f);
        }, null, 0));
        // Circuit: green board, copper traces, light pulses running along them
        skin("circuit", 0x1A3A24, true, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i), y = py(m, i) + pz(m, i);
            float tx = Math.abs(((x * 1.3f) % 1 + 1) % 1 - 0.5f), ty = Math.abs(((y * 1.7f) % 1 + 1) % 1 - 0.5f);
            boolean trace = (tx > 0.43f && hash((int) Math.floor(y * 1.7f), 1, 2) > 0.4f) || (ty > 0.44f && hash((int) Math.floor(x * 1.3f), 2, 3) > 0.5f);
            if (!trace) return mix(hex(0x0E2414), hex(0x1C3C22), noise(x * 3, y * 3, 0));
            float pulse = (float) Math.pow(Math.max(0, Math.sin(x * 0.8f - t * 5 + y)), 16);
            return mix(hex(0xB07838), hex(0xA0FFD0), pulse);
        }, (m, i, t) -> (float) Math.pow(Math.max(0, Math.sin(px(m, i) * 0.8f - t * 5 + py(m, i) + pz(m, i))), 16) * 0.9f, 0));
        skin("blossom", 0xF0B8C8, false, Extra.PETALS, shader((m, i, b, t) -> {
            float n = fbm(px(m, i) * 0.6f, py(m, i) * 0.6f, pz(m, i) * 0.6f, 3);
            int c = mix(hex(0xF4D8E0), hex(0xE07898), smooth(Math.max(0, (n - 0.45f) * 4)));
            return add(c, sheen(m, i, 12, 0.12f));
        }, null, 0));
        // Inferno: charred black with glowing orange where the flames lick
        skin("inferno", 0x3A1608, true, Extra.FLAMES, shader((m, i, b, t) -> {
            float x = px(m, i) * 0.5f, y = py(m, i) * 0.5f - t * 0.9f, z = pz(m, i) * 0.5f;
            float n = fbm(x, y, z, 3);
            float heat = smooth(Math.max(0, Math.min(1, (n - 0.5f) * 3 + Math.max(0, ny(m, i)) * 0.4f)));
            return mix(hex(0x140A06), mix(hex(0xC03A08), hex(0xFFB030), heat), heat);
        }, (m, i, t) -> smooth(Math.max(0, Math.min(1, (fbm(px(m, i) * 0.5f, py(m, i) * 0.5f - t * 0.9f, pz(m, i) * 0.5f, 3) - 0.5f) * 3))), 0));
        // Glitch: blocks of the wrong colour flickering in and out
        skin("glitch", 0x40C8B8, true, Extra.NONE, shader((m, i, b, t) -> {
            int frame = (int) (t * 12);
            int bx = (int) Math.floor(px(m, i) / 1.5f), by = (int) Math.floor(py(m, i) / 0.7f);
            float h = hash(bx, by, frame);
            if (h > 0.9f) return hex(h > 0.96f ? 0x40FFE0 : 0xFF4060);
            if (h > 0.85f) return hex(0xF0F0F0);
            if (hash(by, frame, 9) > 0.93f) return rgb(lum(b) * 0.2f, lum(b) * 1.4f, lum(b) * 1.4f);
            return 0;
        }, (m, i, t) -> hash((int) Math.floor(px(m, i) / 1.5f), (int) Math.floor(py(m, i) / 0.7f), (int) (t * 12)) > 0.85f ? 0.9f : 0, 0));
        skin("hazard", 0xD8B020, false, Extra.NONE, shader((m, i, b, t) -> {
            float s = ((px(m, i) + py(m, i)) * 0.5f % 1 + 1) % 1;
            return add(s < 0.5f ? hex(0xE8B818) : hex(0x1A1A18), sheen(m, i, 14, 0.15f));
        }, null, 0));
        skin("ghillie", 0x5A6038, false, Extra.GHILLIE, shader((m, i, b, t) -> {
            float n = fbm(px(m, i) * 0.8f, py(m, i) * 0.8f, pz(m, i) * 0.8f, 3);
            return mix(hex(0x3E4428), hex(0x7A7448), n);
        }, null, 0));
        // Ember: dark with coals that breathe, brighter just after a shot
        skin("ember", 0x2A1A14, true, Extra.EMBERS, shader((m, i, b, t) -> {
            float n = fbm(px(m, i) * 0.7f, py(m, i) * 0.7f, pz(m, i) * 0.7f, 3);
            float heat = smooth(Math.max(0, (n - 0.55f) * 4)) * (0.6f + 0.4f * (float) Math.sin(t * 2 + n * 9) + GunAnim.heat());
            return mix(scale(b, 0.6f), mix(hex(0xA02808), hex(0xFFA040), Math.min(1, heat)), Math.min(1, heat));
        }, (m, i, t) -> smooth(Math.max(0, (fbm(px(m, i) * 0.7f, py(m, i) * 0.7f, pz(m, i) * 0.7f, 3) - 0.55f) * 4)) * (0.5f + GunAnim.heat()), 0));
        // Aurora: green and teal ribbons rippling along a night-dark finish
        skin("aurora", 0x1A3A3A, true, Extra.NONE, shader((m, i, b, t) -> {
            float x = px(m, i), y = py(m, i), z = pz(m, i);
            float rib = (float) Math.sin(y * 1.4f + Math.sin(x * 0.35f + t * 0.9f) * 2.2f + z * 0.3f);
            float a = smooth(Math.max(0, rib));
            int c = mix(hex(0x0A1418), mix(hex(0x30E080), hex(0x40C0E0), 0.5f + 0.5f * (float) Math.sin(x * 0.2f - t * 0.5f)), a * 0.85f);
            return add(c, sheen(m, i, 30, 0.25f));
        }, (m, i, t) -> smooth(Math.max(0, (float) Math.sin(py(m, i) * 1.4f + Math.sin(px(m, i) * 0.35f + t * 0.9f) * 2.2f + pz(m, i) * 0.3f))) * 0.6f, 0));
    }

    // ---- Extra 3D parts ----
    /** Points on the mesh facing up (or down), picked once per gun and skin. */
    private static final Map<String, float[]> SPOTS = new HashMap<>();

    static float[] spots(GunMesh mesh, String key, boolean up, int count, long seed) {
        return SPOTS.computeIfAbsent(key, k -> {
            Random r = new Random(seed);
            int corners = mesh.quads * 4;
            float[] out = new float[count * 3];
            int got = 0;
            for (int tries = 0; tries < count * 400 && got < count; tries++) {
                int i = r.nextInt(Math.max(1, corners));
                if (corners == 0) break;
                int c = mesh.cls[i];
                if (c == GunMesh.GLASS || c == GunMesh.GLOW || c == 4) continue;
                float ny = mesh.nrm[i * 3 + 1];
                if (up ? ny < 0.75f : ny > -0.7f) continue;
                out[got * 3] = mesh.pos[i * 3];
                out[got * 3 + 1] = mesh.pos[i * 3 + 1];
                out[got * 3 + 2] = mesh.pos[i * 3 + 2];
                got++;
            }
            return java.util.Arrays.copyOf(out, got * 3);
        });
    }

    /** Draws quads in model units through the pose. */
    static final class Emit {
        final Matrix4f m;
        final Matrix3f n;
        final VertexConsumer vc;
        final int light, overlay;

        Emit(PoseStack pose, VertexConsumer vc, int light, int overlay) {
            this.m = pose.last().pose();
            this.n = pose.last().normal();
            this.vc = vc;
            this.light = light;
            this.overlay = overlay;
        }

        void v(float x, float y, float z, int argb, float u, float w, float nx, float ny, float nz, int li) {
            x /= 16f; y /= 16f; z /= 16f;
            float wx = m.m00() * x + m.m10() * y + m.m20() * z + m.m30();
            float wy = m.m01() * x + m.m11() * y + m.m21() * z + m.m31();
            float wz = m.m02() * x + m.m12() * y + m.m22() * z + m.m32();
            float vx = n.m00() * nx + n.m10() * ny + n.m20() * nz, vy = n.m01() * nx + n.m11() * ny + n.m21() * nz, vz = n.m02() * nx + n.m12() * ny + n.m22() * nz;
            vc.vertex(wx, wy, wz).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).uv(u, w).overlayCoords(overlay).uv2(li).normal(vx, vy, vz).endVertex();
        }

        /** A flat quad, both sides (two quads). */
        void card(float[] a, float[] b, float[] c, float[] d, int argb, int li) {
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], wx = d[0] - a[0], wy = d[1] - a[1], wz = d[2] - a[2];
            float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-6f) return;
            nx /= l; ny /= l; nz /= l;
            v(a[0], a[1], a[2], argb, 0, 0, nx, ny, nz, li);
            v(b[0], b[1], b[2], argb, 1, 0, nx, ny, nz, li);
            v(c[0], c[1], c[2], argb, 1, 1, nx, ny, nz, li);
            v(d[0], d[1], d[2], argb, 0, 1, nx, ny, nz, li);
            v(d[0], d[1], d[2], argb, 0, 1, -nx, -ny, -nz, li);
            v(c[0], c[1], c[2], argb, 1, 1, -nx, -ny, -nz, li);
            v(b[0], b[1], b[2], argb, 1, 0, -nx, -ny, -nz, li);
            v(a[0], a[1], a[2], argb, 0, 0, -nx, -ny, -nz, li);
        }

        /** A low dome (a lump of snow, moss): `seg` around, 3 rings up. */
        void dome(float x, float y, float z, float rx, float ry, float rz, int argb, int seg) {
            int rings = 3;
            for (int r = 0; r < rings; r++) {
                float a0 = (float) (Math.PI / 2 * r / rings), a1 = (float) (Math.PI / 2 * (r + 1) / rings);
                for (int s = 0; s < seg; s++) {
                    float b0 = (float) (Math.PI * 2 * s / seg), b1 = (float) (Math.PI * 2 * (s + 1) / seg);
                    float[][] p = new float[4][];
                    float[][] q = {{a0, b0}, {a0, b1}, {a1, b1}, {a1, b0}};
                    for (int k = 0; k < 4; k++) {
                        float ca = (float) Math.cos(q[k][0]), sa = (float) Math.sin(q[k][0]);
                        float cb = (float) Math.cos(q[k][1]), sb = (float) Math.sin(q[k][1]);
                        p[k] = new float[] {ca * cb, sa, ca * sb};
                    }
                    // Outward-facing order (counter-clockwise from outside)
                    int[] order = {0, 3, 2, 1};
                    for (int k : order) {
                        float[] d = p[k];
                        v(x + d[0] * rx, y + d[1] * ry, z + d[2] * rz, argb, d[0] * 0.5f + 0.5f, d[2] * 0.5f + 0.5f, d[0], d[1], d[2], light);
                    }
                }
            }
        }

        /** A four-sided spike from (x,y,z) toward (dx,dy,dz), base half-width w. */
        void spike(float x, float y, float z, float dx, float dy, float dz, float w, int argb) {
            float tx = x + dx, ty = y + dy, tz = z + dz;
            float[][] base = {{x - w, y, z - w}, {x + w, y, z - w}, {x + w, y, z + w}, {x - w, y, z + w}};
            for (int k = 0; k < 4; k++) {
                float[] a = base[k], b = base[(k + 1) % 4];
                float mx = (a[0] + b[0]) / 2 - x, mz = (a[2] + b[2]) / 2 - z;
                float l = (float) Math.sqrt(mx * mx + mz * mz) + 1e-6f;
                float nx = mx / l, nz = mz / l;
                v(a[0], a[1], a[2], argb, 0, 1, nx, 0.4f, nz, light);
                v(tx, ty, tz, argb, 0.5f, 0, nx, 0.4f, nz, light);
                v(tx, ty, tz, argb, 0.5f, 0, nx, 0.4f, nz, light);
                v(b[0], b[1], b[2], argb, 1, 1, nx, 0.4f, nz, light);
            }
        }
    }

    public static void extras(Skin skin, String gunId, GunMesh mesh, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float t) {
        if (skin.extra == Extra.NONE) return;
        long seed = gunId.hashCode() * 31L + skin.id.hashCode();
        String key = gunId + "/" + skin.id;
        float swayX = GunAnim.swayVelocity(0), swayY = GunAnim.swayVelocity(1);
        switch (skin.extra) {
            case SNOW -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(4)), light, overlay);
                float[] s = spots(mesh, key, true, 60, seed);
                Random r = new Random(seed);
                for (int k = 0; k < s.length / 3; k++) {
                    float size = 0.25f + r.nextFloat() * 0.45f;
                    e.dome(s[k * 3], s[k * 3 + 1] - 0.05f, s[k * 3 + 2], size * 1.4f, size * 0.55f, size, hex(0xF2F6FA), 6);
                }
                // Flakes drifting off the gun and falling past it
                for (int k = 0; k < 26; k++) {
                    float ph = hash(k, 1, 2), life = (t * 0.35f + ph) % 1;
                    float x = mesh.minX + (mesh.maxX - mesh.minX) * hash(k, 2, 3) + (float) Math.sin(t + k) * 0.6f;
                    float y = mesh.maxY + 1 - life * (mesh.maxY - mesh.minY + 6);
                    float z = 8 + (hash(k, 3, 4) - 0.5f) * 5 + (float) Math.cos(t * 0.7f + k) * 0.5f;
                    float f = 0.09f;
                    e.card(new float[] {x - f, y, z - f}, new float[] {x + f, y, z - f}, new float[] {x + f, y, z + f}, new float[] {x - f, y, z + f}, hex(0xFFFFFF), light);
                }
            }
            case TORN, GHILLIE -> {
                boolean g = skin.extra == Extra.GHILLIE;
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(9)), light, overlay);
                float[] s = spots(mesh, key + (g ? "u" : "d"), g, g ? 90 : 22, seed);
                Random r = new Random(seed + 5);
                for (int k = 0; k < s.length / 3; k++) {
                    float len = (g ? 1.2f : 2.0f) + r.nextFloat() * (g ? 2.5f : 3.0f);
                    float w = g ? 0.12f + r.nextFloat() * 0.12f : 0.3f + r.nextFloat() * 0.35f;
                    int c = g ? (r.nextBoolean() ? hex(0x6A6A3A) : hex(0x4A5A2A)) : (r.nextInt(3) == 0 ? hex(0x8A7A52) : hex(0x4E5434));
                    float x = s[k * 3], y = s[k * 3 + 1], z = s[k * 3 + 2];
                    float dirY = g ? 0.6f : -1;      // ghillie strands stick up and flop over; torn strips hang
                    int segs = 4;
                    float[] prev0 = {x - w, y, z}, prev1 = {x + w, y, z};
                    for (int j = 1; j <= segs; j++) {
                        float f = j / (float) segs;
                        float swing = (float) Math.sin(t * (2 + r.nextFloat()) + k) * 0.35f * f + swayX * 1.4f * f * f;
                        float lift = g ? (dirY * f - f * f * 1.2f) * len : dirY * len * f;
                        float yy = y + lift + swayY * f * f;
                        float xx = x - swing * 0.6f, zz = z + swing;
                        float[] c0 = {xx - w * (1 - f * 0.5f), yy, zz}, c1 = {xx + w * (1 - f * 0.5f), yy, zz};
                        e.card(prev0, prev1, c1, c0, scale(c, 0.85f + 0.15f * f), light);
                        prev0 = c0;
                        prev1 = c1;
                    }
                }
            }
            case FROST -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(GunMesh.GLASS)), light, overlay);
                float[] s = spots(mesh, key, true, 40, seed);
                Random r = new Random(seed);
                for (int k = 0; k < s.length / 3; k++) {
                    float h = 0.4f + r.nextFloat() * 1.1f;
                    e.spike(s[k * 3], s[k * 3 + 1], s[k * 3 + 2], (r.nextFloat() - 0.5f) * 0.6f, h, (r.nextFloat() - 0.5f) * 0.6f, 0.12f + r.nextFloat() * 0.1f, 0xC0E8F8FF);
                }
            }
            case MOSS -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(1)), light, overlay);
                float[] s = spots(mesh, key, true, 70, seed);
                Random r = new Random(seed);
                for (int k = 0; k < s.length / 3; k++) {
                    float x = s[k * 3], y = s[k * 3 + 1], z = s[k * 3 + 2];
                    if (r.nextInt(3) == 0) {
                        e.dome(x, y - 0.03f, z, 0.35f + r.nextFloat() * 0.3f, 0.18f, 0.3f, hex(0x3E6A1E), 5);
                        continue;
                    }
                    // A tuft of grass blades swaying
                    for (int b = 0; b < 3; b++) {
                        float a = r.nextFloat() * 6.28f, h = 0.5f + r.nextFloat() * 0.9f;
                        float sw = (float) Math.sin(t * 2.2f + k + b) * 0.18f + swayX;
                        float bx = (float) Math.cos(a) * 0.08f, bz = (float) Math.sin(a) * 0.08f;
                        e.card(new float[] {x - bx, y, z - bz}, new float[] {x + bx, y, z + bz}, new float[] {x + sw, y + h, z + sw * 0.5f},
                            new float[] {x + sw, y + h, z + sw * 0.5f}, r.nextBoolean() ? hex(0x5A8A2A) : hex(0x7AA03A), light);
                    }
                }
            }
            case SLIME, BLOOD -> {
                boolean slime = skin.extra == Extra.SLIME;
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(slime ? GunMesh.GLOW : 1)), slime ? 0xF000F0 : light, overlay);
                float[] s = spots(mesh, key, false, 16, seed);
                for (int k = 0; k < s.length / 3; k++) {
                    // A drip: stretches down, thins, lets go, starts again
                    float ph = (t * (0.25f + hash(k, 4, 5) * 0.2f) + hash(k, 5, 6)) % 1;
                    float len = ph < 0.8f ? ph / 0.8f * 1.6f : 1.6f;
                    float x = s[k * 3], y = s[k * 3 + 1], z = s[k * 3 + 2];
                    float w = 0.13f * (1 - ph * 0.4f);
                    int c = slime ? hex(0x6CE020) : hex(0x7A0C0C);
                    e.card(new float[] {x - w, y, z}, new float[] {x + w, y, z}, new float[] {x + w * 0.4f, y - len, z}, new float[] {x - w * 0.4f, y - len, z}, c, e.light);
                    e.card(new float[] {x, y, z - w}, new float[] {x, y, z + w}, new float[] {x, y - len, z + w * 0.4f}, new float[] {x, y - len, z - w * 0.4f}, c, e.light);
                    if (ph >= 0.8f) {
                        float fy = y - 1.6f - (ph - 0.8f) * 30;
                        e.dome(x, fy, z, 0.12f, 0.2f, 0.12f, c, 5);
                    } else {
                        e.dome(x, y - len, z, w * 1.3f, w * 1.6f, w * 1.3f, c, 5);
                    }
                }
            }
            case PETALS -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(1)), light, overlay);
                for (int k = 0; k < 22; k++) {
                    float life = (t * 0.22f + hash(k, 7, 8)) % 1;
                    float x = mesh.minX + (mesh.maxX - mesh.minX) * hash(k, 8, 9) + (float) Math.sin(t * 1.3f + k) * 1.2f;
                    float y = mesh.maxY + 2 - life * (mesh.maxY - mesh.minY + 8);
                    float z = 8 + (hash(k, 9, 1) - 0.5f) * 6;
                    float a = t * 3 + k, f = 0.22f;
                    float ca = (float) Math.cos(a) * f, sa = (float) Math.sin(a) * f;
                    e.card(new float[] {x - ca, y - sa * 0.3f, z - sa}, new float[] {x + sa, y + ca * 0.3f, z - ca}, new float[] {x + ca, y + sa * 0.3f, z + sa},
                        new float[] {x - sa, y - ca * 0.3f, z + ca}, k % 3 == 0 ? hex(0xFFFFFF) : hex(0xF2A0B8), light);
                }
            }
            case FLAMES -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(GunMesh.GLOW)), 0xF000F0, overlay);
                float[] s = spots(mesh, key, true, 28, seed);
                for (int k = 0; k < s.length / 3; k++) {
                    float x = s[k * 3], y = s[k * 3 + 1], z = s[k * 3 + 2];
                    float flick = 0.6f + 0.4f * noise(t * 6 + k, k, 0);
                    float h = (0.7f + hash(k, 1, 1) * 0.9f) * flick + GunAnim.heat() * 0.8f;
                    float w = 0.28f;
                    float lean = -swayX * 2 - 0.3f;
                    int c = mix(hex(0xFF6010), hex(0xFFD050), hash(k, 2, 2));
                    e.card(new float[] {x - w, y, z}, new float[] {x + w, y, z}, new float[] {x + lean, y + h, z}, new float[] {x + lean, y + h, z}, c, e.light);
                    e.card(new float[] {x, y, z - w}, new float[] {x, y, z + w}, new float[] {x + lean, y + h, z}, new float[] {x + lean, y + h, z}, c, e.light);
                }
            }
            case EMBERS -> {
                Emit e = new Emit(pose, buffers.getBuffer(GunMesh.type(GunMesh.GLOW)), 0xF000F0, overlay);
                for (int k = 0; k < 14; k++) {
                    float life = (t * (0.3f + hash(k, 3, 3) * 0.3f) + hash(k, 4, 4)) % 1;
                    float x = mesh.minX + (mesh.maxX - mesh.minX) * hash(k, 5, 5) + (float) Math.sin(t * 2 + k) * 0.4f;
                    float y = mesh.minY + (mesh.maxY - mesh.minY) * 0.5f + life * 5;
                    float z = 8 + (hash(k, 6, 6) - 0.5f) * 2.5f;
                    float f = 0.07f * (1 - life);
                    int c = mix(hex(0xFFC040), hex(0xC03008), life);
                    e.card(new float[] {x - f, y - f, z}, new float[] {x + f, y - f, z}, new float[] {x + f, y + f, z}, new float[] {x - f, y + f, z}, c, e.light);
                }
            }
            default -> { }
        }
    }
}
