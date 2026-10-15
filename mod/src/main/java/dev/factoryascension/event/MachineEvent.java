package dev.factoryascension.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted on the Forge event bus when a machine needs the pack's rules (the scripts listen for it by class name):
 * "salvage" (the Salvager's timer), "preview" (the Fusion Table's slots changed) and "fuse" (its button, with the player).
 */
public class MachineEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final String kind;
    private final Player player;

    public MachineEvent(Level level, BlockPos pos, String kind, Player player) {
        this.level = level;
        this.pos = pos;
        this.kind = kind;
        this.player = player;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public String getKind() { return kind; }
    /** The player who pressed a button, or null. */
    public Player getPlayer() { return player; }
    public boolean hasPlayer() { return player != null; }
    public BlockEntity getBlockEntity() { return level.getBlockEntity(pos); }
}
