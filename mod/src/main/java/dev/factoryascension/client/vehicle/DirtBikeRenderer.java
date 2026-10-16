package dev.factoryascension.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.client.gun.GunMesh;
import dev.factoryascension.vehicle.BikeParts;
import dev.factoryascension.vehicle.DirtBike;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a dirt bike from its parts' meshes (gunmesh/bike/, made by tools/vehicles/bike_mesh.py): the frame (painted in
 * the bike's colour), the engine at the frame's mount, the wheels at its axles (turning), the fork, bars and front wheel
 * turned about the steering head, the whole bike leaning into turns and lifting its front in a wheelie.
 */
public class DirtBikeRenderer extends EntityRenderer<DirtBike> {
    public DirtBikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.7f;
    }

    @Override
    public ResourceLocation getTextureLocation(DirtBike e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(DirtBike b, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        BikeParts.Frame f = BikeParts.frame(b.frame());
        int colour = b.colour();
        int ov = OverlayTexture.NO_OVERLAY;
        float lean = Mth.lerp(partial, b.leanO, b.lean), steer = Mth.lerp(partial, b.steerO, b.steer);
        float wheel = Mth.lerp(partial, b.wheelO, b.wheel), wheelie = Mth.lerp(partial, b.wheelieO, b.wheelie);
        float bodyYaw = Mth.rotLerp(partial, b.yRotO, b.getYRot());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 - bodyYaw));
        pose.mulPose(Axis.XP.rotationDegrees(lean));
        if (wheelie > 0) {
            float rx = f.rear()[0] / 16f, ry = f.rear()[1] / 16f - 7.4f / 16f;
            pose.translate(rx, ry, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(wheelie * 28));
            pose.translate(-rx, -ry, 0);
        }
        draw("bike/" + f.id(), pose, buffers, light, ov, colour);
        // Engine
        pose.pushPose();
        pose.translate(f.engine()[0] / 16f, f.engine()[1] / 16f, 0);
        draw("bike/" + b.engine(), pose, buffers, light, ov, colour);
        pose.popPose();
        // Rear wheel
        pose.pushPose();
        pose.translate(f.rear()[0] / 16f, f.rear()[1] / 16f, 0);
        pose.mulPose(Axis.ZP.rotation(-wheel));
        draw("bike/wheel", pose, buffers, light, ov, colour);
        pose.popPose();
        // Fork, bars and the front wheel, turned about the steering head
        pose.pushPose();
        float hx = f.headBottom()[0] / 16f, hy = f.headBottom()[1] / 16f;
        Vector3f axis = new Vector3f(f.headTop()[0] - f.headBottom()[0], f.headTop()[1] - f.headBottom()[1], 0).normalize();
        pose.translate(hx, hy, 0);
        pose.mulPose(new Quaternionf().fromAxisAngleRad(axis, steer * 0.45f));
        pose.translate(-hx, -hy, 0);
        draw("bike/fork_" + (f.id().equals("frame_long") ? "long" : "standard"), pose, buffers, light, ov, colour);
        pose.translate(f.front()[0] / 16f, f.front()[1] / 16f, 0);
        pose.mulPose(Axis.ZP.rotation(-wheel));
        draw("bike/wheel", pose, buffers, light, ov, colour);
        pose.popPose();
        pose.popPose();
        super.render(b, yaw, partial, pose, buffers, light);
    }

    private static void draw(String mesh, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int colour) {
        GunMesh m = GunMesh.get(mesh);
        if (m != null) m.draw(pose, buffers, light, overlay, colour, null, 0);
    }
}
