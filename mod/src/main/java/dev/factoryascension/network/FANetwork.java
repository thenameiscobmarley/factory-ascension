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
        CHANNEL.registerMessage(6, BenchAction.class, BenchAction::encode, BenchAction::decode, BenchAction::handle);
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
