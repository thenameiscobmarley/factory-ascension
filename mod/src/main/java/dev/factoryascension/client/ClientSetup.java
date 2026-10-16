package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client only: the screens and the Card Binder's floating card. */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ClientConflicts.check();
        event.enqueueWork(() -> {
            try { GunClient.fixOculusReloadKey(); } catch (Throwable ignored) { }
            MenuScreens.register(FactoryAscension.VAULT_MENU.get(), VaultScreen::new);
            MenuScreens.register(FactoryAscension.BINDER_MENU.get(), BinderScreen::new);
            MenuScreens.register(FactoryAscension.FORGE_MENU.get(), ForgeScreen::new);
            MenuScreens.register(FactoryAscension.OPENER_MENU.get(), OpenerScreen::new);
            MenuScreens.register(FactoryAscension.SALVAGER_MENU.get(), SalvagerScreen::new);
            MenuScreens.register(FactoryAscension.FUSION_MENU.get(), FusionScreen::new);
            MenuScreens.register(FactoryAscension.SMELTER_MENU.get(), SmelterScreen::new);
            MenuScreens.register(FactoryAscension.ENGINE_MENU.get(), EngineScreen::new);
            MenuScreens.register(FactoryAscension.WORKBENCH_MENU.get(), WorkbenchScreen::new);
            MenuScreens.register(FactoryAscension.MINI_CRUSHER_MENU.get(), MiniCrusherScreen::new);
            MenuScreens.register(FactoryAscension.WEAPONS_BENCH_MENU.get(), dev.factoryascension.client.gun.WeaponsBenchScreen::new);
            MenuScreens.<dev.factoryascension.menu.VaultMenu, VaultScreen>register(FactoryAscension.TERMINAL_MENU.get(), VaultScreen::new);
        });
    }

    /** Rounds: the case takes the round's colour, the tip its type's colour. */
    @SubscribeEvent
    public static void colours(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0 ? 0xFF000000 | dev.factoryascension.gun.CartridgeItem.colour(stack)
            : layer == 1 ? 0xFF000000 | dev.factoryascension.gun.CartridgeItem.type(stack).tip : -1, FactoryAscension.CARTRIDGE.get());
        event.register((stack, layer) -> layer == 0 ? 0xFF000000 | dev.factoryascension.gun.AttachmentItem.colour(stack) : -1, FactoryAscension.ATTACHMENT.get());
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryAscension.BINDER_BE.get(), BinderRenderer::new);
        MobRenderers.register(event);
        for (var t : dev.factoryascension.npc.Operators.TYPES.values()) event.registerEntityRenderer(t.get(), dev.factoryascension.client.npc.OperatorRenderer::new);
        event.registerEntityRenderer(dev.factoryascension.npc.Operators.GRENADE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
}
