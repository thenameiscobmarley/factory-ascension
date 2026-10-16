package dev.factoryascension.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Right after the game poses a humanoid body: our gun poses on top (client/gun/ArmPoses.after). Named both ways (the
 * readable name in development, the game's own name when it's played) and optional: if neither is found, nothing
 * changes but those poses.
 */
@Mixin(value = HumanoidModel.class, remap = false)
public abstract class HumanoidPoseMixin {
    @Inject(method = {"setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", "m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"},
        at = @At("TAIL"), remap = false, require = 0)
    private void factoryascension$gunPoses(LivingEntity e, float limb, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        dev.factoryascension.client.gun.ArmPoses.after((HumanoidModel<?>) (Object) this, e);
    }
}
