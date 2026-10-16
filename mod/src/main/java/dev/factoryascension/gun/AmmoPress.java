package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Making rounds from anything (the Weapons Workbench's Ammo Press). Any item can be the propellant and any item the
 * case. Some are known (gunpowder is the standard, TNT is a lot, sugar barely works, a nether star is absurd); for
 * everything else the numbers come from what the item is (blocks are heavier, food is weak, rare things are strong)
 * plus a fixed per-item roll, so the same item always makes the same round. Weak cases misfire more.
 */
public final class AmmoPress {
    private AmmoPress() {
    }

    static final Map<String, float[]> PROPELLANTS = new HashMap<>();   // id -> {power}
    static final Map<String, float[]> CASES = new HashMap<>();         // id -> {reliability, colour}

    static {
        prop("minecraft:gunpowder", 1.0f);
        prop("minecraft:blaze_powder", 1.35f);
        prop("minecraft:tnt", 2.2f);
        prop("minecraft:fire_charge", 1.3f);
        prop("minecraft:redstone", 0.8f);
        prop("minecraft:glowstone_dust", 0.9f);
        prop("minecraft:sugar", 0.45f);
        prop("minecraft:coal", 0.7f);
        prop("minecraft:charcoal", 0.65f);
        prop("minecraft:dragon_breath", 2.0f);
        prop("minecraft:nether_star", 3.0f);
        prop("minecraft:ender_pearl", 1.1f);
        prop("minecraft:magma_cream", 1.2f);
        prop("minecraft:ghast_tear", 1.6f);
        prop("minecraft:egg", 0.2f);
        prop("minecraft:wheat_seeds", 0.15f);
        prop("kubejs:ascension_core", 1.8f);
        prop("kubejs:ascension_shard", 1.25f);
        cas("minecraft:copper_ingot", 1.0f, 0xC8803C);
        cas("minecraft:iron_ingot", 1.0f, 0xB8B8BC);
        cas("minecraft:iron_nugget", 0.96f, 0xB0B0B4);
        cas("minecraft:gold_ingot", 0.97f, 0xE8C040);
        cas("minecraft:gold_nugget", 0.94f, 0xE0B838);
        cas("minecraft:netherite_ingot", 1.0f, 0x4A4044);
        cas("minecraft:netherite_scrap", 0.98f, 0x5A4A44);
        cas("minecraft:diamond", 1.0f, 0x60E0E8);
        cas("minecraft:emerald", 0.99f, 0x40D070);
        cas("minecraft:amethyst_shard", 0.95f, 0xA070E0);
        cas("minecraft:paper", 0.72f, 0xE8E4D8);
        cas("minecraft:stick", 0.78f, 0x9A7448);
        cas("minecraft:clay_ball", 0.8f, 0xA0A8B4);
        cas("minecraft:brick", 0.88f, 0xB06040);
        cas("minecraft:slime_ball", 0.6f, 0x70C050);
        cas("minecraft:bone", 0.85f, 0xE8E0C8);
        cas("kubejs:ascendite_ingot", 1.0f, 0x40C8B8);
    }

    static void prop(String id, float power) {
        PROPELLANTS.put(id, new float[] {power});
    }

    static void cas(String id, float rel, int colour) {
        CASES.put(id, new float[] {rel, colour});
    }

    static String id(ItemStack s) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem()));
    }

    /** A fixed roll 0..1 for an item (the same every time). */
    static float roll(String id, int salt) {
        int h = id.hashCode() * 31 + salt * 0x9E3779B9;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535f;
    }

    /** Propellant strength, 1 = gunpowder. */
    public static float power(ItemStack prop) {
        String id = id(prop);
        float[] known = PROPELLANTS.get(id);
        if (known != null) return known[0];
        float p = 0.4f + roll(id, 1) * 1.0f;
        if (prop.getItem() instanceof BlockItem) p *= 1.15f;
        if (prop.isEdible()) p *= 0.6f;
        p *= 1 + 0.15f * prop.getRarity().ordinal();
        return Math.max(0.15f, Math.min(3f, p));
    }

    /** How reliable a case of this is, 0..1 (1 never misfires). */
    public static float reliability(ItemStack casing) {
        String id = id(casing);
        float[] known = CASES.get(id);
        if (known != null) return known[0];
        float r = 0.7f + roll(id, 2) * 0.28f;
        if (casing.getItem() instanceof TieredItem || casing.getItem() instanceof ArmorItem) r = Math.min(1, r + 0.08f);
        if (casing.isEdible()) r -= 0.15f;
        return Math.max(0.4f, Math.min(1f, r));
    }

    /** The case's natural colour: known, or the block's map colour, or a colour rolled from the item. */
    public static int colour(ItemStack casing) {
        String id = id(casing);
        float[] known = CASES.get(id);
        if (known != null) return (int) known[1];
        if (casing.getItem() instanceof BlockItem b) {
            int c = b.getBlock().defaultMapColor().col;
            if (c != 0) return c;
        }
        float h = roll(id, 3);
        return net.minecraft.util.Mth.hsvToRgb(h, 0.45f, 0.75f) & 0xFFFFFF;
    }

    /** How many rounds one press makes of a caliber (the pack's config, guns.ammo.perCraft). */
    public static int perPress(Caliber c) {
        return Math.max(1, FAConfig.ammoPerCraft.getOrDefault(c.id(), 8));
    }

    public static ItemStack press(Caliber cal, AmmoType type, ItemStack prop, ItemStack casing, int colour) {
        return CartridgeItem.make(cal, type, id(prop), id(casing), colour < 0 ? colour(casing) : colour, power(prop), reliability(casing), perPress(cal));
    }
}
