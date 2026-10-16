package dev.factoryascension.gun;

import dev.factoryascension.gun.GunSpec.Kind;
import dev.factoryascension.gun.GunSpec.Reload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Things that go wrong while reloading under stress. Each happens at one step of the reload (taking the old mag out,
 * putting the new one in, chambering, loading a round, or before it all starts), costs extra ticks, plays its own
 * motion (client/gun/GunAnim) and says what happened. The calmer you are the rarer they get (gun/Stress).
 */
public enum Fumble {
    // Getting the old magazine out (or opening up)
    MAG_STUCK(Step.OUT, 12, "The mag's stuck. Tug it again", (s, r) -> r == Reload.MAG),
    DROPPED_MAG(Step.OUT, 10, "Dropped the mag", (s, r) -> r == Reload.MAG),
    CYLINDER_STUCK(Step.OUT, 12, "The cylinder won't swing out", (s, r) -> r == Reload.CYLINDER),
    HINGE_STIFF(Step.OUT, 10, "The action won't break open", (s, r) -> r == Reload.BREAK),
    BOLT_WONT_OPEN(Step.OUT, 9, "The bolt won't lift", (s, r) -> r == Reload.BOLT_CLIP),
    COVER_STUCK(Step.OUT, 14, "The feed cover's stuck", (s, r) -> r == Reload.BELT),
    // Getting the new one in
    FUMBLED_POUCH(Step.IN, 10, "Fumbled the pouch", (s, r) -> r == Reload.MAG || r == Reload.BELT),
    MAG_SLIPPED(Step.IN, 10, "The mag slipped out of your hand", (s, r) -> r == Reload.MAG),
    MISSED_WELL(Step.IN, 8, "Missed the mag well", (s, r) -> r == Reload.MAG),
    MAG_BACKWARDS(Step.IN, 9, "The mag's the wrong way round", (s, r) -> r == Reload.MAG),
    NOT_SEATED(Step.IN, 6, "Mag not seated. Slap it", (s, r) -> r == Reload.MAG),
    WRONG_MAG(Step.IN, 12, "Grabbed an empty mag", (s, r) -> r == Reload.MAG),
    BELT_TANGLED(Step.IN, 20, "The belt's tangled", (s, r) -> r == Reload.BELT),
    // Chambering
    HANDLE_SLIPPED(Step.CHAMBER, 7, "The charging handle slipped", (s, r) -> s.kind != Kind.PISTOL && s.kind != Kind.REVOLVER && r != Reload.BOLT_CLIP),
    SLIDE_STUCK(Step.CHAMBER, 8, "The slide won't go forward", (s, r) -> s.kind == Kind.PISTOL),
    DOUBLE_FEED(Step.CHAMBER, 24, "Double feed! Clearing it", (s, r) -> r == Reload.MAG || r == Reload.BELT),
    STOVEPIPE(Step.CHAMBER, 12, "Stovepipe. Sweep the port", (s, r) -> r == Reload.MAG || r == Reload.TUBE),
    BOLT_STICKY(Step.CHAMBER, 10, "The bolt's sticky. Hit it", (s, r) -> r == Reload.BOLT_CLIP),
    FORGOT_CHAMBER(Step.CHAMBER, 0, "Forgot to chamber a round", (s, r) -> r == Reload.MAG),
    // Loading one round at a time
    DROPPED_ROUND(Step.ROUND, 8, "Dropped a round", (s, r) -> r != Reload.MAG && r != Reload.BELT),
    ROUND_BACKWARDS(Step.ROUND, 7, "Round's in backwards", (s, r) -> r != Reload.MAG && r != Reload.BELT),
    SHELL_JAMMED(Step.ROUND, 12, "A shell jammed in the loading gate", (s, r) -> r == Reload.TUBE),
    // The whole reload
    SHAKY_HANDS(Step.START, 0, "Your hands are shaking", (s, r) -> true);

    public enum Step { START, OUT, IN, CHAMBER, ROUND }

    public final Step step;
    public final int ticks;
    public final String message;
    private final BiPredicate<GunSpec, Reload> fits;

    Fumble(Step step, int ticks, String message, BiPredicate<GunSpec, Reload> fits) {
        this.step = step;
        this.ticks = ticks;
        this.message = message;
        this.fits = fits;
    }

    /** The fumbles that can happen to this gun at this step. */
    public static List<Fumble> at(Step step, GunSpec spec) {
        List<Fumble> out = new ArrayList<>();
        for (Fumble f : values()) if (f.step == step && f.fits.test(spec, spec.reload)) out.add(f);
        return out;
    }
}
