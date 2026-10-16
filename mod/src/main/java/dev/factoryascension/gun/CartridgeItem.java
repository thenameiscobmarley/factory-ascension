package dev.factoryascension.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A round of any caliber, type and making, in one item. NBT: Cal (caliber id), Type (AmmoType), Prop and Case (the item
 * ids it was made from), Color (the case's colour), Power (propellant strength, 1 = gunpowder) and Rel (reliability,
 * 1 = never misfires). The Ammo Press (Weapons Workbench) makes them from any items; plain ones are crafted.
 */
public class CartridgeItem extends Item {
    public CartridgeItem(Properties p) {
        super(p);
    }

    public static ItemStack make(Caliber cal, AmmoType type, String prop, String casing, int colour, float power, float rel, int count) {
        ItemStack s = new ItemStack(dev.factoryascension.FactoryAscension.CARTRIDGE.get(), count);
        CompoundTag t = s.getOrCreateTag();
        t.putString("Cal", cal.id());
        t.putString("Type", type.name());
        t.putString("Prop", prop);
        t.putString("Case", casing);
        t.putInt("Color", colour);
        t.putFloat("Power", power);
        t.putFloat("Rel", rel);
        return s;
    }

    /** A plain round: gunpowder in a copper case. */
    public static ItemStack standard(Caliber cal, AmmoType type, int count) {
        return make(cal, type, "minecraft:gunpowder", "minecraft:copper_ingot", 0xC8803C, 1, 1, count);
    }

    public static Caliber caliber(ItemStack s) {
        return Caliber.byId(s.hasTag() ? s.getTag().getString("Cal") : "");
    }

    public static AmmoType type(ItemStack s) {
        return AmmoType.byName(s.hasTag() ? s.getTag().getString("Type") : "");
    }

    public static float power(ItemStack s) {
        return s.hasTag() && s.getTag().contains("Power") ? s.getTag().getFloat("Power") : 1;
    }

    public static float reliability(ItemStack s) {
        return s.hasTag() && s.getTag().contains("Rel") ? s.getTag().getFloat("Rel") : 1;
    }

    public static int colour(ItemStack s) {
        return s.hasTag() && s.getTag().contains("Color") ? s.getTag().getInt("Color") : 0xC8803C;
    }

    static String itemName(String id) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        return it == null ? id : new ItemStack(it).getHoverName().getString();
    }

    @Override
    public Component getName(ItemStack stack) {
        AmmoType t = type(stack);
        return Component.literal(caliber(stack).label + (t == AmmoType.REGULAR ? "" : " " + t.label) + " Round");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        AmmoType t = type(stack);
        tip.add(Component.literal(t.desc).withStyle(ChatFormatting.GRAY));
        if (stack.hasTag()) {
            String prop = stack.getTag().getString("Prop"), casing = stack.getTag().getString("Case");
            if (!prop.isEmpty()) tip.add(Component.literal("Propellant: " + itemName(prop) + String.format(" (power x%.2f)", power(stack))).withStyle(ChatFormatting.DARK_AQUA));
            if (!casing.isEmpty()) tip.add(Component.literal("Case: " + itemName(casing) + String.format(" (reliability %d%%)", Math.round(reliability(stack) * 100))).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
