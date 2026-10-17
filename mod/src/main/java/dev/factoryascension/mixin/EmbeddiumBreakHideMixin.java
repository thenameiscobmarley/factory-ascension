package dev.factoryascension.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BreakHideMixin for Embeddium, which builds chunk meshes with its own block renderer. Optional, like that one. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class EmbeddiumBreakHideMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void factoryascension$hideMesh(@Coerce Object ctx, @Coerce Object buffers, CallbackInfo ci) {
        if (dev.factoryascension.client.render.BreakChunks.hiddenEmbeddium(ctx)) ci.cancel();
    }
}
