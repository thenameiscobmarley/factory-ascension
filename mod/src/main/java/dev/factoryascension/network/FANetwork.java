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

/** The mod's custom messages: the Ascension Vault's search box, and the shotgun's fire and reload. */
public final class FANetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(FactoryAscension.MODID, "main"),
        () -> VERSION, VERSION::equals, VERSION::equals);

    private FANetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, VaultSearch.class, VaultSearch::encode, VaultSearch::decode, VaultSearch::handle);
        CHANNEL.registerMessage(1, GunAction.class, GunAction::encode, GunAction::decode, GunAction::handle);
    }

    /** Client -> server: fire (aimed or from the hip) or reload the gun in the main hand. */
    public record GunAction(int action, boolean aiming) {
        public static final int FIRE = 0, RELOAD = 1;

        static void encode(GunAction msg, FriendlyByteBuf buf) {
            buf.writeByte(msg.action);
            buf.writeBoolean(msg.aiming);
        }

        static GunAction decode(FriendlyByteBuf buf) {
            return new GunAction(buf.readByte(), buf.readBoolean());
        }

        static void handle(GunAction msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null || player.isSpectator() || !player.isAlive()) return;
                if (msg.action == FIRE) dev.factoryascension.item.ShotgunItem.fire(player, msg.aiming);
                else if (msg.action == RELOAD) dev.factoryascension.item.ShotgunItem.startReload(player, player.getMainHandItem());
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
