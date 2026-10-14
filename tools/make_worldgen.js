// Writes the Ascendite ore worldgen (datapack JSON under kubejs/data/) from the config.
// KubeJS's WorldgenEvents don't work on 1.20.1, so the ore comes in as a normal datapack feature + Forge biome modifier.
// Run after changing global.FA.ascendite.ore:   java -jar rhino-1.7.15.jar -version 200 tools/make_worldgen.js
// (the test harness fails if these files don't match the config)

// The test harness loads this file too, with the config already loaded
if (typeof global == 'undefined' || !global.FA) {
  global = {}
  Platform = { isLoaded: function () { return false } }
  load('kubejs/startup_scripts/00_fa_config.js')
}

function FA_worldgenFiles(O) {
  return {
    'kubejs/data/kubejs/worldgen/configured_feature/ore_ascendite.json': {
      type: 'minecraft:ore',
      config: {
        size: O.veinSize,
        discard_chance_on_air_exposure: 0,
        targets: [
          { target: { predicate_type: 'minecraft:tag_match', tag: 'minecraft:stone_ore_replaceables' }, state: { Name: 'kubejs:ascendite_ore' } },
          { target: { predicate_type: 'minecraft:tag_match', tag: 'minecraft:deepslate_ore_replaceables' }, state: { Name: 'kubejs:deepslate_ascendite_ore' } }
        ]
      }
    },
    'kubejs/data/kubejs/worldgen/placed_feature/ore_ascendite.json': {
      feature: 'kubejs:ore_ascendite',
      placement: [
        { type: 'minecraft:count', count: O.veinsPerChunk },
        { type: 'minecraft:in_square' },
        { type: 'minecraft:height_range', height: { type: 'minecraft:trapezoid', min_inclusive: { absolute: O.minY }, max_inclusive: { absolute: O.maxY } } },
        { type: 'minecraft:biome' }
      ]
    },
    'kubejs/data/kubejs/forge/biome_modifier/ore_ascendite.json': {
      type: 'forge:add_features',
      biomes: '#minecraft:is_overworld',
      features: 'kubejs:ore_ascendite',
      step: 'underground_ores'
    }
  }
}

if (typeof FA_WORLDGEN_CHECK == 'undefined') {
  var files = FA_worldgenFiles(global.FA.ascendite.ore)
  Object.keys(files).forEach(function (path) {
    var f = new java.io.File(path)
    f.getParentFile().mkdirs()
    var w = new java.io.FileWriter(f)
    w.write(JSON.stringify(files[path], null, 2) + '\n')
    w.close()
    print('wrote ' + path)
  })
}
