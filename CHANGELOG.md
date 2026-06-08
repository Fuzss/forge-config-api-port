# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v26.1.5-mc26.1.x] - 2026-06-08

### Fixed

- Fix pack metadata max format version

## [v26.1.4-mc26.1.x] - 2026-04-26

### Changed

- Compile for Minecraft 26.1.2

## [v26.1.3-mc26.1.x] - 2026-04-02

### Changed

- Mark as compatible with all 26.1.x versions

## [v26.1.2-mc26.1.x] - 2026-03-28

### Changed

- Enable support for Minecraft Forge

## [v26.1.1-mc26.1.x] - 2026-03-24

### Fixed

- Fix `accessWidener` entry missing from `fabric.mod.json` (ArchLoom used to add that automatically lol)
- Note that access transformers on NeoForge are not yet implemented, but they should not be required

## [v26.1.0-mc26.1.x] - 2026-03-24

### Changed

- Update to Minecraft 26.1
- Note for developers: This project is no longer built using Architectury Loom, having migrated to Fabric Loom and Mod
  Dev Gradle. Please report any issues particularly regarding the `Common` distribution that may arise from that
  migration
