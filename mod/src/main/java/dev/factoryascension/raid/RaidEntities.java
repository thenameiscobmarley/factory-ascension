package dev.factoryascension.raid;

import dev.factoryascension.FactoryAscension;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Extraction raids and traders: their entities, and starting it all up. */
public final class RaidEntities {
    private RaidEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FactoryAscension.MODID);
    public static final RegistryObject<EntityType<BodyBag>> BODY_BAG = ENTITIES.register("body_bag",
        () -> EntityType.Builder.<BodyBag>of(BodyBag::new, MobCategory.MISC).sized(1.6f, 0.45f).clientTrackingRange(8).build(FactoryAscension.MODID + ":body_bag"));
    public static final RegistryObject<EntityType<Trader>> TRADER = ENTITIES.register("trader",
        () -> EntityType.Builder.<Trader>of(Trader::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build(FactoryAscension.MODID + ":trader"));

    public static void register(IEventBus bus) {
        Extraction.init();
        Traders.init(bus);
        ENTITIES.register(bus);
    }
}
