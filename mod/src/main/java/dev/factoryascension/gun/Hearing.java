package dev.factoryascension.gun;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Who heard a shot: listeners (the NPC operators) get told where and how far it carried. */
public final class Hearing {
    private Hearing() {
    }

    public interface Listener {
        void heard(ServerLevel level, Entity shooter, double range);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    public static void listen(Listener l) {
        LISTENERS.add(l);
    }

    public static void gunshot(ServerLevel level, Entity shooter, double range) {
        range *= dev.factoryascension.world.Weather.soundScale(level, shooter.position());   // storms and fog swallow sound
        for (Listener l : LISTENERS) {
            try {
                l.heard(level, shooter, range);
            } catch (Throwable ignored) {
                // A listener's trouble never stops the shot
            }
        }
    }
}
