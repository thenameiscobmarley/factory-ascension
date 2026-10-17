package dev.factoryascension.gun;

import dev.factoryascension.FactoryAscension;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Server work to run a few ticks from now (a round still in flight). */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Delayed {
    private record Job(long due, Runnable run) {
    }

    private static final List<Job> JOBS = new ArrayList<>();
    private static long now = 0;

    private Delayed() {
    }

    public static void after(int ticks, Runnable r) {
        if (ticks <= 0) {
            r.run();
            return;
        }
        synchronized (JOBS) {
            JOBS.add(new Job(now + ticks, r));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        now++;
        List<Job> due = new ArrayList<>();
        synchronized (JOBS) {
            for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
                Job j = it.next();
                if (j.due() <= now) {
                    due.add(j);
                    it.remove();
                }
            }
        }
        for (Job j : due) {
            try {
                j.run().run();
            } catch (Throwable err) {
                org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("A delayed shot failed", err);
            }
        }
    }
}
