package dev.factoryascension.client;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The world around you, heard (all synthesized: tools/sound_real.py), placed in 3D around you:
 *  - Wind, whenever you're under the sky: gusts every few seconds, stronger the higher you are and in storms.
 *  - Birdsong by day where there are leaves near (a bird somewhere in the trees, now this side, now that).
 *  - Crickets on warm nights.
 *  - Underground: water dripping somewhere off in the dark, and now and then a deep rumble from the stone.
 * Pause > Factory Ascension... > Ambience turns it off.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Ambience {
    private static final RandomSource RNG = RandomSource.create();
    private static int windIn = 40, birdIn = 60, cricketIn = 80, dripIn = 100, rumbleIn = 600;

    private Ambience() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        var level = mc.level;
        if (p == null || level == null || mc.isPaused() || !FAClientConfig.on(FAClientConfig.AMBIENCE)) return;
        if (!level.dimensionType().hasSkyLight()) return;
        var at = p.blockPosition();
        float sky = level.getBrightness(LightLayer.SKY, at) / 15f;
        float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        long time = level.getDayTime() % 24000;
        boolean day = time < 12300 || time > 23500;
        if (sky > 0.4f) {
            // Wind: louder up high and in storms
            if (--windIn <= 0) {
                float height = Math.max(0, Math.min(1, (float) (p.getY() - 63) / 90f));
                float vol = 0.08f + 0.35f * height + 0.3f * rain + 0.3f * thunder;
                play(mc, FASounds.WIND.get(), vol * sky, 0.85f + RNG.nextFloat() * 0.3f, 10);
                windIn = 90 + RNG.nextInt(80) - Math.round(60 * (rain + height));
            }
            // Birds by day near leaves
            if (day && rain < 0.2f && --birdIn <= 0) {
                birdIn = 50 + RNG.nextInt(160);
                if (leavesNear(mc)) play(mc, FASounds.BIRD.get(), 0.25f + RNG.nextFloat() * 0.25f, 0.9f + RNG.nextFloat() * 0.25f, 8 + RNG.nextInt(14));
            }
            // Crickets on warm nights
            if (!day && rain < 0.2f && --cricketIn <= 0) {
                cricketIn = 70 + RNG.nextInt(90);
                if (level.getBiome(at).value().getBaseTemperature() > 0.5f)
                    play(mc, FASounds.CRICKETS.get(), 0.12f + RNG.nextFloat() * 0.12f, 0.95f + RNG.nextFloat() * 0.1f, 6 + RNG.nextInt(10));
            }
        } else if (p.getY() < 50 && sky < 0.1f) {
            // Underground
            if (--dripIn <= 0) {
                dripIn = 60 + RNG.nextInt(220);
                play(mc, FASounds.DRIP.get(), 0.2f + RNG.nextFloat() * 0.3f, 0.85f + RNG.nextFloat() * 0.3f, 6 + RNG.nextInt(14));
            }
            if (--rumbleIn <= 0) {
                rumbleIn = 900 + RNG.nextInt(1800);
                play(mc, FASounds.RUMBLE.get(), 0.3f, 0.8f + RNG.nextFloat() * 0.2f, 12);
            }
        }
    }

    /** Some leaves within a few blocks (a quick sample, not a full search). */
    static boolean leavesNear(Minecraft mc) {
        var p = mc.player.blockPosition();
        for (int i = 0; i < 24; i++) {
            var q = p.offset(RNG.nextInt(17) - 8, RNG.nextInt(9) - 1, RNG.nextInt(17) - 8);
            if (mc.level.getBlockState(q).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    /** A sound `dist` blocks away in a random direction, a little above you. */
    static void play(Minecraft mc, SoundEvent s, float vol, float pitch, int dist) {
        double a = RNG.nextDouble() * Math.PI * 2;
        var p = mc.player;
        mc.getSoundManager().play(new SimpleSoundInstance(s, SoundSource.AMBIENT, vol, pitch, RNG,
            p.getX() + Math.cos(a) * dist, p.getY() + 2 + RNG.nextInt(4), p.getZ() + Math.sin(a) * dist));
    }
}
