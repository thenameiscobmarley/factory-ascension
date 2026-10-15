package dev.factoryascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.blockentity.BinderBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The Card Binder's best card floats and turns slowly above the table. */
public class BinderRenderer implements BlockEntityRenderer<BinderBlockEntity> {
    public BinderRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(BinderBlockEntity binder, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack card = binder.showcase();
        if (card.isEmpty() || binder.getLevel() == null) return;
        float time = binder.getLevel().getGameTime() + partial;
        pose.pushPose();
        pose.translate(0.5, 1.25 + Math.sin(time / 12.0) * 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        pose.scale(0.7f, 0.7f, 0.7f);
        Minecraft.getInstance().getItemRenderer().renderStatic(card, ItemDisplayContext.FIXED, light, overlay, pose, buffers, binder.getLevel(), 0);
        pose.popPose();
    }
}
