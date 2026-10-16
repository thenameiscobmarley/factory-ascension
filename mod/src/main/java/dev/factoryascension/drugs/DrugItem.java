package dev.factoryascension.drugs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import java.util.function.Consumer;

/** One drug: how long it takes to take, how it's taken (smoked, sniffed, swallowed, injected), and what it does. */
public class DrugItem extends Item {
    final int useTicks;
    final UseAnim anim;
    final SoundEvent sound;
    final Consumer<ServerPlayer> apply;
    final String what;

    public DrugItem(Properties props, int useTicks, UseAnim anim, SoundEvent sound, String what, Consumer<ServerPlayer> apply) {
        super(props);
        this.useTicks = useTicks;
        this.anim = anim;
        this.sound = sound;
        this.what = what;
        this.apply = apply;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return anim;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (!level.isClientSide && e instanceof ServerPlayer p) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
            var had = Drugs.before(p);
            apply.accept(p);
            Drugs.mixes(p, had);
            Drugs.dose(p);
            if (!p.isCreative()) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(what).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Taking several stimulants close together is an overdose: hurt, and psychosis").withStyle(ChatFormatting.DARK_RED));
    }
}
