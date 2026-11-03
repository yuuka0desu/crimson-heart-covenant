# Player Maid × Needs of Nature Compat Changelog

## 1.0.0

- Added an independent compatibility mod for Player Maid 2.1.0, Needs of Nature 1.2.0, and Animation Framework 1.2.0.
- Preserves an existing Needs of Nature gender choice and initializes `NONE` player maids as `FEMALE` once.
- Pauses automatic work, range enforcement, teleportation, and native Maid GUI opening while AFW/NoN animations are pending or active.
- Prioritizes empty-hand NoN animation joining over opening the native Maid GUI.
- Maps accepted Player Maid interactions to configurable `playermaid_<action>` AFW tags and falls back to YSM when no matching animation starts.
- Adds `/maidnature status [player]` for permission-aware Needs of Nature state diagnostics.
- Adds server configuration for each compatibility behavior.
