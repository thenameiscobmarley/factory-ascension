# Factory Ascension: handoff for the next chat

Start a new chat with: **"Continue Factory Ascension from CLAUDE_HANDOFF.md"**. Read this whole file first.
Released so far: **v27.0.0** (2026-10-17). Everything below "To do" is still open.

## Rules the user set (always follow)
- End every reply with "(NN% Completed Response)".
- Never spawn subagents or workflows (quota). Keep turns lean: the user is low on quota; batch work, no long reports.
- No third-party files in the mod (sounds, fonts, textures, models): generate everything ourselves.
- Screenshot every change into `mod/run/screenshots/<date>_<topic>/`; never delete any screenshot.
- Never start local web servers. Builds use all cores. Ask before launching a real Minecraft client (the muted
  xvfb dev client via `runclient.sh` is approved and used all the time).
- Mods only from CurseForge with 5M+ downloads. No time-based difficulty scaling. Every balance number goes in the
  pack config (`kubejs/startup_scripts/00_fa_config.js`) and is bridged in `97_fa_mod.js`.
- Be fail-safe: render/shader features turn themselves off on error (FAClientConfig.failSafe / renderFailed).
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- When the user names a style for "GUIs", apply it to EVERY screen (they were annoyed it was skipped for vanilla ones).

## How to build, test, release
- Repo `~/Projects/factory-ascension`, mod source `mod/`. JDK: `JAVA_HOME=~/fa-mod-tools/jdk`.
- Compile: `cd mod && JAVA_HOME=~/fa-mod-tools/jdk timeout 900 ./gradlew compileJava --no-daemon -q 2>&1 | grep -E "error:" -A3`
- Pack harness (must say "all checks passed"): `java -Xmx400m -jar <rhino-1.7.15.jar> -version 200 tools/test/harness.js`
  (rhino jar was in the old session scratchpad; if gone, download rhino 1.7.15 into the new scratchpad).
- Dev client with scripted screenshots: `client/DevShots.java`, started by flags in `mod/build.gradle`
  (`-PfaV26`, `-PfaFixes`, `-PfaChunks`, `-PfaStrafe`, `-PfaPolish`, ...; each jumps to a step number). The run script
  (recreate it in the new scratchpad if missing):
  ```
  cd ~/Projects/factory-ascension/mod
  rm -rf run/saves/devworld && cp -r run/devworld run/saves/devworld && rm -f run/saves/devworld/session.lock
  LIBGL_ALWAYS_SOFTWARE=1 JAVA_HOME=~/fa-mod-tools/jdk timeout 2400 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient -PfaRun -PfaShots <flags> --no-daemon -Dorg.gradle.jvmargs=-Xmx600m > <log> 2>&1
  ```
  Run it in the background, then move `run/screenshots/fa_*.png` into a dated subfolder. Shots log as `[FA devshots]`.
  `-PfaMods=embeddium,jei,...` loads pack jars from `mod/libs` (use it to test with Embeddium).
- Mesh previews without the game: `java tools/guns/MeshPreview.java <out.png> mod/src/main/resources/assets/factoryascension/textures/gunmesh <a.fam> ...`
- Asset generators: `python3 tools/make_mod_assets.py` (all), `tools/guns/build.py [gun ids]` (guns + attachments),
  `tools/guns/attach_mesh.py`, `tools/vehicles/*.py`, `tools/v25_art.py`, `tools/v26_art.py`, `tools/sound_gen.py`.
- Release: bump `mod_version` in `mod/gradle.properties`; commit; `cd mod && ./gradlew build --no-daemon -q --parallel`;
  `python3 tools/package.py X`; `git tag vX && git push origin main vX`;
  `gh release create vX --title "vX for Minecraft 1.20.1" --notes-file notes.md dist/FactoryAscension-*-vX-MC1.20.1*.zip`;
  `FA_VERSION=X RHINO=<rhino jar> tools/build_site.sh`, commit "Site: vX", push, check `gh run list` succeeded.

## Where things are
- Lighting: `client/render/AscensionLighting.java` + `assets/factoryascension/shaders/core/fa_*.fsh` (gi, blur, ssr,
  composite, glow, rays). World-space sun shadows: `client/render/VoxelShadow.java`.
- Depth pack (parallax): `resources/resourcepacks/fa_depth` (+ `assets/sodium/shaders` for Embeddium via
  `mixin/EmbeddiumShaderMixin`); real heights per block: `client/render/DepthHeights.java` (alpha 191..255 = height).
- Breaking effect now: `client/render/BreakChunks.java` + `shaders/core/fa_break.*` (parallax holes, regrow).
  Voxel debris (falling cubes when blocks break/explode): `client/render/FxParticles.java`.
- GUIs: Tarkov inventory `client/tarkov/TarkovScreen.java`, `DarkInventoryScreen.java` (drag and drop), bench
  `client/gun/WeaponsBenchScreen.java`, traders `client/TraderScreen.java`, workshop `client/vehicle/WorkshopScreen.java`,
  Chem Lab `client/ChemLabScreen.java`, machine panels `client/MachineScreen.java`, settings `client/FASettingsScreen.java`.
- Guns: `gun/*`, animation `client/gun/GunAnim.java`, skins `client/gun/GunSkins.java`, meshes `client/gun/GunMesh.java`.
- `/ascension` is a KubeJS command (`kubejs/server_scripts/*`; grep "ascension" command registration).

## To do (status 2026-10-17, end of session 1)
Done this session (WIP commit e82244dd + later): see git log. Status per request:
1. 32x texture pack: DONE as `client/hd/HdPack` + `Upscale` (painted at load from vanilla, cached in
   .minecraft/fa_hd_cache/vN; bump PAINTER_VERSION when Upscale changes). Preview: `tools/hd/Preview.java`.
   Open: the depth pack's relief lighting makes stone look glossy/"melted"; per-material gloss added
   (DepthHeights.gloss -> smallest mip alpha 0.5..0.75 -> fa_parallax.glsl). Re-check the look.
2. Audit: partial. Done: dead methods, duplicate drag code, unread config keys (none real), KubeJS drop handlers
   (separate features). Still: read all Java/JS for overlaps (FxClient debris vs vanilla particles, HUD overlaps).
3. /ascension screen: DONE (`client/AscensionScreen`, KubeJS `faOpenMenu`, `/ascension menu` = chat menu).
4. Dark container screens + drag/drop: DONE (`client/ui/DarkContainers`, `mixin/DarkLabelMixin`). Test with
   Sophisticated Backpacks loaded (-PfaMods=...) still to do.
5. Professional UI: UiKit (`client/ui/UiKit`), FAGui now delegates to it, amber strips + scaled text removed.
   Still: redesign TarkovScreen/WeaponsBenchScreen/TraderScreen layouts with UiKit spacing; recheck every screen.
6. Weapon skins: per-pixel GPU skins DONE (`client/gun/GunSkinGpu`, `shaders/core/fa_gun_skin`). Still: extras
   (snow lumps, torn strips...) quality; check moving skins look right in motion.
7. Breaking: DONE (`client/render/BreakChunks` + `mixin/BreakHideMixin`/`EmbeddiumBreakHideMixin`).
8. Water waves: REMOVED on request (v27.0.0). Do not bring them back.
9. FA item browser: DONE (`client/ui/ItemBrowser`, emblem from `tools/ui_art.py`), Sort + pickup Filter
   (`storage/LootFilter`, packet InvAction).
10. Recipes: nearly all items already had one (drugs = Chem Lab, now shown in the browser). No recipe on purpose:
    cash, ores, realm crystal, trader contracts.
11/12. Sort, filter, pickup feed (`client/ui/PickupFeed`): DONE.
13. Style rule (no amber left strip, no scaled text): applied everywhere found. Vanilla buttons/hotbar stay grey (user choice).
Still open from the older list: reflections (gloss now per material; SSR still only water/wet), Embeddium 3D
texture outlines check, the user's lost requests (ask).
- DevShots flags added: -PfaBreak, -PfaUi, -PfaWater, -PfaHd, -PfaFeed, -PfaSkin2. Never compile while a dev
  client is running (it reads build/classes) and never run two clients at once (shared run/saves/devworld).

## From the user's older prompt: check each really works (fix if not)
- 3D textures with **Embeddium/Sodium**: no outlines on blocks/mobs, no "detached layer" look (test with
  `-PfaMods=embeddium`; dev runs so far had no Embeddium loaded).
- **Reflections look horrible / can't tell lighting is on**: SSR is now only on water and wet ground; the user wants
  visible, good-looking reflections and specular from the lighting itself (metal, wet stone, glass, water).
- Tarkov GUI must look AAA, not Minecraft-y (see 5).
- Crumble physics: some debris pieces didn't fall (FxParticles).
- Skinwalkers: more of them, and spawning in safe zones too (`kubejs/startup_scripts/76_fa_skinwalkers.js`).
- Already done and released (still verify briefly): hip-fire crosshair at the bullet's landing point, NODs with
  green/white phosphor and artifacts, gunshots heard by hordes unless suppressed, stealth/alert operators drawing
  slung guns, Q/E lean while aiming, cursed attachments, drugs (incl. psychosis visions/peeking heads), raid marking
  with bulletproof blocks/doors, grenades and flashbangs used by NPCs, bullet whizzes, own animations visible,
  depth smoothing slider, plate carriers with plates, drug crops and Chem Lab.

## Notes
- Strafing test (DevShots step 900, `-PfaStrafe`) logs speed: it reached 0.84 blocks/tick.
- Real attachment ids are `family.maker.finish.tier` (see `Attachments.java`); DevShots `attOf(name)` finds one.
- KubeJS hides vanilla methods; never store null on `global` (see memory files).
