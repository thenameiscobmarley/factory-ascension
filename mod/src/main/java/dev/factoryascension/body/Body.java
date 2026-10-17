package dev.factoryascension.body;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.network.FANetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

/**
 * Your body, part by part (like Tarkov): head, thorax, stomach, both arms and both legs, each with its own health. A hit
 * lands somewhere (shots by how bodies get hit, falls on the legs, blasts all over); a part at zero is "blacked": a
 * blacked leg limps you, a blacked arm shakes your aim and slows your hands, a blacked stomach makes you thirsty fast.
 * Heavy hits can break an arm or a leg (a Splint sets it) or open heavy bleeding (a Tourniquet stops it); a Surgical Kit
 * brings a blacked part back. Parts mend slowly on their own while you're fed and watered.
 * And your water: it runs down over time (faster sprinting, in the heat, with a blacked stomach); drink to fill it
 * (water bottles, a Canteen, melon, milk, an energy drink). Dry, you're slow and your vision swims; empty, it hurts.
 * Everything here is in the config (body). The client shows it all (client/BodyHud).
 */
public final class Body {
    private Body() {
    }

    public enum Part {
        HEAD("Head", 35), THORAX("Thorax", 85), STOMACH("Stomach", 70), LEFT_ARM("Left arm", 60), RIGHT_ARM("Right arm", 60),
        LEFT_LEG("Left leg", 65), RIGHT_LEG("Right leg", 65);
        public final String label;
        public final float max;

        Part(String label, float max) {
            this.label = label;
            this.max = max;
        }

        boolean arm() { return this == LEFT_ARM || this == RIGHT_ARM; }
        boolean leg() { return this == LEFT_LEG || this == RIGHT_LEG; }
    }

    static RegistryObject<MobEffect> effect(String id, int colour) {
        return FactoryAscension.EFFECTS.register(id, () -> new MobEffect(MobEffectCategory.HARMFUL, colour) {
        });
    }

    public static final RegistryObject<MobEffect> HEAVY_BLEED = effect("heavy_bleeding", 0x8A0C0C);
    public static final RegistryObject<MobEffect> FRACTURE = effect("fracture", 0xC8C0A8);
    public static final RegistryObject<MobEffect> DEHYDRATED = effect("dehydrated", 0xC8A050);

    /** The whole state, kept in the player's persistent data ("FABody"). */
    public static final class State {
        public final float[] hp = new float[Part.values().length];
        public final boolean[] broken = new boolean[Part.values().length];
        public float water = 100;

        State() {
            for (Part p : Part.values()) hp[p.ordinal()] = p.max;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            for (Part p : Part.values()) {
                t.putFloat(p.name(), hp[p.ordinal()]);
                t.putBoolean(p.name() + "_B", broken[p.ordinal()]);
            }
            t.putFloat("Water", water);
            return t;
        }

        static State load(CompoundTag t) {
            State s = new State();
            if (t.isEmpty()) return s;
            for (Part p : Part.values()) {
                if (t.contains(p.name())) s.hp[p.ordinal()] = t.getFloat(p.name());
                s.broken[p.ordinal()] = t.getBoolean(p.name() + "_B");
            }
            if (t.contains("Water")) s.water = t.getFloat("Water");
            return s;
        }

        public boolean blacked(Part p) {
            return hp[p.ordinal()] <= 0;
        }
    }

    private static final java.util.WeakHashMap<Player, State> STATES = new java.util.WeakHashMap<>();
    private static final java.util.WeakHashMap<Player, float[]> SENT = new java.util.WeakHashMap<>();

    public static State of(Player p) {
        return STATES.computeIfAbsent(p, k -> State.load(p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("FABody")));
    }

    static void store(Player p, State s) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put("FABody", s.save());
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Aim: a blacked or broken arm shakes it (a multiplier on spread). */
    public static float aimShake(LivingEntity e) {
        if (!FAConfig.bodyEnabled || !(e instanceof Player p)) return 1;
        State s = of(p);
        float k = 1;
        for (Part a : new Part[] {Part.LEFT_ARM, Part.RIGHT_ARM}) {
            if (s.blacked(a)) k += 0.45f;
            else if (s.broken[a.ordinal()]) k += 0.3f;
            else k += 0.15f * (1 - s.hp[a.ordinal()] / a.max);
        }
        return k;
    }

    /** Hands: blacked or broken arms slow reloads (a multiplier on reload time). */
    public static float handSpeed(LivingEntity e) {
        if (!FAConfig.bodyEnabled || !(e instanceof Player p)) return 1;
        State s = of(p);
        float k = 1;
        for (Part a : new Part[] {Part.LEFT_ARM, Part.RIGHT_ARM}) if (s.blacked(a) || s.broken[a.ordinal()]) k += 0.3f;
        return k;
    }

    // ---- Getting hurt ----
    static Part pick(LivingHurtEvent event, ServerPlayer p) {
        var src = event.getSource();
        var r = p.getRandom();
        if (src.is(DamageTypes.FALL)) return r.nextBoolean() ? Part.LEFT_LEG : Part.RIGHT_LEG;
        // Where the attacker is above or below you, roughly what it can reach
        float x = r.nextFloat();
        if (x < 0.08f) return Part.HEAD;
        if (x < 0.40f) return Part.THORAX;
        if (x < 0.58f) return Part.STOMACH;
        if (x < 0.68f) return Part.LEFT_ARM;
        if (x < 0.78f) return Part.RIGHT_ARM;
        if (x < 0.89f) return Part.LEFT_LEG;
        return Part.RIGHT_LEG;
    }

    static void hurt(LivingHurtEvent event) {
        if (!FAConfig.bodyEnabled || !(event.getEntity() instanceof ServerPlayer p) || p.isCreative()) return;
        float amount = event.getAmount();
        if (amount <= 0 || event.getSource().is(DamageTypes.STARVE) || event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.WITHER)) return;
        State s = of(p);
        boolean blast = event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        java.util.List<Part> parts = new java.util.ArrayList<>();
        if (blast) parts.addAll(java.util.List.of(Part.values()));
        else if (event.getSource().is(DamageTypes.FALL)) { parts.add(Part.LEFT_LEG); parts.add(Part.RIGHT_LEG); }
        else parts.add(pick(event, p));
        float each = amount * FAConfig.bodyDamageScale / parts.size();
        var r = p.getRandom();
        for (Part part : parts) {
            int i = part.ordinal();
            boolean was = s.blacked(part);
            s.hp[i] = Math.max(0, s.hp[i] - each);
            if (!was && s.blacked(part)) p.displayClientMessage(Component.literal(part.label + " destroyed").withStyle(ChatFormatting.DARK_RED), true);
            // Fractures: a hard fall on the legs, or a heavy hit to a limb
            boolean limb = part.arm() || part.leg();
            if (limb && !s.broken[i] && (event.getSource().is(DamageTypes.FALL) ? amount > FAConfig.fractureFall : each > FAConfig.fractureHit && r.nextFloat() < 0.3f)) {
                s.broken[i] = true;
                p.level().playSound(null, p.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);
                p.displayClientMessage(Component.literal(part.label + " fractured: use a Splint").withStyle(ChatFormatting.RED), false);
            }
        }
        // Heavy bleeding from a hard hit (not falls)
        if (!event.getSource().is(DamageTypes.FALL) && amount > FAConfig.heavyBleedHit && r.nextFloat() < FAConfig.heavyBleedChance)
            p.addEffect(new MobEffectInstance(HEAVY_BLEED.get(), 20 * 600, 0));
        // A hit to a blacked part spreads to the rest of you
        store(p, s);
    }

    // ---- Every tick ----
    static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || !FAConfig.bodyEnabled) return;
        if (p.isCreative() || p.isSpectator()) return;
        State s = of(p);
        // Water
        float drain = FAConfig.waterPerTick * (p.isSprinting() ? 2 : 1) * (p.level().getBiome(p.blockPosition()).value().getBaseTemperature() > 1.0f ? 1.5f : 1)
            * (s.blacked(Part.STOMACH) ? 3 : 1);
        s.water = Math.max(0, s.water - drain);
        if (p.tickCount % 20 == 0) {
            if (s.water < 20) p.addEffect(new MobEffectInstance(DEHYDRATED.get(), 60, 0, false, false, true));
            if (s.water < 20) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
            if (s.water <= 0 && p.tickCount % 100 == 0) p.hurt(p.damageSources().starve(), 1);
            // Legs: blacked or broken legs limp you and stop you sprinting
            boolean legs = false;
            for (Part l : new Part[] {Part.LEFT_LEG, Part.RIGHT_LEG}) legs |= s.blacked(l) || s.broken[l.ordinal()];
            if (legs) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, false));
                p.setSprinting(false);
            }
            boolean any = false;
            for (boolean b : s.broken) any |= b;
            if (any) p.addEffect(new MobEffectInstance(FRACTURE.get(), 60, 0, false, false, true));
            // Mending: slowly, while fed and watered, not blacked parts
            if (p.getFoodData().getFoodLevel() > 14 && s.water > 40)
                for (Part part : Part.values()) if (!s.blacked(part)) s.hp[part.ordinal()] = Math.min(part.max, s.hp[part.ordinal()] + FAConfig.mendPerSecond);
            // Heavy bleeding
            if (p.hasEffect(HEAVY_BLEED.get()) && p.tickCount % 60 == 0) p.hurt(p.damageSources().magic(), 1.5f);
            store(p, s);
        }
        if (p.tickCount % 10 == 0) sync(p, s);
    }

    static void sync(ServerPlayer p, State s) {
        float[] v = new float[Part.values().length * 2 + 1];
        for (Part part : Part.values()) {
            v[part.ordinal()] = s.hp[part.ordinal()];
            v[Part.values().length + part.ordinal()] = s.broken[part.ordinal()] ? 1 : 0;
        }
        v[v.length - 1] = Math.round(s.water);
        float[] last = SENT.get(p);
        if (last != null && java.util.Arrays.equals(last, v)) return;
        SENT.put(p, v);
        FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.BodySync(v));
    }

    // ---- Treatment (body/Treatment items call these) ----
    public static boolean splint(ServerPlayer p) {
        State s = of(p);
        for (Part part : Part.values()) {
            if (s.broken[part.ordinal()]) {
                s.broken[part.ordinal()] = false;
                store(p, s);
                p.displayClientMessage(Component.literal(part.label + " splinted").withStyle(ChatFormatting.GREEN), true);
                return true;
            }
        }
        p.displayClientMessage(Component.literal("Nothing's broken"), true);
        return false;
    }

    public static boolean surgery(ServerPlayer p) {
        State s = of(p);
        for (Part part : Part.values()) {
            if (s.blacked(part)) {
                s.hp[part.ordinal()] = part.max * 0.35f;
                store(p, s);
                p.displayClientMessage(Component.literal(part.label + " restored").withStyle(ChatFormatting.GREEN), true);
                return true;
            }
        }
        p.displayClientMessage(Component.literal("No destroyed parts to fix"), true);
        return false;
    }

    public static void heal(ServerPlayer p, float amount) {
        State s = of(p);
        for (Part part : Part.values()) if (!s.blacked(part)) s.hp[part.ordinal()] = Math.min(part.max, s.hp[part.ordinal()] + amount);
        store(p, s);
    }

    public static void drink(ServerPlayer p, float amount) {
        State s = of(p);
        s.water = Math.min(100, s.water + amount);
        store(p, s);
    }

    /** Vanilla drinks count too: water bottles, milk, melon. */
    static void finished(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !FAConfig.bodyEnabled) return;
        var st = event.getItem();
        float w = 0;
        if (st.is(net.minecraft.world.item.Items.POTION) && net.minecraft.world.item.alchemy.PotionUtils.getPotion(st) == net.minecraft.world.item.alchemy.Potions.WATER) w = 35;
        else if (st.is(net.minecraft.world.item.Items.MILK_BUCKET)) w = 25;
        else if (st.is(net.minecraft.world.item.Items.MELON_SLICE)) w = 8;
        else if (st.is(net.minecraft.world.item.Items.SWEET_BERRIES) || st.is(net.minecraft.world.item.Items.GLOW_BERRIES)) w = 3;
        else if (st.getItem() == dev.factoryascension.drugs.Drugs.ENERGY_DRINK.get()) w = 20;
        if (w > 0) drink(p, w);
    }

    /** Dying resets you; so does a new life. */
    static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;
        STATES.remove(event.getEntity());
        store(event.getEntity(), new State());
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Body::hurt);
        MinecraftForge.EVENT_BUS.addListener(Body::tick);
        MinecraftForge.EVENT_BUS.addListener(Body::finished);
        MinecraftForge.EVENT_BUS.addListener(Body::respawn);
        Treatment.init();
    }
}
