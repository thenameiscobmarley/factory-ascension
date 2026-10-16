package dev.factoryascension.armor;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Night vision goggles, worn on the head. N flips them down and up. Down, you see in the dark through them: green
 * phosphor (Gen 2: grainier) or white phosphor (Gen 3: the cold blue-white kind, cleaner), with the tube's round
 * edge, the noise and the bloom around every light (client/NodsClient draws it).
 */
public class NodsItem extends ArmorItem {
    public final boolean white;

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override public int getDurabilityForType(Type t) { return 400; }
        @Override public int getDefenseForType(Type t) { return 1; }
        @Override public int getEnchantmentValue() { return 5; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(net.minecraft.world.item.Items.GLASS_PANE); }
        @Override public String getName() { return FactoryAscension.MODID + ":nods"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    };

    public NodsItem(boolean white, Properties props) {
        super(MATERIAL, Type.HELMET, props);
        this.white = white;
    }

    public static boolean down(ItemStack s) {
        return s.hasTag() && s.getTag().getBoolean("Down");
    }

    public static void setDown(ItemStack s, boolean d) {
        s.getOrCreateTag().putBoolean("Down", d);
    }

    /** The worn goggles, if they're down. */
    @Nullable
    public static NodsItem active(Player p) {
        ItemStack h = p.getItemBySlot(EquipmentSlot.HEAD);
        return h.getItem() instanceof NodsItem n && down(h) ? n : null;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide && down(stack) && player.tickCount % 20 == 0) {
            var nv = player.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 260) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, false));
        }
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return FactoryAscension.MODID + ":textures/models/armor/nods_" + (down(stack) ? "down" : "up") + ".png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal((white ? "White phosphor" : "Green phosphor") + (down(stack) ? ", down" : ", up")).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Wear on your head; N flips them down and up").withStyle(ChatFormatting.DARK_GRAY));
    }
}
