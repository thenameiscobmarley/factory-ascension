package dev.factoryascension.vehicle;

import dev.factoryascension.FactoryAscension;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** The vehicles: the dirt bike (its parts), the quad bike and the pickup truck, fuel, and the wrench. */
public final class Vehicles {
    private Vehicles() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FactoryAscension.MODID);
    public static final RegistryObject<EntityType<DirtBike>> DIRT_BIKE = ENTITIES.register("dirt_bike",
        () -> EntityType.Builder.<DirtBike>of(DirtBike::new, MobCategory.MISC).sized(1.1f, 1.1f).clientTrackingRange(10).updateInterval(1)
            .build(FactoryAscension.MODID + ":dirt_bike"));
    public static final RegistryObject<EntityType<Car>> QUAD = ENTITIES.register("quad_bike",
        () -> EntityType.Builder.<Car>of(Car::new, MobCategory.MISC).sized(1.5f, 1.2f).clientTrackingRange(10).updateInterval(1).build(FactoryAscension.MODID + ":quad_bike"));
    public static final RegistryObject<EntityType<Car>> PICKUP = ENTITIES.register("pickup_truck",
        () -> EntityType.Builder.<Car>of(Car::new, MobCategory.MISC).sized(2.9f, 1.9f).clientTrackingRange(12).updateInterval(1).build(FactoryAscension.MODID + ":pickup_truck"));
    public static final RegistryObject<Item> QUAD_ITEM = FactoryAscension.ITEMS.register("quad_bike", () -> new CarItem(Car.Model.QUAD, new Item.Properties()));
    public static final RegistryObject<Item> PICKUP_ITEM = FactoryAscension.ITEMS.register("pickup_truck", () -> new CarItem(Car.Model.PICKUP, new Item.Properties()));
    public static final RegistryObject<Item> WRENCH = FactoryAscension.ITEMS.register("wrench", () -> new Workshop.Wrench(new Item.Properties().durability(250)));
    public static final RegistryObject<Item> DIRT_BIKE_ITEM = FactoryAscension.ITEMS.register("dirt_bike", () -> new DirtBikeItem(new Item.Properties()));
    public static final Map<String, RegistryObject<Item>> PARTS = new LinkedHashMap<>();
    public static final RegistryObject<Item> FUEL_CAN = FactoryAscension.ITEMS.register("fuel_can", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RACING_FUEL = FactoryAscension.ITEMS.register("racing_fuel", () -> new Item(new Item.Properties().stacksTo(16)));

    static {
        for (String id : BikeParts.ENGINES.keySet()) PARTS.put(id, FactoryAscension.ITEMS.register(id, () -> new BikePartItem("engine", id, new Item.Properties())));
        for (String id : BikeParts.FRAMES.keySet()) PARTS.put(id, FactoryAscension.ITEMS.register(id, () -> new BikePartItem("frame", id, new Item.Properties())));
        for (String id : BikeParts.GEARBOXES.keySet()) PARTS.put(id, FactoryAscension.ITEMS.register(id, () -> new BikePartItem("gearbox", id, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
