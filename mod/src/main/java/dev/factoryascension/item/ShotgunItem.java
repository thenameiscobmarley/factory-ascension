package dev.factoryascension.item;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pump shotgun. Hold right-click to aim down the sights (zoom, tighter spread, slower walk), left-click to fire (holding
 * it keeps firing as fast as the pump allows), R to reload one shell at a time from your inventory. Pellets are your own
 * attack, so the pack's enchants on it (tomes work on it like any weapon) and your gear count. Glass shatters.
 * Numbers: FAConfig.gun* (the pack's config section `guns`). Controls are client side (client/GunClient).
 */
public class ShotgunItem extends Item {
    public ShotgunItem(Properties props) {
        super(props);
    }

    // ---- Shells and state, in the item's NBT ----
    public static int shells(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Shells") : 0;
    }

    private static void setShells(ItemStack stack, int n) {
        stack.getOrCreateTag().putInt("Shells", Math.max(0, Math.min(FAConfig.gunCapacity, n)));
    }

    private static boolean reloading(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getInt("Reload") > 0;
    }

    private static boolean isShell(ItemStack s) {
        return s.is(FactoryAscension.SHOTGUN_SHELL.get());
    }

    // ---- Aiming: right-click and hold ----
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;   // the pose comes from client/GunClient (both hands up, sights to the eye)
    }

    public static boolean aiming(LivingEntity e) {
        return e.isUsingItem() && e.getUseItem().getItem() instanceof ShotgunItem;
    }

    // Never mines or swings
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    // ---- Firing (server; the client sends a message on left-click) ----
    public static void fire(ServerPlayer player, boolean aimingNow) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ShotgunItem) || player.getCooldowns().isOnCooldown(stack.getItem())) return;
        ServerLevel level = player.serverLevel();
        if (shells(stack) <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7f, 1.6f);
            startReload(player, stack);
            return;
        }
        stack.getOrCreateTag().putInt("Reload", 0);                 // firing stops a reload
        if (!player.isCreative()) setShells(stack, shells(stack) - 1);
        player.getCooldowns().addCooldown(stack.getItem(), FAConfig.gunCooldown);
        stack.getOrCreateTag().putInt("Pump", 7);                   // the pump racks a moment after the shot

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        boolean aim = aimingNow && aiming(player);
        double spread = Math.toRadians(aim ? FAConfig.gunSpreadAim : FAConfig.gunSpreadHip);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        RandomSource r = player.getRandom();
        Map<Entity, Float> hits = new HashMap<>();
        double range = FAConfig.gunRange;
        int impacts = 0;
        for (int i = 0; i < FAConfig.gunPellets; i++) {
            double a = spread * Math.sqrt(r.nextDouble()), b = r.nextDouble() * Math.PI * 2;
            Vec3 dir = look.add(right.scale(Math.tan(a) * Math.cos(b))).add(up.scale(Math.tan(a) * Math.sin(b))).normalize();
            Vec3 end = eye.add(dir.scale(range));
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            EntityHitResult ent = ProjectileUtil.getEntityHitResult(player, eye, stop, new AABB(eye, stop).inflate(1),
                e -> e instanceof LivingEntity && e != player && e.isAlive() && !e.isSpectator() && e.isPickable(), eye.distanceToSqr(stop));
            if (ent != null) stop = ent.getLocation();
            // Tracer, then the impact
            double len = eye.distanceTo(stop);
            for (double d = 1.5; d < len; d += 2.5) {
                Vec3 p = eye.add(dir.scale(d));
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (ent != null) {
                double dist = eye.distanceTo(stop);
                float falloff = (float) Mth.clampedLerp(1.0, FAConfig.gunFalloff, (dist - FAConfig.gunFullDamageRange) / Math.max(1, range - FAConfig.gunFullDamageRange));
                hits.merge(ent.getEntity(), FAConfig.gunPelletDamage * falloff, Float::sum);
            } else if (block.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = block.getBlockPos();
                BlockState state = level.getBlockState(pos);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), stop.x, stop.y, stop.z, 4, 0.05, 0.05, 0.05, 0.1);
                level.sendParticles(ParticleTypes.SMOKE, stop.x, stop.y, stop.z, 1, 0, 0, 0, 0.01);
                if (impacts++ < 3) dev.factoryascension.FxEvents.impact(level, stop, pos, state);
                if (FAConfig.gunBreaksGlass && (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES))) level.destroyBlock(pos, false, player);
            }
        }
        // All of a target's pellets land as one hit (otherwise hurt immunity would eat all but the first)
        lastDamage = 0;
        lastLanded = false;
        hits.forEach((e, dmg) -> {
            e.invulnerableTime = 0;
            lastDamage += dmg;
            lastLanded |= e.hurt(player.damageSources().playerAttack(player), dmg);
            if (e instanceof LivingEntity le) le.knockback(Math.min(1.2, dmg / (FAConfig.gunPelletDamage * FAConfig.gunPellets) * 1.2), -look.x, -look.z);
        });
        // Muzzle flash and the bang
        Vec3 muzzle = eye.add(look.scale(1.4)).add(right.scale(aim ? 0 : 0.3)).add(up.scale(aim ? -0.1 : -0.25));
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 6, 0.05, 0.05, 0.05, 0.03);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
        level.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
        dev.factoryascension.FxEvents.smoke(level, muzzle, 0.45f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.7f + r.nextFloat() * 0.1f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2f, 0.55f);
        // A kick back when firing from the hip
        if (!aim) player.push(-look.x * FAConfig.gunKnockback, 0.05, -look.z * FAConfig.gunKnockback);
        player.hurtMarked = true;
        lastHits = hits.size();
    }

    /** How many targets the last shot hit (for the self-test). */
    public static int lastHits = 0;
    public static float lastDamage = 0;
    public static boolean lastLanded = false;

    // ---- Reloading: R (or firing empty) loads one shell every gunReloadTicks while the gun is held ----
    public static void startReload(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ShotgunItem) || shells(stack) >= FAConfig.gunCapacity || reloading(stack)) return;
        if (!player.isCreative() && player.getInventory().items.stream().noneMatch(ShotgunItem::isShell)) {
            player.displayClientMessage(Component.literal("No Shotgun Shells").withStyle(ChatFormatting.RED), true);
            return;
        }
        stack.getOrCreateTag().putInt("Reload", FAConfig.gunReloadTicks);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || !stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        int pump = tag.getInt("Pump");
        if (pump > 0) {
            tag.putInt("Pump", pump - 1);
            if (pump == 1) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 1.9f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        }
        int reload = tag.getInt("Reload");
        if (reload <= 0) return;
        if (!selected) { tag.putInt("Reload", 0); return; }
        if (--reload > 0) { tag.putInt("Reload", reload); return; }
        loadOne(player, stack);
    }

    /** Load one shell (from the inventory, or free in creative); keep reloading until full or out. */
    public static boolean loadOne(ServerPlayer player, ItemStack stack) {
        boolean took = player.isCreative();
        if (!took) for (ItemStack s : player.getInventory().items) {
            if (isShell(s)) { s.shrink(1); took = true; break; }
        }
        if (!took) { stack.getOrCreateTag().putInt("Reload", 0); return false; }
        setShells(stack, shells(stack) + 1);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.7f, 1.3f);
        boolean more = shells(stack) < FAConfig.gunCapacity && (player.isCreative() || player.getInventory().items.stream().anyMatch(ShotgunItem::isShell));
        stack.getOrCreateTag().putInt("Reload", more ? FAConfig.gunReloadTicks : 0);
        if (!more) player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 1.9f);
        return true;
    }

    // ---- Shells shown as the item's bar, and in the tooltip ----
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * shells(stack) / Math.max(1, FAConfig.gunCapacity));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return shells(stack) > 0 ? 0xE8B03A : 0x8C2A1E;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();   // shells and reload ticks change the NBT constantly
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Shells: " + shells(stack) + " / " + FAConfig.gunCapacity).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal(FAConfig.gunPellets + " pellets x " + FAConfig.gunPelletDamage + " damage, up to " + (int) FAConfig.gunRange + " blocks").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Hold right-click: aim down the sights. Left-click: fire. R: reload.").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Takes Ascended tomes like any weapon.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(dev.factoryascension.client.GunClient.EXTENSIONS);
    }
}
