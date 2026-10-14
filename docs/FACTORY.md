# Building the ore line

Most mods want you to hunt for ore. This pack makes every important material renewable
from blocks a cobble generator makes forever, so materials scale with factory size.
Build the line once and only add more copies, never redesign it.

## Raw blocks (infinite)

| Block | How |
|---|---|
| Cobblestone | Vanilla cobble generator + Create mechanical drill |
| Gravel | Crush cobblestone (crushing wheels or millstone) |
| Blackstone | Lava over soul soil next to blue ice, mined by a drill |
| Netherrack | Drill contraptions in the Nether (it never runs out in practice) |
| Quartz | Wash soul sand with an encased fan through water |
| Lava | Infinite with a lava cauldron farm under dripstone, or pump a lava lake |

## Clusters (Create mixer over a basin)

| Cluster | Recipe | Heat | Crush for |
|---|---|---|---|
| Common | 4 gravel + 100 mB lava | Heated | crushed iron (100%), copper (75%), zinc (60%), gold (35%) |
| Gem | 4 cobblestone + 1 quartz + 250 mB lava | Superheated | 2 redstone, lapis (75%), diamond (12%), emerald (8%) |
| Exotic | 4 blackstone + 1 redstone + 250 mB lava | Superheated | Mekanism / TFMG metals, first 4 found |
| Ancient debris | 4 netherrack + 2 gold + 1 diamond + 1000 mB lava | Superheated | (gives ancient debris directly) |

Crushed ores go through Create's normal washing (encased fan through water) for nuggets.

## One line, many modules

```
cobble gen + drills -> belt -> brass tunnel group (Round Robin)
   -> mixer modules (one per cluster type)
   -> crushing wheels -> washing fans
   -> brass funnels with filters sort each metal -> Item Vaults
```

- Every module is identical, so save one with a Schematic and Quill and paste copies.
  Brass tunnels on neighbouring belts link up and share the input on their own.
- Need more of something? Paste another module. The design never changes.
- Item Vaults at the end act as the buffer. When one fills, the line pauses safely.

## Ascension Core line

```
diamonds + polished rose quartz + lava (+ TFMG steel)
   -> sequenced assembly: deploy quartz -> (deploy steel) -> fill 250 mB lava -> press, 3 loops
   -> Ascension Core -> compact 9 (heated) -> Dense -> compact 9 (superheated) -> Singularity
```

Polished rose quartz: mix redstone + quartz into rose quartz, then deployers with sandpaper polish it.
