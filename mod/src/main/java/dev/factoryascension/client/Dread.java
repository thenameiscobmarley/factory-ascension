package dev.factoryascension.client;

import dev.factoryascension.FASounds;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.gun.GunAnim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dread: what makes going out frightening (FAClientConfig.DREAD turns it off).
 *  - Your heart: heard when you're stressed or badly hurt, faster and louder the worse it is; heavy breathing when
 *    you're panicking or have run a long way.
 *  - Distant gunfire: out in the danger zones, now and then, single shots or bursts far off in some direction (more
 *    of it the deeper you go, and at night). Someone else is out there.
 *  - Ringing ears: a blast or a big gun going off right beside you (or your own heavy gun indoors) leaves your ears
 *    ringing for a few seconds; everything else sounds muffled and far away, and the realism camera goes soft.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Dread {
    /** The danger level where you stand (from the server). */
    public static int danger = 0;
    private static float ring = 0;
    private static int heartIn = 0, breathIn = 0, ambientIn = 200, sprintTicks = 0;
    private static final RandomSource RNG = RandomSource.create();

    private Dread() {
    }

    /** 0..1: how loud your ears are ringing. */
    public static float ring() {
        return ring;
    }

    /** 0..1: how deep into the danger zones you are (for the camera's grade). */
    public static float dreadAmount() {
        return Math.min(1, danger / 8f);
    }

    static boolean on() {
        return FAClientConfig.DREAD.get();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        ring = Math.max(0, ring - 0.0055f);
        if (!on() || !p.isAlive()) return;
        SoundManager sm = mc.getSoundManager();
        float hp = p.getHealth() / Math.max(1, p.getMaxHealth());
        float fear = Math.max(GunAnim.stress, hp < 0.5f ? (0.5f - hp) * 2 : 0);
        // Heart
        if (fear > 0.3f && --heartIn <= 0) {
            float k = (fear - 0.3f) / 0.7f;
            heartIn = Math.round(20 * (1.05f - 0.6f * k));
            sm.play(SimpleSoundInstance.forUI(FASounds.HEARTBEAT.get(), 0.95f + RNG.nextFloat() * 0.1f, 0.2f + 0.7f * k));
        }
        // Breath: panic, or a long run
        sprintTicks = p.isSprinting() ? sprintTicks + 1 : Math.max(0, sprintTicks - 2);
        if ((fear > 0.5f || sprintTicks > 140) && --breathIn <= 0) {
            breathIn = 44 + RNG.nextInt(24) - Math.round(fear * 16);
            sm.play(SimpleSoundInstance.forUI(FASounds.BREATH.get(), 0.95f + RNG.nextFloat() * 0.1f, 0.25f + 0.35f * Math.max(fear, Math.min(1, sprintTicks / 400f))));
        }
        // Gunfire somewhere far off, out in the zones
        if (danger > 0 && --ambientIn <= 0) {
            boolean night = mc.level.isNight();
            int base = Math.max(300, 2400 - danger * 220);
            ambientIn = (int) ((base + RNG.nextInt(base)) * (night ? 0.6f : 1f));
            double a = RNG.nextDouble() * Math.PI * 2;
            double x = p.getX() + Math.cos(a) * 14, z = p.getZ() + Math.sin(a) * 14;
            SoundEvent s = RNG.nextFloat() < 0.4f ? FASounds.DISTANT_BURST.get() : FASounds.DISTANT.get();
            sm.play(new SimpleSoundInstance(s, SoundSource.AMBIENT, 0.5f + RNG.nextFloat() * 0.8f, 0.8f + RNG.nextFloat() * 0.3f, RNG, x, p.getY() + 4, z));
            // Sometimes the answer: a few seconds later, more from the same side
            if (RNG.nextFloat() < 0.35f) ambientIn = 40 + RNG.nextInt(80);
        }
    }

    /** Loud things close by ring your ears; while they ring, every other sound is muffled. */
    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s == null || !on()) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        ResourceLocation id = s.getLocation();
        if (id.getNamespace().equals(FactoryAscension.MODID) && id.getPath().startsWith("dread.")) return;
        double d = Math.sqrt(p.distanceToSqr(s.getX(), s.getY(), s.getZ()));
        float hit = 0;
        String path = id.getPath();
        if (path.equals("entity.generic.explode") || path.equals("grenade.explode")) hit = d < 7 ? 1 - (float) d / 9 : 0;
        else if (id.getNamespace().equals(FactoryAscension.MODID) && path.startsWith("gun.") && !path.equals("gun.suppressed") && !path.startsWith("gun.distant")
            && !path.contains("mag") && !path.equals("gun.rack") && !path.equals("gun.bolt") && !path.equals("gun.pump") && !path.equals("gun.dry")
            && !path.equals("gun.casing") && !path.equals("gun.shell_in")) {
            boolean heavy = path.equals("gun.fifty") || path.equals("gun.shotgun") || path.equals("gun.magnum") || path.equals("gun.sniper");
            boolean indoors = mc.level.getBrightness(LightLayer.SKY, p.blockPosition()) < 4;
            if (d < 1.5 && (heavy || indoors)) hit = heavy && indoors ? 0.7f : 0.25f;   // your own
            else if (d >= 1.5 && d < 4) hit = heavy ? 0.6f : 0.3f;                        // someone right beside you
        }
        if (hit > 0.2f) {
            boolean fresh = ring < 0.3f;
            ring = Math.min(1, Math.max(ring, hit));
            if (fresh) mc.getSoundManager().play(SimpleSoundInstance.forUI(FASounds.RINGING.get(), 1, 0.25f + 0.5f * ring));
        }
        if (ring > 0.25f && !(s instanceof TickableSoundInstance)) event.setSound(new Muffled(s, 1 - 0.75f * ring));
    }

    /** Another sound, quieter and duller. */
    static final class Muffled implements SoundInstance {
        final SoundInstance in;
        final float k;

        Muffled(SoundInstance in, float k) {
            this.in = in;
            this.k = k;
        }

        @Override public ResourceLocation getLocation() { return in.getLocation(); }
        @Override public WeighedSoundEvents resolve(SoundManager m) { return in.resolve(m); }
        @Override public Sound getSound() { return in.getSound(); }
        @Override public SoundSource getSource() { return in.getSource(); }
        @Override public boolean isLooping() { return in.isLooping(); }
        @Override public boolean isRelative() { return in.isRelative(); }
        @Override public int getDelay() { return in.getDelay(); }
        @Override public float getVolume() { return in.getVolume() * k; }
        @Override public float getPitch() { return in.getPitch() * (0.85f + 0.15f * k); }
        @Override public double getX() { return in.getX(); }
        @Override public double getY() { return in.getY(); }
        @Override public double getZ() { return in.getZ(); }
        @Override public Attenuation getAttenuation() { return in.getAttenuation(); }
    }
}
