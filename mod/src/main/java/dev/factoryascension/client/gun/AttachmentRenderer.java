package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.gun.AttachmentItem;
import dev.factoryascension.gun.Attachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An attachment as an item (in inventories, JEI, on the ground, in hand): its own 3D model, the one drawn on guns,
 * in its colour, centred and scaled to fill the slot (a red dot as big as a suppressor). Falls back to nothing drawn
 * (never a crash) if the model's missing.
 */
public final class AttachmentRenderer extends BlockEntityWithoutLevelRenderer {
    public static final AttachmentRenderer INSTANCE = new AttachmentRenderer();
    private static final RandomSource RANDOM = RandomSource.create();
    /** Per shape: centre x, y, z and the scale that fits it in a block. */
    private static final Map<String, float[]> FIT = new HashMap<>();

    private AttachmentRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void clear() {
        FIT.clear();
    }

    static List<BakedQuad> quads(BakedModel m) {
        List<BakedQuad> all = new ArrayList<>(m.getQuads(null, null, RANDOM));
        for (Direction d : Direction.values()) all.addAll(m.getQuads(null, d, RANDOM));
        return all;
    }

    static float[] fit(String shape, List<BakedQuad> qs) {
        return FIT.computeIfAbsent(shape, k -> {
            float[] lo = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, hi = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (BakedQuad q : qs) {
                int[] v = q.getVertices();
                int stride = v.length / 4;
                for (int i = 0; i < 4; i++)
                    for (int a = 0; a < 3; a++) {
                        float f = Float.intBitsToFloat(v[i * stride + a]);
                        lo[a] = Math.min(lo[a], f);
                        hi[a] = Math.max(hi[a], f);
                    }
            }
            if (lo[0] > hi[0]) return new float[] {0.5f, 0.5f, 0.5f, 1};
            float size = Math.max(hi[0] - lo[0], Math.max(hi[1] - lo[1], hi[2] - lo[2]));
            return new float[] {(lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2, Math.min(4f, 0.9f / Math.max(0.05f, size))};
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Attachments.Att a = AttachmentItem.att(stack);
        if (a == null || a.shape.equals("none")) return;
        PoseStack.Pose top = pose.last();
        try {
            BakedModel m = Minecraft.getInstance().getModelManager().getModel(GunRenderer.shape(a.shape));
            List<BakedQuad> qs = quads(m);
            float[] f = fit(a.shape, qs);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.scale(f[3], f[3], f[3]);
            pose.translate(-f[0], -f[1], -f[2]);
            var vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
            float r = (a.colour >> 16 & 255) / 255f, g = (a.colour >> 8 & 255) / 255f, b = (a.colour & 255) / 255f;
            for (BakedQuad q : qs) vc.putBulkData(pose.last(), q, q.isTinted() ? r : 1, q.isTinted() ? g : 1, q.isTinted() ? b : 1, light, overlay);
            pose.popPose();
        } catch (Throwable err) {
            while (pose.last() != top && !pose.clear()) pose.popPose();
            dev.factoryascension.client.FAClientConfig.renderFailed("Attachment drawing", err);
        }
    }
}
