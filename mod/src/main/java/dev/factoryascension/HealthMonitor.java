package dev.factoryascension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Health checks: a background thread samples the game every 30 seconds and warns (in the log, and in
 * logs/factoryascension-health.txt) when something keeps growing that shouldn't:
 *  - memory still in use after garbage collection (a memory leak),
 *  - open files (a file leak),
 *  - threads (a thread leak),
 *  - direct (native-backed) buffers.
 * It only reads numbers the JVM already keeps, on its own low-priority thread, so it costs nothing in game.
 * /ascension health shows the latest numbers.
 */
public final class HealthMonitor {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static final int KEEP = 40;              // 20 minutes of samples
    private static final Deque<Sample> SAMPLES = new ArrayDeque<>();
    private static final List<String> WARNINGS = new ArrayList<>();
    private static ScheduledExecutorService timer;
    private static Path logFile;

    public record Sample(long time, long heapAfterGc, long openFiles, int threads, long directBytes) {
    }

    private HealthMonitor() {
    }

    /** Start once (both the game and a dedicated server). */
    public static synchronized void start(Path gameDir) {
        if (timer != null) return;
        logFile = gameDir.resolve("logs").resolve("factoryascension-health.txt");
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Factory Ascension health");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        timer.scheduleAtFixedRate(() -> {
            try { tick(); } catch (Throwable err) { LOG.debug("health sample failed", err); }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /** Memory in use after the last garbage collection (the leak signal: plain heap use goes up and down all the time). */
    static long heapAfterGc() {
        long total = 0;
        boolean any = false;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() != java.lang.management.MemoryType.HEAP) continue;
            MemoryUsage u = pool.getCollectionUsage();
            if (u != null) { total += u.getUsed(); any = true; }
        }
        return any ? total : ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    /**
     * Open files that aren't world region files (.mca/.mcc). The game keeps up to 256 region files open as a cache while
     * you explore, which isn't a leak; anything else that keeps growing is. Linux reads /proc/self/fd; elsewhere it falls
     * back to the plain count.
     */
    public static long openFiles() {
        java.io.File fds = new java.io.File("/proc/self/fd");
        String[] names = fds.list();
        if (names != null) {
            long n = 0;
            for (String name : names) {
                try {
                    String target = Files.readSymbolicLink(fds.toPath().resolve(name)).toString();
                    if (!target.endsWith(".mca") && !target.endsWith(".mcc")) n++;
                } catch (Exception ignored) {
                    // closed while we looked: not counted
                }
            }
            return n;
        }
        return allOpenFiles();
    }

    /** Every open file and socket, where the JVM can tell (Linux and macOS); -1 elsewhere. */
    public static long allOpenFiles() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.UnixOperatingSystemMXBean unix) return unix.getOpenFileDescriptorCount();
        return -1;
    }

    public static int threads() {
        return ManagementFactory.getThreadMXBean().getThreadCount();
    }

    static long directBytes() {
        for (BufferPoolMXBean b : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) if (b.getName().equals("direct")) return b.getMemoryUsed();
        return 0;
    }

    public static Sample sample() {
        return new Sample(System.currentTimeMillis(), heapAfterGc(), openFiles(), threads(), directBytes());
    }

    private static synchronized void tick() {
        Sample s = sample();
        SAMPLES.addLast(s);
        while (SAMPLES.size() > KEEP) SAMPLES.removeFirst();
        if (SAMPLES.size() < 12) return;
        Sample first = SAMPLES.peekFirst();
        // Growing nearly every sample across the window, by a real amount: that's a leak, not normal churn
        check("memory after garbage collection", first.heapAfterGc, s.heapAfterGc, rising(Sample::heapAfterGc), Math.max(256L << 20, first.heapAfterGc / 4), true);
        if (s.openFiles >= 0) check("open files", first.openFiles, s.openFiles, rising(Sample::openFiles), 200, false);
        check("threads", first.threads, s.threads, rising(x -> x.threads), 40, false);
        check("direct buffers", first.directBytes, s.directBytes, rising(Sample::directBytes), 256L << 20, true);
    }

    /** Share of steps in the window where the value went up. */
    private static double rising(java.util.function.ToLongFunction<Sample> f) {
        long prev = Long.MIN_VALUE;
        int up = 0, steps = 0;
        for (Sample x : SAMPLES) {
            long v = f.applyAsLong(x);
            if (prev != Long.MIN_VALUE) { steps++; if (v > prev) up++; }
            prev = v;
        }
        return steps == 0 ? 0 : up / (double) steps;
    }

    private static void check(String what, long from, long to, double risingShare, long minGrowth, boolean bytes) {
        if (to - from < minGrowth || risingShare < 0.7) return;
        String msg = String.format("Possible %s leak: %s went from %s to %s over the last %d minutes and keeps rising. "
            + "If the game slows down, restart it and send this file with the log.", what.equals("open files") ? "file" : what.equals("threads") ? "thread" : "memory",
            what, fmt(from, bytes), fmt(to, bytes), SAMPLES.size() / 2);
        if (WARNINGS.isEmpty() || !WARNINGS.get(WARNINGS.size() - 1).equals(msg)) {
            WARNINGS.add(msg);
            while (WARNINGS.size() > 20) WARNINGS.remove(0);
            LOG.warn("[Factory Ascension health] {}", msg);
            write(msg);
        }
        SAMPLES.clear();   // start a fresh window, so one leak is reported once per window, not every 30 seconds
    }

    private static String fmt(long v, boolean bytes) {
        return bytes ? (v >> 20) + " MB" : String.valueOf(v);
    }

    private static void write(String line) {
        try {
            if (logFile == null) return;
            Files.createDirectories(logFile.getParent());
            if (Files.exists(logFile) && Files.size(logFile) > 1_000_000) Files.delete(logFile);   // never grows without limit itself
            Files.writeString(logFile, java.time.LocalDateTime.now().withNano(0) + "  " + line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
        }
    }

    /** For /ascension health. */
    public static synchronized List<String> report() {
        Sample s = sample();
        List<String> out = new ArrayList<>();
        out.add("Memory in use after garbage collection: " + (s.heapAfterGc >> 20) + " MB of " + (Runtime.getRuntime().maxMemory() >> 20) + " MB");
        out.add("Open files (besides world region files): " + (s.openFiles >= 0 ? s.openFiles : "not available on this system") + " · threads: " + s.threads + " · direct buffers: " + (s.directBytes >> 20) + " MB");
        out.add(SAMPLES.size() + " samples kept; " + (WARNINGS.isEmpty() ? "no leaks seen" : WARNINGS.size() + " warning(s), latest: " + WARNINGS.get(WARNINGS.size() - 1)));
        return out;
    }
}
