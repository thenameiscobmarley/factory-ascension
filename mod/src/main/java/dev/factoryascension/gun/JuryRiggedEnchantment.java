package dev.factoryascension.gun;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Jury-Rigged: tape, zip ties and optimism. A gun with it takes any attachment at the Weapons Workbench, whatever the
 * gun, and up to four in every slot: a sniper scope on a revolver, three suppressors, a drum on a double barrel, an
 * auto sear in a bolt rifle. Found as a treasure book (or made at the workbench); goes on a gun at an anvil.
 */
public class JuryRiggedEnchantment extends Enchantment {
    public static final EnchantmentCategory GUNS = EnchantmentCategory.create("FACTORYASCENSION_GUNS", item -> item instanceof GunItem);
    public static final int MAX_PER_SLOT = 4;

    public JuryRiggedEnchantment() {
        super(Rarity.RARE, GUNS, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public int getMinCost(int level) {
        return 20;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }
}
