# RIPTIDE: KNIFE LAB 4.0 - status and known limitations

## Geometry overhaul (latest)
- First-person emitter: entity render types draw QUADS, but each triangle was submitted as only 3
  vertices, which misaligns every later primitive (the in-game "holes / missing faces"). Each
  triangle is now emitted as a degenerate quad (4 vertices). Not yet confirmed in-game.
- Every blade is now a closed solid: the open root tube is capped and extended as a hidden tang
  that sits inside the guard/handle. Validated: 0 open edges on all 19 meshes.
- Fixed floating/detached parts: Bayonet lug, Skeleton pommel ring (added bottom bar), Shadow
  Daggers (punch bar was not attached to the plate), Gut Knife (blade spine started outside the
  guard), Survival serrations, and blade-to-guard gaps on M9, Bowie, Survival, Butterfly, Flip,
  Huntsman, Navaja, Talon, Ursus, Paracord, Karambit.
- Boxes and handles now have chamfered edges (octagonal sections) instead of sharp cubes.
- Validators used: open-edge count, per-shell orientation (signed volume), part connectivity,
  and blade-root-embedded test. Normals remain flat per triangle (no smoothing).
- A Gradle build / JAR could not be produced in the authoring sandbox (no network). Run the
  included GitHub Actions workflow or `gradle build` locally.

## What has actually been verified
- The pure-Java core (geometry, skin painter, animator, rasterizer, registries) compiles with
  `javac` on JDK 21 with no warnings, and was run. All 19 knife meshes build with no NaNs, and
  all 22 finishes render and visibly change the blade.
- The Minecraft-facing code has NOT been compiled, and nothing has been run inside Minecraft.
  The authoring sandbox could not reach Fabric's Maven or metadata servers (HTTP 403), so Loom,
  Minecraft 1.21.11 and Fabric API could not be downloaded. The only check is a syntax pass in
  which every remaining error was a missing Minecraft symbol.
- No JAR has been built. The first real build is the GitHub Actions run.

## Unverified items (fix these first if CI fails)
1. `gradle.properties`: `loader_version`, `fabric_api_version`, `loom_version` are best guesses.
   CI fails with a clear message if the loader or Fabric API version does not exist. Copy the correct
   values from https://fabricmc.net/develop. Confirm that Loom `1.14-SNAPSHOT` resolves, or pin the
   published 1.14.x release.
2. `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` are not in the repo (they could not be generated
   offline). CI generates them with Gradle 9.2.0. To commit them yourself, run `gradle wrapper
   --gradle-version 9.2.0` inside `riptide-knife-lab/`.
3. 1.21.11 API names written from memory of the 1.21.9+ rendering rewrite. All are isolated in
   `render/McCompat.java`, `mixin/ItemInHandRendererMixin.java` and `input/KnifeKeys.java`:
   - `net.minecraft.resources.Identifier` (the 1.21.11 rename of ResourceLocation)
   - `net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutNoCull(Identifier)`
   - `SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`
   - `DynamicTexture(Supplier<String>, NativeImage)`, `NativeImage.setPixel` pixel order (flag
     `PIXEL_IS_ARGB`; if red and blue look swapped in game, flip it)
   - `GuiGraphics.blit(RenderPipelines.GUI_TEXTURED, ...)` 12-argument overload
   - `KeyMapping.Category.register(Identifier)` and the 4-argument `KeyMapping` constructor
   - `ItemInHandRenderer.renderArmWithItem(...)` parameter list in the mixin (a wrong list makes
     the mixin fail at startup with a clear error naming the real signature)
   - `Inventory.getSelectedSlot()`, `SimpleSoundInstance.forUI(SoundEvent, float, float)`

## Features implemented (in source)
- 19 knives, each with its own procedural geometry, blade profile, handle, guard and pommel.
- 22 original procedural finishes with selectable variants (Doppler and Gamma Doppler phases, etc.).
  Skins are painted at runtime; no image assets and no extracted game assets are shipped.
- First-person replacement of the held sword's visual only (mixin cancels the draw call). No change
  to damage, speed, reach, hit registration, item identity or networking. Left-handed mirroring.
- Frame-time animations: draw, switch-in, idle, inspect, swing; knife-specific draw/inspect for
  Butterfly (handles close, flip, reopen), Karambit (spin about the ring), M9 Bayonet (slow full
  turn), Skeleton (tilt through the open frame, flick), Shadow Daggers (dual, cross on inspect).
- Knife Lab screen (J): search, categories, favorites, recents, software-rendered 3D preview with
  rotate/zoom/auto-spin, finishes and variants, Equip/Inspect/Randomize/Favorite/Reset/Settings/Done.
- Settings screen and persistent JSON config with validation; keybinds J and G through Minecraft's
  Controls screen.

## Known limitations
- Sounds reuse vanilla sound events with changed pitch/volume. No custom audio was created.
- Weapon switching animates the knife coming in. Holstering the old item is vanilla's own render.
- The GUI uses vanilla buttons over custom dark panels, so it is less polished than the specified
  premium design. There are no smooth transitions or custom icons.
- Preview is a CPU rasterizer (about 30 fps cap, only while the GUI is open), not GPU-lit.
- In-world lighting is baked per triangle from a fixed key light plus vanilla light level.
- No runtime testing of any kind has happened: not in-game rendering, not keybinds, not config
  persistence across restarts, not the mixin target.
