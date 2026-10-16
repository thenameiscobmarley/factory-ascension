package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parallax depth without a shader pack: the built-in resource pack "Factory Ascension Depth" (resources/resourcepacks/
 * fa_depth) replaces the game's terrain (solid and cut-out) and entity shaders with versions that march the view ray
 * through each texture's light and dark detail, so blocks and mobs get real depth. It needs the game's own renderer:
 * With Embeddium, which draws blocks with its own shaders (read from its jar, not from resource packs), the pack's
 * versions of those (assets/sodium/shaders) are handed to it (mixin/EmbeddiumShaderMixin) while the pack is on. The
 * smoothing and strength settings are written into the shaders as the pack is read (Defines). It's an ordinary resource
 * pack: if anything looks wrong, switching it off (here, or in Resource Packs) puts the original shaders straight back. If the
 * game ever closes while Embeddium is building our shaders, the next start leaves Embeddium's own ones alone.
 */
public final class DepthShaders {
    public static final String PACK_ID = "factoryascension_depth";

    private DepthShaders() {
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Finder {
        @SubscribeEvent
        public static void packs(AddPackFindersEvent event) {
            if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
            Path path = ModList.get().getModFileById(FactoryAscension.MODID).getFile().findResource("resourcepacks/fa_depth");
            Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Factory Ascension Depth"), false,
                id -> new Defines(new PathPackResources(id, path, false)), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(add -> add.accept(pack));
        }
    }

    public static boolean enabled() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getResourcePackRepository().getSelectedIds().contains(PACK_ID);
    }

    /** Switch the pack on or off and reload the game's resources (a few seconds). */
    public static void set(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        var repo = mc.getResourcePackRepository();
        List<String> ids = new ArrayList<>(repo.getSelectedIds());
        if (on == ids.contains(PACK_ID)) return;
        if (on) ids.add(PACK_ID); else ids.remove(PACK_ID);
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);   // saves the choice and reloads
    }

    /** On the first start with this version, turn it on (once; after that the player's choice stands). */
    public static void enableOnce() {
        if (FAClientConfig.DEPTH_ADDED.get()) return;
        FAClientConfig.DEPTH_ADDED.set(true);
        FAClientConfig.DEPTH_ADDED.save();
        Minecraft mc = Minecraft.getInstance();
        var repo = mc.getResourcePackRepository();
        repo.reload();
        if (repo.getPack(PACK_ID) == null || repo.getSelectedIds().contains(PACK_ID)) return;
        List<String> ids = new ArrayList<>(repo.getSelectedIds());
        ids.add(PACK_ID);
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);   // saves it and reloads once
    }

    // The first time the title screen opens (resources are loaded by then), switch the pack on
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class FirstStart {
        private static boolean done = false;

        @SubscribeEvent
        public static void title(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
            if (done || !(event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
            done = true;
            try {
                enableOnce();
            } catch (Throwable err) {
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Couldn't switch the depth pack on", err);
            }
        }
    }

    // ---- The settings, written into the shaders ----

    private static String bakedDefines = "";

    static String defines() {
        int mips = 4;
        try {
            mips = Minecraft.getInstance().options.mipmapLevels().get();
        } catch (Throwable ignored) {
        }
        return String.format(java.util.Locale.ROOT, "#define FA_SMOOTH %.3f\n#define FA_DEPTH %.3f\n#define FA_MIPS %d\n",
            FAClientConfig.DEPTH_SMOOTH.get(), FAClientConfig.DEPTH_STRENGTH.get(), Math.max(0, mips));
    }

    public static boolean settingsChanged() {
        return !defines().equals(bakedDefines);
    }

    /** The pack, with the settings put in front of the shared include (fa_parallax.glsl). */
    static final class Defines implements net.minecraft.server.packs.PackResources {
        private final net.minecraft.server.packs.PackResources pack;

        Defines(net.minecraft.server.packs.PackResources pack) {
            this.pack = pack;
        }

        @Override
        public net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream> getRootResource(String... path) {
            return pack.getRootResource(path);
        }

        @Override
        public net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream> getResource(PackType type, net.minecraft.resources.ResourceLocation id) {
            var res = pack.getResource(type, id);
            if (res == null || !id.getPath().endsWith("shaders/include/fa_parallax.glsl")) return res;
            return () -> {
                String head = defines();
                bakedDefines = head;
                byte[] body;
                try (var in = res.get()) {
                    body = in.readAllBytes();
                }
                byte[] h = head.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] all = new byte[h.length + body.length];
                System.arraycopy(h, 0, all, 0, h.length);
                System.arraycopy(body, 0, all, h.length, body.length);
                return new java.io.ByteArrayInputStream(all);
            };
        }

        @Override
        public void listResources(PackType type, String ns, String path, ResourceOutput out) {
            pack.listResources(type, ns, path, out);
        }

        @Override
        public java.util.Set<String> getNamespaces(PackType type) {
            return pack.getNamespaces(type);
        }

        @Override
        public <T> T getMetadataSection(net.minecraft.server.packs.metadata.MetadataSectionSerializer<T> s) throws java.io.IOException {
            return pack.getMetadataSection(s);
        }

        @Override
        public String packId() {
            return pack.packId();
        }

        @Override
        public boolean isBuiltin() {
            return pack.isBuiltin();
        }

        @Override
        public void close() {
            pack.close();
        }
    }

    // ---- Embeddium ----

    private static final Path MARKER = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("factoryascension-depth-embeddium.pending");
    private static Boolean embeddiumOk;
    private static int okTicks = 0;

    /**
     * Embeddium asks for a shader's source: ours (from the depth pack) if the pack is on and has one, else null (its own).
     * Called by mixin/EmbeddiumShaderMixin; never throws.
     */
    @javax.annotation.Nullable
    public static String embeddiumSource(net.minecraft.resources.ResourceLocation name) {
        try {
            if (embeddiumOk == null) {
                embeddiumOk = !java.nio.file.Files.exists(MARKER);
                if (!embeddiumOk) org.slf4j.LoggerFactory.getLogger("Factory Ascension")
                    .warn("Depth pack: the game closed last time while Embeddium was using our block shaders; using Embeddium's own this time");
                java.nio.file.Files.deleteIfExists(MARKER);
            }
            if (!embeddiumOk || FAClientConfig.SAFE_MODE.get() || !enabled()) return null;
            var id = new net.minecraft.resources.ResourceLocation(name.getNamespace(), "shaders/" + name.getPath());
            var res = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
            if (res == null || !PACK_ID.equals(res.sourcePackId())) return null;
            String src;
            try (var in = res.open()) {
                src = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            if (!java.nio.file.Files.exists(MARKER)) java.nio.file.Files.writeString(MARKER, "Embeddium is building Factory Ascension's depth shaders");
            okTicks = 0;
            return src;
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Depth pack: couldn't hand Embeddium our shaders, it keeps its own", err);
            return null;
        }
    }

    // A few seconds into a world with our shaders in use, they clearly work: clear the marker
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class EmbeddiumWatch {
        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || Minecraft.getInstance().level == null) return;
            if (++okTicks == 100) {
                try {
                    java.nio.file.Files.deleteIfExists(MARKER);
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }
}
