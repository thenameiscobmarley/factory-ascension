package dev.factoryascension.gun;

import dev.factoryascension.gun.GunSpec.Kind;
import dev.factoryascension.gun.GunSpec.Reload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every gun (besides the original Ascension Shotgun, item/ShotgunItem). Models: tools/guns/. */
public final class Guns {
    public static final float BASE_DAMAGE = 6;
    public static final List<GunSpec> ALL = new ArrayList<>();
    public static final Map<String, GunSpec> BY_ID = new LinkedHashMap<>();

    private Guns() {
    }

    private static GunSpec add(GunSpec g) {
        ALL.add(g);
        BY_ID.put(g.id, g);
        return g;
    }

    static {
        // Pistols and the revolver
        add(new GunSpec("m1911", "M1911", Kind.PISTOL, Caliber.P45).semi(500).mag(7).dmg(1.1f).reload(Reload.MAG, 8, 10, 8)
            .desc("A hundred years old and still hits like a door."));
        add(new GunSpec("p17", "P17 Pistol", Kind.PISTOL, Caliber.P9).semi(600).mag(17).reload(Reload.MAG, 7, 9, 8)
            .desc("Polymer, boring, never jams. Mostly."));
        add(new GunSpec("hawk50", "Hawk .50", Kind.PISTOL, Caliber.P50).semi(240).mag(7).recoil(9, 2).dmg(1.1f).weight(2).reload(Reload.MAG, 9, 12, 10)
            .desc("A hand cannon. Mind your wrists."));
        add(new GunSpec("r357", "R357 Revolver", Kind.REVOLVER, Caliber.R357).semi(300).mag(6).recoil(6, 1.2f).reload(Reload.CYLINDER, 10, 6, 8).perRound(6)
            .desc("Six chances. Reloads one chamber at a time."));
        // SMGs
        add(new GunSpec("mp5", "MP5 SMG", Kind.SMG, Caliber.P9).auto(800).mag(30).reload(Reload.MAG, 12, 14, 12)
            .desc("Smooth, accurate, and pleasant to hold. The classic."));
        add(new GunSpec("mp7", "MP7 PDW", Kind.SMG, Caliber.P46).auto(950).mag(40).grid(3, 2).reload(Reload.MAG, 10, 12, 10)
            .desc("Tiny rounds, very fast, goes straight through armor."));
        add(new GunSpec("uz9", "UZ-9 SMG", Kind.SMG, Caliber.P9).auto(950).mag(32).spread(4.5f, 1.4f).recoil(2.0f, 1.4f).grid(2, 2).reload(Reload.MAG, 9, 11, 10)
            .desc("A box that sprays. Aim is a suggestion."));
        add(new GunSpec("kv45", "KV .45 SMG", Kind.SMG, Caliber.P45).auto(1200).mag(25).recoil(1.1f, 0.6f).reload(Reload.MAG, 11, 13, 10)
            .desc("The recoil goes down instead of up. Empties itself in a second."));
        // Assault rifles
        add(new GunSpec("ak74", "AK-74M", Kind.RIFLE, Caliber.R545).auto(650).mag(30).recoil(2.2f, 0.7f).reload(Reload.MAG, 13, 15, 12)
            .desc("Light recoil, cheap rounds, works in mud."));
        add(new GunSpec("akm", "AKM", Kind.RIFLE, Caliber.R762S).auto(600).mag(30).recoil(3.4f, 1.3f).dmg(1.05f).reload(Reload.MAG, 13, 15, 12)
            .desc("The AK everyone knows. Hits hard, kicks harder."));
        add(new GunSpec("m4a1", "M4A1", Kind.RIFLE, Caliber.R556).auto(800).mag(30).recoil(2.0f, 0.6f).reload(Reload.MAG, 12, 14, 11)
            .desc("Modular, flat-shooting, everywhere."));
        add(new GunSpec("mk17", "MK17 Battle Rifle", Kind.RIFLE, Caliber.R762N).auto(600).mag(20).recoil(4.2f, 1.2f).grid(5, 2).weight(4.2f).reload(Reload.MAG, 13, 15, 12)
            .desc("A full-power rifle round on full auto. Brace yourself."));
        add(new GunSpec("aug", "AUG A3", Kind.RIFLE, Caliber.R556).auto(700).mag(30).recoil(1.8f, 0.5f).zoom(0.35f).reload(Reload.MAG, 14, 16, 12)
            .desc("A bullpup: the magazine sits behind the grip. Built-in optic."));
        // Marksman and sniper rifles
        add(new GunSpec("svd", "SVD Dragunov", Kind.DMR, Caliber.R762R).semi(300).mag(10).reload(Reload.MAG, 14, 16, 12)
            .desc("A long, thin, semi-automatic marksman rifle with a classic scope."));
        add(new GunSpec("sr25", "SR-25 DMR", Kind.DMR, Caliber.R762N).semi(350).mag(20).recoil(3.0f, 0.7f).reload(Reload.MAG, 13, 15, 12)
            .desc("An AR built for precision at range."));
        add(new GunSpec("m700", "M700 Bolt Rifle", Kind.SNIPER, Caliber.R762N).semi(45).mag(5).reload(Reload.BOLT_CLIP, 8, 8, 10).perRound(9)
            .desc("Bolt action. Work the bolt after every shot. Loads round by round."));
        add(new GunSpec("mosin", "Mosin-Nagant", Kind.SNIPER, Caliber.R762R).semi(40).mag(5).dmg(1.05f).grid(6, 1).reload(Reload.BOLT_CLIP, 8, 8, 10).perRound(8)
            .desc("A century-old bolt rifle. Long, heavy, and it still works."));
        add(new GunSpec("ax338", "AX .338", Kind.SNIPER, Caliber.R338).semi(40).mag(10).recoil(9, 1.5f).grid(6, 2).weight(6.8f).zoom(0.8f).reload(Reload.MAG, 15, 18, 14)
            .desc("The long-range sniper. One shot, one problem solved."));
        // Shotguns
        add(new GunSpec("saiga12", "Saiga-12", Kind.SHOTGUN, Caliber.G12).semi(300).mag(8).reload(Reload.MAG, 14, 16, 14)
            .desc("A semi-automatic shotgun with a box magazine."));
        add(new GunSpec("double_barrel", "Double Barrel", Kind.SHOTGUN, Caliber.G12).semi(600).mag(2).pellets(10).spread(8, 3.5f).grid(5, 1).reload(Reload.BREAK, 10, 8, 8).perRound(6)
            .desc("Two barrels, both at once if you're quick."));
        // Machine guns
        add(new GunSpec("pkm", "PKM", Kind.LMG, Caliber.R762R).auto(650).mag(100).recoil(2.6f, 1.6f).weight(8.5f).reload(Reload.BELT, 20, 26, 14)
            .desc("A hundred-round belt of full-power rifle rounds."));
        add(new GunSpec("m249", "M249 SAW", Kind.LMG, Caliber.R556).auto(800).mag(100).recoil(1.9f, 1.3f).weight(7.5f).reload(Reload.BELT, 18, 24, 12)
            .desc("A belt-fed 5.56 that never stops."));
    }
}
