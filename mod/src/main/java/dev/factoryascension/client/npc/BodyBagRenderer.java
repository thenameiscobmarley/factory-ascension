package dev.factoryascension.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.client.gun.GunMesh;
import dev.factoryascension.raid.BodyBag;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A body bag on the ground (gunmesh/car/body_bag, tools/vehicles/car_mesh.py), with whose it was floating over it. */
public class BodyBagRenderer extends EntityRenderer<BodyBag> {
    public BodyBagRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(BodyBag e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(BodyBag b, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - b.getYRot()));
        GunMesh m = GunMesh.get("car/body_bag");
        if (m != null) m.draw(pose, buffers, light, OverlayTexture.NO_OVERLAY, 0x2A2C26, null, 0);
        pose.popPose();
        super.render(b, yaw, partial, pose, buffers, light);
    }

    @Override
    protected boolean shouldShowName(BodyBag b) {
        return !b.ownerName().isEmpty() && entityRenderDispatcher.distanceToSqr(b) < 64;
    }

    @Override
    protected void renderNameTag(BodyBag b, net.minecraft.network.chat.Component name, PoseStack pose, MultiBufferSource buffers, int light) {
        super.renderNameTag(b, net.minecraft.network.chat.Component.literal(b.ownerName() + "'s things"), pose, buffers, light);
    }
}
