# Guns plan (v23+)

User request (2026-10-15): many more guns, far more detailed than the shotgun; ADS must not put the camera in the
stock; walk / strafe / idle / sprint animations, randomised so no two are the same; stress-based reload fumbles (mag
slips and won't come out, etc., many kinds); a Weapons Workbench (same Tarkov style) with 1000+ attachments; an
enchanted book that lets any attachment go on any gun (cursed builds); craft your own bullets from any item as
propellant and casing, with a type (Regular, RIP, Snakeshot, Slug, ...) and a colour.

## Architecture
- `gun/GunSpec` data (one Java list): id, name, class, caliber, fire modes, RPM, mag, damage, spread, recoil, ADS
  (sight height, eye relief, zoom), reload kind (mag / tube / single / break-action / belt), size in the grid, weight.
- `gun/GunItem` one item class for all of them (the shotgun keeps its own class, same animation code).
- Rendering: guns are JSON models from `tools/guns/` (Python, high detail); attachments are ~100+ shape models,
  combined with tints and stats into a 1000+ catalog; a BEWLR draws gun + attachments at mount points.
- `client/gun/GunAnim` procedural first-person animation: smooth noise with per-cycle random parameters
  (never the same twice), walk / strafe tilt / sprint / idle fidgets / ADS / recoil / reload sequences.
- `gun/Stress` server-side 0..1: damage taken, low health, hostiles near, sustained fire, sprinting; decays when calm.
  Reloads roll fumbles weighted by stress; each fumble adds time, an animation, a sound and a line on screen.
- Ammo: one `cartridge` item, NBT: caliber, type, propellant item, casing item, colour. Stats derived from the items.

## Stages
- [x] A. Framework: GunSpec, GunItem, fire/reload packets generalised, ADS fix (per-gun sight height/eye relief),
      GunAnim (walk, strafe, sprint, idle fidgets, all randomised), Stress + reload fumbles (15+ kinds), selftest.
- [x] B. Guns: pistols, revolver, SMGs, rifles, DMR, bolt sniper, shotguns, LMG; high-detail models + previews.
- [x] C. Ammo: cartridge item + ammo press tab (any item as propellant / casing, type, colour), ballistics per type.
- [x] D. Weapons Workbench block + screen (Tarkov style), attachment catalog (1000+), mount rendering, stats.
- [x] E. "Jury-Rigged" enchanted book: any attachment on any gun, stacking slots; cursed builds.
- [ ] Each stage: harness + Full/Half selftest + client screenshots, release vNN.0.0, site news.

Done: all in v24.0.0. Later ideas: arms holding the guns in first person; attachments replacing base parts.
