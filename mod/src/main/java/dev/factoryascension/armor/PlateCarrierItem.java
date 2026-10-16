package dev.factoryascension.armor;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A plate carrier (worn as a chestplate): a front and a back pocket for armor plates. It barely protects by itself;
 * the plates stop bullets (armor/Ballistics). Hold it with a plate in your other hand and right-click to put the plate
 * in (front first, then back); sneak and right-click to take the plates out.
 */
public class PlateCarrierItem extends ArmorItem {
    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override public int getDurabilityForType(Type t) { return 300; }
        @Override public int getDefenseForType(Type t) { return 2; }
        @Override public int getEnchantmentValue() { return 8; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(net.minecraft.world.item.Items.LEATHER); }
        @Override public String getName() { return FactoryAscension.MODID + ":plate_carrier"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    };

    public PlateCarrierItem(Properties props) {
        super(MATERIAL, Type.CHESTPLATE, props);
    }

    // ---- The pockets ----
    @Nullable
    public static ItemStack plate(ItemStack carrier, boolean front) {
        if (!carrier.hasTag()) return null;
        String k = front ? "Front" : "Back";
        if (!carrier.getTag().contains(k)) return null;
        ItemStack p = ItemStack.of(carrier.getTag().getCompound(k));
        return p.isEmpty() ? null : p;
    }

    public static void setPlate(ItemStack carrier, boolean front, @Nullable ItemStack plate) {
        String k = front ? "Front" : "Back";
        if (plate == null || plate.isEmpty()) carrier.getOrCreateTag().remove(k);
        else carrier.getOrCreateTag().put(k, plate.save(new CompoundTag()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack carrier = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown()) {
            boolean any = false;
            for (boolean front : new boolean[] {true, false}) {
                ItemStack p = plate(carrier, front);
                if (p == null) continue;
                setPlate(carrier, front, null);
                if (!player.getInventory().add(p)) player.drop(p, false);
                any = true;
            }
            if (any) level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8f, 0.8f);
            return any ? InteractionResultHolder.sidedSuccess(carrier, level.isClientSide) : InteractionResultHolder.pass(carrier);
        }
        if (other.getItem() instanceof PlateItem) {
            for (boolean front : new boolean[] {true, false}) {
                if (plate(carrier, front) != null) continue;
                setPlate(carrier, front, other.split(1));
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 0.8f, 1.1f);
                return InteractionResultHolder.sidedSuccess(carrier, level.isClientSide);
            }
            player.displayClientMessage(Component.literal("Both pockets are full (sneak + right-click to take the plates out)"), true);
            return InteractionResultHolder.fail(carrier);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        for (boolean front : new boolean[] {true, false}) {
            ItemStack p = plate(stack, front);
            String side = front ? "Front: " : "Back: ";
            if (p == null || !(p.getItem() instanceof PlateItem pi)) tip.add(Component.literal(side + "empty").withStyle(ChatFormatting.DARK_GRAY));
            else tip.add(Component.literal(side + pi.plate.name + String.format(" (class %.1f, %d%%)", pi.plate.protection,
                Math.round(100f * (p.getMaxDamage() - p.getDamageValue()) / p.getMaxDamage()))).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.literal("Plate in your other hand + right-click: put it in. Sneak + right-click: take them out").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Weight with its plates (for the Tarkov inventory's weight). */
    public static float weight(ItemStack carrier) {
        float w = 1.5f;
        for (boolean front : new boolean[] {true, false}) {
            ItemStack p = plate(carrier, front);
            if (p != null && p.getItem() instanceof PlateItem pi) w += pi.plate.weight;
        }
        return w;
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, net.minecraft.world.entity.Entity e) {
        return slot == EquipmentSlot.CHEST;
    }
}
