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

    /** The original shotgun's numbers for the shared animation (aimed: sights on the crosshair, the stock clear of your eye). */
    public static final dev.factoryascension.gun.GunSpec SHOTGUN_ANIM = new dev.factoryascension.gun.GunSpec("shotgun", "Ascension Shotgun",
        dev.factoryascension.gun.GunSpec.Kind.SHOTGUN, dev.factoryascension.gun.Caliber.G12).hip(0.56f, -0.52f, -0.72f).ads(-0.29f, -0.80f);

    private static boolean holding(Player p) {
        return p != null && (p.getMainHandItem().getItem() instanceof ShotgunItem || p.getMainHandItem().getItem() instanceof dev.factoryascension.gun.GunItem);
    }

    static dev.factoryascension.gun.GunSpec animSpec(ItemStack s) {
        dev.factoryascension.gun.GunSpec g = dev.factoryascension.gun.GunItem.spec(s);
        return g != null ? g : SHOTGUN_ANIM;
    }

    private static boolean aimingAny(LivingEntity e) {
        return ShotgunItem.aiming(e) || dev.factoryascension.gun.GunItem.aiming(e);
    }

    /** 0 (hip) .. 1 (fully aimed), eased over a few ticks. */
    private static float aimAmount(LivingEntity e, float partial) {
        if (!aimingAny(e)) return 0;
        float t = Math.min(1, (e.getTicksUsingItem() + partial) / 4f);
        return t * t * (3 - 2 * t);
    }

    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return dev.factoryascension.client.gun.GunRenderer.INSTANCE;   // only used by the guns whose item model is builtin/entity
        }

        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;   // both hands up, shouldered
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
            try {
                dev.factoryascension.client.gun.GunAnim.apply(pose, player, animSpec(stack), stack, partial, equip, arm == HumanoidArm.RIGHT ? 1 : -1);
            } catch (Throwable err) {
                // Never let an animation bug take the gun away: fall back to a plain hold
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                pose.translate(side * 0.5f, -0.5f, -0.72f);
            }
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
            ItemStack held = p.getMainHandItem();
            while (RELOAD.consumeClick()) FANetwork.CHANNEL.sendToServer(new FANetwork.GunAction(FANetwork.GunAction.RELOAD, false));
            boolean clicked = false;
            while (mc.options.keyAttack.consumeClick()) clicked = true;
            dev.factoryascension.gun.GunSpec spec = dev.factoryascension.gun.GunItem.spec(held);
            dev.factoryascension.gun.GunStats stats = dev.factoryascension.gun.GunStats.of(held);
            boolean auto = spec == null || stats.auto;   // the original shotgun pumps as fast as you hold it
            boolean want = clicked || (auto && mc.options.keyAttack.isDown());
            if (want && !p.getCooldowns().isOnCooldown(held.getItem())) {
                boolean aim = aimingAny(p);
                boolean blocked = spec != null && (held.hasTag() && held.getTag().getInt("Bolt") > 0
                    || dev.factoryascension.gun.GunItem.phase(held) == dev.factoryascension.gun.GunItem.OUT
                    || (dev.factoryascension.gun.GunItem.reloading(held) && (spec.reload == dev.factoryascension.gun.GunSpec.Reload.MAG || spec.reload == dev.factoryascension.gun.GunSpec.Reload.BELT)));
                if (!blocked) {
                    FANetwork.CHANNEL.sendToServer(new FANetwork.GunAction(FANetwork.GunAction.FIRE, aim));
                    boolean loaded = spec != null ? dev.factoryascension.gun.GunItem.ammo(held) > 0 || p.isCreative() : ShotgunItem.shells(held) > 0;
                    if (loaded) {
                        float rUp = stats != null ? stats.recoilUp : animSpec(held).recoilUp, rSide = stats != null ? stats.recoilSide : animSpec(held).recoilSide;
                        float up = rUp * (aim ? 0.6f : 1) * (spec == null ? 1.8f : 1);
                        p.setXRot(p.getXRot() - up);
                        p.setYRot(p.getYRot() + (p.getRandom().nextFloat() - 0.5f) * rSide * (aim ? 1 : 2));
                        kick += up * 0.6f;
                        dev.factoryascension.client.gun.GunAnim.kick(rUp, rSide, aim);
                    }
                    // Until the server's cooldown arrives, don't send again
                    p.getCooldowns().addCooldown(held.getItem(), spec == null ? 4 : stats.cooldown());
                }
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

        static float zoomOf(Player p) {
            dev.factoryascension.gun.GunStats g = dev.factoryascension.gun.GunStats.of(p.getMainHandItem());
            return g != null ? g.zoom : 0.25f;
        }

        /** Zoom while aiming. */
        @SubscribeEvent
        public static void fov(ComputeFovModifierEvent event) {
            float aim = aimAmount(event.getPlayer(), Minecraft.getInstance().getFrameTime());
            if (aim > 0) event.setNewFovModifier(event.getFovModifier() * (1 - (aim > 0 ? zoomOf(event.getPlayer()) : 0.25f) * aim));
        }
    }
}
