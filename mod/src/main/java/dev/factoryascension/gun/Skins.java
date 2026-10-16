package dev.factoryascension.gun;

import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gun skins: the list (names and blurbs), shared by the server (the Weapons Workbench puts them on) and the client
 * (client/gun/GunSkins draws them: animated colours on the gun's mesh, and for some extra 3D parts). A gun's skin is
 * its NBT "Skin" (an id here).
 */
public final class Skins {
    public record Info(String id, String name, String blurb) {
    }

    public static final Map<String, Info> ALL = new LinkedHashMap<>();

    private static void add(String id, String name, String blurb) {
        ALL.put(id, new Info(id, name, blurb));
    }

    static {
        add("obsidian", "Obsidian", "Volcanic glass. Magma creeps through the cracks.");
        add("snow", "Snow", "Packed snow on every ledge, flakes drifting off it.");
        add("black_water", "Black Water", "Dark water rolling down the gun in waves.");
        add("torn", "Torn", "Shredded camo wrap, strips flapping as you move.");
        add("gold_leaf", "Gold Leaf", "Gilded. A shine runs along it.");
        add("lava", "Lava", "Molten rock flowing under a cooling crust.");
        add("frost", "Frost", "Ice crystals growing out of it.");
        add("neon_grid", "Neon Grid", "Lines of light scrolling across a dark finish.");
        add("digital", "Digital Woodland", "Pixelated woodland camo.");
        add("arctic", "Arctic Camo", "White and grey blotches.");
        add("tiger", "Tiger Stripe", "Jungle stripes.");
        add("carbon", "Carbon Fibre", "Woven carbon with a sheen that moves.");
        add("damascus", "Damascus", "Folded steel, wavy layers.");
        add("rust", "Rust", "Left out for years. Flaking.");
        add("moss", "Overgrown", "Moss and grass growing on it, swaying.");
        add("bone", "Bone", "Ivory, cracked with age.");
        add("galaxy", "Galaxy", "Stars and drifting nebulae.");
        add("toxic", "Toxic", "Glowing slime oozing off it in drips.");
        add("blood", "Blood Moon", "Dark red, dripping.");
        add("chrome", "Chrome", "A mirror finish that catches the sky.");
        add("circuit", "Circuit", "Traces with pulses running along them.");
        add("blossom", "Cherry Blossom", "Pink and white, petals falling.");
        add("inferno", "Inferno", "Charred and burning, flames licking off the top.");
        add("glitch", "Glitch", "It doesn't render right. On purpose.");
        add("hazard", "Hazard", "Yellow and black warning stripes.");
        add("ghillie", "Ghillie", "Shaggy strands of burlap and leaves.");
        add("ember", "Ember", "Coals that glow brighter when you fire.");
        add("aurora", "Aurora", "Green and teal light ribbons rippling across it.");
    }

    private Skins() {
    }

    public static String of(ItemStack gun) {
        return gun.hasTag() ? gun.getTag().getString("Skin") : "";
    }

    public static void set(ItemStack gun, String id) {
        if (id == null || id.isEmpty() || !ALL.containsKey(id)) gun.getOrCreateTag().remove("Skin");
        else gun.getOrCreateTag().putString("Skin", id);
    }
}
