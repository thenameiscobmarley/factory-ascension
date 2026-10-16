package dev.factoryascension.client.gun;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import dev.factoryascension.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Akimbo in first person: with an Akimbo kit on the gun and nothing in the other hand, a second copy of the gun is
 * drawn in the other hand, animated on its own side (GunAnim with the other side), so the two sway and kick apart.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AkimboHands {
    private AkimboHands() {
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND || !event.getItemStack().isEmpty()) return;
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        ItemStack main = p.getMainHandItem();
        GunSpec spec = GunItem.spec(main);
        if (spec == null) return;
        GunStats st = GunStats.of(main);
        if (st == null || !st.akimbo) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            int side = p.getMainArm() == HumanoidArm.RIGHT ? -1 : 1;
            GunAnim.apply(pose, p, spec, main, event.getPartialTick(), event.getEquipProgress(), side);
            mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, main,
                side < 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, false, pose, event.getMultiBufferSource(), event.getPackedLight());
        } catch (Throwable ignored) {
            // Drawing the second gun is only for looks; never let it break the frame
        }
        pose.popPose();
        event.setCanceled(true);
    }
}
