// What every release added, by area of the site. The home page's "New in" card and the New / Everything-before books in
// each chapter are built from this (site.js), so new things never get mixed in with old ones.
// area: home, luck, gear, enchants, machines, world. icon: a path under fa/. Newest release first.
window.FA_NEWS = [
  // ---- v22.0.0 ----
  { v: "22.0.0", area: "home", icon: "items/flare_green.png", title: "Tarkov-style gear screen", text: "The inventory now looks like Tarkov's: gear frames around your silhouette, a backpack panel with filters, quick use slots and your stats along the bottom." },

  // ---- v21.0.0 ----
  { v: "21.0.0", area: "home", icon: "items/flare_green.png", title: "Tarkov inventory", text: "Your inventory is a grid where items take their real size. Turn them with R, watch your weight, and open it out of a duffel bag." },
  { v: "21.0.0", area: "gear", icon: "items/flare_red.png", title: "Flare Gun and airdrops", text: "Red flares signal. Green flares call a supply crate that falls from the sky full of loot." },
  { v: "21.0.0", area: "home", icon: "items/ascension_core.png", title: "Depth without a shader pack", text: "A built-in texture pack gives every block and mob real parallax depth. No shader pack, Oculus or Embeddium needed." },

  // ---- v20.0.0 ----
  { v: "20.0.0", area: "home", icon: "items/mythic_fragment.png", title: "Ascension Lighting", text: "Screen-space traced lighting: soft contact shadows, bounced colour and sun rays. Works on AMD and NVIDIA." },
  { v: "20.0.0", area: "home", icon: "items/ascension_core.png", title: "Factory Ascension settings", text: "A button in the pause menu for every graphics option, with a safe mode that turns effects off if anything goes wrong." },
  { v: "20.0.0", area: "world", icon: "blocks/realm_moss_side.png", title: "Smoke, debris and living water", text: "Soft smoke from explosions and gunfire, blocks that shatter into bouncing voxels, and foaming, splashing water." },
  { v: "20.0.0", area: "home", icon: "blocks/engine_coil_side.png", title: "Health checks", text: "The mod watches memory, open files and threads, and warns before a leak can slow your game." },

  // ---- v19.0.0 ----
  { v: "19.0.0", area: "home", icon: "items/blank_ascension_tome.png", title: "Clear version numbers", text: "Releases are now v19, v20 and so on, and every download says it's for Minecraft 1.20.1." },
  { v: "19.0.0", area: "enchants", icon: "items/tome_soulbound.png", title: "Soulbound", text: "A new enchant: the item stays with you through death, can't be dropped and never burns. An Unbinding Scroll takes it off." },
  { v: "19.0.0", area: "world", icon: "items/gear_cache.png", title: "Rarer, tidier structures", text: "Factory Ruins and the other structures are much rarer, fit inside one chunk, and ruins only sit on flat, solid ground." },

  // ---- 1.18 ----
  { v: "1.18.0", area: "gear", icon: "items/ascendite_sword.png", title: "The Ascension Shotgun", text: "Aim down the sights, pump and reload shell by shell. Tomes work on it like any weapon." },
  { v: "1.18.0", area: "world", icon: "items/void_eye.png", title: "Hordes, 15x stronger", text: "Horde mobs take a fifteenth of the damage and hit fifteen times as hard." },
  { v: "1.18.0", area: "enchants", icon: "items/enchantment_pack.png", title: "Mob loot switch", text: "Sneak + swap hands turns Plunder, Midas, Gem Hunter, heads and Looting off when you have enough mob parts." },
  // ---- 1.17 ----
  { v: "1.17.0", area: "world", icon: "items/ascended_soul.png", title: "Realm bosses", text: "The Astral Sovereign and the Void Behemoth, plus Astral Wisps and Void Knights." },
  { v: "1.17.0", area: "gear", icon: "items/polishing_cloth.png", title: "Pets and charms", text: "Five Ascension Collars, a Pet Whistle, and ten charms that work from your pocket." },
  // ---- 1.16 ----
  { v: "1.16.0", area: "machines", icon: "blocks/quarry_front.png", title: "Ascension Quarry", text: "Mines out a 17 x 17 hole to the bottom of the world, faster the faster it spins." },
  { v: "1.16.0", area: "machines", icon: "blocks/grinder_front.png", title: "Mob Grinder", text: "Kills what's in front of it, and the kills count as yours." },
  { v: "1.16.0", area: "machines", icon: "blocks/warp_hopper_side.png", title: "Warp Hopper", text: "Key two to the same item and items travel between them, even across dimensions." },
  // ---- 1.15 / 1.14 ----
  { v: "1.15.0", area: "world", icon: "items/gear_cache.png", title: "Ruins, crypts and vaults", text: "Factory Ruins, Ascension Crypts, Sky Shrines and Realm Vaults, with spawners and loot." },
  { v: "1.14.0", area: "world", icon: "blocks/realm_gate_side.png", title: "The Ascended Realm", text: "A dimension of floating islands behind a Realm Gate, with ores found nowhere else." },
  // ---- 1.13 ----
  { v: "1.13.0", area: "machines", icon: "blocks/engine_coil_side.png", title: "One engine rod", text: "Coils, Gearboxes and Burners lie on their side and join into a single long engine." },
  { v: "1.13.0", area: "enchants", icon: "items/tome_wings.png", title: "3D tomes", text: "Every tome is a real 3D book, in all 18 foils." },
  { v: "1.13.0", area: "gear", icon: "items/booster_box.png", title: "Backpacks up to 1,728 slots", text: "Six Ascension tiers of Sophisticated Backpacks; every upgrade works." },
  // ---- 1.12 ----
  { v: "1.12.0", area: "home", icon: "items/ascension_core.png", title: "Safety net and backups", text: "Freeze protection for Ultimates, a world backup every time you close it, and a warning when a mod is missing." },
  // ---- 1.11 ----
  { v: "1.11.0", area: "gear", icon: "blocks/ascension_workbench_front.png", title: "Legendary stars", text: "The Ascension Workbench takes gear to 10 stars." },
  { v: "1.11.0", area: "machines", icon: "blocks/mini_crusher_front.png", title: "Mini Crusher", text: "Crushing without the wheels: a motor and a battery." },
  { v: "1.11.0", area: "luck", icon: "items/foil_sheet_gold.png", title: "Cased foils", text: "Ten cased foils, 3D models and a quest book." },
  // ---- 1.10 / 1.9 ----
  { v: "1.10.0", area: "gear", icon: "items/attachment_vampire_fangs.png", title: "Sword attachments", text: "Battery-powered attachments: a headlamp, a chainsaw, an electric spike wrap and more." },
  { v: "1.9.0", area: "machines", icon: "blocks/storage_terminal_front.png", title: "Universal Storage Terminal", text: "Every linked chest, one searchable screen." },
  { v: "1.9.0", area: "luck", icon: "items/mythic_fragment.png", title: "Meteors drop anything", text: "Meteor Showers can drop any item in the pack; loot follows the danger curve." },
  // ---- 1.8 - 1.6 ----
  { v: "1.8.0", area: "machines", icon: "items/ascension_core.png", title: "Early automated cores", text: "Cores without heat, glass recipes, and a description on everything." },
  { v: "1.7.0", area: "machines", icon: "blocks/engine_core_front.png", title: "Engine screens", text: "Every Modular Engine part has its own screen." },
  { v: "1.6.0", area: "world", icon: "items/void_eye.png", title: "14 more mobs and 4 bosses", text: "The Rust Colossus, Broodmother, Hollow King and Wisp Tyrant, and the gear-matching tomes." },
  // ---- 1.5 - 1.1 ----
  { v: "1.5.0", area: "world", icon: "items/cow_heart.png", title: "Skinwalkers", text: "That cow has been staring at you for a while." },
  { v: "1.4.0", area: "world", icon: "items/void_eye.png", title: "20 new mobs", text: "A body part from every mob, connected glass and Shiny Reflections." },
  { v: "1.3.0", area: "machines", icon: "items/room_tnt_sandstone_16.png", title: "Factory Room TNT", text: "A lined, lit room in one click, up to 50 x 50." },
  { v: "1.2.0", area: "world", icon: "items/ascendite_sword.png", title: "Danger from your home", text: "Danger zones count from your /home, closer and steeper." },
  { v: "1.1.0", area: "machines", icon: "blocks/super_smelter_front_lit.png", title: "The first machines", text: "Modular Engine, Super Smelter, Salvager and Tome Fusion Table." },
  // ---- 1.0: the pack ----
  { v: "1.0.0", area: "enchants", icon: "items/tome_wings.png", title: "487 Ascended enchants", text: "No level cap: Fusions, Triads, Mythics and twelve Ultimates." },
  { v: "1.0.0", area: "luck", icon: "items/enchantment_pack.png", title: "Every chest is a lottery", text: "Enchantment Packs, Booster Boxes, Gear Caches and jackpots in the loot." },
  { v: "1.0.0", area: "enchants", icon: "items/chaos_tome.png", title: "Chaos Tomes", text: "Random enchants, rolled to suit the gear." },
  { v: "1.0.0", area: "world", icon: "items/ascended_soul.png", title: "Danger zones", text: "The farther out, the tougher the mobs and the better the loot." },
  { v: "1.0.0", area: "home", icon: "items/ascendite_ingot.png", title: "Factory Ascension", text: "A Create pack where your factory is your power: Ascendite, cores and Liquid Essence." }
];
