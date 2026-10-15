package dev.factoryascension;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Freeze reporter: if one server tick takes longer than 10 seconds, writes what the server thread is doing (its stack
 * trace) to logs/factoryascension-freeze.txt and the log, again at 30 and 60 seconds. A freeze that never ends shows
 * the same place every time, which says exactly what's stuck, even if the game has to be killed afterwards.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FreezeReporter {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension freeze reporter");
    private static final long[] REPORT_AT = {10_000, 30_000, 60_000};
    private static volatile long tickStart = 0;
    private static volatile Thread serverThread;
    private static volatile boolean running = false;

    private FreezeReporter() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tickStart = System.currentTimeMillis();
        else tickStart = 0;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        serverThread = server.getRunningThread();
        running = true;
        Thread watcher = new Thread(FreezeReporter::watch, "Factory Ascension freeze reporter");
        watcher.setDaemon(true);
        watcher.start();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        running = false;
    }

    private static void watch() {
        long reportedTick = -1;
        int reports = 0;
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            long start = tickStart;
            if (start == 0) continue;
            if (start != reportedTick) {
                reportedTick = start;
                reports = 0;
            }
            long took = System.currentTimeMillis() - start;
            if (reports < REPORT_AT.length && took >= REPORT_AT[reports]) {
                report(took);
                reports++;
            }
        }
    }

    private static void report(long took) {
        Thread t = serverThread;
        if (t == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("==== ").append(LocalDateTime.now()).append(": one server tick has been running for ").append(took / 1000).append(" s ====\n");
        sb.append("What the server thread is doing (please send this file):\n");
        for (StackTraceElement e : t.getStackTrace()) sb.append("    at ").append(e).append('\n');
        sb.append('\n');
        LOG.error(sb.toString());
        try {
            Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("factoryascension-freeze.txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
