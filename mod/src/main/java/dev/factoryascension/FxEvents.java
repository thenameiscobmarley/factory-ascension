package dev.factoryascension;

import dev.factoryascension.network.FANetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Server side of the effects: tells players near a block break or an explosion what happened, so their game can show
 * voxel debris and smoke (client/render/FxClient). Only positions and block ids go over the network, a few hundred
 * bytes at most; each player's own settings decide what they see.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FxEvents {
    private FxEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void blockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        send(level, pos, new FANetwork.Fx(0, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, new int[] {Block.getId(event.getState())}, new long[] {pos.asLong()}));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Vec3 c = event.getExplosion().getPosition();
        List<BlockPos> blocks = event.getAffectedBlocks();
        int n = Math.min(blocks.size(), 48);   // a sample is plenty to read as "everything blew apart"
        int[] states = new int[n];
        long[] positions = new long[n];
        int step = Math.max(1, blocks.size() / Math.max(1, n));
        int k = 0;
        for (int i = 0; i < blocks.size() && k < n; i += step) {
            BlockState s = level.getBlockState(blocks.get(i));
            if (s.isAir()) continue;
            states[k] = Block.getId(s);
            positions[k++] = blocks.get(i).asLong();
        }
        float size = (float) Math.max(1.5, Math.cbrt(blocks.size() + 1) * 1.2);
        send(level, BlockPos.containing(c), new FANetwork.Fx(2, c.x, c.y, c.z, size, java.util.Arrays.copyOf(states, k), java.util.Arrays.copyOf(positions, k)));
    }

    /** Smoke somewhere (a gunshot, ...): `size` blocks across. */
    public static void smoke(ServerLevel level, Vec3 at, float size) {
        send(level, BlockPos.containing(at), new FANetwork.Fx(1, at.x, at.y, at.z, size, new int[0], new long[0]));
    }

    /** Chips off a block where a bullet hit. */
    public static void impact(ServerLevel level, Vec3 at, BlockPos pos, BlockState state) {
        send(level, pos, new FANetwork.Fx(3, at.x, at.y, at.z, 0, new int[] {Block.getId(state)}, new long[] {pos.asLong()}));
    }

    private static void send(ServerLevel level, BlockPos pos, FANetwork.Fx fx) {
        if (!level.hasChunkAt(pos)) return;
        FANetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), fx);
    }
}
