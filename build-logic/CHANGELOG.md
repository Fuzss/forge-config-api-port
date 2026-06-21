# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.1.16] - 2026-06-21

### Changed

- Change `Trinkets` mod metadata to link to `Trinkets Updated` instead

## [v1.1.15] - 2026-06-19

### Fixed

- Fix Gradle version breaking backward compatibility

## [v1.1.14] - 2026-06-18

### Changed

- Downgrade Loom again to stay compatible with older Gradle versions

## [v1.1.13] - 2026-06-18

### Changed

- Revert disabling `validateAccessTransformers` on NeoForge

## [v1.1.12] - 2026-06-18

### Changed

- Disable `validateAccessTransformers` on NeoForge, as it breaks the creation of game artifacts

### Fixed

- Fix empty lines in generated access transformers

## [v1.1.11] - 2026-06-18

### Changed

- Update Gradle, Loom, and Mod Publish Plugin
- Migrate to Loom's new property-based `RunConfiguration` API

### Fixed

- Fix `ClassTweakerTransformation` stripping injected interface and enum extension lines on Fabric

## [v1.1.10] - 2026-04-27

### Fixed

- Fix GitHub commitish

## [v1.1.9] - 2026-04-26

### Changed

- Update Mod Publish Plugin to `2.0.0-beta.1`
- Create tag for GitHub releases from the current version branch instead of `main`
- Turn off the `isDisableRecompilation` flag for the common subproject

## [v1.1.8] - 2026-04-26

### Added

- Add `Project.packageName` property

## [v1.1.7] - 2026-04-24

### Fixed

- Fix mod artifacts missing from `accessTransformers` configuration

## [v1.1.6] - 2026-04-24

### Changed

- Set `validateAccessTransformers` to `false` by default in Mod Dev Gradle
- The Tiny Takeover Spotless task now migrates common packages from Puzzles Lib

## [v1.1.5] - 2026-04-20

### Changed

- Set caching strategies for custom tasks

### Fixed

- Fix `runtimeOnly` and `modRuntimeOnly` configurations on NeoForge

## [v1.1.4] - 2026-04-15

### Added

- Add a basic Spotless task for Tiny Takeover

### Changed

- Update to Fabric Loom 1.16
- Remove the project name from publication display names
- Point the common Mixin package to the updated location

### Fixed

- Fix `accesstransformer` publications by enabling the `GenerateModuleMetadata` task

## [v1.1.3] - 2026-04-02

### Changed

- Add `minecraft` version to `metadata.json`

## [v1.1.2] - 2026-04-02

### Changed

- Relax Minecraft version constraints for published artifacts and when uploading
- Set additional CurseForge metadata when uploading

## [v1.1.1] - 2026-03-31

### Fixed

- Fix NeoForge runs

## [v1.1.0] - 2026-03-24

### Changed

- Replace Architectury Loom with Loom and Mod Dev Gradle
