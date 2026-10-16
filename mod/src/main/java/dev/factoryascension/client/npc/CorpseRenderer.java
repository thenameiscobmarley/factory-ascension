package dev.factoryascension.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.npc.Corpse;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A fallen operator: its own skin, lying on its back, one arm out. */
public class CorpseRenderer extends EntityRenderer<Corpse> {
    private final PlayerModel<net.minecraft.world.entity.LivingEntity> model;

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        model = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        shadowRadius = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(Corpse c) {
        return new ResourceLocation(FactoryAscension.MODID, "textures/entity/operator/" + c.kind() + ".png");
    }

    @Override
    public void render(Corpse c, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180 - c.getYRot()));
        pose.translate(0, 0.15, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-90));         // on its back
        pose.translate(0, -1.0, 0);
        pose.scale(-0.9375f, -0.9375f, 0.9375f);
        pose.translate(0, -1.501, 0);
        int seed = c.getId();
        model.young = false;
        model.crouching = false;
        model.head.setRotation(0, (seed % 7 - 3) * 0.15f, 0);
        model.hat.copyFrom(model.head);
        model.body.setRotation(0, 0, 0);
        model.rightArm.setRotation(0, 0, 0.25f + (seed % 3) * 0.3f);
        model.leftArm.setRotation(0, 0, -0.2f);
        model.rightLeg.setRotation(0, 0, 0.08f);
        model.leftLeg.setRotation(0, 0, -0.12f - (seed % 2) * 0.1f);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(c))), light, OverlayTexture.NO_OVERLAY, 0.85f, 0.82f, 0.8f, 1);
        pose.popPose();
        super.render(c, yaw, partial, pose, buffers, light);
    }
}
