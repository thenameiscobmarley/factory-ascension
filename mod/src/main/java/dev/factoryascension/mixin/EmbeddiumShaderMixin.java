package dev.factoryascension.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Embeddium reads its block shaders from its own jar, so resource packs can't change them. While the Factory Ascension
 * Depth pack is on, this hands it the pack's versions instead (client/DepthShaders.embeddiumSource; null = its own).
 * Optional: without Embeddium (or if its code ever changes) it simply does nothing.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class EmbeddiumShaderMixin {
    @Inject(method = "getShaderSource", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void factoryascension$depthSource(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        String src = dev.factoryascension.client.DepthShaders.embeddiumSource(name);
        if (src != null) cir.setReturnValue(src);
    }
}
