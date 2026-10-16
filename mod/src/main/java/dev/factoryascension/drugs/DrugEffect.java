package dev.factoryascension.drugs;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A drug's effect: the numbers live in drugs/Drugs (and the pack's config); this is its icon and colour. */
public class DrugEffect extends MobEffect {
    public DrugEffect(MobEffectCategory cat, int colour) {
        super(cat, colour);
    }
}
