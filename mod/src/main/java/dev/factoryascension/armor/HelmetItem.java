package dev.factoryascension.armor;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A ballistic helmet: an armor class (like the plates: a round has to beat it), a chance to make a round glance off
 * (round shells do; bigger is better), and durability that wears as it's hit. Head hits are about one in eight
 * (armor/Ballistics); a stopped round is a bruise and a jolt, a glancing one is a sharp ping and nothing else.
 */
public class HelmetItem extends ArmorItem {
    public enum Kind {
        STEEL("steel_helmet", "SSh-68 Steel Helmet", 3.0f, 0.18f, 160, 1.6f),
        PASGT("pasgt_helmet", "PASGT Helmet", 3.2f, 0.22f, 220, 1.4f),
        FAST("fast_helmet", "FAST Helmet", 3.9f, 0.28f, 260, 1.0f),
        ALTYN("altyn_helmet", "Altyn Helmet (visor)", 5.0f, 0.38f, 420, 3.2f);
        public final String id, name;
        public final float protection, ricochet, weight;
        public final int durability;

        Kind(String id, String name, float protection, float ricochet, int durability, float weight) {
            this.id = id;
            this.name = name;
            this.protection = protection;
            this.ricochet = ricochet;
            this.durability = durability;
            this.weight = weight;
        }
    }

    public final Kind kind;

    static ArmorMaterial material(Kind k) {
        return new ArmorMaterial() {
            @Override public int getDurabilityForType(Type t) { return k.durability; }
            @Override public int getDefenseForType(Type t) { return 2; }
            @Override public int getEnchantmentValue() { return 4; }
            @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT); }
            @Override public String getName() { return FactoryAscension.MODID + ":" + k.id; }
            @Override public float getToughness() { return k.protection > 4 ? 2 : 0; }
            @Override public float getKnockbackResistance() { return 0; }
        };
    }

    public HelmetItem(Kind kind, Properties p) {
        super(material(kind), Type.HELMET, p);
        this.kind = kind;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return FactoryAscension.MODID + ":textures/models/armor/" + kind.id + ".png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(String.format("Class %.1f, %d%% ricochet, %.1f kg", kind.protection, Math.round(kind.ricochet * 100), kind.weight)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Condition " + Math.round(100 - 100f * stack.getDamageValue() / stack.getMaxDamage()) + "%").withStyle(ChatFormatting.DARK_GRAY));
    }
}
