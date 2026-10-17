package dev.factoryascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The pack's default settings, applied once (the first time the title screen opens with this version; after that your
 * own changes stand): GUI scale 3, 20 chunks, fullscreen, 69% mouse sensitivity, FOV 95, Shift sprints, Ctrl crouches,
 * Tab opens the inventory, E shows the player list, FTB Ultimine on mouse button 5, smoke 3.0, lighting High.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PackDefaults {
    private static boolean done = false;

    private PackDefaults() {
    }

    @SubscribeEvent
    public static void title(ScreenEvent.Init.Post event) {
        if (done || !(event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
        done = true;
        try {
            addPacksOnce(Minecraft.getInstance());
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Couldn't switch the mod's resource packs on", err);
        }
        if (FAClientConfig.PACK_DEFAULTS.get()) return;
        try {
            apply(Minecraft.getInstance());
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Couldn't apply the pack's default settings", err);
        }
        FAClientConfig.PACK_DEFAULTS.set(true);
        FAClientConfig.SMOKE_AMOUNT.set(3.0);
        FAClientConfig.LIGHTING.set(3);
        FAClientConfig.PACK_DEFAULTS.save();
    }

    /**
     * The first time the title screen opens (resources are loaded by then): switch on the mod's built-in packs, the
     * depth pack and the HD pack, each only once ever (so turning one off sticks), with a single reload for both.
     */
    static void addPacksOnce(Minecraft mc) {
        var repo = mc.getResourcePackRepository();
        repo.reload();
        var ids = new java.util.ArrayList<>(repo.getSelectedIds());
        boolean changed = false;
        if (!FAClientConfig.DEPTH_ADDED.get()) {
            FAClientConfig.DEPTH_ADDED.set(true);
            if (repo.getPack(DepthShaders.PACK_ID) != null && !ids.contains(DepthShaders.PACK_ID)) { ids.add(DepthShaders.PACK_ID); changed = true; }
        }
        if (!FAClientConfig.HD_ADDED.get()) {
            FAClientConfig.HD_ADDED.set(true);
            if (repo.getPack(dev.factoryascension.client.hd.HdPack.PACK_ID) != null && !ids.contains(dev.factoryascension.client.hd.HdPack.PACK_ID)) {
                ids.add(dev.factoryascension.client.hd.HdPack.PACK_ID);
                changed = true;
            }
        }
        FAClientConfig.HD_ADDED.save();
        if (!changed) return;
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);   // saves the list and reloads once
    }

    static void apply(Minecraft mc) {
        var o = mc.options;
        o.guiScale().set(3);
        o.renderDistance().set(20);
        o.sensitivity().set(0.345);          // the slider shows this as 69%
        o.fov().set(95);
        o.fullscreen().set(true);
        bind(o.keySprint, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT);
        bind(o.keyShift, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL);
        bind(o.keyInventory, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB);
        bind(o.keyPlayerList, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E);
        for (KeyMapping k : o.keyMappings) {
            if (k.getName().toLowerCase(java.util.Locale.ROOT).contains("ultimine")) bind(k, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5);
        }
        KeyMapping.resetMapping();
        o.save();
        if (!mc.getWindow().isFullscreen()) mc.getWindow().toggleFullScreen();
        mc.resizeDisplay();
    }

    static void bind(KeyMapping k, InputConstants.Type type, int code) {
        k.setKey(type.getOrCreate(code));
    }
}
