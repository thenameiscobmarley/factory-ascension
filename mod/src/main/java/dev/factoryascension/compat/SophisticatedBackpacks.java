package dev.factoryascension.compat;

import dev.factoryascension.FAConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlock;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ascension backpacks: tiers past Sophisticated Backpacks' Netherite one, as real Sophisticated backpacks, so every
 * Sophisticated upgrade works in them, they can be placed, dyed and worn like any other. Only loaded when
 * Sophisticated Backpacks is installed. Slot counts come from FAConfig (the pack's config, section backpacks).
 */
public final class SophisticatedBackpacks {
    /** Tier ids, smallest first (each is upgraded from the one before, the first from a Netherite Backpack). */
    public static final String[] TIERS = {"ascendite", "voidsteel", "celestium", "ascendium", "singularity", "ascended"};
    private static final List<RegistryObject<Block>> BLOCKS = new ArrayList<>();

    private SophisticatedBackpacks() {
    }

    public static void register(DeferredRegister<Block> blocks, DeferredRegister<Item> items, List<RegistryObject<Item>> tab) {
        for (int i = 0; i < TIERS.length; i++) {
            final int tier = i;
            String id = TIERS[i] + "_backpack";
            RegistryObject<Block> block = blocks.register(id, () -> new BackpackBlock(1200f));
            BLOCKS.add(block);
            tab.add(items.register(id, () -> new BackpackItem(() -> FAConfig.backpackSlots[tier], () -> FAConfig.backpackUpgrades[tier],
                () -> (BackpackBlock) block.get())));
        }
    }

    /** Sophisticated's backpack block entity only accepts its own blocks; add ours so placed backpacks keep their things. */
    public static void allowPlacing() {
        BlockEntityType<?> type = ModBlocks.BACKPACK_TILE_TYPE.get();
        Set<Block> valid = new HashSet<>(ObfuscationReflectionHelper.<Set<Block>, BlockEntityType<?>>getPrivateValue(BlockEntityType.class, type, "f_58915_"));
        BLOCKS.forEach(b -> valid.add(b.get()));
        ObfuscationReflectionHelper.setPrivateValue(BlockEntityType.class, type, valid, "f_58915_");
    }
}
