package dev.factoryascension.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The depth pack: a LabPBR resource pack made on the player's own PC from the textures their game has loaded (vanilla,
 * every mod, this pack), so a parallax shader pack (Oculus + Complementary) gives every block, item, mob and armor real
 * depth. Nothing derived from Mojang's textures is ever shipped: it's built locally, into resourcepacks/FactoryAscension-Depth.
 * For each texture: _n.png (normal from the height, DirectX/LabPBR, height in alpha, ambient occlusion in blue) and
 * _s.png (rough by default, metals shiny, glowing blocks emissive). Runs on its own thread; every image is freed.
 */
public final class DepthPack {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    public static final String FOLDER = "FactoryAscension-Depth";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final String[] METALS = {"iron", "gold", "copper", "netherite", "steel", "brass", "zinc", "ascendite", "voidsteel", "celestium", "ascendium", "chain", "anvil", "lantern", "rail"};
    private static final String[] GLOWS = {"lava", "glowstone", "lamp_on", "sea_lantern", "shroomlight", "magma", "fire", "froglight", "glow", "torch", "beacon", "end_rod", "crying"};

    private DepthPack() {
    }

    public static boolean running() {
        return RUNNING.get();
    }

    /** Make the pack. `status` gets progress messages (on the generator thread). */
    public static void generate(Consumer<String> status) {
        if (!RUNNING.compareAndSet(false, true)) { status.accept("Already making the depth pack..."); return; }
        Minecraft mc = Minecraft.getInstance();
        Map<ResourceLocation, Resource> all;
        try {
            all = mc.getResourceManager().listResources("textures", rl -> rl.getPath().endsWith(".png") && wanted(rl.getPath()));
        } catch (Throwable err) {
            RUNNING.set(false);
            status.accept("Couldn't list the textures: " + err.getMessage());
            return;
        }
        Path root = mc.gameDirectory.toPath().resolve("resourcepacks").resolve(FOLDER);
        Thread t = new Thread(() -> {
            int done = 0, failed = 0;
            try {
                clear(root);
                Files.createDirectories(root);
                Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"Factory Ascension depth pack: made from your own textures, for parallax shader packs\"}}", StandardCharsets.UTF_8);
                List<Map.Entry<ResourceLocation, Resource>> list = new ArrayList<>(all.entrySet());
                for (Map.Entry<ResourceLocation, Resource> e : list) {
                    try {
                        make(root, e.getKey(), e.getValue());
                        done++;
                    } catch (Throwable err) {
                        failed++;
                    }
                    if ((done + failed) % 250 == 0) status.accept("Depth pack: " + (done + failed) + " / " + list.size() + " textures...");
                }
                final int made = done;
                status.accept("Depth pack done: " + made + " textures (" + failed + " skipped). Turning it on...");
                mc.execute(() -> enable(mc, status, made));
            } catch (Throwable err) {
                LOG.error("Depth pack failed", err);
                status.accept("Depth pack failed: " + err.getMessage());
            } finally {
                RUNNING.set(false);
            }
        }, "Factory Ascension depth pack");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static boolean wanted(String path) {
        if (path.endsWith("_n.png") || path.endsWith("_s.png")) return false;
        return path.startsWith("textures/block/") || path.startsWith("textures/item/") || path.startsWith("textures/entity/") || path.startsWith("textures/models/armor/");
    }

    /** Only ever deletes inside its own folder. */
    private static void clear(Path root) throws java.io.IOException {
        if (!Files.isDirectory(root) || !root.getFileName().toString().equals(FOLDER)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).filter(p -> !p.equals(root)).forEach(p -> {
                try { Files.delete(p); } catch (java.io.IOException ignored) { }
            });
        }
    }

    private static void make(Path root, ResourceLocation id, Resource res) throws java.io.IOException {
        NativeImage src;
        try (InputStream in = res.open()) {
            src = NativeImage.read(in);
        }
        try (src) {   // the image's native memory is freed however this ends
            int w = src.getWidth(), h = src.getHeight();
            if (w > 1024 || h > 4096) return;
            // Animated block textures are frames stacked downward: treat each frame as its own tile
            int frame = (h > w && h % w == 0) ? w : h;
            boolean tile = id.getPath().startsWith("textures/block/");
            float[] height = new float[w * h];
            float lo = 1, hi = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int c = src.getPixelRGBA(x, y);   // ABGR
                float a = (c >>> 24) / 255f;
                float lum = ((c & 255) * 0.299f + (c >> 8 & 255) * 0.587f + (c >> 16 & 255) * 0.114f) / 255f;
                float v = a < 0.1f ? 0 : lum;
                height[y * w + x] = v;
                if (a >= 0.1f) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
            }
            float range = Math.max(0.3f, hi - lo);   // low-contrast textures (quartz, wool) stay flatter instead of being stretched to full depth
            // Smooth the height a little first (centre-weighted 3x3): every pixel being its own bump looks gritty
            float[] blurred = new float[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int f0s = (y / frame) * frame;
                float sum = 0, wsum = 0;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    float wt = (dx == 0 && dy == 0) ? 4 : (dx == 0 || dy == 0) ? 2 : 1;
                    sum += norm(height, w, h, frame, f0s, x + dx, y + dy, tile, lo, range) * wt;
                    wsum += wt;
                }
                blurred[y * w + x] = sum / wsum;
            }
            height = blurred;
            lo = 0;
            range = 1;
            String name = id.getPath();
            boolean entity = name.startsWith("textures/entity/") || name.startsWith("textures/models/");
            float depth = entity ? 0.45f : 0.7f;           // mobs and armor get shallower relief
            float strength = entity ? 1.0f : 1.6f;
            NativeImage n = new NativeImage(w, h, false);
            NativeImage s = new NativeImage(w, h, false);
            try {
                boolean metal = has(name, METALS), glow = has(name, GLOWS);
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int f0 = (y / frame) * frame;
                    float hc = norm(height, w, h, frame, f0, x, y, tile, lo, range);
                    float dx = norm(height, w, h, frame, f0, x + 1, y, tile, lo, range) - norm(height, w, h, frame, f0, x - 1, y, tile, lo, range);
                    float dy = norm(height, w, h, frame, f0, x, y + 1, tile, lo, range) - norm(height, w, h, frame, f0, x, y - 1, tile, lo, range);
                    float nx = -dx * strength, ny = -dy * strength, nz = 1;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    nx /= len; ny /= len;
                    // Ambient occlusion: pixels lower than their neighbours get a little darker
                    float around = (norm(height, w, h, frame, f0, x + 1, y, tile, lo, range) + norm(height, w, h, frame, f0, x - 1, y, tile, lo, range)
                        + norm(height, w, h, frame, f0, x, y + 1, tile, lo, range) + norm(height, w, h, frame, f0, x, y - 1, tile, lo, range)) / 4;
                    int ao = clamp(255 - Math.max(0, around - hc) * 400);
                    int alpha = clamp(255 - (1 - hc) * depth * 254);   // LabPBR: 255 is the surface, lower is deeper
                    int srcA = src.getPixelRGBA(x, y) >>> 24;
                    // NativeImage stores ABGR: red is the lowest byte
                    n.setPixelRGBA(x, y, (alpha << 24) | (ao << 16) | (clamp((ny * 0.5f + 0.5f) * 255) << 8) | clamp((nx * 0.5f + 0.5f) * 255));
                    int smooth = metal ? 170 : clamp(30 + hc * 40), f0v = metal ? 230 : 10;
                    int emissive = 255;
                    if (glow) {
                        int c = src.getPixelRGBA(x, y);
                        float lum = ((c & 255) + (c >> 8 & 255) + (c >> 16 & 255)) / (3f * 255f);
                        emissive = lum > 0.55f ? clamp((lum - 0.55f) / 0.45f * 254) : 255;
                    }
                    if (srcA < 25) { alpha = 255; emissive = 255; }
                    s.setPixelRGBA(x, y, (emissive << 24) | (0 << 16) | (f0v << 8) | smooth);
                }
                Path base = root.resolve("assets").resolve(id.getNamespace());
                String stem = name.substring(0, name.length() - 4);
                Path np = base.resolve(stem + "_n.png"), sp = base.resolve(stem + "_s.png");
                Files.createDirectories(np.getParent());
                n.writeToFile(np);
                s.writeToFile(sp);
                // Animated textures need the same animation on their maps
                ResourceLocation meta = new ResourceLocation(id.getNamespace(), name + ".mcmeta");
                var metaRes = Minecraft.getInstance().getResourceManager().getResource(meta);
                if (metaRes.isPresent()) {
                    try (InputStream in = metaRes.get().open()) {
                        byte[] bytes = in.readAllBytes();
                        Files.write(base.resolve(stem + "_n.png.mcmeta"), bytes);
                        Files.write(base.resolve(stem + "_s.png.mcmeta"), bytes);
                    }
                }
            } finally {
                n.close();
                s.close();
            }
        }
    }

    private static float norm(float[] height, int w, int h, int frame, int f0, int x, int y, boolean tile, float lo, float range) {
        if (tile) {
            x = Math.floorMod(x, w);
            y = f0 + Math.floorMod(y - f0, frame);
        } else {
            x = Math.max(0, Math.min(w - 1, x));
            y = Math.max(0, Math.min(h - 1, y));
        }
        return Math.max(0, Math.min(1, (height[y * w + x] - lo) / range));
    }

    private static boolean has(String name, String[] words) {
        for (String word : words) if (name.contains(word)) return true;
        return false;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    /** Turn the pack on (and reload resources) so it's used straight away. */
    private static void enable(Minecraft mc, Consumer<String> status, int made) {
        try {
            var repo = mc.getResourcePackRepository();
            repo.reload();
            String id = "file/" + FOLDER;
            if (repo.getPack(id) == null) { status.accept("Depth pack made (" + made + " textures). Turn on '" + FOLDER + "' in Resource Packs."); return; }
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
            if (!selected.contains(id)) selected.add(id);
            repo.setSelected(selected);
            mc.options.updateResourcePacks(repo);
            status.accept("Depth pack on (" + made + " textures). For depth, use Oculus with a parallax shader (Complementary: Surface > POM).");
        } catch (Throwable err) {
            LOG.error("Couldn't turn the depth pack on", err);
            status.accept("Depth pack made (" + made + " textures). Turn on '" + FOLDER + "' in Resource Packs.");
        }
    }
}
