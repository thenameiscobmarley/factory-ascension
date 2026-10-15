package dev.factoryascension;

import dev.factoryascension.block.BinderBlock;
import dev.factoryascension.block.ForgeBlock;
import dev.factoryascension.block.OpenerBlock;
import dev.factoryascension.block.VaultBlock;
import dev.factoryascension.blockentity.BinderBlockEntity;
import dev.factoryascension.blockentity.ForgeBlockEntity;
import dev.factoryascension.blockentity.OpenerBlockEntity;
import dev.factoryascension.blockentity.VaultBlockEntity;
import dev.factoryascension.menu.BinderMenu;
import dev.factoryascension.menu.ForgeMenu;
import dev.factoryascension.menu.OpenerMenu;
import dev.factoryascension.menu.VaultMenu;
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
import net.minecraftforge.eventbus.api.IEventBus;
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

    public static final RegistryObject<Block> VAULT = block("ascension_vault", () -> new VaultBlock(machine()));
    public static final RegistryObject<Block> BINDER = block("card_binder", () -> new BinderBlock(machine().noOcclusion()));
    public static final RegistryObject<Block> FORGE = block("essence_forge", () -> new ForgeBlock(machine()));
    public static final RegistryObject<Block> OPENER = block("pack_opener", () -> new OpenerBlock(machine()));

    public static final RegistryObject<BlockEntityType<VaultBlockEntity>> VAULT_BE = BLOCK_ENTITIES.register("ascension_vault",
        () -> BlockEntityType.Builder.of(VaultBlockEntity::new, VAULT.get()).build(null));
    public static final RegistryObject<BlockEntityType<BinderBlockEntity>> BINDER_BE = BLOCK_ENTITIES.register("card_binder",
        () -> BlockEntityType.Builder.of(BinderBlockEntity::new, BINDER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ForgeBlockEntity>> FORGE_BE = BLOCK_ENTITIES.register("essence_forge",
        () -> BlockEntityType.Builder.of(ForgeBlockEntity::new, FORGE.get()).build(null));
    public static final RegistryObject<BlockEntityType<OpenerBlockEntity>> OPENER_BE = BLOCK_ENTITIES.register("pack_opener",
        () -> BlockEntityType.Builder.of(OpenerBlockEntity::new, OPENER.get()).build(null));

    public static final RegistryObject<MenuType<VaultMenu>> VAULT_MENU = MENUS.register("ascension_vault",
        () -> IForgeMenuType.create((id, inv, buf) -> new VaultMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<BinderMenu>> BINDER_MENU = MENUS.register("card_binder",
        () -> IForgeMenuType.create((id, inv, buf) -> new BinderMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<ForgeMenu>> FORGE_MENU = MENUS.register("essence_forge",
        () -> IForgeMenuType.create((id, inv, buf) -> new ForgeMenu(id, inv, buf.readBlockPos())));
    public static final RegistryObject<MenuType<OpenerMenu>> OPENER_MENU = MENUS.register("pack_opener",
        () -> IForgeMenuType.create((id, inv, buf) -> new OpenerMenu(id, inv, buf.readBlockPos())));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.factoryascension"))
        .icon(() -> new ItemStack(VAULT.get()))
        .displayItems((params, out) -> {
            out.accept(VAULT.get());
            out.accept(BINDER.get());
            out.accept(FORGE.get());
            out.accept(OPENER.get());
        })
        .build());

    private static RegistryObject<Block> block(String name, Supplier<Block> make) {
        RegistryObject<Block> b = BLOCKS.register(name, make);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public FactoryAscension() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        FANetwork.register();
    }
}
