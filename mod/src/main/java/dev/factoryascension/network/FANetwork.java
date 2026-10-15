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

/** The mod's one custom message: the Ascension Vault's search box (sorting and scrolling use vanilla menu buttons). */
public final class FANetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(FactoryAscension.MODID, "main"),
        () -> VERSION, VERSION::equals, VERSION::equals);

    private FANetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, VaultSearch.class, VaultSearch::encode, VaultSearch::decode, VaultSearch::handle);
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
