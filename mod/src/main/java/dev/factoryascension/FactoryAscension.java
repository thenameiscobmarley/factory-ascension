package dev.factoryascension;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.factoryascension.block.BinderBlock;
import dev.factoryascension.block.BurnerBlock;
import dev.factoryascension.block.ConnectedGlassBlock;
import dev.factoryascension.block.EngineBlock;
import dev.factoryascension.block.EngineModuleBlock;
import dev.factoryascension.block.ForgeBlock;
import dev.factoryascension.block.FusionBlock;
import dev.factoryascension.block.OpenerBlock;
import dev.factoryascension.block.SalvagerBlock;
import dev.factoryascension.block.SmelterBlock;
import dev.factoryascension.block.VaultBlock;
import dev.factoryascension.blockentity.BinderBlockEntity;
import dev.factoryascension.blockentity.BurnerBlockEntity;
import dev.factoryascension.blockentity.EngineBlockEntity;
import dev.factoryascension.blockentity.ForgeBlockEntity;
import dev.factoryascension.blockentity.FusionBlockEntity;
import dev.factoryascension.blockentity.OpenerBlockEntity;
import dev.factoryascension.blockentity.SalvagerBlockEntity;
import dev.factoryascension.blockentity.SmelterBlockEntity;
import dev.factoryascension.blockentity.VaultBlockEntity;
import dev.factoryascension.menu.BinderMenu;
import dev.factoryascension.menu.ForgeMenu;
import dev.factoryascension.menu.FusionMenu;
import dev.factoryascension.menu.OpenerMenu;
import dev.factoryascension.menu.SalvagerMenu;
import dev.factoryascension.menu.SmelterMenu;
import dev.factoryascension.menu.VaultMenu;
import dev.factoryascension.mob.FAMobs;
import dev.factoryascension.network.FANetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Blocks and machines for the Factory Ascension pack. The pack's KubeJS scripts run the rules (costs, card odds,
 * upgrades); this mod adds the blocks, their inventories and their screens. It's the "Full" download's extra jar.
 */
@Mod(FactoryAscension.MODID)
public class FactoryAscension {
    public static final String MODID = "factoryascension";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4f, 1200f).sound(SoundType.NETHERITE_BLOCK)
            .requiresCorrectToolForDrops().lightLevel(s -> 6);
    }

    private static BlockBehaviour.Properties engine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    // Connected glass: frames only where there's no matching glass next to it
    public static final String[] GLASS = {"factory_glass", "ascendite_glass", "gilded_glass", "smoked_glass"};
    public static final java.util.List<RegistryObject<Block>> GLASS_BLOCKS = java.util.Arrays.stream(GLASS)
        .map(id -> block(id, () -> new ConnectedGlassBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GLASS)))).toList();

    public static final RegistryObject<Block> VAULT = block("ascension_vault", () -> new VaultBlock(machine()));
    public static final RegistryObject<Block> BINDER = block("card_binder", () -> new BinderBlock(machine().noOcclusion()));
    public static final RegistryObject<Block> FORGE = block("essence_forge", () -> new ForgeBlock(machine()));
    public static final RegistryObject<Block> OPENER = block("pack_opener", () -> new OpenerBlock(machine()));
    public static final RegistryObject<Block> SALVAGER = block("salvager", () -> new SalvagerBlock(machine()));
    public static final RegistryObject<Block> FUSION = block("tome_fusion_table", () -> new FusionBlock(machine().noOcclusion()));
    public static final RegistryObject<Block> SMELTER = block("super_smelter", () -> new SmelterBlock(machine().lightLevel(s -> s.getValue(SmelterBlock.LIT) ? 13 : 3)));
    // Modular Engine: early-game parts (iron-tier, mine with any pickaxe)
    public static final RegistryObject<Block> ENGINE_CORE = block("engine_core", () -> new EngineBlock(engine()));
    public static final RegistryObject<Block> COIL = block("engine_coil", () -> new EngineModuleBlock(engine()));
    public static final RegistryObject<Block> GEARBOX = block("engine_gearbox", () -> new EngineModuleBlock(engine()));
    public static final RegistryObject<Block> BURNER = block("engine_burner", () -> new BurnerBlock(engine().lightLevel(s -> s.getValue(BurnerBlock.LIT) ? 13 : 0)));

    public static final RegistryObject<BlockEntityType<VaultBlockEntity>> VAULT_BE = BLOCK_ENTITIES.register("ascension_vault",
        () -> BlockEntityType.Builder.of(VaultBlockEntity::new, VAULT.get()).build(null));
    public static final RegistryObject<BlockEntityType<BinderBlockEntity>> BINDER_BE = BLOCK_ENTITIES.register("card_binder",
        () -> BlockEntityType.Builder.of(BinderBlockEntity::new, BINDER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ForgeBlockEntity>> FORGE_BE = BLOCK_ENTITIES.register("essence_forge",
        () -> BlockEntityType.Builder.of(ForgeBlockEntity::new, FORGE.get()).build(null));
    public static final RegistryObject<BlockEntityType<OpenerBlockEntity>> OPENER_BE = BLOCK_ENTITIES.register("pack_opener",
        () -> BlockEntityType.Builder.of(OpenerBlockEntity::new, OPENER.get()).build(null));

    public static final RegistryObject<BlockEntityType<SalvagerBlockEntity>> SALVAGER_BE = BLOCK_ENTITIES.register("salvager",
        () -> BlockEntityType.Builder.of(SalvagerBlockEntity::new, SALVAGER.get()).build(null));
    public static final RegistryObject<BlockEntityType<FusionBlockEntity>> FUSION_BE = BLOCK_ENTITIES.register("tome_fusion_table",
        () -> BlockEntityType.Builder.of(FusionBlockEntity::new, FUSION.get()).build(null));
    public static final RegistryObject<BlockEntityType<SmelterBlockEntity>> SMELTER_BE = BLOCK_ENTITIES.register("super_smelter",
        () -> BlockEntityType.Builder.of(SmelterBlockEntity::create, SMELTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<EngineBlockEntity>> ENGINE_BE = BLOCK_ENTITIES.register("engine_core",
        () -> BlockEntityType.Builder.of(EngineBlockEntity::create, ENGINE_CORE.get()).build(null));
    public static final RegistryObject<BlockEntityType<BurnerBlockEntity>> BURNER_BE = BLOCK_ENTITIES.register("engine_burner",
        () -> BlockEntityType.Builder.of(BurnerBlockEntity::new, BURNER.get()).build(null));

    public static final RegistryObject<MenuType<VaultMenu>> VAULT_MENU = MENUS.register("ascension_vault",
        () -> IForgeMenuType.create((id, inv, buf) -> new VaultMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<BinderMenu>> BINDER_MENU = MENUS.register("card_binder",
        () -> IForgeMenuType.create((id, inv, buf) -> new BinderMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<ForgeMenu>> FORGE_MENU = MENUS.register("essence_forge",
        () -> IForgeMenuType.create((id, inv, buf) -> new ForgeMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<OpenerMenu>> OPENER_MENU = MENUS.register("pack_opener",
        () -> IForgeMenuType.create((id, inv, buf) -> new OpenerMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<MenuType<SalvagerMenu>> SALVAGER_MENU = MENUS.register("salvager",
        () -> IForgeMenuType.create((id, inv, buf) -> new SalvagerMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<FusionMenu>> FUSION_MENU = MENUS.register("tome_fusion_table",
        () -> IForgeMenuType.create((id, inv, buf) -> new FusionMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<SmelterMenu>> SMELTER_MENU = MENUS.register("super_smelter",
        () -> IForgeMenuType.create((id, inv, buf) -> new SmelterMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.factoryascension"))
        .icon(() -> new ItemStack(VAULT.get()))
        .displayItems((params, out) -> {
            out.accept(VAULT.get());
            out.accept(BINDER.get());
            out.accept(FORGE.get());
            out.accept(OPENER.get());
            out.accept(SALVAGER.get());
            out.accept(FUSION.get());
            out.accept(SMELTER.get());
            out.accept(ENGINE_CORE.get());
            out.accept(COIL.get());
            out.accept(GEARBOX.get());
            out.accept(BURNER.get());
            GLASS_BLOCKS.forEach(g -> out.accept(g.get()));
            FAMobs.ALL.forEach(m -> out.accept(m.egg().get()));
        })
        .build());

    private static RegistryObject<Block> block(String name, Supplier<Block> make) {
        RegistryObject<Block> b = BLOCKS.register(name, make);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public FactoryAscension() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, dev.factoryascension.client.FAClientConfig.SPEC);
        FAMobs.register(bus);   // first: its spawn eggs go into ITEMS before ITEMS is registered
        MinecraftForge.EVENT_BUS.addListener(FAMobs::onDetonate);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        FANetwork.register();
        bus.addListener(this::commonSetup);
    }

    /** Create's stress numbers for the item tooltips and goggles (the real values come from FAConfig at run time). */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(SMELTER.get(), () -> FAConfig.smelterStress[0]);
            BlockStressValues.CAPACITIES.register(ENGINE_CORE.get(), () -> FAConfig.engineSuPerCoil / FAConfig.engineRpm[0]);
        });
    }
}
