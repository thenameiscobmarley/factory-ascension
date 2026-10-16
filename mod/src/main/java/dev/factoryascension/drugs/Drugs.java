package dev.factoryascension.drugs;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Drugs (a game, a single-player one: these are its effects, not anyone's advice). Each changes how you fight and how
 * the world looks (client/TripClient):
 *  - Joint: calm (stress melts away and builds slowly), slower, hungry, the world warm and hazy, aim a little loose.
 *  - Cocaine: fast, reloads quick, stress barely touches you; then a crash.
 *  - Methamphetamine: very fast, strong, no hunger, long; jittery hands, paranoia (now and then psychosis); a hard crash.
 *  - Morphine: hits hurt less, nothing frightens you, you move slower.
 *  - Adrenaline: a burst of health and speed, stress gone.
 *  - Magic mushrooms: the world breathes and shifts colour, and things are there that aren't.
 *  - Psychosis (an overdose, meth's comedown, mushrooms): figures standing far off who vanish when you come close, heads
 *    looking round corners that slide back when you turn to them, footsteps behind you (client/Hallucinations).
 * Crops and the Chem Lab make them (drugs/DrugBlocks).
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Drugs {
    private Drugs() {
    }

    static RegistryObject<MobEffect> effect(String id, MobEffectCategory c, int colour) {
        return FactoryAscension.EFFECTS.register(id, () -> new DrugEffect(c, colour));
    }

    public static final RegistryObject<MobEffect> STONED = effect("stoned", MobEffectCategory.NEUTRAL, 0x6AA040);
    public static final RegistryObject<MobEffect> WIRED = effect("wired", MobEffectCategory.BENEFICIAL, 0xE8E8F0);
    public static final RegistryObject<MobEffect> TWEAKING = effect("tweaking", MobEffectCategory.NEUTRAL, 0x9AD8F0);
    public static final RegistryObject<MobEffect> CRASH = effect("crash", MobEffectCategory.HARMFUL, 0x5A5048);
    public static final RegistryObject<MobEffect> NUMB = effect("numb", MobEffectCategory.BENEFICIAL, 0xD8C8A0);
    public static final RegistryObject<MobEffect> TRIPPING = effect("tripping", MobEffectCategory.NEUTRAL, 0xE070D0);
    public static final RegistryObject<MobEffect> PSYCHOSIS = effect("psychosis", MobEffectCategory.HARMFUL, 0x2A3A30);
    public static final RegistryObject<MobEffect> ROLLING = effect("rolling", MobEffectCategory.BENEFICIAL, 0xF080B0);
    public static final RegistryObject<MobEffect> SEDATED = effect("sedated", MobEffectCategory.NEUTRAL, 0x8090B0);
    public static final RegistryObject<MobEffect> DISSOCIATED = effect("dissociated", MobEffectCategory.NEUTRAL, 0x60A0A8);
    public static final RegistryObject<MobEffect> DRUNK = effect("drunk", MobEffectCategory.NEUTRAL, 0xC89040);

    static RegistryObject<Item> item(String id, int ticks, UseAnim anim, net.minecraft.sounds.SoundEvent s, String what, java.util.function.Consumer<ServerPlayer> apply) {
        return FactoryAscension.ITEMS.register(id, () -> new DrugItem(new Item.Properties().stacksTo(16), ticks, anim, s, what, apply));
    }

    static MobEffectInstance fx(MobEffect e, int sec, int amp) {
        return new MobEffectInstance(e, sec * 20, amp, false, false, true);   // no swirls in your face; the icon shows
    }

    public static final RegistryObject<Item> JOINT = item("joint", 50, UseAnim.TOOT_HORN, SoundEvents.FIRE_EXTINGUISH, "Smoke it (hold right-click): calm, slow, hungry, hazy",
        p -> { p.addEffect(fx(STONED.get(), 240, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 120, 0)); p.addEffect(fx(MobEffects.HUNGER, 60, 0));
            dev.factoryascension.gun.Stress.set(p, 0); });
    public static final RegistryObject<Item> COCAINE = item("cocaine", 24, UseAnim.EAT, SoundEvents.SAND_BREAK, "Sniff it: fast, quick hands, unshakeable; then the crash",
        p -> { p.addEffect(fx(WIRED.get(), 120, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 120, 1)); p.addEffect(fx(MobEffects.DIG_SPEED, 120, 1)); });
    public static final RegistryObject<Item> METH = item("methamphetamine", 32, UseAnim.TOOT_HORN, SoundEvents.FIRE_EXTINGUISH, "Smoke it: very fast and strong for a long time, jittery, paranoid; a hard crash",
        p -> { p.addEffect(fx(TWEAKING.get(), 300, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 300, 2)); p.addEffect(fx(MobEffects.DAMAGE_BOOST, 300, 0));
            p.addEffect(fx(MobEffects.SATURATION, 5, 0)); });
    public static final RegistryObject<Item> MORPHINE = item("morphine", 40, UseAnim.BOW, SoundEvents.BOTTLE_EMPTY, "Inject it: hits hurt less and nothing scares you, but you're slower",
        p -> { p.addEffect(fx(NUMB.get(), 180, 0)); p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 180, 1)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 180, 0));
            p.removeEffect(FactoryAscension.BLEEDING.get()); });
    public static final RegistryObject<Item> ADRENALINE = item("adrenaline_shot", 12, UseAnim.BOW, SoundEvents.BOTTLE_EMPTY, "Inject it: health and speed, right now",
        p -> { p.heal(8); p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 25, 1)); p.addEffect(fx(MobEffects.REGENERATION, 10, 1)); dev.factoryascension.gun.Stress.set(p, 0); });
    public static final RegistryObject<Item> MUSHROOMS = item("magic_mushrooms", 32, UseAnim.EAT, SoundEvents.GENERIC_EAT, "Eat them: the world breathes, and there are things in it that aren't there",
        p -> { p.addEffect(fx(TRIPPING.get(), 300, 0)); p.addEffect(fx(PSYCHOSIS.get(), 200, 0)); p.addEffect(fx(MobEffects.CONFUSION, 8, 0)); });

    public static final RegistryObject<Item> LSD = item("lsd_tab", 16, UseAnim.EAT, SoundEvents.GENERIC_EAT, "Put it on your tongue: hours of colour, breathing walls and trails",
        p -> { p.addEffect(fx(TRIPPING.get(), 600, 1)); if (p.getRandom().nextFloat() < 0.25f) p.addEffect(fx(PSYCHOSIS.get(), 180, 0)); });
    public static final RegistryObject<Item> ECSTASY = item("ecstasy", 16, UseAnim.EAT, SoundEvents.GENERIC_EAT, "Swallow it: everything is warm and wonderful, you heal, you're thirsty; then a low",
        p -> { p.addEffect(fx(ROLLING.get(), 240, 0)); p.addEffect(fx(MobEffects.REGENERATION, 60, 0)); p.addEffect(fx(MobEffects.HUNGER, 120, 0));
            dev.factoryascension.gun.Stress.set(p, 0); });
    public static final RegistryObject<Item> XANAX = item("xanax", 16, UseAnim.EAT, SoundEvents.GENERIC_EAT, "Swallow it: calm, steady hands, nothing frightens you; heavy eyes",
        p -> { p.addEffect(fx(SEDATED.get(), 300, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 120, 0)); dev.factoryascension.gun.Stress.set(p, 0); });
    public static final RegistryObject<Item> KETAMINE = item("ketamine", 24, UseAnim.EAT, SoundEvents.SAND_BREAK, "Sniff it: you float outside yourself; pain is far away, so is everything else",
        p -> { p.addEffect(fx(DISSOCIATED.get(), 150, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 150, 2)); p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 150, 1)); });
    public static final RegistryObject<Item> VODKA = item("vodka", 32, UseAnim.DRINK, SoundEvents.GENERIC_DRINK, "Drink it: brave, tough, warm, and the room won't hold still",
        p -> { p.addEffect(fx(DRUNK.get(), 240, 0)); p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 120, 0)); p.addEffect(fx(MobEffects.DAMAGE_BOOST, 120, 0));
            dev.factoryascension.gun.Stress.add(p, -0.4f); });
    public static final RegistryObject<Item> ENERGY_DRINK = item("energy_drink", 24, UseAnim.DRINK, SoundEvents.GENERIC_DRINK, "Drink it: a bit faster, a bit sharper, a bit shaky",
        p -> { p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 90, 0)); p.addEffect(fx(MobEffects.DIG_SPEED, 90, 0)); p.addEffect(fx(MobEffects.SATURATION, 2, 0)); });

    // ---- Mixes: taking one while another is still on you ----
    /** A combination: two effects on at once (the one you just took and one you already had) and what it does. */
    record Mix(String name, RegistryObject<MobEffect> a, RegistryObject<MobEffect> b, String what, java.util.function.Consumer<ServerPlayer> apply) {
    }

    static final java.util.List<Mix> MIXES = java.util.List.of(
        new Mix("Speedball", WIRED, NUMB, "fast, strong and fearless, and your heart doesn't like it", p -> {
            p.addEffect(fx(MobEffects.DAMAGE_BOOST, 90, 1)); p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 90, 1)); p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 90, 1));
            if (p.getRandom().nextFloat() < 0.2f) heart(p, 8); }),
        new Mix("Overamped", WIRED, TWEAKING, "your heart is hammering out of rhythm", p -> {
            p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 60, 3)); p.addEffect(fx(PSYCHOSIS.get(), 240, 1));
            if (p.getRandom().nextFloat() < 0.35f) heart(p, 12); }),
        new Mix("Candyflip", ROLLING, TRIPPING, "the trip turns warm and enormous", p -> {
            p.addEffect(fx(TRIPPING.get(), 400, 2)); p.addEffect(fx(MobEffects.REGENERATION, 60, 1)); }),
        new Mix("Hippie flip", ROLLING, PSYCHOSIS, "warm, and the shapes in the corners are friendly (mostly)", p -> {
            p.addEffect(fx(TRIPPING.get(), 300, 1)); p.addEffect(fx(MobEffects.REGENERATION, 40, 0)); }),
        new Mix("Crossfade", STONED, DRUNK, "the spins: you're greening out", p -> {
            p.addEffect(fx(MobEffects.CONFUSION, 20, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); p.addEffect(fx(MobEffects.HUNGER, 30, 2)); }),
        new Mix("Blackout", SEDATED, DRUNK, "you don't remember the next bit", p -> {
            p.addEffect(fx(MobEffects.BLINDNESS, 12, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 20, 4)); p.addEffect(fx(MobEffects.WEAKNESS, 120, 1));
            p.hurt(p.damageSources().magic(), 4); }),
        new Mix("Deep hole", DISSOCIATED, DRUNK, "you're very far away now", p -> {
            p.addEffect(fx(DISSOCIATED.get(), 200, 1)); p.addEffect(fx(MobEffects.BLINDNESS, 6, 0)); p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 30, 4)); }),
        new Mix("Wake and bake", STONED, WIRED, "calm and quick at once", p -> {
            p.addEffect(fx(MobEffects.MOVEMENT_SPEED, 120, 0)); p.addEffect(fx(MobEffects.DIG_SPEED, 120, 1)); })
    );

    /** Your heart gives out for a moment. */
    static void heart(ServerPlayer p, float dmg) {
        p.hurt(p.damageSources().magic(), dmg);
        p.addEffect(fx(MobEffects.CONFUSION, 12, 0));
        p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
        p.displayClientMessage(Component.literal("Your chest seizes. You can't breathe.").withStyle(ChatFormatting.DARK_RED), false);
    }

    /** Before a drug takes hold: what's already on you (for the mixes). */
    static java.util.Set<MobEffect> before(ServerPlayer p) {
        java.util.Set<MobEffect> s = new java.util.HashSet<>();
        for (var i : p.getActiveEffects()) s.add(i.getEffect());
        return s;
    }

    /** After it took hold: any mix of something new with something you already had goes off. */
    static void mixes(ServerPlayer p, java.util.Set<MobEffect> had) {
        for (Mix m : MIXES) {
            MobEffect a = m.a().get(), b = m.b().get();
            boolean on = p.hasEffect(a) && p.hasEffect(b);
            boolean fresh = !(had.contains(a) && had.contains(b));
            if (!on || !fresh) continue;
            m.apply().accept(p);
            p.displayClientMessage(Component.literal(m.name() + ": " + m.what()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** Doses close together: an overdose. */
    private static final Map<ServerPlayer, long[]> DOSES = new WeakHashMap<>();

    static void dose(ServerPlayer p) {
        long now = p.level().getGameTime();
        long[] d = DOSES.computeIfAbsent(p, k -> new long[3]);
        System.arraycopy(d, 0, d, 1, 2);
        d[0] = now;
        if (d[2] > 0 && now - d[2] < 1200) {
            p.hurt(p.damageSources().magic(), 6);
            p.addEffect(fx(PSYCHOSIS.get(), 300, 1));
            p.addEffect(fx(MobEffects.CONFUSION, 15, 0));
            p.displayClientMessage(Component.literal("Overdose. Your heart is pounding out of your chest.").withStyle(ChatFormatting.DARK_RED), true);
            d[0] = d[1] = d[2] = 0;
        }
    }

    /** How stressful things are with what you've taken (gun/Stress multiplies gains by this). */
    public static float stressGain(LivingEntity e) {
        float k = 1;
        if (e.hasEffect(STONED.get())) k *= 0.5f;
        if (e.hasEffect(WIRED.get())) k *= 0.3f;
        if (e.hasEffect(TWEAKING.get())) k *= 0.5f;
        if (e.hasEffect(NUMB.get())) k *= 0.4f;
        if (e.hasEffect(SEDATED.get())) k *= 0.2f;
        if (e.hasEffect(ROLLING.get())) k *= 0.3f;
        if (e.hasEffect(DRUNK.get())) k *= 0.6f;
        if (e.hasEffect(DISSOCIATED.get())) k *= 0.3f;
        if (e.hasEffect(CRASH.get()) || e.hasEffect(PSYCHOSIS.get())) k *= 1.6f;
        return k;
    }

    /** Reload speed (gun/GunItem multiplies step times by this). */
    public static float reloadTime(LivingEntity e) {
        float k = 1;
        if (e.hasEffect(WIRED.get())) k *= 0.72f;
        if (e.hasEffect(TWEAKING.get())) k *= 0.8f;
        if (e.hasEffect(STONED.get())) k *= 1.2f;
        if (e.hasEffect(CRASH.get())) k *= 1.25f;
        if (e.hasEffect(DRUNK.get())) k *= 1.3f;
        if (e.hasEffect(SEDATED.get())) k *= 1.15f;
        if (e.hasEffect(DISSOCIATED.get())) k *= 1.5f;
        return k;
    }

    /** Aim (spread multiplier). */
    public static float spread(LivingEntity e) {
        float k = 1;
        if (e.hasEffect(STONED.get())) k *= 1.3f;
        if (e.hasEffect(TWEAKING.get())) k *= 1.2f;
        if (e.hasEffect(WIRED.get())) k *= 0.9f;
        if (e.hasEffect(DRUNK.get())) k *= 1.6f;
        if (e.hasEffect(SEDATED.get())) k *= 0.8f;      // steady hands
        if (e.hasEffect(DISSOCIATED.get())) k *= 1.4f;
        return k;
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().hasEffect(NUMB.get())) event.setAmount(event.getAmount() * 0.8f);
        if (event.getEntity().hasEffect(DISSOCIATED.get())) event.setAmount(event.getAmount() * 0.85f);
    }

    /** When a stimulant wears off: the crash (and after meth, sometimes the voices). */
    @SubscribeEvent
    public static void expired(MobEffectEvent.Expired event) {
        var inst = event.getEffectInstance();
        if (inst == null || !(event.getEntity() instanceof ServerPlayer p)) return;
        MobEffect e = inst.getEffect();
        if (e == ROLLING.get()) {
            p.addEffect(fx(CRASH.get(), 90, 0));
            return;
        }
        if (e == WIRED.get() || e == TWEAKING.get()) {
            p.addEffect(fx(CRASH.get(), e == TWEAKING.get() ? 150 : 60, 0));
            p.addEffect(fx(MobEffects.WEAKNESS, e == TWEAKING.get() ? 150 : 60, 0));
            p.addEffect(fx(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            p.addEffect(fx(MobEffects.HUNGER, 40, 1));
            if (e == TWEAKING.get() && p.getRandom().nextFloat() < 0.5f) p.addEffect(fx(PSYCHOSIS.get(), 240, 0));
        }
    }

    /** Loaded with the mod (registers the effects and items). */
    public static void init() {
    }
}
