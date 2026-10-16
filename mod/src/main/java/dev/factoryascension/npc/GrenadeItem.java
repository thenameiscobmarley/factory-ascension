package dev.factoryascension.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A frag grenade: hold right-click to pull the pin and cook it (don't hold too long), let go to throw. */
public class GrenadeItem extends Item {
    static final int FUSE = 55;

    public GrenadeItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 1.8f);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity e, int left) {
        int held = getUseDuration(stack) - left;
        if (level.isClientSide) return;
        Grenade g = new Grenade(level, e);
        g.setFuse(FUSE - held);
        g.shootFromRotation(e, e.getXRot(), e.getYRot(), 0, 1.1f, 1);
        level.addFreshEntity(g);
        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f, 0.5f);
        if (!(e instanceof Player p && p.isCreative())) stack.shrink(1);
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int left) {
        int held = getUseDuration(stack) - left;
        if (!level.isClientSide && held >= FUSE) {
            // Held it too long
            e.releaseUsingItem();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Hold right-click to pull the pin (it's cooking), let go to throw. Bounces; goes off after about 3 seconds")
            .withStyle(ChatFormatting.GRAY));
    }
}
