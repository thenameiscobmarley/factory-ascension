package dev.factoryascension.client.hd;

import com.mojang.blaze3d.platform.NativeImage;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Factory Ascension HD": a built-in resource pack with every vanilla block and item texture at twice the detail
 * (16x becomes 32x), painted by client/hd/Upscale from the game's own textures as they load. Nothing is shipped: each
 * one is made the first time and kept in .minecraft/fa_hd_cache (made again when the painter changes). Animated
 * textures keep their frames (their .mcmeta is passed through). Textures already bigger than 16x (other packs, our own)
 * are left alone. It's an ordinary pack: turn it off in Resource Packs.
 */
public final class HdPack implements PackResources {
    public static final String PACK_ID = "factoryascension_hd";
    /** Bump when Upscale changes, so cached textures are made again. */
    static final int PAINTER_VERSION = 3;
    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final byte[] META = ("{\"pack\":{\"pack_format\":15,\"description\":\"Factory Ascension HD: every block and item at 32x\"}}").getBytes(StandardCharsets.UTF_8);
    private static final Map<String, byte[]> MEMORY = new ConcurrentHashMap<>();
    private final String id;

    HdPack(String id) {
        this.id = id;
    }

    static PackResources vanilla() {
        return Minecraft.getInstance().getVanillaPackResources();
    }

    static boolean handles(ResourceLocation loc) {
        String p = loc.getPath();
        return loc.getNamespace().equals("minecraft") && (p.startsWith("textures/block/") || p.startsWith("textures/item/"));
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta") ? () -> new ByteArrayInputStream(META) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
        if (type != PackType.CLIENT_RESOURCES || !handles(loc)) return null;
        IoSupplier<InputStream> base = vanilla().getResource(type, loc);
        if (base == null) return null;
        if (loc.getPath().endsWith(".png.mcmeta")) return base;   // animations: the same frames, just bigger
        if (!loc.getPath().endsWith(".png")) return null;
        return () -> new ByteArrayInputStream(hd(loc, base));
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput out) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals("minecraft")) return;
        vanilla().listResources(type, namespace, path, (loc, base) -> {
            if (!handles(loc)) return;
            if (loc.getPath().endsWith(".png")) out.accept(loc, () -> new ByteArrayInputStream(hd(loc, base)));
            else if (loc.getPath().endsWith(".png.mcmeta")) out.accept(loc, base);
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of("minecraft") : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws java.io.IOException {
        return AbstractPackResources.getMetadataFromStream(serializer, new ByteArrayInputStream(META));
    }

    @Override
    public String packId() {
        return id;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void close() {
    }

    // ---- Making (and keeping) the HD textures ----

    static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("fa_hd_cache").resolve("v" + PAINTER_VERSION);
    }

    /** The HD version's PNG bytes; the original's if anything goes wrong (it's never worse than without the pack). */
    static byte[] hd(ResourceLocation loc, IoSupplier<InputStream> base) throws java.io.IOException {
        String key = loc.getPath();
        byte[] mem = MEMORY.get(key);
        if (mem != null) return mem;
        byte[] original;
        try (InputStream in = base.get()) {
            original = in.readAllBytes();
        }
        Path file = cacheDir().resolve(key.replace('/', '_'));
        try {
            byte[] made;
            if (Files.isRegularFile(file)) {
                made = Files.readAllBytes(file);
            } else {
                made = paint(original, key);
                Files.createDirectories(file.getParent());
                Files.write(file, made);
            }
            MEMORY.put(key, made);
            return made;
        } catch (Throwable err) {
            LOG.warn("Factory Ascension HD: kept {} as it was ({})", key, err.toString());
            MEMORY.put(key, original);
            return original;
        }
    }

    static byte[] paint(byte[] png, String path) throws java.io.IOException {
        try (NativeImage img = NativeImage.read(new ByteArrayInputStream(png))) {
            int w = img.getWidth(), h = img.getHeight();
            if (w > 16 || h % w != 0 || h > 16 * 64) return png;   // already detailed, or not a block/item sheet
            int[] argb = new int[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) argb[y * w + x] = abgrToArgb(img.getPixelRGBA(x, y));
            int[] hd = Upscale.x2(argb, w, h, path.hashCode(), path.startsWith("textures/item/") ? Upscale.Style.ITEM : Upscale.Style.BLOCK);
            try (NativeImage out = new NativeImage(w * 2, h * 2, false)) {
                for (int y = 0; y < h * 2; y++)
                    for (int x = 0; x < w * 2; x++) out.setPixelRGBA(x, y, abgrToArgb(hd[y * w * 2 + x]));   // the swap is its own inverse
                return out.asByteArray();
            }
        }
    }

    static int abgrToArgb(int c) {
        return c & 0xFF00FF00 | (c & 0xFF) << 16 | (c >> 16 & 0xFF);
    }

    // ---- The pack in the game's list, switched on once on the first start ----

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Finder {
        @SubscribeEvent
        public static void packs(AddPackFindersEvent event) {
            if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
            Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Factory Ascension HD"), false, HdPack::new,
                PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(add -> add.accept(pack));
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class FirstStart {
        private static boolean done = false;

        @SubscribeEvent
        public static void title(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
            if (done || !(event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
            done = true;
            try {
                if (FAClientConfig.HD_ADDED.get()) return;
                FAClientConfig.HD_ADDED.set(true);
                FAClientConfig.HD_ADDED.save();
                var mc = Minecraft.getInstance();
                var repo = mc.getResourcePackRepository();
                repo.reload();
                if (repo.getPack(PACK_ID) == null || repo.getSelectedIds().contains(PACK_ID)) return;
                var ids = new java.util.ArrayList<>(repo.getSelectedIds());
                ids.add(PACK_ID);
                repo.setSelected(ids);
                mc.options.updateResourcePacks(repo);
            } catch (Throwable err) {
                LOG.error("Couldn't switch the HD pack on", err);
            }
        }
    }
}
