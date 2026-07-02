# Block Reskinner

Block Reskinner is a Fabric client + server mod for Minecraft 1.21.11. It lets a player apply a server-stored visual skin to a block without replacing the real world block state.

Example: reskin an obsidian portal frame as oak planks. The block still behaves, mines, drops, resists explosions, and participates in portal logic as obsidian. Clients with the mod installed render the visual override; vanilla clients see the original block.

## Versions

- Minecraft: 1.21.11
- Java: 21
- Fabric Loader: 0.18.4
- Fabric API: 0.141.4+1.21.11
- Loom: 1.17.13
- Gradle wrapper: 9.6.1

## Run

```bash
./gradlew build
./gradlew runClient
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Server launch is also configured:

```bash
./gradlew runServer
```

## How To Test

1. Start a dev client with `./gradlew runClient`.
2. Create or enter a world and give yourself `blockreskinner:reskin_wand`.
3. Right-click a supported block with the wand.
4. Search for a visual block skin and press Apply.
5. Shift-right-click the same block with the wand to clear it.

For multiplayer testing, run a Fabric server with this mod and Fabric API installed. Every client that should see reskins also needs the mod installed. Vanilla clients can join only if your server setup permits them, but they will not see visual overrides.

## Known Limitations

- Rendering is implemented through a vanilla `BlockRenderManager.renderBlock` mixin. This works for the vanilla Fabric renderer path, but Sodium/Indium compatibility has not been validated.
- Sync currently broadcasts edits to players in the edited dimension instead of narrowing to exact chunk-tracking players.
- Connected blocks support fences, walls, panes, and iron bars as a separate category, but the MVP uses vanilla model states and simple connection overrides.
- The allowed visual block list is conservative. Blocks with block entities, fluids, special renderers, redstone-like behavior, plants, doors, trapdoors, and other risky shapes are intentionally excluded.

## Files To Inspect First

- `src/main/java/com/skrra/blockreskinner/item/ReskinWandItem.java`
- `src/main/java/com/skrra/blockreskinner/networking/ModNetworking.java`
- `src/main/java/com/skrra/blockreskinner/skin/ServerSkinStorage.java`
- `src/client/java/com/skrra/blockreskinner/render/VisualStateResolver.java`
- `src/client/java/com/skrra/blockreskinner/mixin/BlockRenderManagerMixin.java`
- `src/client/java/com/skrra/blockreskinner/screen/BlockSkinScreen.java`
