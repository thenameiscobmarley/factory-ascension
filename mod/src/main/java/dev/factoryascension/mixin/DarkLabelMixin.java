package dev.factoryascension.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Container screens' dark-grey labels ("Inventory", "Furnace"...) on the dark restyled panels: drawn light instead
 * (client/ui/DarkContainers.labelColor). Both names, as in the dev and the real game. Optional: does nothing if gone.
 */
@Mixin(value = GuiGraphics.class, remap = false)
public abstract class DarkLabelMixin {
    @ModifyVariable(method = {"drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", "m_280056_(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
        "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", "m_280614_(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
        "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", "m_280649_(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I"},
        at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false, require = 0)
    private int factoryascension$lightLabel(int color) {
        return dev.factoryascension.client.ui.DarkContainers.labelColor(color);
    }
}
