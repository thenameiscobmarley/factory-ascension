// Factory Ascension - settings.
// This is a startup script: after editing, fully restart the game (/reload is not enough).

global.FA = {
  // ---- Ascension cost ----
  // Essence needed to go from level L to L+1 = ceil(costBase * costGrowth ^ L).
  // 1.12 means every level costs 12% more than the last one, so the factory
  // has to keep growing for you to keep levelling. There is no level cap.
  costBase: 1,
  costGrowth: 1.12,

  // Essence each item gives when absorbed (right-click; sneak + right-click absorbs the whole stack)
  essenceValues: {
    'kubejs:ascension_core': 1,
    'kubejs:dense_ascension_core': 9,
    'kubejs:ascension_singularity': 81
  },

  // ---- Rewards per level ----
  damageDealtPerLevel: 0.15,  // +15% damage per level, no cap
  damageTakenPerLevel: 0.05,  // damage taken is divided by (1 + 0.05 * level), so it never reaches zero
  healthPerLevel: 2,          // +1 heart per level. Vanilla caps max health at 1024, install AttributeFix to lift that
  speedPerLevel: 0.01,        // +1% move speed per level...
  speedCap: 0.5,              // ...up to +50%. Much faster than that outruns chunk loading
  lootPerLevel: 0.05,         // mob drops x(1 + 0.05 * level), no cap
  maxExtraDropStacks: 8,      // most extra item stacks one kill can spawn, so huge levels can't lag the server

  // ---- Feel ----
  healOnLevelUp: true,
  announceEvery: 10,          // tell the whole server every N levels (0 = never)

  // Safety stop for the level-up loop, not a gameplay limit
  maxLevel: 1000000
}

// Levels of online players, kept in memory so combat hooks never touch world data on every hit.
// The server script fills this in on login, respawn and level-up.
global.FA_LEVELS = global.FA_LEVELS || {}
