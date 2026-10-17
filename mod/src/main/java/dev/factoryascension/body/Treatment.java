package dev.factoryascension.body;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/** The kit that keeps you going: splints, tourniquets, surgical kits, canteens, and the gun cleaning kit. */
public final class Treatment {
    private Treatment() {
    }

    /** Hold right-click for `ticks`, then `apply` (false = nothing to do: it isn't used up). Damageable items lose a use. */
    public static class UseItem extends Item {
        final int ticks;
        final UseAnim anim;
        final SoundEvent sound;
        final String what;
        final Predicate<ServerPlayer> apply;

        public UseItem(Properties p, int ticks, UseAnim anim, SoundEvent sound, String what, Predicate<ServerPlayer> apply) {
            super(p);
            this.ticks = ticks;
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
            return ticks;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return anim;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
            if (e instanceof ServerPlayer p && apply.test(p)) {
                level.playSound(null, p.blockPosition(), sound, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
                if (!p.isCreative()) {
                    if (stack.isDamageableItem()) stack.hurtAndBreak(1, p, x -> { });
                    else stack.shrink(1);
                }
            }
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal(what).withStyle(ChatFormatting.GRAY));
        }
    }

    static RegistryObject<Item> item(String id, int stack, int uses, int ticks, UseAnim anim, SoundEvent s, String what, Predicate<ServerPlayer> apply) {
        return FactoryAscension.ITEMS.register(id, () -> new UseItem(uses > 0 ? new Item.Properties().durability(uses) : new Item.Properties().stacksTo(stack),
            ticks, anim, s, what, apply));
    }

    public static final RegistryObject<Item> SPLINT = item("splint", 8, 0, 60, UseAnim.BOW, SoundEvents.ARMOR_EQUIP_LEATHER,
        "Hold right-click: sets a broken arm or leg", Body::splint);
    public static final RegistryObject<Item> TOURNIQUET = item("tourniquet", 8, 0, 30, UseAnim.BOW, SoundEvents.ARMOR_EQUIP_LEATHER,
        "Hold right-click: stops heavy bleeding", p -> {
            if (!p.hasEffect(Body.HEAVY_BLEED.get())) {
                p.displayClientMessage(Component.literal("You're not bleeding heavily"), true);
                return false;
            }
            p.removeEffect(Body.HEAVY_BLEED.get());
            Body.State s = Body.of(p);
            return true;
        });
    public static final RegistryObject<Item> SURGICAL_KIT = item("surgical_kit", 1, 3, 160, UseAnim.BOW, SoundEvents.ARMOR_EQUIP_CHAIN,
        "Hold right-click (8 s): brings a destroyed body part back (3 uses)", Body::surgery);
    public static final RegistryObject<Item> CANTEEN = item("canteen", 1, 0, 32, UseAnim.DRINK, SoundEvents.GENERIC_DRINK,
        "Hold right-click: drink (a quarter of your water). Right-click water with it to fill it", p -> {
            ItemStack c = p.getUseItem().isEmpty() ? p.getMainHandItem() : p.getUseItem();
            int sips = c.getOrCreateTag().getInt("Sips");
            if (sips <= 0) {
                p.displayClientMessage(Component.literal("The canteen's empty: right-click water to fill it"), true);
                return false;
            }
            c.getOrCreateTag().putInt("Sips", sips - 1);
            Body.drink(p, 25);
            return false;   // the canteen itself stays
        });
    public static final RegistryObject<Item> CLEANING_KIT = item("weapon_cleaning_kit", 1, 6, 80, UseAnim.BOW, SoundEvents.ARMOR_EQUIP_CHAIN,
        "Hold right-click with a gun in your other hand: cleans it (restores condition, 6 uses)", p -> {
            ItemStack gun = p.getOffhandItem();
            if (dev.factoryascension.gun.GunItem.spec(gun) == null) gun = p.getMainHandItem();
            if (dev.factoryascension.gun.GunItem.spec(gun) == null) {
                p.displayClientMessage(Component.literal("Hold the gun in your other hand"), true);
                return false;
            }
            var t = gun.getOrCreateTag();
            t.putFloat("Wear", Math.max(0, t.getFloat("Wear") - dev.factoryascension.FAConfig.cleaningKitRestore));
            t.putBoolean("Jam", false);
            p.displayClientMessage(Component.literal("Cleaned: condition " + Math.round(100 - t.getFloat("Wear")) + "%").withStyle(ChatFormatting.GREEN), true);
            return true;
        });

    /** Filling the canteen: right-click while looking at water. */
    static void fill(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        ItemStack st = event.getItemStack();
        if (st.getItem() != CANTEEN.get() || event.getLevel().isClientSide) return;
        var hit = event.getEntity().pick(5, 0, true);
        if (hit.getType() == HitResult.Type.BLOCK && event.getLevel().getFluidState(((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos()).is(Fluids.WATER)
            && st.getOrCreateTag().getInt("Sips") < 4) {
            st.getOrCreateTag().putInt("Sips", 4);
            event.getLevel().playSound(null, event.getEntity().blockPosition(), SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
            event.getEntity().displayClientMessage(Component.literal("Canteen filled (4 drinks)"), true);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Treatment::fill);
    }
}
