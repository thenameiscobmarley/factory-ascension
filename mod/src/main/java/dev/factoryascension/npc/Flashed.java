package dev.factoryascension.npc;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Flashed: a white-out that fades (client/FlashOverlay draws it) and ringing ears. */
public class Flashed extends MobEffect {
    public Flashed() {
        super(MobEffectCategory.HARMFUL, 0xF0F0F0);
    }
}
