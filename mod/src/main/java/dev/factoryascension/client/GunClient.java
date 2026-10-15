package dev.factoryascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.item.ShotgunItem;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client side of the shotgun (item/ShotgunItem): both hands on the gun, sights to the eye and a zoom while aiming,
 * left-click (or holding it) fires, R reloads, and the camera kicks up on each shot.
 */
public final class GunClient {
    private GunClient() {
    }

    public static final KeyMapping RELOAD = new KeyMapping("key.factoryascension.reload", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.factoryascension");

    private static boolean holding(Player p) {
        return p != null && p.getMainHandItem().getItem() instanceof ShotgunItem;
    }

    /** 0 (hip) .. 1 (fully aimed), eased over a few ticks. */
    private static float aimAmount(LivingEntity e, float partial) {
        if (!ShotgunItem.aiming(e)) return 0;
        float t = Math.min(1, (e.getTicksUsingItem() + partial) / 4f);
        return t * t * (3 - 2 * t);
    }

    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;   // both hands up, shouldered
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float aim = aimAmount(player, partial);
            // Hip: low on the right. Aimed: centered under the eye so the sights line up with the crosshair
            float x = side * (0.56f * (1 - aim) + 0.0f * aim);
            float y = -0.52f * (1 - aim) + -0.36f * aim + equip * -0.6f;
            float z = -0.72f * (1 - aim) + -0.62f * aim;
            pose.translate(x, y, z);
            return true;
        }
    };

    // ---- Mod bus: the reload key ----
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Keys {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(RELOAD);
        }
    }

    /**
     * Oculus binds R to "reload shaders" too, so every shotgun reload would also reload the shader pack (a long
     * freeze). Once, if both are on R, the shader key is unbound (it's a debugging key; rebind it in Controls if wanted).
     */
    public static void fixOculusReloadKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || FAClientConfig.OCULUS_KEY_MOVED.get()) return;
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getName().equals("iris.keybind.reload") && k.getKey().equals(RELOAD.getKey())) {
                k.setKey(InputConstants.UNKNOWN);
                KeyMapping.resetMapping();
                mc.options.save();
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").info("Unbound Oculus's reload-shaders key (it was R, the shotgun's reload)");
            }
        }
        FAClientConfig.OCULUS_KEY_MOVED.set(true);
        FAClientConfig.OCULUS_KEY_MOVED.save();
    }

    // ---- Forge bus: input, zoom, recoil ----
    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        private static float kick = 0;   // camera recoil still to recover

        /** Before vanilla reads the keys: left-clicks fire (vanilla ignores attack clicks while aiming, so take them here). */
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (event.phase == TickEvent.Phase.END) {
                // Recoil eases back down
                if (p != null && kick > 0.05f) {
                    float back = kick * 0.25f;
                    p.setXRot(p.getXRot() + back);
                    kick -= back;
                }
                return;
            }
            if (p == null || mc.screen != null || !holding(p)) return;
            while (RELOAD.consumeClick()) FANetwork.CHANNEL.sendToServer(new FANetwork.GunAction(FANetwork.GunAction.RELOAD, false));
            boolean clicked = false;
            while (mc.options.keyAttack.consumeClick()) clicked = true;
            if ((clicked || mc.options.keyAttack.isDown()) && !p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())) {
                boolean aim = ShotgunItem.aiming(p);
                FANetwork.CHANNEL.sendToServer(new FANetwork.GunAction(FANetwork.GunAction.FIRE, aim));
                if (ShotgunItem.shells(p.getMainHandItem()) > 0) {
                    float up = aim ? 5f : 9f;
                    p.setXRot(p.getXRot() - up);
                    p.setYRot(p.getYRot() + (p.getRandom().nextFloat() - 0.5f) * (aim ? 1.5f : 3f));
                    kick += up * 0.6f;
                }
                // Until the server's cooldown arrives, don't send again
                p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), 4);
            }
        }

        /** No mining or swinging with the gun in hand. */
        @SubscribeEvent
        public static void click(InputEvent.InteractionKeyMappingTriggered event) {
            Player p = Minecraft.getInstance().player;
            if (event.isAttack() && holding(p)) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }

        /** Zoom while aiming. */
        @SubscribeEvent
        public static void fov(ComputeFovModifierEvent event) {
            float aim = aimAmount(event.getPlayer(), Minecraft.getInstance().getFrameTime());
            if (aim > 0) event.setNewFovModifier(event.getFovModifier() * (1 - 0.25f * aim));
        }
    }
}
