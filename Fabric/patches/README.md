# Vendored source patches

This directory is the source of truth for the upstream loader classes that the `Fabric` module vendors. It
lets them be regenerated from upstream by applying small, reviewable patches instead of maintaining copies by
hand.

Everything is operated through two Gradle tasks. You need only the Gradle wrapper, a `patch` binary
(macOS/Linux provide one), and network access to resolve the sources jars (cached after the first run).

## Pipeline overview

```
   net.neoforged:neoforge:<version>:sources             (package root net.neoforged.neoforge)
   net.neoforged.fancymodloader:loader:<version>:sources (package root net.neoforged.fml)
                          |
                          v
                  extract into build/vendored-sources/upstream/<source>/
                          |
            manifest entry? | apply patches/<full/path>.patch (if present)
                          v
        relocate `net.neoforged` -> `fuzs.forgeconfigapiport.fabric`
                          v
   src/main/java/<relocated path>   (committed)
```

1. Each configured source jar is extracted to a scratch directory.
2. Each `generated` file listed in `manifest` is copied from the extracted upstream, its patch (if any) is
   applied with `patch -p1`, relocated (see [Relocation](#relocation)), and then written out.
3. The result is written into the module source tree; commit it like any other generated file.
4. `checkVendoredSources` performs the same steps into a scratch directory and fails if the committed sources
   differ.

## Files in this directory

- `manifest` — lists every vendored file and how it is handled. **Source of truth.**
- `lock` — records the pinned source versions and the upstream hashes of `owned` files. Generated; do not edit
  by hand.
- `<full/path>.patch` — a unified diff for one file, stored at the file's **full package path** (mirroring
  upstream), e.g. `net/neoforged/fml/config/ModConfigs.java.patch`.

## Gradle tasks

Run from the repository root.

```sh
# Regenerate the vendored sources and refresh the lock file.
./gradlew :Fabric:syncVendoredSources

# Verify the committed sources match a fresh sync (also runs as part of `check`/`build`).
./gradlew :Fabric:checkVendoredSources

# Full build (compiles, runs checkVendoredSources, Spotless, jars).
./gradlew :Fabric:build
```

`syncVendoredSources` prints one line per manifest entry, tagged `[patched]`, `[verbatim]`, `[unchanged]` or
`[changed]`, plus a summary. Add `--info` to also see the raw `patch` output for each patched file.

`checkVendoredSources` is quiet on success (`Vendored sources are in sync.`). On failure it lists every problem
and tells you to run the sync task.

Scratch output (safe to delete, not committed):

- `build/vendored-sources/upstream/<source>/<full/path>` — pristine extracted upstream (use this when authoring
  patches).
- `build/vendored-sources/staging/<full/path>` — upstream with the patch applied.
- `build/vendored-sources/owned-upstream/<full/path>` — copy of an `owned` file's upstream counterpart, written
  when it changes so you can review it.

## Configuration

The sync is configured in `build.gradle.kts` as a list of `SourceSpec`s passed to the tasks:

```kotlin
SourceSpec(
    name = "neoforge",                              // referenced by manifest entries
    version = neoforgeVendoredVersion,              // recorded in lock
    packageRoot = "net.neoforged.neoforge",         // dot-form upstream package root
    relocateTo = "$vendoredPackagePrefix.neoforge", // dot-form target package
    sourcesJar = neoforgeVendoredSources.singleFile,
)
```

Two sources are configured: `neoforge` (`net.neoforged.neoforge`) and `fml`
(`net.neoforged.fml`, from the FancyModLoader `loader` sources artifact). Versions come from the shared
catalog (`neoforge.version`) and this project's catalog (`fancymodloader`).

## Relocation

Unlike `Common-NeoForgeApi`, the `Fabric` module does not keep the upstream `net.neoforged.*` packages. It drops
the `net.neoforged` prefix and keeps the rest, so both upstream roots stay separate:

- `net.neoforged.fml.*` → `fuzs.forgeconfigapiport.fabric.fml.*`
- `net.neoforged.neoforge.*` → `fuzs.forgeconfigapiport.fabric.neoforge.*`

Relocation is **manifest-driven**, not a blind prefix replace: `VendoredSources.relocationMap` derives the
`old FQN -> new FQN` pairs from the manifest entries and rewrites exactly those references (plus each file's own
`package` declaration). Everything else is left alone, which matters because some `net.neoforged.*` classes are
*not* vendored here:

- `IConfigSpec`, `ModConfigSpec` and `TranslatableEnum` live in `Common-NeoForgeApi` (shared with `Forge`) and
  stay in `net.neoforged.*`. A relocated file that uses one of them needs an **added import** (e.g.
  `import net.neoforged.fml.config.IConfigSpec;`), because it is no longer in the same package.
- References between sources are handled as well: `ConfigurationScreen` (source `neoforge`) importing a class
  from source `fml` is rewritten to the `fabric.fml.*` target.

## `manifest` format

One entry per line; `#` starts a comment; blank lines are ignored. Whitespace-separated.

```
<mode> <source> <path>     # generated and owned
local <path>               # local (no upstream counterpart)
```

- `<source>` — the `SourceSpec.name` the file comes from.
- `<path>` — the **full package path** of the upstream file, e.g.
  `net/neoforged/fml/config/ModConfigs.java`.

Modes:

- **`generated`** — fetched from upstream, patch applied if `patches/<path>.patch` exists, then written to the
  source tree. Never edit these files by hand; edit the patch instead.
- **`owned`** — a hand-maintained file that has an upstream counterpart. The sync never writes it; it only
  fetches upstream to report drift (its hash is recorded in `lock`).
- **`local`** — a hand-maintained file with no upstream counterpart. Ignored by the sync.

## `lock` format

```
version.<source>=<version>            # pinned upstream version per source
<full/path>=<sha256>                  # upstream hash of each `owned` file
```

Generated by `syncVendoredSources`. It must be committed. `checkVendoredSources` fails if it is missing, if a
source's version changed, or if an `owned` file's upstream hash changed.

## Authoring and editing patches

Patches are plain unified diffs applied with `patch -p1`, so the labels must be `a/<full/path>` and
`b/<full/path>`. Always author them against **pristine upstream**, never against the copied file.

1. Populate the upstream sources (any sync run does this):

   ```sh
   ./gradlew :Fabric:syncVendoredSources
   ```

2. Copy the pristine upstream file to a scratch location (keep the upstream copy untouched):

   ```sh
   P=net/neoforged/fml/config/ModConfigs.java
   cp "build/vendored-sources/upstream/fml/$P" /tmp/ModConfigs.java
   ```

3. If a patch already exists, apply it to the scratch copy so you start from the current edits instead of
   reproducing them by hand:

   ```sh
   patch /tmp/ModConfigs.java "patches/$P.patch"
   ```

   `patch` prints `patching file ...` and exits `0` on success. This is the same operation the sync performs.
   It fails when the upstream file changed since the patch was written; in that case `patch` writes a `.rej`
   file with the rejected hunks next to the scratch copy. Apply those hunks manually, or fall back to editing
   the pristine file.

4. Edit the scratch copy.

5. Produce the patch with matching labels and place it at the full package path:

   ```sh
   mkdir -p "patches/$(dirname "$P")"
   diff -u --label "a/$P" --label "b/$P"      "build/vendored-sources/upstream/fml/$P" /tmp/ModConfigs.java      > "patches/$P.patch"
   ```

6. Regenerate and validate:

   ```sh
   ./gradlew :Fabric:syncVendoredSources :Fabric:checkVendoredSources
   ```

### Patch conventions

Keep patches minimal and stable; they are re-applied on every upstream update, so small diffs conflict less:

- **Keep import changes minimal.** Reference `fuzs.*` helper types fully-qualified instead of importing them.
  Standard Fabric/vanilla/JDK types and `net.neoforged.*` types that stay in place may be imported.
- **Do not make javadoc/comment-only changes.** Dangling `@link`/`@value` warnings are acceptable.
- **Add** an import for a `net.neoforged.*` class that stays in place when relocation moves the file out of the
  shared package (e.g. `IConfigSpec`); otherwise only **remove** imports for types that do not exist on the
  target platform.
- **Prefer adding a method or class over patching a call site.** Adding is more stable than patching.
- **For large, self-contained removals, comment the block out with `/* ... */` instead of deleting it.** It
  yields a smaller patch whose hunks depend only on the block boundaries (not its body), so upstream edits
  inside the block do not break the patch. Do this only when the block contains no javadoc (`/** ... */`) —
  block comments cannot nest; delete outright in that case. The trade-off is that the generated file keeps the
  code as commented-out.
- **Do not vendor extra classes just to avoid patching call sites.** The shared `Common-NeoForgeApi` classes keep
  their original packages, so adding more of them increases the chance of clashes with other mods that bundle
  the same classes. Patch the call sites instead.
- A patch must not contain a package rename; relocation happens after the patch is applied.

## Common operations

### Add a new vendored file

1. Add a `generated <source> <full/path>` line to `manifest` (or `owned`/`local`).
2. Optionally author a patch (see above).
3. Run `./gradlew :Fabric:syncVendoredSources :Fabric:checkVendoredSources`.

### Update the upstream version

1. Bump the version in the version catalog (`neoforge.version` in the shared catalog, or `fancymodloader` in
   `gradle/libs.versions.toml`).
2. `./gradlew :Fabric:syncVendoredSources`
3. If a patch no longer applies, the task aborts and prints the raw `patch` output; re-author the patch
   against the new upstream. `[changed]` owned files must be reviewed in
   `build/vendored-sources/owned-upstream/<path>`.
4. `./gradlew :Fabric:build`

## Design notes

A few substitutions recur across the vendored Fabric config classes:

- NeoForge's `net.neoforged.fml.ModContainer` is replaced by Fabric's
  `net.fabricmc.loader.api.ModContainer` (same simple name, so the upstream signatures are preserved). Its methods map
  as `getModId()` -> `getMetadata().getId()` and `getModInfo().getDisplayName()` -> `getMetadata().getName()`.
  `ConfigRegistry.register(...)` still takes a mod id and resolves the container internally.
- NeoForge's `ModConfigEvent` event-bus dispatch is replaced by the Fabric callbacks in
  `fuzs.forgeconfigapiport.fabric.impl.core.ModConfigEventsHelper`.
- Spec validation that `ModConfigSpec` cannot host on the common module lives in the local helper
  `fuzs.forgeconfigapiport.fabric.impl.config.SpecValidator`.
- The config-sync network classes (`ConfigSync`, `SyncConfig`, `ICustomConfigurationTask`, `ConfigFilePayload`) are
  vendored too; their loader-only bits live in Fabric helpers outside the mirror:
  `fuzs.forgeconfigapiport.fabric.impl.network.FriendlyByteBufHelper` (`UNBOUNDED_BYTE_ARRAY`, from
  `NeoForgeStreamCodecs`) and `...ConfigSyncHelper` (`handleClientLoginSuccess`, which has no NeoForge counterpart).
- `CommonConfig` mirrors `FMLConfig` (nested `ConfigValue<T>`, same entry names, ordered `ConfigSpec`), but
  reads through Fabric paths.

## Rules of thumb

- `patches/` is the source of truth; the vendored files under `src/main/java/` are generated. Never edit them by
  hand — edit the patch and run `syncVendoredSources`.
- Always commit the regenerated sources, `manifest`, `lock` and any patch changes together.
