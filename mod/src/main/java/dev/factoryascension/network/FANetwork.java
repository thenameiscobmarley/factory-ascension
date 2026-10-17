package dev.factoryascension.network;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.menu.VaultMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** The mod's custom messages: the Ascension Vault's search box, the guns' fire and reload, effects, and the Tarkov inventory grid. */
public final class FANetwork {
    private static final String VERSION = "8";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(FactoryAscension.MODID, "main"),
        () -> VERSION, VERSION::equals, VERSION::equals);

    private FANetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, VaultSearch.class, VaultSearch::encode, VaultSearch::decode, VaultSearch::handle);
        CHANNEL.registerMessage(1, GunAction.class, GunAction::encode, GunAction::decode, GunAction::handle);
        CHANNEL.registerMessage(2, Fx.class, Fx::encode, Fx::decode, Fx::handle);
        CHANNEL.registerMessage(3, TarkovSync.class, TarkovSync::encode, TarkovSync::decode, TarkovSync::handle);
        CHANNEL.registerMessage(4, TarkovPlan.class, TarkovPlan::encode, TarkovPlan::decode, TarkovPlan::handle);
        CHANNEL.registerMessage(5, StressSync.class, StressSync::encode, StressSync::decode, StressSync::handle);
        CHANNEL.registerMessage(7, NodsToggle.class, NodsToggle::encode, NodsToggle::decode, NodsToggle::handle);
        CHANNEL.registerMessage(6, BenchAction.class, BenchAction::encode, BenchAction::decode, BenchAction::handle);
        CHANNEL.registerMessage(8, BodySync.class, BodySync::encode, BodySync::decode, BodySync::handle);
        CHANNEL.registerMessage(9, Suppress.class, Suppress::encode, Suppress::decode, Suppress::handle);
        CHANNEL.registerMessage(10, FogSync.class, FogSync::encode, FogSync::decode, FogSync::handle);
        CHANNEL.registerMessage(11, RaidSync.class, RaidSync::encode, RaidSync::decode, RaidSync::handle);
        CHANNEL.registerMessage(12, TraderView.class, TraderView::encode, TraderView::decode, TraderView::handle);
        CHANNEL.registerMessage(13, TraderAction.class, TraderAction::encode, TraderAction::decode, TraderAction::handle);
        CHANNEL.registerMessage(14, VehicleAction.class, VehicleAction::encode, VehicleAction::decode, VehicleAction::handle);
        CHANNEL.registerMessage(15, TurretFire.class, TurretFire::encode, TurretFire::decode, TurretFire::handle);
        CHANNEL.registerMessage(16, ChemMix.class, ChemMix::encode, ChemMix::decode, ChemMix::handle);
        CHANNEL.registerMessage(17, MachineInfo.class, MachineInfo::encode, MachineInfo::decode, MachineInfo::handle);
        CHANNEL.registerMessage(18, MachineAsk.class, MachineAsk::encode, MachineAsk::decode, MachineAsk::handle);
    }

    /** Client -> server, at the Weapons Workbench: 0 install an attachment, 1 take one off (by index), 2 press rounds,
     *  3 build a gun (att = its id), 4 put a skin on the gun (att = the skin, "" takes it off). */
    public record BenchAction(int kind, String att, int index, int caliber, int type, int colour) {
        public static final int INSTALL = 0, REMOVE = 1, PRESS = 2, BUILD = 3, SKIN = 4;

        static void encode(BenchAction m, FriendlyByteBuf b) {
            b.writeByte(m.kind);
            b.writeUtf(m.att, 128);
            b.writeVarInt(m.index);
            b.writeVarInt(m.caliber);
            b.writeVarInt(m.type);
            b.writeInt(m.colour);
        }

        static BenchAction decode(FriendlyByteBuf b) {
            return new BenchAction(b.readByte(), b.readUtf(128), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readInt());
        }

        static void handle(BenchAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null || !(p.containerMenu instanceof dev.factoryascension.gun.WeaponsBenchMenu bench) || !bench.stillValid(p)) return;
                switch (m.kind) {
                    case INSTALL -> dev.factoryascension.gun.Bench.install(p, bench.gun(), m.att);
                    case REMOVE -> dev.factoryascension.gun.Bench.remove(p, bench.gun(), m.index);
                    case BUILD -> dev.factoryascension.gun.Bench.build(p, m.att);
                    case SKIN -> dev.factoryascension.gun.Bench.skin(p, bench.gun(), m.att);
                    case PRESS -> {
                        var cals = dev.factoryascension.gun.Caliber.values();
                        var types = dev.factoryascension.gun.AmmoType.values();
                        if (m.caliber < 0 || m.caliber >= cals.length || m.type < 0 || m.type >= types.length) return;
                        dev.factoryascension.gun.Bench.press(p, bench.bench.getItem(dev.factoryascension.gun.WeaponsBenchMenu.CASE),
                            bench.bench.getItem(dev.factoryascension.gun.WeaponsBenchMenu.POWDER), cals[m.caliber], types[m.type], m.colour);
                    }
                    default -> { }
                }
                bench.broadcastChanges();
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: how stressed you are (0..1), for shaking hands and the stats row, and the danger level where you are. */
    public record StressSync(float stress, int danger) {
        static void encode(StressSync msg, FriendlyByteBuf buf) {
            buf.writeFloat(msg.stress);
            buf.writeVarInt(msg.danger);
        }

        static StressSync decode(FriendlyByteBuf buf) {
            return new StressSync(buf.readFloat(), buf.readVarInt());
        }

        static void handle(StressSync msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> { dev.factoryascension.client.gun.GunAnim.stress = msg.stress; dev.factoryascension.client.Dread.danger = msg.danger; }));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: your body (each part's health, which are broken, your water): body/Body. */
    public record BodySync(float[] v) {
        static void encode(BodySync msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.v.length);
            for (float f : msg.v) buf.writeFloat(f);
        }

        static BodySync decode(FriendlyByteBuf buf) {
            float[] v = new float[Math.min(64, buf.readVarInt())];
            for (int i = 0; i < v.length; i++) v[i] = buf.readFloat();
            return new BodySync(v);
        }

        static void handle(BodySync msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.BodyHud.state = msg.v));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: the raid you're on (raid/Extraction): extraction points, ticks left, seconds held; or how it ended. */
    public record RaidSync(int[] xz, String[] names, int left, int hold, String ended) {
        static void encode(RaidSync m, FriendlyByteBuf b) {
            b.writeVarIntArray(m.xz);
            b.writeVarInt(m.names.length);
            for (String n : m.names) b.writeUtf(n);
            b.writeVarInt(m.left);
            b.writeVarInt(m.hold);
            b.writeUtf(m.ended);
        }

        static RaidSync decode(FriendlyByteBuf b) {
            int[] xz = b.readVarIntArray(16);
            String[] n = new String[Math.min(8, b.readVarInt())];
            for (int i = 0; i < n.length; i++) n[i] = b.readUtf(64);
            return new RaidSync(xz, n, b.readVarInt(), b.readVarInt(), b.readUtf(16));
        }

        static void handle(RaidSync m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.RaidHud.update(m.xz, m.names, m.left, m.hold, m.ended)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: open a trader (raid/Traders): everything the screen shows, as one compound. */
    public record TraderView(net.minecraft.nbt.CompoundTag data) {
        static void encode(TraderView m, FriendlyByteBuf b) {
            b.writeNbt(m.data);
        }

        static TraderView decode(FriendlyByteBuf b) {
            return new TraderView(b.readNbt());
        }

        static void handle(TraderView m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.TraderScreen.show(m.data)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: at a trader: buy offer `index`, sell inventory slot `index`, take or hand in task `index`. */
    public record TraderAction(int trader, int kind, int index) {
        public static final int BUY = 0, SELL = 1, TAKE = 2, HAND_IN = 3;

        static void encode(TraderAction m, FriendlyByteBuf b) {
            b.writeVarInt(m.trader);
            b.writeVarInt(m.kind);
            b.writeVarInt(m.index);
        }

        static TraderAction decode(FriendlyByteBuf b) {
            return new TraderAction(b.readVarInt(), b.readVarInt(), b.readVarInt());
        }

        static void handle(TraderAction m, Supplier<NetworkEvent.Context> ctx) {
            var p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) dev.factoryascension.raid.Traders.action(p, m.trader, m.kind, m.index); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: in the vehicle workshop, fit the part from inventory slot `slot` to vehicle `vehicle`. */
    public record VehicleAction(int vehicle, int slot) {
        static void encode(VehicleAction m, FriendlyByteBuf b) {
            b.writeVarInt(m.vehicle);
            b.writeVarInt(m.slot);
        }

        static VehicleAction decode(FriendlyByteBuf b) {
            return new VehicleAction(b.readVarInt(), b.readVarInt());
        }

        static void handle(VehicleAction m, Supplier<NetworkEvent.Context> ctx) {
            var p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) dev.factoryascension.vehicle.Workshop.fit(p, m.vehicle, m.slot); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: at a Chem Lab, make recipe `index` (`times` over). */
    public record ChemMix(net.minecraft.core.BlockPos pos, int index, int times) {
        static void encode(ChemMix m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeVarInt(m.index);
            b.writeVarInt(m.times);
        }

        static ChemMix decode(FriendlyByteBuf b) {
            return new ChemMix(b.readBlockPos(), b.readVarInt(), b.readVarInt());
        }

        static void handle(ChemMix m, Supplier<NetworkEvent.Context> ctx) {
            var p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) dev.factoryascension.drugs.DrugBlocks.ChemLab.mix(p, m.pos, m.index, m.times); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: a machine's panel (client/MachineScreen): its name, rows {label, value, mood}. */
    public record MachineInfo(net.minecraft.core.BlockPos pos, String title, String[] rows) {
        public static void send(net.minecraft.server.level.ServerPlayer p, net.minecraft.core.BlockPos pos) {
            if (p.distanceToSqr(pos.getCenter()) > 100) return;
            if (!(p.level().getBlockEntity(pos) instanceof dev.factoryascension.machine.KineticMachineBlock.Status st)) return;
            java.util.List<String> flat = new java.util.ArrayList<>();
            var info = st.info();
            if (info.isEmpty()) info = java.util.List.<String[]>of(new String[] {"Status", st.status().getString(), ""});
            for (String[] r : info) for (int i = 0; i < 3; i++) flat.add(i < r.length ? r[i] : "");
            CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), new MachineInfo(pos, p.level().getBlockState(pos).getBlock().getName().getString(), flat.toArray(new String[0])));
        }

        static void encode(MachineInfo m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeUtf(m.title);
            b.writeVarInt(m.rows.length);
            for (String r : m.rows) b.writeUtf(r);
        }

        static MachineInfo decode(FriendlyByteBuf b) {
            var pos = b.readBlockPos();
            String t = b.readUtf();
            String[] r = new String[Math.min(96, b.readVarInt())];
            for (int i = 0; i < r.length; i++) r[i] = b.readUtf();
            return new MachineInfo(pos, t, r);
        }

        static void handle(MachineInfo m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.MachineScreen.show(m.pos, m.title, m.rows)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: refresh the machine panel that's open. */
    public record MachineAsk(net.minecraft.core.BlockPos pos) {
        static void encode(MachineAsk m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
        }

        static MachineAsk decode(FriendlyByteBuf b) {
            return new MachineAsk(b.readBlockPos());
        }

        static void handle(MachineAsk m, Supplier<NetworkEvent.Context> ctx) {
            var p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) MachineInfo.send(p, m.pos); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: the truck's gunner is holding the trigger. */
    public record TurretFire() {
        static void encode(TurretFire m, FriendlyByteBuf b) {
        }

        static TurretFire decode(FriendlyByteBuf b) {
            return new TurretFire();
        }

        static void handle(TurretFire m, Supplier<NetworkEvent.Context> ctx) {
            var p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null && p.getVehicle() instanceof dev.factoryascension.vehicle.Car c) c.fire(p); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: how thick the fog is (world/Weather). */
    public record FogSync(float fog) {
        static void encode(FogSync msg, FriendlyByteBuf buf) {
            buf.writeFloat(msg.fog);
        }

        static FogSync decode(FriendlyByteBuf buf) {
            return new FogSync(buf.readFloat());
        }

        static void handle(FogSync msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> dev.factoryascension.world.Weather.fog = msg.fog);
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: a round just went past you (how close: 0..1): the view flinches and blurs. */
    public record Suppress(float amount) {
        static void encode(Suppress msg, FriendlyByteBuf buf) {
            buf.writeFloat(msg.amount);
        }

        static Suppress decode(FriendlyByteBuf buf) {
            return new Suppress(buf.readFloat());
        }

        static void handle(Suppress msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.Suppression.hit(msg.amount)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: where each main-inventory slot's item sits in the Tarkov grid, and the grid's size. */
    public record TarkovSync(boolean enabled, int width, int height, int[] pos) {
        static void encode(TarkovSync msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.enabled);
            buf.writeByte(msg.width);
            buf.writeByte(msg.height);
            buf.writeVarIntArray(msg.pos);
        }

        static TarkovSync decode(FriendlyByteBuf buf) {
            return new TarkovSync(buf.readBoolean(), buf.readByte(), buf.readByte(), buf.readVarIntArray(64));
        }

        static void handle(TarkovSync msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.tarkov.TarkovClient.sync(msg.enabled, msg.width, msg.height, msg.pos)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: put a main-inventory slot's item (or the next item into that empty slot) at x, y in the grid. */
    public record TarkovPlan(int slot, int x, int y, boolean turned) {
        static void encode(TarkovPlan msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.slot);
            buf.writeByte(msg.x);
            buf.writeByte(msg.y);
            buf.writeBoolean(msg.turned);
        }

        static TarkovPlan decode(FriendlyByteBuf buf) {
            return new TarkovPlan(buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean());
        }

        static void handle(TarkovPlan msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) dev.factoryascension.tarkov.TarkovServer.plan(player, msg.slot, msg.x, msg.y, msg.turned);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server -> client: something to show (FxEvents): 0 a broken block, 1 smoke, 2 an explosion, 3 a bullet impact. */
    public record Fx(int kind, double x, double y, double z, float size, int[] states, long[] positions) {
        static void encode(Fx msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.kind);
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeFloat(msg.size);
            buf.writeVarIntArray(msg.states);
            buf.writeLongArray(msg.positions);
        }

        static Fx decode(FriendlyByteBuf buf) {
            return new Fx(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readVarIntArray(64), buf.readLongArray(null, 64));
        }

        static void handle(Fx msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.factoryascension.client.render.FxClient.handle(msg.kind, msg.x, msg.y, msg.z, msg.size, msg.states, msg.positions)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: fire (aimed or from the hip) or reload the gun in the main hand. */
    /** lean: -1 (left) .. 1 (right); dyaw / dpitch: where the gun points off the crosshair at the hip, degrees. */
    public record GunAction(int action, boolean aiming, float lean, float dyaw, float dpitch) {
        public static final int FIRE = 0, RELOAD = 1;

        public GunAction(int action, boolean aiming) {
            this(action, aiming, 0, 0, 0);
        }

        static void encode(GunAction msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.action);
            buf.writeBoolean(msg.aiming);
            buf.writeFloat(msg.lean);
            buf.writeFloat(msg.dyaw);
            buf.writeFloat(msg.dpitch);
        }

        static GunAction decode(FriendlyByteBuf buf) {
            return new GunAction(buf.readByte(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        static void handle(GunAction msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || player.isSpectator() || !player.isAlive()) return;
                if (player.getMainHandItem().getItem() instanceof dev.factoryascension.gun.GunItem) {
                    if (msg.action == FIRE) {
                        dev.factoryascension.gun.GunItem.shotLean = Math.max(-1, Math.min(1, msg.lean));
                        dev.factoryascension.gun.GunItem.shotYaw = Math.max(-10, Math.min(10, msg.dyaw));
                        dev.factoryascension.gun.GunItem.shotPitch = Math.max(-10, Math.min(10, msg.dpitch));
                        try {
                            dev.factoryascension.gun.GunItem.fire(player, msg.aiming);
                        } finally {
                            dev.factoryascension.gun.GunItem.shotLean = dev.factoryascension.gun.GunItem.shotYaw = dev.factoryascension.gun.GunItem.shotPitch = 0;
                        }
                    }
                    else if (msg.action == RELOAD) dev.factoryascension.gun.GunItem.startReload(player, player.getMainHandItem());
                    return;
                }
                boolean flare = player.getMainHandItem().getItem() instanceof dev.factoryascension.item.FlareGunItem;
                if (msg.action == FIRE) {
                    if (flare) dev.factoryascension.item.FlareGunItem.fire(player);
                    else dev.factoryascension.item.ShotgunItem.fire(player, msg.aiming);
                } else if (msg.action == RELOAD) {
                    if (flare) dev.factoryascension.item.FlareGunItem.reload(player, player.getMainHandItem());
                    else dev.factoryascension.item.ShotgunItem.startReload(player, player.getMainHandItem());
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: flip the worn night vision goggles down / up. */
    public record NodsToggle() {
        static void encode(NodsToggle m, FriendlyByteBuf b) {
        }

        static NodsToggle decode(FriendlyByteBuf b) {
            return new NodsToggle();
        }

        static void handle(NodsToggle m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                var head = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                if (!(head.getItem() instanceof dev.factoryascension.armor.NodsItem)) return;
                boolean down = !dev.factoryascension.armor.NodsItem.down(head);
                dev.factoryascension.armor.NodsItem.setDown(head, down);
                if (!down) p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                p.level().playSound(null, p.blockPosition(), down ? net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE : net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.8f);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> server: new search text for the open vault. */
    public record VaultSearch(int containerId, String text) {
        static void encode(VaultSearch msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.containerId);
            buf.writeUtf(msg.text, 64);
        }

        static VaultSearch decode(FriendlyByteBuf buf) {
            return new VaultSearch(buf.readVarInt(), buf.readUtf(64));
        }

        static void handle(VaultSearch msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.containerMenu instanceof VaultMenu menu && menu.containerId == msg.containerId) menu.setFilter(msg.text);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
