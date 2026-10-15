package dev.factoryascension.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds mods that clash with the mod's client features and turns the feature off, with a message in chat:
 * "The Oculus mod conflicts with this mod's Shiny Reflections feature: Turned Shiny Reflections off."
 */
public final class ClientConflicts {
    /** [mod id, display name]. OptiFine isn't a Forge mod with an id, so it's found by one of its classes. */
    private static final String[][] SHADER_MODS = {{"oculus", "Oculus"}, {"iris", "Iris"}, {"optifine", "OptiFine"}};
    private static final List<Component> MESSAGES = new ArrayList<>();
    public static boolean reflectionsOff = false;

    private ClientConflicts() {
    }

    private static boolean present(String id) {
        if (id.equals("optifine")) {
            try {
                Class.forName("net.optifine.Config", false, ClientConflicts.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return ModList.get().isLoaded(id);
    }

    /** Called once at client setup. */
    public static void check() {
        for (String[] mod : SHADER_MODS) {
            if (!present(mod[0])) continue;
            reflectionsOff = true;
            MESSAGES.add(message(mod[1], "Shiny Reflections"));
            break;
        }
    }

    static Component message(String mod, String feature) {
        return Component.literal("[Factory Ascension] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("The " + mod + " mod conflicts with this mod's " + feature + " feature: Turned " + feature + " off.").withStyle(ChatFormatting.YELLOW));
    }

    /** Shown once per game session, when you first join a world. */
    static void tell() {
        if (MESSAGES.isEmpty() || !FAClientConfig.CONFLICT_MESSAGES.get()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        MESSAGES.forEach(m -> player.displayClientMessage(m, false));
        MESSAGES.clear();
    }
}
