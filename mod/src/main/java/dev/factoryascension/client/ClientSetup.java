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
            MenuScreens.register(FactoryAscension.VAULT_MENU.get(), VaultScreen::new);
            MenuScreens.register(FactoryAscension.BINDER_MENU.get(), BinderScreen::new);
            MenuScreens.register(FactoryAscension.FORGE_MENU.get(), ForgeScreen::new);
            MenuScreens.register(FactoryAscension.OPENER_MENU.get(), OpenerScreen::new);
            MenuScreens.register(FactoryAscension.SALVAGER_MENU.get(), SalvagerScreen::new);
            MenuScreens.register(FactoryAscension.FUSION_MENU.get(), FusionScreen::new);
            MenuScreens.register(FactoryAscension.SMELTER_MENU.get(), SmelterScreen::new);
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryAscension.BINDER_BE.get(), BinderRenderer::new);
        MobRenderers.register(event);
    }
}
