# Changelog

## 2.0.0

### Highlights

- Added bidirectional covenant support between players and Touhou Little Maid entities.
- Added hierarchical servant relationships with inherited management permissions.
- Added automatic task synchronization from Maid entities to subordinate player maids.
- Fixed the client startup crash caused by the unregistered `playermaid:gui_maid_proxy` entity type.

### Maid Entity Covenants

- Players can now hold a cake, sneak, and right-click a tamed Touhou Maid to become that Maid entity's player maid.
- Entity covenant creation validates existing managers, tame state, relationship depth, and relationship cycles.
- Successful covenants initialize player maid settings, play feedback effects, notify the Maid's player owner, and synchronize the Maid's current task immediately.
- Entity contracts are released when the direct Maid manager dies, subject to server configuration.

### Hierarchical Management

- Added a persistent relationship graph supporting both player and Touhou Maid nodes.
- A player who owns a Maid entity inherits management access to that Maid's subordinate player maids.
- Management permissions propagate through deeper player-maid hierarchies while direct ownership remains distinct.
- Inherited managers can manage inventory, tasks, follow/stay state, recalls, activity range, schedules, GUI settings, Curios, baubles, beds, Soul Talismans, and servant bells.
- Releasing a covenant, romance interactions, marriage, and bond progression remain limited to direct player-to-player relationships.
- Added cycle detection, configurable maximum hierarchy depth, offline relationship persistence, and migration of legacy 1.0 player relationships.

### Task Synchronization

- Added server-side monitoring of Touhou Maid task changes through `MaidTickEvent`.
- Maid tasks now propagate automatically to every online subordinate player maid in the relationship tree.
- Synchronized tasks store their complete namespaced task ID, source Maid UUID, display name, and synchronization time.
- Supported built-in tasks map to player work modes and automatically enable work execution.
- Unknown or third-party tasks retain their complete ID and display a compatibility notice instead of being mapped incorrectly.
- Updated the maid GUI snapshot protocol to version 2 and added full task IDs to task displays.

### GUI and Networking

- Registered `ModEntities` on the mod event bus before client renderer setup.
- Registered attributes for the temporary GUI Maid proxy entity.
- Registered the player maid network channel during common initialization.
- Added an explicit diagnostic if the GUI proxy entity type is missing during client renderer registration.
- Added a per-target native GUI session lock to prevent simultaneous inherited managers from overwriting a player maid's inventory.
- Updated menu, network packet, native GUI, and container validity checks to use hierarchical server-authoritative permissions.

### Player Maid Behavior

- Player maids can now follow a living Maid entity as their direct manager.
- Teleport, feeding, healing, and extinguishing behavior now supports both player and Maid entity managers.
- Status output identifies the direct manager type and top-level player manager.
- Online maid lists distinguish direct and inherited relationships.

### Configuration

Added the following server options:

- `entityMaidContracts`
- `inheritManagement`
- `syncMaidTasks`
- `clearContractsOnMaidDeath`
- `maxRelationDepth`

### Testing

- Added JUnit 5 support.
- Added relationship graph tests for player hierarchies, Maid entity bridges, inherited permissions, cycle prevention, maximum depth, and permission revocation.
- Verified with `./gradlew clean build` on Minecraft 1.20.1 / Forge 47.3.22.
