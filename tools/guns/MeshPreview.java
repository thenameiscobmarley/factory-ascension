import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPInputStream;

/**
 * Renders gun meshes (.fam) to PNG for checking them without the game: a side view and a three-quarter view, with
 * the same colours, wear, highlight and detail textures as client/gun/GunMesh, and Minecraft-like lighting.
 *
 *   java tools/guns/MeshPreview.java <out.png> <texdir> <a.fam> [b.fam ...]
 * Several meshes go in a grid (two views each).
 */
public class MeshPreview {
    static String[] CLASSES = {"metal", "polymer", "wood", "rubber", "plain", "glass", "glow", "knurl", "check", "fabric", "rail"};
    static BufferedImage[] tex = new BufferedImage[CLASSES.length];

    record Mesh(float[] pos, float[] nrm, float[] uv, int[] col, int[] cls, int quads) {}

    static Mesh load(String path) throws Exception {
        byte[] raw;
        try (var in = new GZIPInputStream(new FileInputStream(path))) { raw = in.readAllBytes(); }
        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        b.position(3);
        boolean hasAo = b.get() == '3';
        int nm = b.getShort() & 0xFFFF;
        int[] mCls = new int[nm], mRgb = new int[nm], mFl = new int[nm];
        for (int i = 0; i < nm; i++) {
            int len = b.get() & 0xFF;
            b.position(b.position() + len);
            mCls[i] = b.get() & 0xFF;
            mRgb[i] = (b.get() & 0xFF) << 16 | (b.get() & 0xFF) << 8 | (b.get() & 0xFF);
            mFl[i] = b.get() & 0xFF;
        }
        int nv = b.getInt();
        float[] vx = new float[nv * 3], vn = new float[nv * 3];
        int[] vm = new int[nv];
        float[] vw = new float[nv], vao = new float[nv];
        for (int i = 0; i < nv; i++) {
            for (int k = 0; k < 3; k++) vx[i * 3 + k] = b.getShort() / 500f;
            float nx = b.get() / 127f, ny = b.get() / 127f, nz = b.get() / 127f;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-4) { ny = 1; l = 1; }
            vn[i * 3] = nx / l; vn[i * 3 + 1] = ny / l; vn[i * 3 + 2] = nz / l;
            vm[i] = b.get() & 0xFF;
            vw[i] = (b.get() & 0xFF) / 255f;
            vao[i] = hasAo ? (b.get() & 0xFF) / 255f : 1f;
        }
        int nq = b.getInt();
        boolean big = b.get() != 0;
        int[] q = new int[nq * 4];
        for (int i = 0; i < nq * 4; i++) q[i] = big ? b.getInt() : (b.getShort() & 0xFFFF);
        float[] pos = new float[nq * 12], nrm = new float[nq * 12], uv = new float[nq * 8];
        int[] col = new int[nq * 4], cls = new int[nq * 4];
        for (int i = 0; i < nq; i++) {
            int a = q[i * 4], bb = q[i * 4 + 1], cc = q[i * 4 + 2], d = q[i * 4 + 3];
            float e1x = vx[cc * 3] - vx[a * 3], e1y = vx[cc * 3 + 1] - vx[a * 3 + 1], e1z = vx[cc * 3 + 2] - vx[a * 3 + 2];
            float e2x = vx[d * 3] - vx[bb * 3], e2y = vx[d * 3 + 1] - vx[bb * 3 + 1], e2z = vx[d * 3 + 2] - vx[bb * 3 + 2];
            float fx = Math.abs(e1y * e2z - e1z * e2y), fy = Math.abs(e1z * e2x - e1x * e2z), fz = Math.abs(e1x * e2y - e1y * e2x);
            int axis = fx >= fy && fx >= fz ? 0 : (fy >= fz ? 1 : 2);
            for (int k = 0; k < 4; k++) {
                int v = q[i * 4 + k], dst = i * 4 + k;
                for (int j = 0; j < 3; j++) { pos[dst * 3 + j] = vx[v * 3 + j]; nrm[dst * 3 + j] = vn[v * 3 + j]; }
                float x = vx[v * 3], y = vx[v * 3 + 1], z = vx[v * 3 + 2];
                int m = vm[v];
                float off = m * 0.371f, u, w;
                if (axis == 0) { u = z / 8; w = -y / 8; } else if (axis == 1) { u = x / 8; w = z / 8; } else { u = x / 8; w = -y / 8; }
                uv[dst * 2] = u + off; uv[dst * 2 + 1] = w + off * 0.61f;
                col[dst] = bake(mRgb[m], mCls[m], mFl[m], vw[v], vn[v * 3], vn[v * 3 + 1], vn[v * 3 + 2], vao[v]);
                cls[dst] = mCls[m];
            }
        }
        return new Mesh(pos, nrm, uv, col, cls, nq);
    }

    static int clamp(float v) { return Math.max(0, Math.min(255, Math.round(v * 255))); }

    static int bake(int rgb, int cls, int flags, float wear, float nx, float ny, float nz, float ao) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        if (cls == 6 || cls == 5) return rgb;
        boolean metal = cls == 0 || cls == 7 || cls == 10;
        if ((flags & 4) == 0 && wear > 0) {
            float k = wear * (metal ? 0.55f : 0.22f);
            float tr = metal ? 0.62f : r * 1.35f + 0.04f, tg = metal ? 0.63f : g * 1.35f + 0.04f, tb = metal ? 0.65f : b * 1.35f + 0.04f;
            r += (tr - r) * k; g += (tg - g) * k; b += (tb - b) * k;
        }
        float h = Math.max(0, nx * 0.25f + ny * 0.88f + nz * 0.4f);
        float sheen = (float) Math.pow(h, (flags & 2) != 0 ? 22 : 8) * ((flags & 2) != 0 ? 0.32f : (metal ? 0.14f : 0.06f));
        float top = 0.94f + 0.06f * ny;
        // Baked occlusion: enclosed spots darker, and no sheen down in the crevices
        float occ = 0.38f + 0.62f * ao * ao;
        sheen *= ao;
        r = r * top * occ + sheen; g = g * top * occ + sheen; b = b * top * occ + sheen;
        float s = 1 / 0.88f;
        return clamp(r * s) << 16 | clamp(g * s) << 8 | clamp(b * s);
    }

    // Minecraft's two item lights (approximately, in view space)
    static float[] L0 = norm(0.2f, 1.0f, -0.7f), L1 = norm(-0.2f, 1.0f, 0.7f);

    static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[] {x / l, y / l, z / l};
    }

    static void render(Mesh m, BufferedImage img, float[] zbuf, int ox, int oy, int w, int h, float yaw, float pitch) {
        // Fit: rotate, then scale the bounds into the box
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        int n = m.quads * 4;
        float[] P = new float[n * 3], N = new float[n * 3];
        float minx = 1e9f, maxx = -1e9f, miny = 1e9f, maxy = -1e9f;
        for (int i = 0; i < n; i++) {
            float x = m.pos[i * 3] - 8, y = m.pos[i * 3 + 1] - 8, z = m.pos[i * 3 + 2] - 8;
            // yaw about y, then pitch about x
            float x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
            float y2 = y * cp - z1 * sp, z2 = y * sp + z1 * cp;
            P[i * 3] = x1; P[i * 3 + 1] = y2; P[i * 3 + 2] = z2;
            float nx = m.nrm[i * 3], ny = m.nrm[i * 3 + 1], nz = m.nrm[i * 3 + 2];
            float nx1 = nx * cy + nz * sy, nz1 = -nx * sy + nz * cy;
            N[i * 3] = nx1; N[i * 3 + 1] = ny * cp - nz1 * sp; N[i * 3 + 2] = ny * sp + nz1 * cp;
            minx = Math.min(minx, x1); maxx = Math.max(maxx, x1); miny = Math.min(miny, y2); maxy = Math.max(maxy, y2);
        }
        float s = Math.min((w - 20) / (maxx - minx), (h - 20) / (maxy - miny));
        float cx = (minx + maxx) / 2, cyy = (miny + maxy) / 2;
        for (int qd = 0; qd < m.quads; qd++) {
            for (int tri = 0; tri < 2; tri++) {
                int[] idx = tri == 0 ? new int[] {0, 1, 2} : new int[] {0, 2, 3};
                float[] sx = new float[3], syy = new float[3], sz = new float[3];
                int[] ci = new int[3];
                for (int k = 0; k < 3; k++) {
                    int i = qd * 4 + idx[k];
                    ci[k] = i;
                    sx[k] = ox + w / 2f + (P[i * 3] - cx) * s;
                    syy[k] = oy + h / 2f - (P[i * 3 + 1] - cyy) * s;
                    sz[k] = P[i * 3 + 2];
                }
                float area = (sx[1] - sx[0]) * (syy[2] - syy[0]) - (sx[2] - sx[0]) * (syy[1] - syy[0]);
                if (Math.abs(area) < 1e-6) continue;
                // Back faces: Minecraft culls them (counter-clockwise in view = front). Screen y is down, so front has area < 0
                if (area > 0 && m.cls[ci[0]] != 5) continue;
                int x0 = (int) Math.max(ox, Math.floor(Math.min(sx[0], Math.min(sx[1], sx[2]))));
                int x1 = (int) Math.min(ox + w - 1, Math.ceil(Math.max(sx[0], Math.max(sx[1], sx[2]))));
                int y0 = (int) Math.max(oy, Math.floor(Math.min(syy[0], Math.min(syy[1], syy[2]))));
                int y1 = (int) Math.min(oy + h - 1, Math.ceil(Math.max(syy[0], Math.max(syy[1], syy[2]))));
                for (int py = y0; py <= y1; py++) {
                    for (int px = x0; px <= x1; px++) {
                        float fx = px + 0.5f, fy = py + 0.5f;
                        float w0 = ((sx[1] - fx) * (syy[2] - fy) - (sx[2] - fx) * (syy[1] - fy)) / area;
                        float w1 = ((sx[2] - fx) * (syy[0] - fy) - (sx[0] - fx) * (syy[2] - fy)) / area;
                        float w2 = 1 - w0 - w1;
                        if (w0 < -1e-4 || w1 < -1e-4 || w2 < -1e-4) continue;
                        float z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
                        int zi = py * img.getWidth() + px;
                        if (z <= zbuf[zi]) continue;
                        zbuf[zi] = z;
                        float nx = 0, ny = 0, nz = 0, u = 0, v = 0, r = 0, g = 0, b = 0;
                        float[] ww = {w0, w1, w2};
                        for (int k = 0; k < 3; k++) {
                            int i = ci[k];
                            nx += N[i * 3] * ww[k]; ny += N[i * 3 + 1] * ww[k]; nz += N[i * 3 + 2] * ww[k];
                            u += m.uv[i * 2] * ww[k]; v += m.uv[i * 2 + 1] * ww[k];
                            r += (m.col[i] >> 16 & 255) * ww[k]; g += (m.col[i] >> 8 & 255) * ww[k]; b += (m.col[i] & 255) * ww[k];
                        }
                        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        nx /= l; ny /= l; nz /= l;
                        int c = m.cls[ci[0]];
                        float light = c == 6 ? 1 : Math.min(1, 0.4f + 0.6f * (Math.max(0, nx * L0[0] + ny * L0[1] + nz * L0[2]) + Math.max(0, nx * L1[0] + ny * L1[1] + nz * L1[2])));
                        float t = 1;
                        BufferedImage tx = tex[c];
                        if (tx != null) {
                            int tu = Math.floorMod((int) Math.floor(u * tx.getWidth()), tx.getWidth()), tv = Math.floorMod((int) Math.floor(v * tx.getHeight()), tx.getHeight());
                            t = (tx.getRGB(tu, tv) & 255) / 255f;
                        }
                        int R = clamp(r / 255 * t * light), G = clamp(g / 255 * t * light), B = clamp(b / 255 * t * light);
                        img.setRGB(px, py, 0xFF000000 | R << 16 | G << 8 | B);
                    }
                }
            }
        }
    }

    public static void main(String[] a) throws Exception {
        String out = a[0];
        for (int i = 0; i < CLASSES.length; i++) {
            File f = new File(a[1], CLASSES[i] + ".png");
            if (f.exists()) tex[i] = ImageIO.read(f);
        }
        int count = a.length - 2;
        int cols = Math.min(2, count), rows = (count + cols - 1) / cols;
        int cw = 900, ch = 300;
        BufferedImage img = new BufferedImage(cols * cw * 2 / 2, rows * ch * 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) img.setRGB(x, y, 0xFF9AA4AE - ((y * 40 / img.getHeight()) * 0x010101));
        float[] zb = new float[img.getWidth() * img.getHeight()];
        java.util.Arrays.fill(zb, -1e9f);
        for (int k = 0; k < count; k++) {
            Mesh m = load(a[k + 2]);
            int gx = (k % cols) * cw, gy = (k / cols) * ch * 2;
            render(m, img, zb, gx, gy, cw, ch, 0, 0);
            render(m, img, zb, gx, gy + ch, cw, ch, (float) Math.toRadians(-35), (float) Math.toRadians(25));
        }
        ImageIO.write(img, "png", new File(out));
    }
}
