package dev.factoryascension.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Embeddium's terrain shader gets the waves' camera and clock (client/render/Waves.embeddium). Optional. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface", remap = false)
public abstract class EmbeddiumWavesMixin {
    @Inject(method = "setupState", at = @At("TAIL"), remap = false, require = 0)
    private void factoryascension$waves(CallbackInfo ci) {
        dev.factoryascension.client.render.Waves.embeddium();
    }
}
