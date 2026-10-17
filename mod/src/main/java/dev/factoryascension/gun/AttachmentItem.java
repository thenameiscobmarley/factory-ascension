package dev.factoryascension.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A gun attachment off the gun (one item for all of them; its catalog id is in NBT "Att"). Put on at the Weapons Workbench. */
public class AttachmentItem extends Item {
    public AttachmentItem(Properties p) {
        super(p);
    }

    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(dev.factoryascension.FactoryAscension.ATTACHMENT.get());
        s.getOrCreateTag().putString("Att", id);
        return s;
    }

    @Nullable
    public static Attachments.Att att(ItemStack s) {
        return s.hasTag() ? Attachments.BY_ID.get(s.getTag().getString("Att")) : null;
    }

    public static int colour(ItemStack s) {
        Attachments.Att a = att(s);
        return a == null ? 0x808080 : a.colour;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return dev.factoryascension.client.gun.AttachmentRenderer.INSTANCE;
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        Attachments.Att a = att(stack);
        return Component.literal(a == null ? "Attachment" : a.name);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        Attachments.Att a = att(stack);
        if (a == null) return;
        tip.add(Component.literal(a.slot.label + " attachment").withStyle(ChatFormatting.GOLD));
        for (String line : Bench.describe(a)) tip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Put it on a gun at the Weapons Workbench").withStyle(ChatFormatting.DARK_GRAY));
    }
}
