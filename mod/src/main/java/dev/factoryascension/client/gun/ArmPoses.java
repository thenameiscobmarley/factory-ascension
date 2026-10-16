package dev.factoryascension.client.gun;

import dev.factoryascension.gun.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How a body holds a gun, seen from outside (you in third person, other players, the operators): shouldered and aimed,
 * a low ready carry, reloading (the support hand going down to the mag pouch and back, the gun tipped), fumbling (a
 * jerky tug, both hands at it), patching up (both hands at the chest). The motion runs off the entity's own tick count,
 * so every reload looks a little different on every body.
 *
 * No custom arm-pose entries (Forge's ArmPose.create): those extend the game's own list, and whether the game's lookup
 * table was built before or after they joined depends on which other mods load and when (Balm, Mekanism...), which
 * crashed the game near armed people ("Index 10 out of bounds for length 10"). Instead the game poses the body with its
 * own poses (shouldered: CROSSBOW_HOLD; otherwise EMPTY) and mixin/HumanoidPoseMixin calls {@link #after} right after,
 * where these arms are set.
 */
public final class ArmPoses {
    public enum Pose { NONE, LOW_READY, RELOAD, FUMBLE, MEDIC }

    private ArmPoses() {
    }

    static float t(LivingEntity e) {
        return e.tickCount + (e.getId() % 17) * 0.37f;
    }

    /** The game's own pose for a player's gun hand: shouldered while aiming or idle; ours (after) for the rest. */
    public static HumanoidModel.ArmPose vanillaFor(LivingEntity e, ItemStack gun) {
        return poseFor(e, gun) == Pose.NONE ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.EMPTY;
    }

    public static Pose poseFor(LivingEntity e, ItemStack gun) {
        int ph = GunItem.phase(gun);
        if (ph == GunItem.FUMBLE) return Pose.FUMBLE;
        if (ph != GunItem.NONE) return Pose.RELOAD;
        if (e.isUsingItem() && e.getUseItem() == gun) return Pose.NONE;
        if (e.isSprinting()) return Pose.LOW_READY;
        return Pose.NONE;
    }

    /** An operator: by its action and alert. */
    public static Pose forOperator(dev.factoryascension.npc.Operator o) {
        if (o.getMainHandItem().isEmpty()) return Pose.NONE;
        return switch (o.action()) {
            case 1 -> Pose.RELOAD;
            case 2 -> Pose.FUMBLE;
            case 3 -> Pose.MEDIC;
            default -> o.aiming() || o.alertLevel() != 2 ? Pose.NONE : Pose.LOW_READY;
        };
    }

    /** Right after the game posed a body: our gun poses on top. Never throws. */
    public static void after(HumanoidModel<?> model, LivingEntity e) {
        try {
            Pose p;
            if (e instanceof dev.factoryascension.npc.Operator o) p = forOperator(o);
            else if (e instanceof Player pl && GunItem.spec(pl.getMainHandItem()) != null) p = poseFor(pl, pl.getMainHandItem());
            else return;
            apply(model, e, p);
        } catch (Throwable ignored) {
        }
    }

    static void apply(HumanoidModel<?> model, LivingEntity e, Pose p) {
        switch (p) {
            case LOW_READY -> {
                float sway = Mth.sin(t(e) * 0.08f) * 0.04f;
                model.rightArm.xRot = -0.75f + sway;
                model.rightArm.yRot = -0.35f;
                model.rightArm.zRot = 0;
                model.leftArm.xRot = -0.95f + sway;
                model.leftArm.yRot = 0.55f;
                model.leftArm.zRot = 0;
            }
            case RELOAD -> {
                float k = (t(e) % 24) / 24f;                 // one hand trip to the pouch and back
                float down = Mth.sin(k * (float) Math.PI);
                model.rightArm.xRot = -0.85f + down * 0.15f;
                model.rightArm.yRot = -0.45f;
                model.rightArm.zRot = 0.15f;
                model.leftArm.xRot = -1.05f + down * 1.0f;   // down to the belt, back up to the mag well
                model.leftArm.yRot = 0.35f - down * 0.3f;
                model.leftArm.zRot = -0.1f;
                model.head.xRot += 0.35f;                    // looking at the gun
            }
            case FUMBLE -> {
                float tt = t(e);
                float jerk = Mth.sin(tt * 1.7f) * 0.25f + Mth.sin(tt * 3.1f) * 0.12f;
                model.rightArm.xRot = -0.7f + jerk;
                model.rightArm.yRot = -0.5f;
                model.leftArm.xRot = -0.4f - jerk * 1.6f;     // yanking at it
                model.leftArm.yRot = 0.2f;
                model.leftArm.zRot = -0.25f + jerk * 0.3f;
                model.head.xRot += 0.5f;
                model.head.yRot += Mth.sin(tt * 0.9f) * 0.2f;
            }
            case MEDIC -> {
                float w = Mth.sin(t(e) * 0.6f) * 0.15f;
                model.rightArm.xRot = -0.9f + w;
                model.rightArm.yRot = -0.6f;
                model.leftArm.xRot = -0.9f - w;
                model.leftArm.yRot = 0.6f;
                model.head.xRot += 0.6f;
            }
            default -> {
            }
        }
        model.hat.copyFrom(model.head);
    }
}
