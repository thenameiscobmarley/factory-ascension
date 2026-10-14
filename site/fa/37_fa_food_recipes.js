// priority: 640
// Factory Ascension - cooking mob food. Furnace, smoker and campfire all work, like vanilla meat
// (cook time from the config: mobFood.cookSeconds).

ServerEvents.recipes(event => {
  let MF = global.FA.mobFood
  let ticks = MF.cookSeconds * 20
  let id = name => 'kubejs:factory_ascension/' + name
  MF.foods.forEach(fd => {
    let raw = 'kubejs:' + fd.raw, cooked = 'kubejs:' + fd.cooked
    event.smelting(cooked, raw).xp(0.35).cookingTime(ticks).id(id('cook_' + fd.raw))
    event.smoking(cooked, raw).xp(0.35).cookingTime(Math.round(ticks / 2)).id(id('smoke_' + fd.raw))
    event.campfireCooking(cooked, raw).xp(0.35).cookingTime(ticks * 3).id(id('campfire_' + fd.raw))
  })
})
