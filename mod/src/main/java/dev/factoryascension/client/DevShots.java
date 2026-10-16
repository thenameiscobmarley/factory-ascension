package dev.factoryascension.client;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.render.AscensionLighting;
import dev.factoryascension.client.render.FxParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer test run (only with -Dfa.devshots=true, never in a player's game): builds a scene in the loaded world, then
 * screenshots and times the graphics at every setting, sets off TNT for smoke and debris, opens the pause menu and the
 * settings screen, makes the depth pack, logs "[FA devshots]" lines and quits. Used to check the effects without a
 * person at the screen.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DevShots {
    static java.util.List<String> skinShots = new java.util.ArrayList<>();

    /** For the logs: every operator near the player, what it's doing. */
    static String npcReport(net.minecraft.client.Minecraft mc) {
        var server = mc.getSingleplayerServer();
        StringBuilder b = new StringBuilder();
        var sp = server.getPlayerList().getPlayers().get(0);
        for (var o : sp.serverLevel().getEntitiesOfClass(dev.factoryascension.npc.Operator.class, sp.getBoundingBox().inflate(80)))
            b.append(o.type.id).append(o.isAlive() ? "" : "(dead)").append(" hp=").append(Math.round(o.getHealth()))
                .append(" target=").append(o.getTarget() == null ? "-" : o.getTarget().getType().getDescriptionId().replace("entity.", "")).append("; ");
        b.append(" player hp=").append(Math.round(sp.getHealth()));
        return b.toString();
    }
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static final boolean ON = "true".equals(System.getProperty("fa.devshots"));
    /** -Dfa.devshots=pack: the shader-pack check (Oculus + a parallax shader + the depth pack) */
    private static final boolean PACK = "pack".equals(System.getProperty("fa.devshots"));
    private static int packStep = 0, packWait = 0, drawnBefore = 0;

    @SubscribeEvent
    public static void packTick(TickEvent.ClientTickEvent event) {
        if (!PACK || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (packWait > 0) { packWait--; return; }
        switch (packStep++) {
            case 0 -> { for (String c : SCENE) cmd(mc, c); cmd(mc, "tp @a 6.5 -58.4 1.6 0 10"); packWait = 400; }   // shaders take a while to compile
            case 1 -> { drawnBefore = AscensionLighting.framesDrawn; packWait = 60; }
            case 2 -> { log("shader pack active " + AscensionLighting.shaderPackActive() + "; Ascension Lighting frames while it was on: "
                    + (AscensionLighting.framesDrawn - drawnBefore) + "; resource packs " + mc.options.resourcePacks); shot(mc, "shaderpack_wall"); cmd(mc, "tp @a -2 -57.5 -6 0 12"); packWait = 80; }
            case 3 -> { shot(mc, "shaderpack_scene"); packWait = 10; }
            case 4 -> { log("done"); mc.stop(); }
            default -> { }
        }
    }
    private static int tick = 0, step = 0, wait = 0, waitedBoom = 0;
    private static long frameStart = 0, frameNanos = 0, lightNanos = 0;
    private static int frames = 0;
    private static final List<String> report = new ArrayList<>();

    private DevShots() {
    }

    private static int sliderIx = 0;

    private static void cmd(Minecraft mc, String c) {
        var server = mc.getSingleplayerServer();
        if (server != null) server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "fa_" + name + ".png", mc.getMainRenderTarget(), msg -> { });
        log("screenshot " + name);
    }

    private static void log(String s) {
        LOG.info("[FA devshots] {}", s);
        report.add(s);
    }

    // Frame timing: whole frames (render tick to render tick) and the lighting passes alone
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        if (frameStart > 0 && frames >= 0) { frameNanos += now - frameStart; frames++; }
        frameStart = now;
    }

    @SubscribeEvent
    public static void lighting(RenderLevelStageEvent event) {
        if (ON && event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) lightNanos += AscensionLighting.lastNanos;
    }

    private static void resetTiming() {
        frameNanos = 0; lightNanos = 0; frames = 0; frameStart = 0;
    }

    private static String timing() {
        return frames == 0 ? "no frames" : String.format("%.1f ms/frame over %d frames (lighting CPU side %.2f ms)", frameNanos / 1e6 / frames, frames, lightNanos / 1e6 / Math.max(1, frames));
    }

    private static int freeX = 0, freeY = 0;

    private static final String[] SCENE = {
        "time set 6000", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "gamerule doMobSpawning false",
        "fill 4 -60 4 9 -55 9 stone_bricks hollow", "fill 4 -60 6 4 -58 7 air", "fill 2 -55 2 11 -55 11 quartz_block",
        "fill -12 -61 -4 -4 -62 4 water", "fill -8 -60 6 -6 -52 8 cobblestone", "setblock -7 -51 7 water",
        "fill -3 -60 12 3 -57 12 red_wool", "fill 12 -60 2 13 -59 3 tnt", "fill 10 -60 -2 11 -57 -1 oak_planks",
        "gamemode spectator @a", "tp @a -2 -57.5 -6 0 12"
    };

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        tick++;
        if (wait > 0) { wait--; return; }
        try {
            switch (step++) {
                case 0 -> { for (String c : SCENE) cmd(mc, c); FAClientConfig.SAFE_MODE.set(false); wait = 100;
                    if (Boolean.getBoolean("fa.quick")) step = 200;   // only the inventory shots
                    if (Boolean.getBoolean("fa.guns")) step = 300;    // only the gun shots
                    if (Boolean.getBoolean("fa.bench")) step = 400;   // only the workbench and attachment shots
                    if (Boolean.getBoolean("fa.skins")) step = 500;   // only the smooth guns, skins, akimbo and gangster shots
                    if (Boolean.getBoolean("fa.npc")) step = 600;     // only the operators, structures, realism camera and new bench tabs
                    if (Boolean.getBoolean("fa.lens")) step = 650;    // only the realism camera
                    if (Boolean.getBoolean("fa.v25")) step = 700;
                    if (Boolean.getBoolean("fa.sliders")) step = 750;
                    if (Boolean.getBoolean("fa.tab")) step = 760;
                    if (Boolean.getBoolean("fa.enemies")) step = 780;
                    if (Boolean.getBoolean("fa.aaa")) step = 800;      // only: the cinematic look, haze, sun, aim depth of field, structure spacing  // only: stand among enemies (and a gas station, and water) for a while     // only: the creative tab (JEI's list) and tree shadows near and far  // only: drag every slider in the settings screen (crash hunt)     // only the depth pack, drugs, NODs, flashbang, raid blocks, crops
                    log("mods: oculus " + net.minecraftforge.fml.ModList.get().isLoaded("oculus") + ", embeddium " + net.minecraftforge.fml.ModList.get().isLoaded("embeddium")
                        + ", flywheel compat " + net.minecraftforge.fml.ModList.get().isLoaded("irisflw") + "; shader pack active " + AscensionLighting.shaderPackActive()); }
                case 1 -> { FAClientConfig.LIGHTING.set(0); resetTiming(); wait = 60; }
                case 2 -> { log("lighting off: " + timing()); shot(mc, "lighting_off"); FAClientConfig.LIGHTING.set(1); resetTiming(); wait = 60; }
                case 3 -> { log("lighting low: " + timing() + ", sun " + java.util.Arrays.toString(AscensionLighting.lastSun)); shot(mc, "lighting_low"); FAClientConfig.LIGHTING.set(2); resetTiming(); wait = 60; }
                case 4 -> { log("lighting medium: " + timing()); shot(mc, "lighting_medium"); FAClientConfig.LIGHTING.set(3); resetTiming(); wait = 60; }
                case 5 -> { log("lighting high: " + timing() + ", targets held " + AscensionLighting.targetsHeld() + ", frames drawn " + AscensionLighting.framesDrawn);
                    shot(mc, "lighting_high"); AscensionLighting.debugView = 1; step = 50; wait = 20; }
                case 50 -> { shot(mc, "debug_shadow"); AscensionLighting.debugView = 2; wait = 20; }
                case 51 -> { shot(mc, "debug_bounce"); AscensionLighting.debugView = 4; step = 52; wait = 20; }
                case 52 -> { shot(mc, "debug_copy"); AscensionLighting.debugView = 0; FAClientConfig.LIGHTING.set(0); step = 53; wait = 20; }
                case 53 -> { shot(mc, "lighting_off2"); FAClientConfig.LIGHTING.set(3); step = 54; wait = 20; }
                case 54 -> { shot(mc, "lighting_high2"); FAClientConfig.LIGHTING.set(2); step = 6;
                    cmd(mc, "time set 12400"); cmd(mc, "tp @a -2 -56 -6 90 -4"); wait = 60; }
                case 6 -> { log("sunset sun " + java.util.Arrays.toString(AscensionLighting.lastSun)); shot(mc, "sunset_rays"); cmd(mc, "time set 6000"); cmd(mc, "tp @a 12.5 -56 -7 0 18"); wait = 40; }
                case 7 -> { cmd(mc, "setblock 12 -60 4 redstone_block"); wait = 84; }
                case 8 -> { if (FxParticles.liveDebris() == 0 && wait == 0 && tick < 100000 && waitedBoom++ < 40) { step--; wait = 1; return; }
                    log("explosion: debris " + FxParticles.liveDebris() + ", smoke " + FxParticles.liveSmoke() + ", foam " + FxParticles.liveFoam()); wait = 6; }
                case 9 -> { shot(mc, "explosion"); step = 90; wait = 30; }
                case 90 -> { shot(mc, "explosion_after"); cmd(mc, "tp @a -4 -58 0 30 25"); step = 10; wait = 60; }
                case 10 -> { log("water: foam " + FxParticles.liveFoam()); shot(mc, "water"); mc.setScreen(new PauseScreen(true)); wait = 20; }
                case 11 -> { shot(mc, "pause_menu"); mc.setScreen(new FASettingsScreen(mc.screen)); wait = 20; }
                case 12 -> { shot(mc, "settings"); mc.setScreen(null); log("depth pack on " + DepthShaders.enabled()); wait = 40; }
                case 13 -> { cmd(mc, "tp @a 6.5 -58.4 1.6 0 10"); cmd(mc, "summon minecraft:zombie 8 -60 3.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[150f,0f]}"); step = 130; wait = 40; }
                case 130 -> { shot(mc, "depth_wall_on"); cmd(mc, "tp @a 3.2 -58.6 2.2 -62 8"); step = 134; wait = 30; }
                case 134 -> { shot(mc, "depth_graze_on"); cmd(mc, "tp @a 9.5 -58.4 0.6 -30 15"); step = 131; wait = 30; }
                case 131 -> { shot(mc, "depth_mob_on"); DepthShaders.set(false); step = 132; wait = 40; }
                case 132 -> { if (mc.getOverlay() != null) { step = 132; wait = 20; return; }   // still reloading
                    log("depth pack on after turning it off: " + DepthShaders.enabled()); wait = 40; step = 135; }
                case 135 -> { shot(mc, "depth_mob_off"); cmd(mc, "tp @a 6.5 -58.4 1.6 0 10"); step = 133; wait = 30; }
                case 133 -> { shot(mc, "depth_wall_off"); cmd(mc, "tp @a 3.2 -58.6 2.2 -62 8"); step = 136; wait = 30; }
                case 136 -> { shot(mc, "depth_graze_off"); DepthShaders.set(true); step = 137; wait = 40; }
                case 137 -> { if (mc.getOverlay() != null) { step = 137; wait = 20; return; } step = 200; wait = 20; }
                // Tarkov inventory: the opening (bag up, zip, the grid growing out), the full grid, holding an item over it, turning and placing it
                case 200 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 20"); cmd(mc, "clear @a");
                    for (String g : new String[] {"torch 64", "bread 32", "iron_pickaxe", "iron_axe", "oak_log 64", "stone 64", "arrow 64", "apple 16", "water_bucket",
                        "iron_chestplate", "diamond_sword", "cobblestone 40", "factoryascension:shotgun", "factoryascension:flare_gun", "bow", "iron_helmet",
                        "iron_leggings", "factoryascension:shotgun_shell 24", "factoryascension:flare_green 2", "diamond 12", "golden_apple 3", "shield"})
                        cmd(mc, "give @a " + g);
                    wait = 60; }
                case 201 -> { log("tarkov inventory on: " + dev.factoryascension.client.tarkov.TarkovClient.enabled);
                    dev.factoryascension.client.tarkov.TarkovClient.debugTime = 0.37f; mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                    log("screen: " + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName())); wait = 40; }
                case 202 -> { shot(mc, "inv_bag"); dev.factoryascension.client.tarkov.TarkovClient.debugTime = 0.52f; wait = 40; }
                case 203 -> { shot(mc, "inv_growing"); dev.factoryascension.client.tarkov.TarkovClient.debugTime = 3f; wait = 40; }
                case 204 -> { shot(mc, "inv_open");
                    if (mc.screen instanceof dev.factoryascension.client.tarkov.TarkovScreen ts) {
                        int s = -1;
                        for (int i = 0; i < 27; i++) if (mc.player.getInventory().items.get(9 + i).is(net.minecraft.world.item.Items.IRON_CHESTPLATE)) s = i;
                        log("chestplate in main slot " + s + " at " + (s < 0 ? "-" : Integer.toHexString(dev.factoryascension.client.tarkov.TarkovClient.LAYOUT.pos[s])));
                        if (s >= 0) { double[] c = ts.devCellCenter(s); ts.mouseClicked(c[0], c[1], 0); }
                        int spot = dev.factoryascension.tarkov.TarkovLayout.findSpot(dev.factoryascension.client.tarkov.TarkovClient.LAYOUT.occupancy(mc.player.getInventory().items), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
                        freeX = dev.factoryascension.tarkov.TarkovLayout.x(spot) + 1; freeY = dev.factoryascension.tarkov.TarkovLayout.y(spot) + 1;
                        log("free 3x3 spot at " + (freeX - 1) + "," + (freeY - 1));
                        double[] to = ts.devCell(freeX, freeY);
                        double k = mc.getWindow().getGuiScale();
                        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), to[0] * k, to[1] * k);
                        log("holding " + mc.player.containerMenu.getCarried());
                    }
                    wait = 40; }
                case 205 -> { shot(mc, "inv_holding"); if (mc.screen != null) mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_R, 0, 0); wait = 40; }
                case 206 -> { shot(mc, "inv_turned");
                    if (mc.screen instanceof dev.factoryascension.client.tarkov.TarkovScreen ts) { double[] to = ts.devCell(freeX, freeY); ts.mouseClicked(to[0], to[1], 0); }
                    wait = 20; }
                case 207 -> { int s = -1;
                    for (int i = 0; i < 27; i++) if (mc.player.getInventory().items.get(9 + i).is(net.minecraft.world.item.Items.IRON_CHESTPLATE)) s = i;
                    log("placed: carried " + mc.player.containerMenu.getCarried() + ", chestplate slot " + s + " at " + (s < 0 ? "-" : Integer.toHexString(dev.factoryascension.client.tarkov.TarkovClient.LAYOUT.pos[s])));
                    shot(mc, "inv_placed"); mc.options.guiScale().set(2); mc.resizeDisplay(); step = 208; wait = 40; }
                // Guns in first person: hip, aimed (sights on the crosshair, stock clear of the eye), reloading, walking
                case 300 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "clear @a");
                    cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:m4a1");
                    cmd(mc, "give @a factoryascension:cartridge{Cal:\"r556\",Type:\"REGULAR\"} 64");
                    mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); wait = 60; }
                case 301 -> { shot(mc, "gun_m4_hip"); mc.options.keyUse.setDown(true); wait = 40; }
                case 302 -> { shot(mc, "gun_m4_ads"); mc.options.keyUse.setDown(false);
                    dev.factoryascension.network.FANetwork.CHANNEL.sendToServer(new dev.factoryascension.network.FANetwork.GunAction(1, false)); wait = 14; }
                case 303 -> { log("reload phase " + dev.factoryascension.gun.GunItem.phase(mc.player.getMainHandItem()) + " at " + dev.factoryascension.gun.GunItem.phaseProgress(mc.player.getMainHandItem()));
                    shot(mc, "gun_m4_reload"); wait = 60; }
                case 304 -> { log("m4 after reload: " + dev.factoryascension.gun.GunItem.ammo(mc.player.getMainHandItem()));
                    cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:ak74"); mc.options.keyUse.setDown(true); wait = 50; }
                case 305 -> { shot(mc, "gun_ak_ads"); cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:p17"); wait = 50; }
                case 306 -> { shot(mc, "gun_p17_ads"); cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:ax338"); wait = 50; }
                case 307 -> { shot(mc, "gun_ax338_ads"); cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:shotgun"); wait = 50; }
                case 308 -> { shot(mc, "gun_shotgun_ads"); mc.options.keyUse.setDown(false);
                    cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:mk17"); mc.options.keyUp.setDown(true); wait = 30; }
                case 309 -> { shot(mc, "gun_walk"); mc.options.keyUp.setDown(false); mc.options.keyLeft.setDown(true); wait = 20; }
                case 310 -> { shot(mc, "gun_strafe"); mc.options.keyLeft.setDown(false); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); wait = 30; }
                case 311 -> { shot(mc, "gun_third"); cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:p17"); wait = 30; }
                case 312 -> { shot(mc, "gun_third_pistol"); mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                // Attachments on a gun, and the Weapons Workbench
                case 400 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "clear @a");
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        net.minecraft.world.item.ItemStack gun = new net.minecraft.world.item.ItemStack(dev.factoryascension.FactoryAscension.GUNS.get("m4a1").get());
                        java.util.List<String> ids = new java.util.ArrayList<>();
                        for (String fam : new String[] {"suppressor_long", "acog", "grip_vertical", "mag_drum", "combo_unit", "stock_precision"})
                            for (var a : dev.factoryascension.gun.Attachments.ALL) if (a.family.equals(fam) && a.tier >= 2) { ids.add(a.id); break; }
                        dev.factoryascension.gun.GunStats.setIds(gun, ids);
                        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
                    });
                    mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); wait = 60; }
                case 401 -> { log("attached: " + dev.factoryascension.gun.GunStats.ids(mc.player.getMainHandItem()));
                    shot(mc, "att_hip"); mc.options.keyUse.setDown(true); wait = 40; }
                case 402 -> { shot(mc, "att_ads"); mc.options.keyUse.setDown(false);
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        var pos = sp.blockPosition().north(2);
                        sp.level().setBlockAndUpdate(pos, dev.factoryascension.FactoryAscension.WEAPONS_BENCH.get().defaultBlockState());
                        net.minecraft.world.item.ItemStack gun = sp.getMainHandItem().copy();
                        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                        sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
                        net.minecraftforge.network.NetworkHooks.openScreen(sp, new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, pl) -> { var m = new dev.factoryascension.gun.WeaponsBenchMenu(id, inv, pos); m.bench.setItem(0, gun);
                                m.bench.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL, 8)); m.bench.setItem(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SUGAR, 8)); return m; },
                            net.minecraft.network.chat.Component.literal("Weapons Workbench")), pos);
                    });
                    wait = 40; }
                case 403 -> { log("screen: " + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
                    if (mc.screen instanceof dev.factoryascension.client.gun.WeaponsBenchScreen b) b.devTab(0, dev.factoryascension.gun.Attachments.Slot.OPTIC);
                    wait = 30; }
                case 404 -> { shot(mc, "bench_modding");
                    if (mc.screen instanceof dev.factoryascension.client.gun.WeaponsBenchScreen b) b.devTab(1, null); wait = 30; }
                case 405 -> { shot(mc, "bench_press"); mc.setScreen(null); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                // Smooth gun meshes with skins; akimbo; gangster grip (fa.shot = "gun:skin:atts,..." list, or the default set)
                case 500 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "clear @a"); cmd(mc, "time set noon");
                    String list = System.getProperty("fa.shot", "m4a1::,m4a1:obsidian:,ak74:snow:,mk17:black_water:,m4a1:torn:,akm:lava:,m4a1:moss:,p17::akimbo,m1911::gangster_grip,svd:toxic:,saiga12:inferno:");
                    skinShots = new java.util.ArrayList<>(java.util.List.of(list.split(",")));
                    mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); wait = 40; }
                case 501 -> {
                    if (skinShots.isEmpty()) { cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; break; }
                    String[] parts = (skinShots.get(0) + "::").split(":", -1);
                    String gid = parts[0], skin = parts[1], atts = parts[2];
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        var reg = dev.factoryascension.FactoryAscension.GUNS.get(gid);
                        net.minecraft.world.item.ItemStack gun = reg != null ? new net.minecraft.world.item.ItemStack(reg.get()) : net.minecraft.world.item.ItemStack.EMPTY;
                        dev.factoryascension.gun.Skins.set(gun, skin);
                        java.util.List<String> ids = new java.util.ArrayList<>();
                        for (String fam : atts.split("\\+")) if (!fam.isEmpty())
                            for (var a : dev.factoryascension.gun.Attachments.ALL) if (a.family.equals(fam)) { ids.add(a.id); break; }
                        dev.factoryascension.gun.GunStats.setIds(gun, ids);
                        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
                    });
                    wait = 45; step = 502; }
                case 502 -> {
                    String name = skinShots.remove(0).replace(':', '_').replace('+', '_');
                    shot(mc, "skin_" + name); step = 501; wait = 2; }
                // Operators, structures, the realism camera, the bench's GUNSMITH and SKINS tabs
                case 600 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "clear @a"); cmd(mc, "time set noon");
                    cmd(mc, "effect give @a resistance 99999 4 true"); cmd(mc, "effect give @a regeneration 99999 2 true");
                    FAClientConfig.REALISM.set(false);
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        var lvl = sp.serverLevel();
                        // A lineup of every kind, standing still, facing you
                        int i = 0;
                        for (var t : dev.factoryascension.npc.OpType.values()) {
                            var pos = net.minecraft.core.BlockPos.containing(sp.getX() - 9.5 + i, sp.getY(), sp.getZ() + 7);
                            var o = dev.factoryascension.npc.Operators.spawn(lvl, t, pos, net.minecraft.world.entity.MobSpawnType.COMMAND, false);
                            if (o != null) { o.setNoAi(true); o.setYRot(180); o.setYHeadRot(180); o.yBodyRot = 180; o.setCustomNameVisible(true); }
                            i++;
                        }
                    });
                    mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); wait = 80; }
                case 601 -> { shot(mc, "npc_lineup"); mc.player.setYRot(0); mc.player.setXRot(0); cmd(mc, "tp @a ~ ~ ~-3 0 0"); wait = 50; }
                case 602 -> { shot(mc, "npc_lineup_near"); FAClientConfig.REALISM.set(true); wait = 40; }
                case 603 -> { shot(mc, "npc_lineup_realism"); FAClientConfig.REALISM.set(false);
                    cmd(mc, "kill @e[type=!player]");
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        var lvl = sp.serverLevel();
                        // A fight: two raiders ahead, a scav squad off to the side that hates them
                        dev.factoryascension.npc.Operators.squad(lvl, sp.getX() + 2, sp.getZ() + 16, dev.factoryascension.npc.OpType.RAIDER, 2, null);
                        dev.factoryascension.npc.Operators.squad(lvl, sp.getX() - 14, sp.getZ() + 20, dev.factoryascension.npc.OpType.SCAV, 3, null);
                        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(dev.factoryascension.FactoryAscension.GUNS.get("hk416").get()));
                    });
                    wait = 120; }
                case 604 -> { log("npc fight: " + npcReport(mc)); shot(mc, "npc_fight"); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); wait = 60; }
                case 605 -> { log("npc fight later: " + npcReport(mc)); shot(mc, "npc_fight_third"); mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                    cmd(mc, "kill @e[type=!player]"); cmd(mc, "gamemode creative @a");
                    cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "place feature factoryascension:military_outpost -2 -60 22"); wait = 80; }
                case 606 -> { cmd(mc, "tp @a -2 -38 0 0 40"); wait = 120; }
                case 607 -> { log("operators after the outpost: " + npcReport(mc)); shot(mc, "struct_outpost");
                    cmd(mc, "kill @e[type=!player]");
                    cmd(mc, "place feature factoryascension:scav_camp -40 -60 -6"); cmd(mc, "place feature factoryascension:sniper_tower 36 -60 -6");
                    cmd(mc, "place feature factoryascension:warlord_fort -2 -60 -50"); wait = 80; }
                case 608 -> { cmd(mc, "tp @a -2 -30 -14 180 50"); wait = 120; }
                case 609 -> { shot(mc, "struct_fort"); cmd(mc, "tp @a -14 -40 -6 90 40"); wait = 80; }
                case 610 -> { shot(mc, "struct_camp"); cmd(mc, "tp @a 14 -40 -6 -90 30"); wait = 80; }
                case 611 -> { shot(mc, "struct_tower"); cmd(mc, "kill @e[type=!player]");
                    cmd(mc, "place feature factoryascension:bunker -2 -60 60"); cmd(mc, "place feature factoryascension:crash_site -40 -60 60");
                    cmd(mc, "place feature factoryascension:gas_station 36 -60 60"); wait = 80; }
                case 612 -> { cmd(mc, "tp @a -2 -32 30 0 45"); wait = 120; }
                case 613 -> { shot(mc, "struct_bunker_crash_gas"); cmd(mc, "kill @e[type=!player]");
                    cmd(mc, "place feature factoryascension:radio_tower -40 -60 -100"); cmd(mc, "place feature factoryascension:checkpoint -2 -60 -100");
                    cmd(mc, "place feature factoryascension:trench 36 -60 -100"); cmd(mc, "place feature factoryascension:cult_shrine -2 -60 -140");
                    cmd(mc, "place feature factoryascension:ruins 70 -60 -6"); wait = 80; }
                case 614 -> { cmd(mc, "tp @a -2 -30 -70 180 40"); wait = 120; }
                case 615 -> { shot(mc, "struct_more"); cmd(mc, "kill @e[type=!player]"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "gamemode survival @a");
                    // The bench: GUNSMITH and SKINS
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var sp = server.getPlayerList().getPlayers().get(0);
                        var pos = sp.blockPosition().north(2);
                        sp.level().setBlockAndUpdate(pos, dev.factoryascension.FactoryAscension.WEAPONS_BENCH.get().defaultBlockState());
                        net.minecraft.world.item.ItemStack gun = new net.minecraft.world.item.ItemStack(dev.factoryascension.FactoryAscension.GUNS.get("ak12").get());
                        sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64));
                        net.minecraftforge.network.NetworkHooks.openScreen(sp, new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, pl) -> { var m = new dev.factoryascension.gun.WeaponsBenchMenu(id, inv, pos); m.bench.setItem(0, gun); return m; },
                            net.minecraft.network.chat.Component.literal("Weapons Workbench")), pos);
                    });
                    wait = 40; }
                case 616 -> { if (mc.screen instanceof dev.factoryascension.client.gun.WeaponsBenchScreen b) { b.devTab(2, null); b.devPick("minigun"); } wait = 40; }
                case 617 -> { shot(mc, "bench_gunsmith"); if (mc.screen instanceof dev.factoryascension.client.gun.WeaponsBenchScreen b) { b.devTab(3, null); b.devPick("black_water"); } wait = 40; }
                case 618 -> { shot(mc, "bench_skins"); if (mc.screen instanceof dev.factoryascension.client.gun.WeaponsBenchScreen b) { b.devTab(1, null); } wait = 30; }
                case 619 -> { shot(mc, "bench_press18"); mc.setScreen(null); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                // The realism camera: on (calm, then hurt and stressed, then ears ringing), and off
                case 650 -> { cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a -2 -60 -6 0 0"); cmd(mc, "time set noon");
                    FAClientConfig.REALISM.set(true); wait = 40; }
                case 651 -> { shot(mc, "lens_on"); dev.factoryascension.client.gun.GunAnim.stress = 1; cmd(mc, "damage @s 12"); wait = 30; }
                case 652 -> { shot(mc, "lens_hurt"); cmd(mc, "effect give @a instant_health 1 3"); dev.factoryascension.client.gun.GunAnim.stress = 0; wait = 30; }
                case 653 -> { FAClientConfig.REALISM.set(false); wait = 40; }
                case 654 -> { shot(mc, "lens_off"); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                // ---- v25: depth pack (with Embeddium in -PfaGfx runs), mobs, drugs, NODs, flashbang, raid blocks, crops ----
                case 700 -> { cmd(mc, "gamemode creative @a"); cmd(mc, "time set noon"); cmd(mc, "weather clear"); FAClientConfig.REALISM.set(false);
                    cmd(mc, "fill -9 -61 -4 5 -56 6 air"); cmd(mc, "fill -9 -61 0 5 -61 6 grass_block");
                    String[] wall = {"stone_bricks", "cobblestone", "oak_planks", "bricks", "factoryascension:ballistic_concrete", "mossy_cobblestone", "deepslate_tiles", "spruce_log"};
                    for (int i = 0; i < wall.length; i++) cmd(mc, "fill " + (-8 + i * 2) + " -60 4 " + (-7 + i * 2) + " -57 4 " + wall[i]);
                    cmd(mc, "fill -8 -61 1 5 -61 3 cobblestone");
                    cmd(mc, "setblock -3 -60 3 factoryascension:chem_lab"); cmd(mc, "setblock 0 -60 3 factoryascension:steel_door[half=lower]");
                    cmd(mc, "setblock 0 -59 3 factoryascension:steel_door[half=upper]"); cmd(mc, "setblock 1 -59 3 factoryascension:ballistic_glass");
                    cmd(mc, "setblock 2 -59 3 factoryascension:ballistic_glass_pane"); cmd(mc, "setblock 3 -60 3 factoryascension:steel_trapdoor[half=top]");
                    cmd(mc, "fill -8 -61 2 -6 -61 2 farmland"); cmd(mc, "setblock -8 -60 2 factoryascension:cannabis[age=7]");
                    cmd(mc, "setblock -7 -60 2 factoryascension:coca[age=7]"); cmd(mc, "setblock -6 -60 2 factoryascension:cannabis[age=3]");
                    cmd(mc, "tp @a -1.5 -59.4 -1.5 0 12"); DepthShaders.set(true); wait = 260; }
                case 701 -> { log("depth on: pack " + DepthShaders.enabled() + ", embeddium " + net.minecraftforge.fml.ModList.get().isLoaded("embeddium")); shot(mc, "v25_depth_on"); 
                    cmd(mc, "tp @a -6.5 -59 1.2 -50 18"); wait = 60; }
                case 702 -> { shot(mc, "v25_depth_close"); FAClientConfig.DEPTH_SMOOTH.set(1.0); FAClientConfig.DEPTH_STRENGTH.set(2.0); mc.reloadResourcePacks(); wait = 600; }
                case 703 -> { shot(mc, "v25_depth_smooth_deep"); FAClientConfig.DEPTH_SMOOTH.set(0.0); FAClientConfig.DEPTH_STRENGTH.set(1.0); mc.reloadResourcePacks(); wait = 600; }
                case 704 -> { shot(mc, "v25_depth_sharp"); FAClientConfig.DEPTH_SMOOTH.set(0.5); DepthShaders.set(false); wait = 260; }
                case 705 -> { shot(mc, "v25_depth_off"); DepthShaders.set(true); wait = 260; }
                case 706 -> { cmd(mc, "tp @a -1.5 -59.4 -1.5 0 12"); cmd(mc, "summon zombie -2.5 -60 1.5 {NoAI:1b,Rotation:[180f,0f]}");
                    cmd(mc, "summon factoryascension:raider -0.5 -60 1.5 {NoAI:1b,Rotation:[180f,0f]}"); wait = 60; }
                case 707 -> { shot(mc, "v25_mobs_depth"); cmd(mc, "kill @e[type=!player]"); cmd(mc, "gamemode survival @a");
                    cmd(mc, "effect give @a factoryascension:stoned 60 0"); wait = 200; }
                case 708 -> { shot(mc, "v25_stoned"); cmd(mc, "effect clear @a"); cmd(mc, "effect give @a factoryascension:tripping 60 1"); wait = 200; }
                case 709 -> { shot(mc, "v25_tripping"); cmd(mc, "effect clear @a"); cmd(mc, "effect give @a factoryascension:psychosis 60 1"); wait = 200; }
                case 710 -> { cmd(mc, "tp @a -1.5 -59.4 -1.5 180 4"); wait = 20; }
                case 711 -> { Hallucinations.devFigure(); wait = 30; }
                case 712 -> { shot(mc, "v25_psychosis"); cmd(mc, "effect clear @a"); cmd(mc, "effect give @a factoryascension:tweaking 60 0"); wait = 200; }
                case 713 -> { shot(mc, "v25_tweaking"); cmd(mc, "effect clear @a"); cmd(mc, "tp @a -1.5 -59.4 -1.5 0 12"); wait = 120; }
                case 714 -> { cmd(mc, "time set midnight"); cmd(mc, "item replace entity @a armor.head with factoryascension:nods_green{Down:1b}"); wait = 80; }
                case 715 -> { shot(mc, "v25_nods_green"); cmd(mc, "item replace entity @a armor.head with factoryascension:nods_white{Down:1b}"); wait = 60; }
                case 716 -> { shot(mc, "v25_nods_white"); cmd(mc, "item replace entity @a armor.head with air"); cmd(mc, "time set noon");
                    cmd(mc, "effect give @a factoryascension:flashed 4 0"); wait = 14; }
                case 717 -> { shot(mc, "v25_flashed"); cmd(mc, "item replace entity @a armor.chest with factoryascension:plate_carrier");
                    cmd(mc, "item replace entity @a armor.head with factoryascension:nods_green"); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); wait = 120; }
                case 718 -> { shot(mc, "v25_carrier_worn"); mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                    cmd(mc, "item replace entity @a armor.chest with air"); cmd(mc, "item replace entity @a armor.head with air");
                    cmd(mc, "tp @a -1.5 -58.4 0.3 0 25"); wait = 40; }
                case 719 -> { shot(mc, "v25_blocks"); cmd(mc, "give @a factoryascension:ak12"); cmd(mc, "give @a factoryascension:plate_carrier");
                    cmd(mc, "give @a factoryascension:hybrid_plate 2"); cmd(mc, "give @a factoryascension:joint 4"); cmd(mc, "give @a factoryascension:flashbang 3");
                    mc.gui.getChat().clearMessages(false); mc.getToasts().clear(); dev.factoryascension.client.tarkov.TarkovClient.debugTime = 3f;
                    mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); wait = 60; }
                case 720 -> { shot(mc, "v25_inventory"); mc.setScreen(null); dev.factoryascension.client.tarkov.TarkovClient.debugTime = null;
                    FAClientConfig.REALISM.set(true); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                case 750 -> { mc.setScreen(new FASettingsScreen(null)); wait = 20; }
                case 751 -> {
                    // One slider per tick, like a person: click, drag, let go
                    var scr = mc.screen;
                    java.util.List<net.minecraft.client.gui.components.AbstractSliderButton> sliders = new java.util.ArrayList<>();
                    if (scr != null) for (var w : scr.children()) if (w instanceof net.minecraft.client.gui.components.AbstractSliderButton sl) sliders.add(sl);
                    if (sliderIx < sliders.size()) {
                        var sl = sliders.get(sliderIx);
                        double y = sl.getY() + 10, x = sl.getX() + sl.getWidth() * (0.2 + 0.6 * ((sliderIx * 7) % 10) / 10.0);
                        log("drag slider " + sliderIx + " '" + sl.getMessage().getString() + "'");
                        sl.mouseClicked(x, y, 0);
                        for (int k = 1; k <= 8; k++) sl.mouseDragged(x + k * 4, y, 0, 4, 0);
                        sl.mouseReleased(x + 32, y, 0);
                        sliderIx++;
                        step = 751;
                        wait = 2;
                    } else {
                        log("dragged " + sliders.size() + " sliders, all fine");
                        wait = 60;
                    }
                }
                case 752 -> { shot(mc, "sliders_after"); mc.setScreen(null); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                case 760 -> {
                    net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
                    var items = dev.factoryascension.FactoryAscension.TAB.get().getDisplayItems();
                    long atts = items.stream().filter(i -> i.getItem() == dev.factoryascension.FactoryAscension.ATTACHMENT.get()).count();
                    long missing = dev.factoryascension.FactoryAscension.ITEMS.getEntries().stream().map(e -> e.get())
                        .filter(it -> items.stream().noneMatch(st -> st.getItem() == it)).count();
                    log("tab: " + items.size() + " stacks, " + atts + " attachments (of " + dev.factoryascension.gun.Attachments.ALL.size() + "), mod items missing from it: " + missing);
                    cmd(mc, "gamemode creative @a"); cmd(mc, "time set 2000"); cmd(mc, "weather clear"); FAClientConfig.LIGHTING.set(3); FAClientConfig.REALISM.set(false);
                    cmd(mc, "fill -12 -61 -12 12 -61 30 grass_block"); cmd(mc, "fill -12 -60 -12 12 -50 30 air");
                    cmd(mc, "place feature minecraft:oak 0 -60 8"); cmd(mc, "place feature minecraft:oak 4 -60 12");
                    cmd(mc, "tp @a -3 -58.4 0 -20 25"); wait = 120; }
                case 761 -> { shot(mc, "shadow_near"); cmd(mc, "tp @a -6 -52 -18 -10 22"); wait = 120; }
                case 762 -> { shot(mc, "shadow_far"); FAClientConfig.REALISM.set(true); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                case 780 -> {
                    log("mods: " + net.minecraftforge.fml.ModList.get().getMods().stream().map(m -> m.getModId()).sorted().collect(java.util.stream.Collectors.joining(",")));
                    cmd(mc, "gamemode survival @a"); cmd(mc, "difficulty hard"); cmd(mc, "time set 2000");
                    cmd(mc, "effect give @a resistance 1000000 255 true"); cmd(mc, "effect give @a regeneration 1000000 255 true"); cmd(mc, "effect give @a saturation 1000000 255 true");
                    cmd(mc, "fill -20 -61 -20 30 -50 40 air"); cmd(mc, "fill -20 -61 -20 30 -61 40 grass_block");
                    cmd(mc, "fill -6 -61 -6 -2 -61 -2 water");
                    cmd(mc, "place feature factoryascension:gas_station 14 -60 18");
                    cmd(mc, "tp @a 0 -60 0 45 10");
                    cmd(mc, "give @a factoryascension:ak12"); cmd(mc, "give @a factoryascension:cartridge 64");
                    wait = 60; }
                case 781 -> {
                    String[] ops = {"scav", "raider", "breacher", "grenadier", "marksman", "rogue", "cultist", "juggernaut", "hunter", "bodyguard", "machine_gunner", "looter"};
                    for (int i = 0; i < ops.length; i++) {
                        double a = i * Math.PI * 2 / ops.length;
                        cmd(mc, String.format(java.util.Locale.ROOT, "summon factoryascension:%s %.1f -60 %.1f", ops[i], Math.cos(a) * 14, Math.sin(a) * 14));
                    }
                    for (String m : new String[] {"zombie", "skeleton", "spider", "husk", "drowned", "pillager", "vindicator", "witch"})
                        cmd(mc, "summon minecraft:" + m + " " + (int) (Math.random() * 16 - 8) + " -60 " + (int) (Math.random() * 16 - 8));
                    cmd(mc, "summon minecraft:villager 3 -60 3");
                    wait = 600; }
                case 782 -> { shot(mc, "enemies_1"); log("after 30 s among enemies: still running"); wait = 600; }
                case 783 -> { shot(mc, "enemies_2"); cmd(mc, "tp @a 14 -60 14 0 10"); wait = 600; }
                case 784 -> { shot(mc, "enemies_3"); log("after 90 s among enemies and at the gas station: still running, " +
                    mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mc.player.getBoundingBox().inflate(64)).size() + " living things near");
                    cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                case 800 -> {
                    var sp = mc.getSingleplayerServer();
                    if (sp != null) {
                        var dir = new java.io.File(mc.gameDirectory, "screenshots");
                        double[] r = dev.factoryascension.worldgen.SiteGrid.survey(sp.overworld().getSeed(), sp.overworld().getSharedSpawnPos(), 10000,
                            new java.io.File(dir, "fa_aaa_structure_map.png"));
                        log("structure sites in 20000 x 20000 blocks: " + (int) r[0] + ", closest two " + Math.round(r[1]) + " blocks apart");
                    }
                    cmd(mc, "gamemode creative @a"); cmd(mc, "time set 1000"); cmd(mc, "weather clear");
                    FAClientConfig.REALISM.set(false); FAClientConfig.LIGHTING.set(3);
                    cmd(mc, "fill -30 -61 -10 30 -50 120 air"); cmd(mc, "fill -30 -61 -10 30 -61 120 grass_block");
                    for (int z = 10; z <= 110; z += 14) { cmd(mc, "place feature minecraft:oak -8 -60 " + z); cmd(mc, "place feature minecraft:birch 9 -60 " + (z + 6)); }
                    cmd(mc, "fill -3 -61 20 3 -61 40 water");
                    cmd(mc, "tp @a 0 -58 0 0 4"); FAClientConfig.CINEMATIC.set(false); FAClientConfig.HAZE.set(false); wait = 160; }
                case 801 -> { shot(mc, "aaa_off"); FAClientConfig.CINEMATIC.set(true); FAClientConfig.HAZE.set(true); wait = 60; }
                case 802 -> { shot(mc, "aaa_on"); cmd(mc, "time set 23300"); cmd(mc, "tp @a 0 -58 0 -90 -12"); wait = 100; }
                case 803 -> { shot(mc, "aaa_sunrise_glare"); cmd(mc, "time set 18000"); cmd(mc, "tp @a 0 -58 0 0 4");
                    for (int z = 8; z <= 40; z += 8) { cmd(mc, "setblock -2 -60 " + z + " lantern"); cmd(mc, "setblock 2 -60 " + (z + 4) + " torch"); } wait = 100; }
                case 804 -> { shot(mc, "aaa_night_bloom"); cmd(mc, "time set 6000"); cmd(mc, "gamemode survival @a"); cmd(mc, "tp @a 0 -60 0 0 2");
                    cmd(mc, "item replace entity @a weapon.mainhand with factoryascension:m4a1");
                    cmd(mc, "summon zombie 0 -60 9 {NoAI:1b}"); wait = 60; }
                case 805 -> { dev.factoryascension.client.gun.GunAnim.devAim = true; wait = 40; }
                case 806 -> { shot(mc, "aaa_aim_dof"); dev.factoryascension.client.gun.GunAnim.devAim = false; wait = 10; }
                case 807 -> { FAClientConfig.REALISM.set(true); cmd(mc, "gamemode spectator @a"); step = 16; wait = 10; }
                case 208 -> { log("after resize: " + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()) + " " + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight());
                    shot(mc, "inv_scale2"); mc.options.guiScale().set(0); mc.resizeDisplay(); mc.setScreen(null); dev.factoryascension.client.tarkov.TarkovClient.debugTime = null;
                    cmd(mc, "gamemode spectator @a"); step = Boolean.getBoolean("fa.quick") ? 16 : 14; wait = 10; }
                case 14 -> {
                    FAClientConfig.LIGHTING.set(0);
                    wait = 10;
                }
                case 15 -> { log("after turning lighting off, targets held " + AscensionLighting.targetsHeld() + "; safe mode " + FAClientConfig.SAFE_MODE.get());
                    AscensionLighting.free(); log("freed: targets held " + AscensionLighting.targetsHeld()); wait = 10; }
                case 16 -> { log("done"); mc.stop(); }
                default -> { }
            }
        } catch (Throwable err) {
            log("step " + (step - 1) + " failed: " + err);
            LOG.error("[FA devshots] failed", err);
            mc.stop();
        }
    }
}
