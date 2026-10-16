package dev.factoryascension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own sounds (made by tools/sound_gen.py: assets/factoryascension/sounds.json). */
public final class FASounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FactoryAscension.MODID);

    private static RegistryObject<SoundEvent> s(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(FactoryAscension.MODID, id)));
    }

    public static final RegistryObject<SoundEvent> PISTOL = s("gun.pistol"), MAGNUM = s("gun.magnum"), SMG = s("gun.smg"), RIFLE = s("gun.rifle"),
        BATTLE = s("gun.battle"), SNIPER = s("gun.sniper"), FIFTY = s("gun.fifty"), SHOTGUN = s("gun.shotgun"), LMG = s("gun.lmg"),
        SUPPRESSED = s("gun.suppressed"), DISTANT = s("gun.distant"), DISTANT_BURST = s("gun.distant_burst"), DRY = s("gun.dry"),
        MAG_OUT = s("gun.mag_out"), MAG_IN = s("gun.mag_in"), RACK = s("gun.rack"), BOLT = s("gun.bolt"), PUMP = s("gun.pump"),
        SHELL_IN = s("gun.shell_in"), CASING = s("gun.casing"), HEARTBEAT = s("dread.heartbeat"), BREATH = s("dread.breath"),
        RINGING = s("dread.ringing"), HELICOPTER = s("dread.helicopter"), GRENADE = s("grenade.explode"),
        CRACK = s("gun.crack"), WHIZ = s("gun.whiz"), ENGINE = s("vehicle.engine"), ENGINE_V12 = s("vehicle.engine_v12"),
        WIND = s("ambient.wind"), BIRD = s("ambient.bird"), CRICKETS = s("ambient.crickets"), DRIP = s("ambient.drip"), RUMBLE = s("ambient.rumble");

    private FASounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
