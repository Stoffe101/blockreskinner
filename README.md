# Block Reskinner

Block Reskinner is a Fabric client + server mod for Minecraft 1.21.11. It lets players apply server-stored visual skins to blocks without replacing the real world block state.

Example: an obsidian Nether portal frame can be rendered as oak planks for modded clients. The block is still obsidian for mining speed, drops, collision, explosion resistance, portal behavior, redstone checks, and all other server logic.

## Versions

- Minecraft: 1.21.11
- Java: 21
- Fabric Loader: 0.18.4
- Fabric API: 0.141.4+1.21.11
- Loom: 1.17.13
- Gradle wrapper: 9.6.1

## Required On Client And Server

This is a required client + server mod. The server stores and syncs visual overrides. Clients with the mod installed render those overrides. Vanilla clients are not expected to see reskinned blocks.

The mod does not call `world.setBlockState(pos, visualState)` to apply a skin. The real block remains unchanged.

## Using The Wand

Get the wand in creative search or with:

```mcfunction
/give @s blockreskinner:reskin_wand
```

Use it like this:

1. Right-click a supported block with the Reskin Wand.
2. Search or scroll for a visual skin.
3. Pick a skin and press Apply.
4. Shift-right-click a reskinned block with the wand to clear it.

## Skin Modes

Normal full-block mode is used for ordinary cube-like blocks. The Full Blocks selection stays strict: it only includes safe full cube model blocks. Thin, tiny, multipart, block-entity, fluid, redstone, door, trapdoor, fence, wall, pane, bar, and chain-like models are excluded from Full Blocks.

On top of that, a few decorative categories expose safe non-cube visuals:

- **Plants & Leaves** — all leaf blocks plus safe single-block plants (fern, dead bush, short grass, flowers, mushrooms, fungi, saplings, roots and similar cross-model plants). Tall two-block plants such as large fern and tall grass are intentionally unsupported for now: they would need two-block visual skin support, and showing only one half would look broken.
- **Crystals & Buds** — amethyst cluster and the small/medium/large amethyst buds, each selectable in all six facings (Up, Down, North, East, South, West).
- **Skulls & Heads** — vanilla mob heads: Skeleton Skull, Wither Skeleton Skull, Zombie Head, Creeper Head, Piglin Head, and Dragon Head. Floor variants come in four cardinal rotations and wall variants in four facings. Heads have no chunk model, so a dedicated client-only renderer draws the vanilla skull model at skinned positions — no block entity is ever created and the real block is untouched. Player heads are deferred for now: they need GameProfile/profile-component data that a plain block state cannot carry (storing, syncing, and resolving skin textures per player is future work).

Log and pillar entries are shown separately:

- Vertical: axis Y
- East/West: axis X
- North/South: axis Z

Connected block mode is used only when the real clicked block is a supported connected block:

- Fences
- Walls
- Glass panes
- Iron bars

The connected screen shows the current real connection state and a separate visual override. `Auto` follows the real block's connection, `Connected` forces the visual side on, and `Disconnected` forces the visual side off. The real collision and behavior do not change.

## Jade Compatibility

Jade support is optional. If Jade is installed, Block Reskinner adds tooltip lines only for reskinned blocks:

- `Visual Skin: Emerald Ore`
- `Visual Material: Deepslate`
- `Connection Overrides: E Connected, S Disconnected`

Jade still identifies the real block normally. For example, obsidian reskinned to emerald ore still appears as obsidian with an added visual skin line.

For manual Jade testing, place a compatible Jade Fabric jar in `run/mods`, start `./gradlew runClient`, reskin a block, and look at it with Jade enabled. The mod also launches normally without Jade installed.

## Build And Test

Build locally:

```bash
./gradlew clean build
```

Run a local client:

```bash
./gradlew runClient
```

Run a local server:

```bash
./gradlew runServer
```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew`.

## Creating A Release

1. Push `master` or `main`.
2. Go to GitHub -> Actions -> Create Release -> Run workflow.
3. Enter the version without a leading `v`, for example `0.6.0-beta.2`.
4. The workflow creates tag/release `v0.6.0-beta.2` and uploads `blockreskinner-0.6.0-beta.2.jar`.
5. If release creation fails with permission denied, forbidden, or resource not accessible, go to GitHub repository -> Settings -> Actions -> General -> Workflow permissions and enable Read and write permissions, then save. Organization-level Actions settings can override repository settings, so check the organization workflow permissions too if this persists.

## Suggested Test Checklist

1. Place obsidian and reskin it to oak planks or deepslate.
2. Confirm the block visually changes but still mines/drops/behaves as obsidian.
3. Build and light a Nether portal, then reskin the frame.
4. Confirm the portal stays active.
5. Try log and pillar axis variants and confirm the labels are clear.
6. Confirm chain and other unsafe non-full models do not appear in normal full-block mode.
7. Place fences, walls, panes, or iron bars next to connectable neighbors.
8. Open connected mode and confirm current real connections are shown.
9. Change visual connection overrides and confirm only rendering changes.
10. Leave and rejoin the world and confirm saved skins sync back.

## Known Limitations

- Rendering currently targets the vanilla Fabric chunk renderer path. Sodium/Indium compatibility has not been validated.
- Connected rendering uses compatible connected block models and vanilla-like state properties. It does not yet implement arbitrary material-texture remapping.
- Sync currently broadcasts edits to players in the edited dimension instead of narrowing to exact chunk-tracking players.
- The safe visual list is intentionally conservative. An advanced unsafe model mode may be added later.

## Files To Inspect First

- `src/main/java/com/skrra/blockreskinner/item/ReskinWandItem.java`
- `src/main/java/com/skrra/blockreskinner/networking/ModNetworking.java`
- `src/main/java/com/skrra/blockreskinner/skin/SkinQueries.java`
- `src/main/java/com/skrra/blockreskinner/skin/ServerSkinStorage.java`
- `src/client/java/com/skrra/blockreskinner/render/VisualStateResolver.java`
- `src/client/java/com/skrra/blockreskinner/mixin/SectionBuilderMixin.java`
- `src/client/java/com/skrra/blockreskinner/screen/BlockSkinScreen.java`
- `src/client/java/com/skrra/blockreskinner/screen/ConnectedBlockSkinScreen.java`
