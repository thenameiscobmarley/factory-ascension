package dev.factoryascension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * World backups: when a world closes (after its final save), it's zipped into backups/factoryascension/ in the game
 * folder, keeping the last few per world. If the world is ever opened without a mod it needs, Minecraft drops that
 * mod's blocks and items as they load, and nothing can stop that from inside the game; this keeps the last good files.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldBackups {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension backups");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private WorldBackups() {
    }

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve("backups").resolve("factoryascension");
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        if (!FACommonConfig.BACKUPS.get()) return;
        MinecraftServer server = event.getServer();
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        String name = world.getFileName().toString().replaceAll("[^A-Za-z0-9 _.-]", "_");
        int keep = FACommonConfig.KEEP.get();
        // Zipping a big world takes a moment: do it beside the game, which waits for it before it quits
        Thread t = new Thread(() -> backup(world, name, keep), "Factory Ascension backup");
        t.setDaemon(false);
        t.start();
    }

    private static void backup(Path world, String name, int keep) {
        Path dir = folder();
        Path zip = dir.resolve(name + "_" + LocalDateTime.now().format(STAMP) + ".zip");
        try {
            Files.createDirectories(dir);
            Path tmp = dir.resolve(zip.getFileName() + ".part");
            try (OutputStream out = Files.newOutputStream(tmp); ZipOutputStream z = new ZipOutputStream(out); Stream<Path> files = Files.walk(world)) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    if (Files.isDirectory(p) || p.getFileName().toString().equals("session.lock")) continue;
                    z.putNextEntry(new ZipEntry(name + "/" + world.relativize(p).toString().replace('\\', '/')));
                    Files.copy(p, z);
                    z.closeEntry();
                }
            }
            Files.move(tmp, zip);
            LOG.info("Backed up world '{}' to {}", name, zip);
            // Keep only the newest few for this world
            try (Stream<Path> old = Files.list(dir)) {
                List<Path> mine = old.filter(p -> p.getFileName().toString().startsWith(name + "_") && p.toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(Path::toString).reversed()).toList();
                for (int i = keep; i < mine.size(); i++) Files.deleteIfExists(mine.get(i));
            }
        } catch (IOException e) {
            LOG.error("Couldn't back up world '{}'", name, e);
        }
    }
}
