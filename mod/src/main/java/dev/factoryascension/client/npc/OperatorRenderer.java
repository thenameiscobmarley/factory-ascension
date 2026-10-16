package dev.factoryascension.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.gun.GunRenderer;
import dev.factoryascension.npc.Operator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * An operator looks like a player: the player model (with its jacket, sleeves and trouser layers, which their skins use
 * for helmets, plate carriers and pouches), armor over it, the gun shouldered in both hands when it's aiming, crouched
 * when it's in cover. Up close its gun is drawn smooth, like yours.
 */
public class OperatorRenderer extends HumanoidMobRenderer<Operator, PlayerModel<Operator>> {
    public OperatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(Operator e) {
        return new ResourceLocation(FactoryAscension.MODID, "textures/entity/operator/" + e.type.id + ".png");
    }

    @Override
    public void render(Operator e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PlayerModel<Operator> m = getModel();
        boolean armed = !e.getMainHandItem().isEmpty();
        m.rightArmPose = e.aiming() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : armed ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        m.leftArmPose = e.aiming() ? HumanoidModel.ArmPose.CROSSBOW_HOLD : HumanoidModel.ArmPose.EMPTY;
        m.crouching = e.isCrouching();
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        boolean near = e.position().distanceToSqr(cam) < 100;
        GunRenderer.closeUp = near;
        try {
            super.render(e, yaw, partial, pose, buffers, light);
        } finally {
            GunRenderer.closeUp = false;
        }
    }
}
