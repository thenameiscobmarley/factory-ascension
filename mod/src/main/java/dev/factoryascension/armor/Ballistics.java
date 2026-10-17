package dev.factoryascension.armor;

import dev.factoryascension.gun.AmmoType;
import dev.factoryascension.gun.Caliber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A bullet hitting someone wearing armor plates. The hit lands on the torso about two times in three (the rest are arms,
 * legs, head: no plate); from the front it meets the front plate, from behind the back one. A plate stops a round whose
 * penetration is under its class (sometimes even a bit over, as it's fresh); what's left is a bruise (its blunt share).
 * A round that beats the plate goes through at most of its damage. Either way the plate wears, ceramic fastest; worn
 * plates protect less. Operators have their own plates (npc/Operator), the same rules.
 */
public final class Ballistics {
    private Ballistics() {
    }

    /** How hard a round is to stop: pistols ~1-2, intermediate rifles ~3, full-power ~3.6, magnums of the rifle world more. */
    public static float penetration(Caliber c, AmmoType t) {
        float p = switch (c) {
            case P9, P45, P762T, R357, R44 -> 1.8f;
            case P50 -> 2.4f;
            case P57, P46 -> 2.9f;
            case R545, R556, R300, R9X39 -> 3.1f;
            case R762S -> 3.3f;
            case R762N, R762R -> 3.7f;
            case R338 -> 4.8f;
            case B50 -> 6.5f;
            case G12 -> t == AmmoType.SLUG ? 2.2f : 1.0f;
        };
        if (t == AmmoType.ARMOR_PIERCING) p += 1.1f;
        if (t == AmmoType.HOLLOW_POINT || t == AmmoType.FRANGIBLE || t == AmmoType.RUBBER) p -= 1.0f;
        if (t == AmmoType.FLECHETTE) p += 0.6f;
        return p;
    }

    /** The damage that gets through to `victim` (and wears the plate). */
    public static float hit(LivingEntity victim, Entity shooter, float damage, Caliber cal, AmmoType type) {
        ItemStack plate = null, carrier = null;
        boolean front = shooter.position().subtract(victim.position()).normalize().dot(Vec3.directionFromRotation(0, victim.getYRot())) > 0;
        RandomSource r = victim.getRandom();
        // The head, about one hit in eight: a helmet may stop it or make it glance off
        float where = r.nextFloat();
        if (where < 0.12f) return helmet(victim, damage, penetration(cal, type), r);
        if (where > 0.70f) return damage;                    // arms or legs: no plate
        ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof PlateCarrierItem) {
            carrier = chest;
            plate = PlateCarrierItem.plate(chest, front);
        }
        if (plate == null && victim instanceof dev.factoryascension.npc.Operator o) return o.plateHit(damage, penetration(cal, type), front);
        if (plate == null || !(plate.getItem() instanceof PlateItem pi)) return damage;
        float health = 1 - plate.getDamageValue() / (float) plate.getMaxDamage();
        float effective = pi.plate.protection * (0.55f + 0.45f * health);
        float pen = penetration(cal, type);
        boolean stopped = pen < effective || (pen < effective + 0.5f && r.nextFloat() < health * 0.5f);
        int wear = Math.max(1, Math.round(damage * (stopped ? 1.0f : 0.6f) * (pi.plate == Plate.CERAMIC ? 2.2f : pi.plate == Plate.HYBRID ? 1.4f : 1)));
        plate.setDamageValue(plate.getDamageValue() + wear);
        boolean broke = plate.getDamageValue() >= plate.getMaxDamage();
        PlateCarrierItem.setPlate(carrier, front, broke ? null : plate);
        if (victim.level() instanceof ServerLevel sl) {
            sl.playSound(null, victim.getX(), victim.getY() + 1.2, victim.getZ(), broke ? SoundEvents.ITEM_BREAK : stopped ? SoundEvents.ANVIL_LAND : SoundEvents.ARMOR_EQUIP_IRON,
                SoundSource.PLAYERS, stopped ? 0.35f : 0.6f, stopped ? 1.8f : 1.2f);
        }
        return stopped ? damage * pi.plate.blunt : damage * 0.7f;
    }

    /** A round meeting the head: through it if no helmet or it beats the class; else a glance (nothing) or a stop (a bruise). */
    static float helmet(LivingEntity victim, float damage, float pen, RandomSource r) {
        ItemStack head = victim.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof HelmetItem hi)) return damage * 1.25f;          // a bare head hurts more
        float health = 1 - head.getDamageValue() / (float) Math.max(1, head.getMaxDamage());
        float effective = hi.kind.protection * (0.55f + 0.45f * health);
        ServerLevel sl = victim.level() instanceof ServerLevel l ? l : null;
        double y = victim.getEyeY();
        if (pen < effective + 0.8f && r.nextFloat() < hi.kind.ricochet * (0.5f + 0.5f * health)) {
            if (sl != null) sl.playSound(null, victim.getX(), y, victim.getZ(), dev.factoryascension.FASounds.RICOCHET.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            jolt(victim, 0.6f);
            head.setDamageValue(head.getDamageValue() + 2);
            return 0;
        }
        boolean stopped = pen < effective;
        head.setDamageValue(head.getDamageValue() + Math.max(1, Math.round(damage * (stopped ? 1.2f : 0.7f))));
        if (head.getDamageValue() >= head.getMaxDamage()) {
            victim.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            if (sl != null) sl.playSound(null, victim.getX(), y, victim.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
        } else if (sl != null) sl.playSound(null, victim.getX(), y, victim.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8f, 0.7f);
        jolt(victim, stopped ? 1.0f : 0.5f);
        return stopped ? damage * 0.18f : damage * 0.85f;
    }

    static void jolt(LivingEntity victim, float k) {
        if (victim instanceof net.minecraft.server.level.ServerPlayer sp)
            dev.factoryascension.network.FANetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new dev.factoryascension.network.FANetwork.Suppress(k));
    }
}
