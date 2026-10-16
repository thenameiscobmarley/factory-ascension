package dev.factoryascension.npc;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Bandage and medkit: hold right-click to use (you can walk, slowly, but not fight). A bandage stops bleeding and
 * heals a little; a medkit stops bleeding and poison and heals a lot over a few seconds. Both take time: getting caught
 * mid-patch-up is how it goes.
 */
public class MedItem extends Item {
    final boolean kit;

    public MedItem(Properties props, boolean kit) {
        super(props);
        this.kit = kit;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return kit ? 80 : 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int left) {
        if (!level.isClientSide && left % 12 == 0) level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (!level.isClientSide) {
            e.removeEffect(FactoryAscension.BLEEDING.get());
            if (kit) {
                e.removeEffect(MobEffects.POISON);
                e.removeEffect(MobEffects.WITHER);
                e.heal(6);
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            } else {
                e.heal(2);
            }
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 0.8f);
            if (e instanceof net.minecraft.server.level.ServerPlayer sp) dev.factoryascension.gun.Stress.add(sp, -0.15f);
            if (!(e instanceof Player p && p.isCreative())) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kit ? "Hold right-click (4s): stops bleeding and poison, heals 3 hearts and more over time"
            : "Hold right-click (2s): stops bleeding, heals a heart").withStyle(ChatFormatting.GRAY));
    }
}
