package dev.factoryascension.drugs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Villagers take drugs too. Right-click one with a drug: it takes it (and pays you a few emeralds, and likes you better:
 * cheaper trades), holds it up and smokes it (puffs of smoke, its head tipped back), and then it hits: on meth or
 * cocaine it tweaks (twitching, snapping its head round, jumping, darting about, chattering in a high voice); on a joint
 * it's slow and happy; drunk it staggers and burps; tripping it stares at the sky; sedated it barely moves.
 */
public final class VillagerHighs {
    private VillagerHighs() {
    }

    static final int SMOKE_TICKS = 80, HIGH_TICKS = 20 * 60;

    static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractVillager v) || v.isBaby()) return;
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof DrugItem)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (v.level().isClientSide) return;
        var d = v.getPersistentData();
        long now = v.level().getGameTime();
        if (d.getLong("FAHighUntil") > now || d.getLong("FASmokeEnd") > now) {
            v.playSound(SoundEvents.VILLAGER_NO, 1, 1.4f);
            return;
        }
        ItemStack one = held.copyWithCount(1);
        if (!event.getEntity().isCreative()) held.shrink(1);
        v.setItemSlot(EquipmentSlot.MAINHAND, one);
        d.putString("FADrug", BuiltInRegistries.ITEM.getKey(one.getItem()).toString());
        d.putLong("FASmokeEnd", now + SMOKE_TICKS);
        v.playSound(SoundEvents.VILLAGER_YES, 1, 1.1f);
        // Payment, and a friend for life (well, for a while)
        RandomSource r = v.getRandom();
        v.spawnAtLocation(new ItemStack(Items.EMERALD, 1 + r.nextInt(3)));
        if (v instanceof Villager vil) vil.getGossips().add(event.getEntity().getUUID(), GossipType.MINOR_POSITIVE, 25);
    }

    static MobEffect effectFor(String drug) {
        return switch (drug) {
            case "factoryascension:methamphetamine", "factoryascension:cocaine", "factoryascension:energy_drink", "factoryascension:adrenaline_shot" -> Drugs.TWEAKING.get();
            case "factoryascension:joint" -> Drugs.STONED.get();
            case "factoryascension:vodka" -> Drugs.DRUNK.get();
            case "factoryascension:magic_mushrooms", "factoryascension:lsd_tab" -> Drugs.TRIPPING.get();
            case "factoryascension:ecstasy" -> Drugs.ROLLING.get();
            case "factoryascension:ketamine" -> Drugs.DISSOCIATED.get();
            default -> Drugs.SEDATED.get();
        };
    }

    static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractVillager v) || !(v.level() instanceof ServerLevel level)) return;
        var d = v.getPersistentData();
        if (!d.contains("FADrug")) return;
        long now = level.getGameTime();
        RandomSource r = v.getRandom();
        Vec3 mouth = v.getEyePosition().add(v.getLookAngle().scale(0.35)).add(0, -0.15, 0);
        long smokeEnd = d.getLong("FASmokeEnd");
        if (now < smokeEnd) {
            // Smoking: stands still, head back, a puff every second or so
            v.getNavigation().stop();
            v.setXRot(-25);
            if ((smokeEnd - now) % 18 == 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, 1, 0.02, 0.02, 0.02, 0.005);
                level.sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, 4, 0.05, 0.05, 0.05, 0.01);
                level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.15f, 1.6f);
            }
            if (now + 1 == smokeEnd) {
                v.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                MobEffect e = effectFor(d.getString("FADrug"));
                v.addEffect(new MobEffectInstance(e, HIGH_TICKS, 0));
                if (e == Drugs.TWEAKING.get()) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, HIGH_TICKS, 1));
                if (e == Drugs.STONED.get() || e == Drugs.SEDATED.get() || e == Drugs.DISSOCIATED.get()) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HIGH_TICKS, 1));
                d.putLong("FAHighUntil", now + HIGH_TICKS);
            }
            return;
        }
        if (now > d.getLong("FAHighUntil")) {
            d.remove("FADrug");
            d.remove("FAHighUntil");
            d.remove("FASmokeEnd");
            return;
        }
        if (v.hasEffect(Drugs.TWEAKING.get())) {
            // Tweaking: twitches, head snaps, jumps, darts off somewhere, chatters high
            if (r.nextInt(5) == 0) v.getLookControl().setLookAt(v.getX() + r.nextGaussian() * 4, v.getEyeY() + r.nextGaussian() * 2, v.getZ() + r.nextGaussian() * 4);
            if (r.nextInt(8) == 0) v.push((r.nextDouble() - 0.5) * 0.25, 0, (r.nextDouble() - 0.5) * 0.25);
            if (r.nextInt(40) == 0 && v.onGround()) v.getJumpControl().jump();
            if (r.nextInt(45) == 0) v.getNavigation().moveTo(v.getX() + r.nextInt(13) - 6, v.getY(), v.getZ() + r.nextInt(13) - 6, 1.3);
            if (r.nextInt(30) == 0) v.playSound(r.nextBoolean() ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.VILLAGER_TRADE, 1, 1.7f + r.nextFloat() * 0.4f);
            if (r.nextInt(12) == 0) level.sendParticles(ParticleTypes.CRIT, v.getX(), v.getEyeY() + 0.3, v.getZ(), 2, 0.2, 0.1, 0.2, 0.05);
        } else if (v.hasEffect(Drugs.STONED.get()) || v.hasEffect(Drugs.ROLLING.get())) {
            if (r.nextInt(60) == 0) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getEyeY() + 0.4, v.getZ(), 3, 0.3, 0.2, 0.3, 0);
            if (r.nextInt(50) == 0) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, 1, 0.02, 0.02, 0.02, 0.003);
            if (r.nextInt(90) == 0) v.playSound(SoundEvents.VILLAGER_YES, 0.8f, 0.7f);
        } else if (v.hasEffect(Drugs.DRUNK.get())) {
            if (r.nextInt(10) == 0) v.push((r.nextDouble() - 0.5) * 0.15, 0, (r.nextDouble() - 0.5) * 0.15);
            if (r.nextInt(120) == 0) v.playSound(SoundEvents.PLAYER_BURP, 1, 0.7f);
        } else if (v.hasEffect(Drugs.TRIPPING.get())) {
            v.getNavigation().stop();
            v.getLookControl().setLookAt(v.getX() + Math.sin(now * 0.03) * 3, v.getEyeY() + 6, v.getZ() + Math.cos(now * 0.03) * 3);
            if (r.nextInt(15) == 0) level.sendParticles(ParticleTypes.ENCHANT, v.getX(), v.getEyeY() + 0.5, v.getZ(), 4, 0.4, 0.4, 0.4, 0.3);
        } else {
            v.getNavigation().stop();
            v.setXRot(15);
        }
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(VillagerHighs::interact);
        MinecraftForge.EVENT_BUS.addListener(VillagerHighs::tick);
    }
}
