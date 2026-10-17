package dev.factoryascension.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.client.render.BreakChunks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A block being mined is drawn by BreakChunks (piece by piece, with the broken-off pieces missing), so the chunk mesh
 * and the game's crack overlay leave it out. Both methods are Forge's own overloads (same name in dev and in the
 * real game). Optional: if they ever change, it does nothing.
 */
@Mixin(value = BlockRenderDispatcher.class, remap = false)
public abstract class BreakHideMixin {
    @Inject(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void factoryascension$hideMesh(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose, VertexConsumer out, boolean cull,
                                           RandomSource r, ModelData data, RenderType type, CallbackInfo ci) {
        if (BreakChunks.hidden(pos)) ci.cancel();
    }

    @Inject(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
        at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void factoryascension$hideCracks(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose, VertexConsumer out, ModelData data, CallbackInfo ci) {
        if (BreakChunks.hidden(pos)) ci.cancel();
    }
}
