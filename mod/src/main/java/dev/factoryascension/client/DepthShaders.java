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
 * Embeddium draws terrain with its own shaders, so blocks get no depth with it (mobs still do). It's an ordinary resource pack: if anything
 * looks wrong, switching it off (here, or in Resource Packs) puts the original shaders straight back.
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
                id -> new PathPackResources(id, path, false), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
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
}
