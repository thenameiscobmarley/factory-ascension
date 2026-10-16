package dev.factoryascension.gun;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/** What the Weapons Workbench does: put attachments on and take them off, and press rounds. Server side. */
public final class Bench {
    private Bench() {
    }

    public static boolean juryRigged(ItemStack gun) {
        return EnchantmentHelper.getTagEnchantmentLevel(FactoryAscension.JURY_RIGGED.get(), gun) > 0;
    }

    /** Can this attachment go on this gun now? null = yes, else why not. */
    public static String canInstall(ItemStack gun, Attachments.Att a) {
        GunSpec spec = GunItem.spec(gun);
        if (spec == null) return "Put a gun on the bench";
        List<String> ids = GunStats.ids(gun);
        int inSlot = 0;
        for (String id : ids) {
            Attachments.Att o = Attachments.BY_ID.get(id);
            if (o != null && o.slot == a.slot) inSlot++;
        }
        if (juryRigged(gun)) return inSlot >= JuryRiggedEnchantment.MAX_PER_SLOT ? "That slot is full (" + JuryRiggedEnchantment.MAX_PER_SLOT + " at most)" : null;
        if (!a.fits(spec)) return "Doesn't fit a " + spec.kind.name().toLowerCase() + " (a Jury-Rigged gun takes anything)";
        return null;   // one per slot: a new one replaces the old (it comes back to you)
    }

    /** The cost: {part item count, tier item count}. */
    public static int[] cost(Attachments.Att a) {
        return new int[] {a.cost, Math.max(0, a.tier - 1)};
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
    }

    static int count(ServerPlayer p, Item it) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    static void take(ServerPlayer p, Item it, int n) {
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) return;
            if (!s.is(it)) continue;
            int k = Math.min(n, s.getCount());
            s.shrink(k);
            n -= k;
        }
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Install: uses a matching attachment item you carry, else makes it from parts. */
    public static void install(ServerPlayer p, ItemStack gun, String attId) {
        Attachments.Att a = Attachments.BY_ID.get(attId);
        if (a == null || gun.isEmpty()) return;
        String why = canInstall(gun, a);
        if (why != null) { p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true); return; }
        boolean paid = p.isCreative();
        if (!paid) {
            for (ItemStack s : p.getInventory().items) {
                if (s.getItem() instanceof AttachmentItem && attId.equals(s.getTag() == null ? "" : s.getTag().getString("Att"))) { s.shrink(1); paid = true; break; }
            }
        }
        if (!paid) {
            int[] c = cost(a);
            Item part = item(FAConfig.benchPartItem), tier = item(FAConfig.benchTierItem);
            if (count(p, part) < c[0] || (c[1] > 0 && count(p, tier) < c[1])) {
                p.displayClientMessage(Component.literal("Needs " + c[0] + " " + new ItemStack(part).getHoverName().getString()
                    + (c[1] > 0 ? " and " + c[1] + " " + new ItemStack(tier).getHoverName().getString() : "")).withStyle(ChatFormatting.RED), true);
                return;
            }
            take(p, part, c[0]);
            if (c[1] > 0) take(p, tier, c[1]);
        }
        List<String> ids = GunStats.ids(gun);
        if (!juryRigged(gun)) {
            // One per slot: the old one comes off
            for (int i = ids.size() - 1; i >= 0; i--) {
                Attachments.Att o = Attachments.BY_ID.get(ids.get(i));
                if (o != null && o.slot == a.slot) { give(p, AttachmentItem.of(ids.get(i))); ids.remove(i); }
            }
        }
        ids.add(attId);
        GunStats.setIds(gun, ids);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    public static void remove(ServerPlayer p, ItemStack gun, int index) {
        List<String> ids = GunStats.ids(gun);
        if (index < 0 || index >= ids.size()) return;
        give(p, AttachmentItem.of(ids.remove(index)));
        GunStats.setIds(gun, ids);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    /** Press rounds: one of each input makes a caliber's worth of rounds. */
    public static ItemStack press(ServerPlayer p, ItemStack casing, ItemStack prop, Caliber cal, AmmoType type, int colour) {
        if (casing.isEmpty() || prop.isEmpty()) {
            p.displayClientMessage(Component.literal("Put something in the case and powder slots").withStyle(ChatFormatting.RED), true);
            return ItemStack.EMPTY;
        }
        ItemStack out = AmmoPress.press(cal, type, prop, casing, colour);
        casing.shrink(1);
        prop.shrink(1);
        give(p, out.copy());
        p.level().playSound(null, p.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.35f, 1.8f);
        return out;
    }

    /** An attachment's effects, as short lines for tooltips and the bench. */
    public static List<String> describe(Attachments.Att a) {
        List<String> out = new ArrayList<>();
        Attachments.Mod m = a.mod;
        pct(out, "Damage", m.damage, false);
        pct(out, "Fire rate", m.rpm, false);
        pct(out, "Spread", m.spread, true);
        pct(out, "Recoil", m.recoil, true);
        pct(out, "Range", m.range, false);
        pct(out, "Reload time", m.reload, true);
        pct(out, "Aim time", m.ads, true);
        pct(out, "Loudness", m.sound, true);
        if (m.magMult != 1) out.add(String.format("Magazine x%.2f", m.magMult));
        if (m.zoom > 0) out.add(String.format("Zoom %.0f%%", m.zoom * 100));
        if (m.pelletsAdd > 0) out.add("+" + m.pelletsAdd + " pellets");
        if (m.auto) out.add("Full auto");
        if (m.noBolt) out.add("No bolt to work");
        if (m.laser) out.add("Laser");
        if (m.light) out.add("Light (see in the dark)");
        if (m.weight != 0) out.add(String.format("%+.1f kg", m.weight));
        return out;
    }

    static void pct(List<String> out, String what, float mult, boolean lowerIsBetter) {
        if (Math.abs(mult - 1) < 0.005f) return;
        out.add(String.format("%s %+d%%", what, Math.round((mult - 1) * 100)));
    }
}
