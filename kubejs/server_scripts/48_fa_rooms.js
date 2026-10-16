// priority: 480
// Factory Ascension - Factory Room TNT (config: roomCharges). Right-click a block with one: after the fuse it blasts out
// a room in front of you, lined with the style's block, lit, with a doorway on your side. The room's shape comes from
// global.FA_roomPlan / FA_roomSpot (00_fa_config.js); the carving is spread over ticks (blocksPerTick) so big rooms
// never freeze the server. Bedrock and anything with an inventory or machine inside is never touched.
// Uses faSound from 10_fa_ascension.js (same server scope).

const $RoomBlockPos = Java.loadClass('net.minecraft.core.BlockPos')
const $RoomItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')

// 'kubejs:room_tnt_<style>_<size>' -> { style, size }
let faRoomTypes = {}
global.FA.roomCharges.styles.forEach(st => global.FA.roomCharges.sizes.forEach(sz => {
  faRoomTypes['kubejs:room_tnt_' + st.id + '_' + sz.id] = { style: st, size: sz }
}))

// Rooms waiting for their fuse or being carved
let faRoomJobs = []

ServerEvents.recipes(event => {
  let R = global.FA.roomCharges
  if (!R.enabled) return
  R.styles.forEach(st => R.sizes.forEach((sz, i) => {
    let id = 'kubejs:room_tnt_' + st.id + '_' + sz.id
    let ins = ['minecraft:tnt', st.shell, st.shell, st.shell, st.shell, st.light, st.light]
    if (i > 0) {
      ins = []
      for (let n = 0; n < sz.craft; n++) ins.push('kubejs:room_tnt_' + st.id + '_' + R.sizes[i - 1].id)
    }
    event.shapeless(id, ins).id('kubejs:factory_ascension/' + id.slice(7))
  }))
})

// Which way a player faces, as one of the four directions (Minecraft yaw: 0 south, 90 west, 180 north, 270 east)
function faFacing(yaw) {
  let q = ((Math.round(yaw / 90) % 4) + 4) % 4
  return [[0, 1], [-1, 0], [0, -1], [1, 0]][q]
}

function faWorldHeight(level) {
  try {
    return [level.getMinBuildHeight(), level.getMaxBuildHeight()]
  } catch (err) {
    return [-64, 320]
  }
}

// Arm a charge: clicked block (x, z), floor one below the player's feet, facing the player's way
function faArmRoom(player, level, clicked, stack) {
  let R = global.FA.roomCharges
  let t = faRoomTypes[String(stack.getId())]
  if (!R.enabled || !t) return 0
  let f = faFacing(player.yaw)
  let plan = global.FA_roomPlan(t.size.id, clicked.getX(), Math.floor(player.getY()) - 1, clicked.getZ(), f[0], f[1], t.style.id)
  let hgt = faWorldHeight(level)
  if (plan.floorY < hgt[0] + 1 || plan.floorY + plan.H + 1 >= hgt[1]) {
    player.displayClientMessage(Text.red('Not enough room up or down here for a ' + plan.W + 'x' + plan.L + ' room.'), true)
    return 0
  }
  if (!player.isCreative()) stack.shrink(1)
  let server = level.getServer()
  faRoomJobs.push({ level: level, plan: plan, style: t.style, i: 0, start: server.getTickCount() + Math.round(R.fuseSeconds * 20),
    player: String(player.stringUuid), ores: {}, changed: 0, kept: 0, boomed: false })
  let at = (clicked.getX() + 0.5) + ' ' + (clicked.getY() + 1) + ' ' + (clicked.getZ() + 0.5)
  server.runCommandSilent('execute in ' + level.dimension + ' run playsound minecraft:entity.tnt.primed block @a ' + at + ' 1 1')
  server.runCommandSilent('execute in ' + level.dimension + ' run particle minecraft:smoke ' + at + ' 0.2 0.2 0.2 0.02 20 normal')
  player.displayClientMessage(Text.gold('Fuse lit! ').append(Text.gray('A ' + plan.W + 'x' + plan.L + ' ' + t.style.name + ' room in ' + R.fuseSeconds + 's: stand in the doorway or behind it.')), true)
  return 1
}
global.FA_armRoom = (player, level, clicked, stack) => faArmRoom(player, level, clicked, stack)

BlockEvents.rightClicked(event => {
  let stack = event.player.getMainHandItem()
  if (!stack || stack.isEmpty() || !faRoomTypes[String(stack.getId())]) return
  if (event.hand && String(event.hand) != 'MAIN_HAND') return
  faArmRoom(event.player, event.level, event.block.pos, stack)
  event.cancel()
})

// Carve part of one room: up to `budget` spots. Returns how many were used.
function faCarve(job, budget) {
  let level = job.level, plan = job.plan, R = global.FA.roomCharges
  let states = job.states || (job.states = {
    air: Block.getBlock('minecraft:air').defaultBlockState(),
    shell: Block.getBlock(job.style.shell).defaultBlockState(),
    light: Block.getBlock(job.style.light).defaultBlockState(),
    window: Block.getBlock(job.style.window || job.style.shell).defaultBlockState()
  })
  let used = 0
  while (used < budget && job.i < plan.total) {
    let spot = global.FA_roomSpot(plan, job.i++)
    used++
    if (!spot.kind) continue
    let pos = new $RoomBlockPos(spot.x, spot.y, spot.z)
    let state = level.getBlockState(pos)
    let isAir = state.isAir()
    if (spot.kind == 'air' && isAir) continue
    // Never: bedrock (unbreakable) or anything holding items or running (chests, machines, spawners)
    if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0) {
      job.kept++
      continue
    }
    if (!isAir && R.collectOres && level.getBlock(pos).hasTag('forge:ores')) {
      let id = String(level.getBlock(pos).id)
      job.ores[id] = (job.ores[id] || 0) + 1
    }
    // Flag 2: tell clients, but don't wake neighbors (no sand avalanches or water rushing in mid-build)
    level.setBlock(pos, states[spot.kind], 2)
    job.changed++
  }
  return used
}

function faFinishRoom(job) {
  let level = job.level, server = level.getServer()
  let mid = global.FA_roomMiddle(job.plan)
  let oreCount = 0
  Object.keys(job.ores).forEach(id => {
    let n = job.ores[id]
    oreCount += n
    while (n > 0) {
      let take = Math.min(64, n)
      n -= take
      let e = new $RoomItemEntity(level, mid.x + 0.5, mid.y + 0.5, mid.z + 0.5, Item.of(id, take))
      e.setDefaultPickUpDelay()
      level.addFreshEntity(e)
    }
  })
  let player = false
  server.getPlayerList().getPlayers().forEach(p => { if (String(p.stringUuid) == job.player) player = p })
  if (player) {
    player.tell(Text.green('Room ready: ').append(Text.white(job.plan.W + ' x ' + job.plan.L + ', ' + job.plan.H + ' high, ' + job.style.name))
      .append(Text.gray(' (' + global.FA_num(job.changed) + ' blocks' + (oreCount ? ', ' + oreCount + ' ores dropped in the middle' : '') +
        (job.kept ? ', ' + job.kept + ' left alone: bedrock, chests or machines' : '') + ')')))
  }
}

ServerEvents.tick(event => {
  if (faRoomJobs.length == 0) return
  let server = event.server
  let now = server.getTickCount()
  let budget = Math.max(100, global.FA.roomCharges.blocksPerTick)
  try {
    for (let k = 0; k < faRoomJobs.length && budget > 0; k++) {
      let job = faRoomJobs[k]
      if (job.start > now) continue
      if (!job.boomed) {
        job.boomed = true
        let m = global.FA_roomMiddle(job.plan)
        let at = m.x + ' ' + (m.y + 2) + ' ' + m.z
        server.runCommandSilent('execute in ' + job.level.dimension + ' run playsound minecraft:entity.generic.explode block @a ' + at + ' 4 0.7')
        server.runCommandSilent('execute in ' + job.level.dimension + ' run particle minecraft:explosion_emitter ' + at + ' 2 1 2 0 6 force')
      }
      budget -= faCarve(job, budget)
      if (job.i >= job.plan.total) {
        faFinishRoom(job)
        faRoomJobs.splice(k, 1)
        k--
      }
    }
  } catch (err) {
    console.error('[Factory Ascension] room charge failed: ' + err)
    faRoomJobs.shift()
  }
})

// For /ascension selftest: carve a room right away (no fuse)
global.FA_carveRoomNow = (level, sizeId, styleId, x, floorY, z, fx, fz) => {
  let style = global.FA.roomCharges.styles.filter(s => s.id == styleId)[0]
  let job = { level: level, plan: global.FA_roomPlan(sizeId, x, floorY, z, fx, fz, styleId), style: style, i: 0, player: '00000000-0000-0000-0000-000000000000',
    ores: {}, changed: 0, kept: 0 }
  while (job.i < job.plan.total) faCarve(job, 100000)
  return job
}
global.FA_roomJobCount = () => faRoomJobs.length
global.FA_cancelLastRoom = () => faRoomJobs.pop() ? 1 : 0
