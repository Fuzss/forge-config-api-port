# AGENTS.md

Guidance for agents working in this repository.

## Project

Forge Config API Port provides NeoForge's and Forge's config systems to other modding ecosystems, designed for a
multi-loader architecture where a shared common module is used by several loaders. Gradle subprojects: `Common`,
`Common-ForgeApi`, `Common-NeoForgeApi`, `Fabric`, `Forge`, `NeoForge` (see `settings.gradle.kts`, composed by the
`fuzs.multiloader` convention plugins). Java 25 toolchain; use the Gradle wrapper.

Several modules vendor classes copied from NeoForge / FancyModLoader / Minecraft Forge and keep them in sync
automatically. **Most of the non-obvious work happens in that sync system and its patch conventions.**

## Build and verify

```sh
./gradlew :Fabric:build                 # compile + drift check + jar
./gradlew :Common-NeoForgeApi:build
./gradlew :NeoForge:build
./gradlew :Forge:compileJava
./gradlew :Fabric:compileJava           # fast compile only
./gradlew :Fabric:checkVendoredSources  # vendored-source drift check (part of `check`)
```

Java version comes from the `sharedLibs` version catalog (`java` key). Upstream versions: NeoForge from
`neoforge.version` in `sharedLibs`, FancyModLoader from `fancymodloader` in `gradle/libs.versions.toml`
(`libs`).

## Vendored upstream sources

Two modules currently vendor upstream sources, each as its own self-contained sync unit:

- `Common-NeoForgeApi` — `net.neoforged.neoforge.common.{ModConfigSpec,TranslatableEnum}` and
  `net.neoforged.fml.config.IConfigSpec`.
- `Fabric` — `net.neoforged.fml.config.{ConfigTracker,ConfigWatcher,LoadedConfig,ModConfig,ModConfigs}` and
  `net.neoforged.neoforge.client.gui.ConfigurationScreen`.

Unlike other projects, **the common modules are not relocated**: these classes must keep their exact upstream
packages (`net.neoforged.*`) so mods can use the NeoForge/Forge API across loaders. That is the whole point of
this project. Consequences:

- **Never vendor an extra class just to avoid patching a call site** in these modules. Adding upstream classes
  increases the chance of clashes with other mods that bundle the same classes. Patch the call sites instead.
- The Fabric-only classes (in `Fabric`) can be relocated in a future major, but for now they keep their packages
  too.

The sync is driven by `buildSrc` (`fuzs.multiloader.vendoredsources` package) and configured per module in
`<module>/build.gradle.kts` as a list of `SourceSpec`s (name, version, package root, `relocateTo = null` for
identity, sources jar). Sources configured:

- `neoforge` — `net.neoforged:neoforge:<version>:sources`, package root `net.neoforged.neoforge`.
- `fml` — `net.neoforged.fancymodloader:loader:<version>:sources`, package root `net.neoforged.fml`.

All sync state lives in `<module>/patches/`:

- `patches/manifest` — the source of truth. Format: `<mode> <source> <path>` (or `local <path>`), where `<path>`
  is the **full package path** of the upstream file. Modes:
  - `generated` — fetched from the source's `:sources` artifact; if `patches/<path>.patch` exists it is applied.
    **Never edit these files by hand — edit the patch.**
  - `owned` — hand-maintained file that has an upstream counterpart. Upstream is fetched only to report drift.
  - `local` — hand-maintained file with no upstream counterpart.
- `patches/lock` — pinned source versions (`version.<source>=...`) and upstream hashes for `owned` files. Do not
  edit it by hand.
- `patches/<full/path>.patch` — patches, stored at the full package path (mirroring upstream).
- `patches/README.md` — the detailed operator guide for the sync pipeline (task output, patch authoring steps,
  troubleshooting). Read it before changing the pipeline.

Tasks (per configured module):

- `./gradlew :<module>:syncVendoredSources` — regenerate `generated` sources and update the lock.
- `./gradlew :<module>:checkVendoredSources` — regenerate into `build/`, compare, and fail on any drift.

Updating to a new upstream version:

1. Bump the version in the version catalog (and the matching `SourceSpec.version`).
2. `./gradlew :<module>:syncVendoredSources`
3. Review any patch failures and `owned` drift reports; port manually where needed.
4. `./gradlew :<module>:build`

## Patch conventions

Patches are authored against pristine upstream and stored at the full upstream path
(`patches/<full/path>.patch`, unified diff with `a/` `b/` labels). Keep them minimal and stable:

- **Minimize import changes.** Prefer fully-qualified references for helper types over adding imports; standard
  Fabric/vanilla/JDK types may be imported.
- **Do not make javadoc/comment-only changes.** Dangling `@link`/`@value` warnings are acceptable.
- **Do not include pointless/equivalent rewrites.** Only patch what differs semantically; e.g. do not rewrite
  `Collections.unmodifiableList(...)` to `List.copyOf(...)`, or add a `this.` qualifier upstream does not have.
- Only **remove** imports for types that do not exist on the target platform.
- **Prefer adding methods/classes over patching upstream call sites.** Adding is more stable. Existing examples:
  - `fuzs.forgeconfigapiport.fabric.impl.core.ModConfigEventsHelper` replaces NeoForge's `ModConfigEvent`
    event-bus dispatch.
  - `fuzs.forgeconfigapiport.fabric.impl.config.ModConfigSpecValidator` hosts the spec validation that
    `ModConfigSpec` cannot perform on the common module.
  - Fabric's `net.fabricmc.loader.api.ModContainer` substitutes NeoForge's `net.neoforged.fml.ModContainer`
    (same simple name, so the upstream signatures are preserved; bodies use `getMetadata().getId()/getName()`).
  - `ForgeConfigApiPortConfig` supplies config values NeoForge reads from `FMLConfig`.
- **For large, self-contained removals, comment the block out with `/* ... */` instead of deleting it.** It
  yields a smaller patch whose hunks depend only on the block boundaries (not its body), so upstream edits
  inside the block do not break the patch. Do this only when the block contains no javadoc (`/** ... */`) —
  block comments cannot nest; delete outright in that case. The trade-off is that the generated file keeps the
  code as commented-out.

A new patch is created by diffing a pristine upstream copy against your edited copy, e.g.
`diff -u --label a/<full/path> --label b/<full/path> <upstream> <edited>`. Do not hand-write hunk headers.

## Status

- `Common-NeoForgeApi` (NeoForge API in common) and `Fabric` are migrated to `generated` + patched.
- `Common-ForgeApi` (Forge API shim: `ForgeConfigSpec` + Forge `IConfigSpec`) is still `owned`/deferred.
  Resolving the Forge sources requires a ForgeGradle/mavenizer context (`net.minecraftforge:forge` +
  `net.minecraftforge:fmlcore`), which the common modules do not have.

## Conventions and gotchas

- Do not edit `generated` files (listed as `generated` in the manifest); edit the patch and run
  `syncVendoredSources`.
- Do not commit generated output by hand — always let `syncVendoredSources` write it, then run
  `checkVendoredSources`.
- Keep the `@Deprecated` `String`-based `ConfigurationScreen` constructors overloads alongside the
  `ModContainer` ones so the original API surface stays available.
- `ConfigRegistry` (`fuzs...fabric.api.v5`) is the public entry point and still takes a mod id; the
  `ModContainer` change is internal to the vendored classes.
