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
        // ---- The second wave (v25): smooth high-detail meshes (tools/guns/designs2.py) ----
        // Pistols
        add(new GunSpec("glock18", "G18 Auto Pistol", Kind.PISTOL, Caliber.P9).auto(1100).mag(19).recoil(2.6f, 1.8f).spread(4.5f, 1.4f).reload(Reload.MAG, 7, 9, 8)
            .desc("A pistol that thinks it's an SMG. Empties in a second."));
        add(new GunSpec("m9", "M9", Kind.PISTOL, Caliber.P9).semi(550).mag(15).reload(Reload.MAG, 7, 9, 8)
            .desc("A long-slide service pistol with an open top."));
        add(new GunSpec("usp45", "USP .45", Kind.PISTOL, Caliber.P45).semi(480).mag(12).dmg(1.05f).reload(Reload.MAG, 7, 9, 8)
            .desc("A heavy polymer .45. Takes a can well."));
        add(new GunSpec("five7", "Five-seveN", Kind.PISTOL, Caliber.P57).semi(600).mag(20).reload(Reload.MAG, 7, 9, 8)
            .desc("Twenty tiny, fast rounds that punch through armor."));
        add(new GunSpec("p226", "P226", Kind.PISTOL, Caliber.P9).semi(560).mag(15).reload(Reload.MAG, 7, 9, 8)
            .desc("An alloy-framed classic with a decocker."));
        add(new GunSpec("cz75", "CZ 75", Kind.PISTOL, Caliber.P9).semi(580).mag(16).spread(2.8f, 0.6f).reload(Reload.MAG, 7, 9, 8)
            .desc("All steel, sits low in the hand, shoots flat."));
        add(new GunSpec("makarov", "PM Makarov", Kind.PISTOL, Caliber.P9).semi(500).mag(8).dmg(0.9f).grid(1, 1).weight(0.7f).reload(Reload.MAG, 6, 8, 7)
            .desc("Small, simple, everywhere."));
        add(new GunSpec("tt33", "TT-33", Kind.PISTOL, Caliber.P762T).semi(520).mag(8).reload(Reload.MAG, 7, 9, 8)
            .desc("A hot little bottleneck round from a thin old pistol."));
        add(new GunSpec("luger", "P08 Luger", Kind.PISTOL, Caliber.P9).semi(520).mag(8).spread(2.8f, 0.6f).reload(Reload.MAG, 8, 10, 9)
            .desc("The toggle lock folds up like a knee with every shot."));
        add(new GunSpec("c96", "C96 Broomhandle", Kind.PISTOL, Caliber.P762T).semi(450).mag(10).reload(Reload.BOLT_CLIP, 8, 8, 9).perRound(6)
            .desc("The magazine's in front of the trigger. Loaded from stripper clips."));
        // Revolvers
        add(new GunSpec("python", "Python .357", Kind.REVOLVER, Caliber.R357).semi(300).mag(6).recoil(5.5f, 1.1f).spread(2.6f, 0.5f).reload(Reload.CYLINDER, 10, 6, 8).perRound(6)
            .desc("A vent-ribbed, full-lug .357. Smooth as glass."));
        add(new GunSpec("m29", "Model 29 .44", Kind.REVOLVER, Caliber.R44).semi(250).mag(6).recoil(8, 1.6f).reload(Reload.CYLINDER, 10, 6, 8).perRound(6)
            .desc("The most powerful handgun in the world, once."));
        add(new GunSpec("judge", "Judge", Kind.REVOLVER, Caliber.G12).semi(280).mag(5).pellets(6).spread(6, 3).range(30).recoil(7, 1.6f).reload(Reload.CYLINDER, 10, 7, 8).perRound(7)
            .desc("A revolver that takes shotgun shells."));
        // SMGs
        add(new GunSpec("vector", "Vector .45", Kind.SMG, Caliber.P45).auto(1200).mag(25).recoil(1.0f, 0.5f).reload(Reload.MAG, 10, 12, 10)
            .desc("The recoil is sent downward. Absurdly fast, absurdly flat."));
        add(new GunSpec("p90", "P90", Kind.SMG, Caliber.P57).auto(900).mag(50).grid(3, 2).reload(Reload.MAG, 14, 16, 10)
            .desc("Fifty rounds lie across the top. Spits brass straight down."));
        add(new GunSpec("ump45", "UMP-45", Kind.SMG, Caliber.P45).auto(600).mag(25).dmg(1.05f).reload(Reload.MAG, 11, 13, 10)
            .desc("A slow, heavy-hitting polymer SMG."));
        add(new GunSpec("thompson", "Thompson", Kind.SMG, Caliber.P45).auto(700).mag(50).weight(4.9f).grid(4, 2).reload(Reload.MAG, 16, 18, 12)
            .desc("The Chicago typewriter. Fifty in the drum."));
        add(new GunSpec("ppsh", "PPSh-41", Kind.SMG, Caliber.P762T).auto(1000).mag(71).weight(5.4f).grid(4, 2).reload(Reload.MAG, 16, 18, 12)
            .desc("Seventy-one rounds in the drum, and it fires them all fast."));
        add(new GunSpec("mac10", "MAC-10", Kind.SMG, Caliber.P45).auto(1100).mag(30).spread(5, 1.8f).recoil(2.2f, 1.6f).grid(2, 2).quiet(0.4f).reload(Reload.MAG, 8, 10, 9)
            .desc("A suppressed brick that empties in under two seconds."));
        add(new GunSpec("bizon", "PP-19 Bizon", Kind.SMG, Caliber.P9).auto(680).mag(64).reload(Reload.MAG, 15, 18, 11)
            .desc("A helical magazine under the barrel holds sixty-four."));
        add(new GunSpec("mp40", "MP40", Kind.SMG, Caliber.P9).auto(500).mag(32).spread(3, 0.8f).reload(Reload.MAG, 11, 13, 10)
            .desc("Slow, steady, controllable."));
        // Assault rifles
        add(new GunSpec("hk416", "HK416", Kind.RIFLE, Caliber.R556).auto(850).mag(30).recoil(1.9f, 0.55f).reload(Reload.MAG, 12, 14, 11)
            .desc("A piston AR. Runs dirty, runs hot, keeps running."));
        add(new GunSpec("mcx", "MCX .300", Kind.RIFLE, Caliber.R300).auto(800).mag(30).quiet(0.35f).reload(Reload.MAG, 12, 14, 11)
            .desc("Built around its suppressor. Heavy subsonic punch, quiet."));
        add(new GunSpec("ak12", "AK-12", Kind.RIFLE, Caliber.R545).auto(700).mag(30).recoil(2.0f, 0.6f).reload(Reload.MAG, 13, 15, 12)
            .desc("The modern AK: rails, a folding stock, the same mud-proof heart."));
        add(new GunSpec("aks74u", "AKS-74U", Kind.RIFLE, Caliber.R545).auto(700).mag(30).range(90).spread(3.8f, 0.7f).grid(3, 2).weight(2.7f).reload(Reload.MAG, 12, 14, 11)
            .desc("A rifle cut down to a carbine. Loud, short, and mean."));
        add(new GunSpec("galil", "Galil ACE", Kind.RIFLE, Caliber.R762S).auto(650).mag(35).recoil(3.0f, 1.0f).reload(Reload.MAG, 13, 15, 12)
            .desc("An AK action dressed in rails and a red dot."));
        add(new GunSpec("as_val", "AS VAL", Kind.RIFLE, Caliber.R9X39).auto(900).mag(20).range(90).quiet(0.2f).reload(Reload.MAG, 11, 13, 11)
            .desc("A rifle that's mostly suppressor. Heavy, slow, silent rounds."));
        add(new GunSpec("scar_l", "SCAR-L", Kind.RIFLE, Caliber.R556).auto(625).mag(30).recoil(1.7f, 0.5f).reload(Reload.MAG, 12, 14, 11)
            .desc("Light, modular, rails all round."));
        add(new GunSpec("g36", "G36", Kind.RIFLE, Caliber.R556).auto(750).mag(30).zoom(0.3f).reload(Reload.MAG, 12, 14, 11)
            .desc("A carry handle with a built-in optic, see-through mags."));
        add(new GunSpec("famas", "FAMAS", Kind.RIFLE, Caliber.R556).auto(1000).mag(25).grid(4, 2).reload(Reload.MAG, 14, 16, 12)
            .desc("The bugle: a fast bullpup under a long carry handle."));
        add(new GunSpec("l85", "L85A2", Kind.RIFLE, Caliber.R556).auto(700).mag(30).zoom(0.45f).spread(3.4f, 0.3f).grid(4, 2).reload(Reload.MAG, 14, 16, 12)
            .desc("A bullpup with a 4x SUSAT scope. Accurate, finally."));
        add(new GunSpec("tavor", "Tavor X95", Kind.RIFLE, Caliber.R556).auto(850).mag(30).grid(4, 2).reload(Reload.MAG, 13, 15, 12)
            .desc("A compact bullpup, rifle barrel in a carbine's length."));
        add(new GunSpec("fal", "FN FAL", Kind.RIFLE, Caliber.R762N).auto(650).mag(20).recoil(4.0f, 1.2f).grid(5, 2).weight(4.3f).reload(Reload.MAG, 13, 15, 12)
            .desc("The right arm of the free world. Full-power, full auto."));
        add(new GunSpec("m1_garand", "M1 Garand", Kind.RIFLE, Caliber.R762N).semi(400).mag(8).dmg(1.05f).grid(6, 1).weight(4.3f).reload(Reload.MAG, 8, 10, 9)
            .desc("Eight rounds in an en-bloc clip. Ping."));
        // Marksman
        add(new GunSpec("mk14", "Mk 14 EBR", Kind.DMR, Caliber.R762N).semi(380).mag(20).recoil(3.4f, 0.8f).reload(Reload.MAG, 13, 15, 12)
            .desc("An M14 in a chassis with a scope. Fast, heavy, precise."));
        add(new GunSpec("vss", "VSS Vintorez", Kind.DMR, Caliber.R9X39).auto(800).mag(10).quiet(0.2f).range(140).reload(Reload.MAG, 11, 13, 11)
            .desc("A silent scoped rifle that also goes full auto."));
        // Snipers
        add(new GunSpec("awm", "AWM", Kind.SNIPER, Caliber.R338).semi(40).mag(5).recoil(9, 1.4f).grid(6, 2).weight(6.5f).zoom(0.82f).reload(Reload.MAG, 14, 16, 12)
            .desc("A green bolt-action magnum. Ends arguments at a kilometre."));
        add(new GunSpec("m82", "M82 Barrett", Kind.SNIPER, Caliber.B50).semi(120).mag(10).manual(false).recoil(12, 2.2f).grid(6, 2).weight(13).zoom(0.8f).range(320).reload(Reload.MAG, 16, 20, 14)
            .desc("A semi-automatic .50 anti-materiel rifle. Shoots through things."));
        add(new GunSpec("kar98k", "Kar98k", Kind.SNIPER, Caliber.R762N).semi(42).mag(5).dmg(1.05f).zoom(0.2f).grid(6, 1).reload(Reload.BOLT_CLIP, 8, 8, 10).perRound(8)
            .desc("A turned-down bolt, iron sights, and a long reach."));
        add(new GunSpec("winchester", "Winchester 1873", Kind.RIFLE, Caliber.R44).semi(80).mag(12).manual(true).dmg(1.1f).grid(6, 1).weight(4.2f).reload(Reload.TUBE, 0, 0, 10).perRound(9)
            .desc("The gun that won the West. Work the lever, load through the gate."));
        // Shotguns
        add(new GunSpec("m870", "M870", Kind.SHOTGUN, Caliber.G12).semi(70).mag(6).manual(true).reload(Reload.TUBE, 0, 0, 10).perRound(10)
            .desc("Pump action. Rack it after every shot."));
        add(new GunSpec("m1014", "M1014", Kind.SHOTGUN, Caliber.G12).semi(350).mag(7).reload(Reload.TUBE, 0, 0, 10).perRound(10)
            .desc("A semi-automatic combat shotgun with ghost-ring sights."));
        add(new GunSpec("spas12", "SPAS-12", Kind.SHOTGUN, Caliber.G12).semi(80).mag(8).manual(true).reload(Reload.TUBE, 0, 0, 10).perRound(10)
            .desc("A pump with a heat shield and attitude."));
        add(new GunSpec("aa12", "AA-12", Kind.SHOTGUN, Caliber.G12).auto(300).mag(20).recoil(2.4f, 1.2f).weight(5.2f).reload(Reload.MAG, 16, 18, 12)
            .desc("A full-auto shotgun with a twenty-shell drum. Barely kicks."));
        add(new GunSpec("ksg", "KSG", Kind.SHOTGUN, Caliber.G12).semi(75).mag(14).manual(true).grid(4, 2).reload(Reload.TUBE, 0, 0, 10).perRound(9)
            .desc("A bullpup pump with two tubes: fourteen shells."));
        add(new GunSpec("sawnoff", "Sawn-Off", Kind.SHOTGUN, Caliber.G12).semi(600).mag(2).pellets(10).spread(10, 5).range(25).grid(3, 1).weight(2).reload(Reload.BREAK, 9, 7, 7).perRound(5)
            .desc("Two barrels, cut short. Devastating, up close."));
        // Machine guns
        add(new GunSpec("rpk", "RPK", Kind.LMG, Caliber.R762S).auto(600).mag(75).recoil(2.4f, 1.2f).weight(5.2f).reload(Reload.MAG, 16, 20, 12)
            .desc("A long, heavy AK with a 75-round drum and a bipod."));
        add(new GunSpec("mg42", "MG 42", Kind.LMG, Caliber.R762N).auto(1200).mag(50).recoil(2.8f, 1.8f).weight(11.6f).reload(Reload.BELT, 20, 26, 14)
            .desc("Hitler's buzzsaw: twelve hundred rounds a minute."));
        add(new GunSpec("m60", "M60", Kind.LMG, Caliber.R762N).auto(550).mag(100).recoil(2.4f, 1.4f).weight(10.5f).reload(Reload.BELT, 20, 26, 14)
            .desc("The pig. A hundred-round belt."));
        add(new GunSpec("dp28", "DP-28", Kind.LMG, Caliber.R762R).auto(550).mag(47).recoil(2.4f, 1.3f).weight(9.1f).reload(Reload.MAG, 18, 22, 12)
            .desc("A flat pan of forty-seven rounds on top, a bipod at the front."));
        add(new GunSpec("minigun", "M134 Minigun", Kind.LMG, Caliber.R762N).auto(2400).mag(500).spinup(16).spread(5, 3).recoil(1.0f, 1.4f).weight(18).grid(6, 3).reload(Reload.BELT, 26, 34, 16)
            .desc("Six barrels. Spin it up, then nothing in front of you survives."));
    }
}
