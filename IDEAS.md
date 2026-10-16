# Ideas for later

Ideas the player asked to keep, with what was asked for word for word where it matters. Nothing here is built yet.

## Vehicle builder (start with a dirt bike)

**Started (v25):** the dirt bike is in, with swappable engines (125/250/450/V12), frames (standard/long), gearboxes and fuels,
each part its own mesh. Still to come: more vehicles, a garage screen, suspension/wheel/exhaust parts.

**Build a dirt bike first.**

A full vehicle builder, like the weapon customisation: assemble a vehicle from parts, and every part changes how it drives.

- **Parts:** engine, gearbox, fuel type, frame (and later wheels, suspension, exhaust...).
- **Stats they change:** top speed, torque, acceleration, fuel efficiency (and weight, handling).
- **The model changes with the parts:** a bigger engine is visibly bigger, a longer frame is longer.
- **Crazy builds are allowed:** for example, put a **V12 in the dirt bike**, and **modify the frame to be bigger** so the V12 fits.
- Fits the pack: engines and gearboxes could come from the factory (Modular Engine parts), fuel from Create/TFMG fluids.

Notes for building it: a vehicle entity whose model is assembled from part models (like the 3D block models), stats computed from
the parts, a workbench screen like the Ascension Workbench, and a test track in the self-test.

## Tarkov-style inventory with a diegetic duffel bag

Replace the inventory grid with a **midnight-dark UI like Escape from Tarkov**:

- **Items take more than one slot** (a rifle 4x1, a backpack 3x3...), so space is a puzzle.
- **Press R to rotate** an item while dragging it, to make it fit.
- **Diegetic UI transition** when opening it: the player looks down, grips a **duffel bag**, **unzips it**, and the inventory
  fades in, starting as a small item barely visible inside the bag that grows and fades in until it fills the screen.
- **More Tarkov mechanics**: rigs and containers with their own grids, searching containers, inspecting unknown items,
  extraction-style raids, and a **flare gun** (signal flares; could tie into world events or calling for help).

Notes for building it: a custom container screen and slot system (multi-cell items with rotation saved in NBT), a first-person
animation layer for the bag, and compatibility with Sophisticated Backpacks and the Universal Storage Terminal. The flare gun
can reuse the shotgun's gun code (aiming, firing, ammo).
