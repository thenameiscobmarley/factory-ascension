package dev.factoryascension.item;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.blockentity.TerminalBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * Storage Linker: right-click a Universal Storage Terminal to bind to it, then right-click chests to link or unlink
 * them. Sneak + right-click the air to unbind. While held it outlines the terminal (gold) and its chests (teal).
 */
public class StorageLinkerItem extends Item {
    private static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(0.25f, 0.9f, 0.85f), 1.2f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.8f, 0.25f), 1.6f);

    public StorageLinkerItem(Properties props) {
        super(props);
    }

    private static BlockPos bound(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Terminal") || !tag.getString("Dim").equals(level.dimension().location().toString())) return null;
        return BlockPos.of(tag.getLong("Terminal"));
    }

    private static void say(Player player, Component msg) {
        player.displayClientMessage(msg, true);
    }

    // Runs before the block's own right-click, so clicking a chest links it instead of opening it
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TerminalBlockEntity t) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("Terminal", pos.asLong());
            tag.putString("Dim", level.dimension().location().toString());
            say(player, Component.translatable("gui.factoryascension.linker_bound", t.linkCount()).withStyle(ChatFormatting.GOLD));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
            return InteractionResult.SUCCESS;
        }
        BlockPos tpos = bound(stack, level);
        if (tpos == null) {
            if (TerminalBlockEntity.linkable(level, pos) == null) return InteractionResult.PASS;
            say(player, Component.translatable("gui.factoryascension.linker_unbound").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!(level.isLoaded(tpos) && level.getBlockEntity(tpos) instanceof TerminalBlockEntity terminal)) {
            if (TerminalBlockEntity.linkable(level, pos) == null) return InteractionResult.PASS;
            say(player, Component.translatable("gui.factoryascension.linker_lost").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        TerminalBlockEntity.LinkResult r = terminal.toggle(pos);
        if (r == TerminalBlockEntity.LinkResult.NOT_STORAGE) return InteractionResult.PASS;   // not a chest: use the block normally
        String key = switch (r) {
            case LINKED -> "gui.factoryascension.linker_linked";
            case UNLINKED -> "gui.factoryascension.linker_unlinked";
            case TOO_FAR -> "gui.factoryascension.linker_too_far";
            default -> "gui.factoryascension.linker_full";
        };
        boolean good = r == TerminalBlockEntity.LinkResult.LINKED || r == TerminalBlockEntity.LinkResult.UNLINKED;
        say(player, Component.translatable(key, terminal.linkCount(), dev.factoryascension.FAConfig.storageMaxLinks, dev.factoryascension.FAConfig.storageRange)
            .withStyle(good ? ChatFormatting.AQUA : ChatFormatting.RED));
        level.playSound(null, pos, r == TerminalBlockEntity.LinkResult.LINKED ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_BREAK,
            SoundSource.PLAYERS, 1f, r == TerminalBlockEntity.LinkResult.LINKED ? 1.6f : 0.8f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.hasTag() && stack.getTag().contains("Terminal")) {
            if (!level.isClientSide) {
                stack.removeTagKey("Terminal");
                stack.removeTagKey("Dim");
                say(player, Component.translatable("gui.factoryascension.linker_cleared").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    // Held: outline the terminal and every linked chest, for this player only
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || level.getGameTime() % 10 != 0) return;
        if (!selected && player.getOffhandItem() != stack) return;
        BlockPos tpos = bound(stack, level);
        if (tpos == null || !level.isLoaded(tpos) || !(level.getBlockEntity(tpos) instanceof TerminalBlockEntity t)) return;
        ServerLevel sl = (ServerLevel) level;
        sl.sendParticles(player, GOLD, true, tpos.getX() + 0.5, tpos.getY() + 1.1, tpos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0);
        int shown = 0;
        for (BlockPos p : t.linkList()) {
            if (shown++ >= 96) break;
            if (p.distSqr(player.blockPosition()) > 64 * 64) continue;
            sl.sendParticles(player, TEAL, true, p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Terminal");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Terminal")) {
            BlockPos p = BlockPos.of(tag.getLong("Terminal"));
            tip.add(Component.translatable("gui.factoryascension.linker_tip_bound", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.AQUA));
        } else {
            tip.add(Component.translatable("gui.factoryascension.linker_tip_unbound").withStyle(ChatFormatting.GRAY));
        }
    }

}
