package dev.factoryascension.gun;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A gun's numbers with its attachments on (gun/Attachments). Attachments are in the gun's NBT as "Att", a list of
 * attachment ids; each multiplies or adds to the base GunSpec. Worked out when needed (it's a few multiplications).
 */
public final class GunStats {
    public final GunSpec spec;
    public final List<Attachments.Att> atts;
    public float damage, spreadHip, spreadAim, range, recoilUp, recoilSide, zoom, reload, sound, ads, weight, sightRaise;
    public int rpm, mag, pellets;
    public boolean auto, laser, light, noBolt, akimbo, gangster, blinding, thermal;

    private GunStats(GunSpec s, List<Attachments.Att> atts) {
        this.spec = s;
        this.atts = atts;
        damage = s.damage;
        spreadHip = s.spreadHip;
        spreadAim = s.spreadAim;
        range = s.range;
        recoilUp = s.recoilUp;
        recoilSide = s.recoilSide;
        zoom = s.zoom;
        rpm = s.rpm;
        pellets = s.pellets;
        auto = s.auto;
        reload = 1;
        sound = s.sound;
        ads = 1;
        weight = s.weight;
        float magMult = 1;
        int magAdd = 0;
        float bestZoom = 0;
        for (Attachments.Att a : atts) {
            Attachments.Mod m = a.mod;
            damage *= m.damage;
            spreadHip *= m.spread;
            spreadAim *= m.spread;
            range *= m.range;
            recoilUp *= m.recoil;
            recoilSide *= m.recoil;
            rpm = Math.round(rpm * m.rpm);
            reload *= m.reload;
            sound *= m.sound;
            ads *= m.ads;
            weight += m.weight;
            magMult *= m.magMult;
            magAdd += m.magAdd;
            pellets += m.pelletsAdd;
            if (m.zoom > bestZoom) { bestZoom = m.zoom; sightRaise = m.sightRaise; }
            auto |= m.auto;
            laser |= m.laser;
            light |= m.light;
            noBolt |= m.noBolt;
            akimbo |= m.akimbo;
            gangster |= m.gangster;
            blinding |= m.blinding;
            thermal |= m.thermal;
        }
        if (bestZoom > 0) zoom = Math.max(zoom, bestZoom);
        mag = Math.max(1, Math.min(2000, Math.round(s.mag * magMult) + magAdd));
        rpm = Math.max(10, Math.min(2400, rpm));
        reload = Math.max(0.2f, reload);
    }

    public int cooldown() {
        return Math.max(1, Math.round(1200f / rpm));
    }

    public static List<String> ids(ItemStack gun) {
        List<String> out = new ArrayList<>();
        if (!gun.hasTag()) return out;
        ListTag list = gun.getTag().getList("Att", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static void setIds(ItemStack gun, List<String> ids) {
        ListTag list = new ListTag();
        for (String id : ids) list.add(StringTag.valueOf(id));
        if (list.isEmpty()) gun.getOrCreateTag().remove("Att");
        else gun.getOrCreateTag().put("Att", list);
    }

    public static GunStats of(ItemStack gun) {
        GunSpec s = GunItem.spec(gun);
        if (s == null) return null;
        List<Attachments.Att> atts = new ArrayList<>();
        for (String id : ids(gun)) {
            Attachments.Att a = Attachments.BY_ID.get(id);
            if (a != null) atts.add(a);
        }
        return new GunStats(s, atts);
    }
}
