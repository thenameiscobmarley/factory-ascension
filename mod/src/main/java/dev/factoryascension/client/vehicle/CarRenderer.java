package dev.factoryascension.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.client.gun.GunMesh;
import dev.factoryascension.vehicle.Car;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the quad and the truck from their meshes (gunmesh/car/, tools/vehicles/car_mesh.py): the body (painted in its
 * colour) pitched and rolled, four wheels turning (the front ones steered), and on the truck, the gun turned to where
 * its gunner looks (kicking back as it fires).
 */
public class CarRenderer extends EntityRenderer<Car> {
    public CarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 1.2f;
    }

    @Override
    public ResourceLocation getTextureLocation(Car e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public boolean shouldRender(Car e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return f.isVisible(e.getBoundingBox().inflate(2.5));
    }

    @Override
    public void render(Car c, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PoseStack.Pose top = pose.last();
        try {
            Car.Model m = c.model();
            int colour = c.colour(), ov = OverlayTexture.NO_OVERLAY;
            float bodyYaw = Mth.rotLerp(partial, c.yRotO, c.getYRot());
            float steer = Mth.lerp(partial, c.steerO, c.steer), wheel = Mth.lerp(partial, c.wheelO, c.wheel);
            float pitch = Mth.lerp(partial, c.pitchO, c.pitch), roll = Mth.lerp(partial, c.rollO, c.roll);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
            pose.mulPose(Axis.ZP.rotationDegrees(pitch));
            // Wheels (not rolled with the body: the springs take it)
            String wm = "car/wheel_" + m.id;
            for (int k = 0; k < 4; k++) {
                boolean front = k < 2;
                float side = k % 2 == 0 ? -1 : 1;
                pose.pushPose();
                pose.translate((front ? m.front : m.rear) / 16f, m.wheelR / 16f, side * (m.track - 2.5f) / 16f);
                if (front) pose.mulPose(Axis.YP.rotationDegrees(steer * 28));
                if (side < 0) pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.mulPose(Axis.ZP.rotation(-wheel * (side < 0 ? -1 : 1)));
                draw(wm, pose, buffers, light, ov, colour);
                pose.popPose();
            }
            pose.mulPose(Axis.XP.rotationDegrees(roll));
            draw("car/" + m.id, pose, buffers, light, ov, colour);
            if (c.hasGun()) {
                var g = c.gunner();
                float gy = g != null ? Mth.rotLerp(partial, g.yHeadRotO, g.yHeadRot) : bodyYaw;
                float gp = g != null ? Mth.lerp(partial, g.xRotO, g.getXRot()) : 0;
                float kick = Mth.lerp(partial, c.recoilO, c.recoil);
                pose.pushPose();
                pose.translate(Car.MOUNT_X / 16f, Car.MOUNT_Y / 16f, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-(gy - bodyYaw)));
                pose.mulPose(Axis.ZP.rotationDegrees(-Mth.clamp(gp, -40, 30) + kick * 1.5f));
                pose.translate(-kick * 0.06f, -2.2f / 16f, 0);
                draw("car/turret", pose, buffers, light, ov, colour);
                pose.popPose();
            }
            pose.popPose();
            super.render(c, yaw, partial, pose, buffers, light);
        } catch (Throwable err) {
            while (pose.last() != top && !pose.clear()) pose.popPose();
            dev.factoryascension.client.FAClientConfig.renderFailed("Car drawing", err);
        }
    }

    static void draw(String mesh, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int colour) {
        GunMesh m = GunMesh.get(mesh);
        if (m != null) m.draw(pose, buffers, light, overlay, colour, null, 0);
    }
}
