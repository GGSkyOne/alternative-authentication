# Changelog

All notable changes to this project will be documented in this file.

---

## [1.3.1] - 2026-06-20

- Added `ChatSessionUpdateMixin` to prevent players with third-party authentication from being kicked for invalid profile key signatures.
- Bumped to 1.3.1.

---

## [1.3.0] - 2026-06-20

- Updated to support Minecraft 26.2 ("Chaos Cubed").
- Bumped Fabric Loader to 0.19.3, Fabric API to 0.152.2+26.2, Fabric Loom to 1.17-SNAPSHOT.
- Bumped Java compile target from 21 to 25.
- Bumped Gradle wrapper from 9.4.1 to 9.5.1.
- Fixed `fabric.mod.json` dependency version constraints.
- Added new `preventFallbackIfPlayerExists` option.
- Added config version system and reworked config related stuff.
- Added more detailed debug logs.
- Internal refactors and improvements.

--- 

## [1.2.0] - 2025-12-29

- Updated to support 1.21.5-1.21.11.
- Fixed whitelist functionality that was broken since 1.21.5.
- Fixed checking authentication functionality that was broken since 1.21.9.
- Internal refactors and performance improvements.
- Bumped to 1.2.0

---

## [1.1.1] - 2025-02-19

- `property_url` functionality has been returned since its absence broke skins for Ely.by players.
- Bumped to 1.1.1

---

## [1.1.0] - 2025-02-19

- Backported to support 1.14-1.21.4
- Internal refactors and performance improvements
- Bumped to 1.1.0

---
 
## [1.0.0] - 2024-09-26

- Initial release.