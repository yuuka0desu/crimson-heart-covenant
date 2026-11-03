# Changelog

## 2.1.3

Maid entity friendly-fire fix.

### Fixed

- Fixed Touhou Little Maid entity (`EntityMaid`) attacks being able to damage their own owner. The friendly-fire guard in `LivingAttackEvent` only matched `ServerPlayer` attackers, so damage coming from a maid entity was skipped entirely — including arrows the maid had fired earlier, which is why those arrows could still kill the owner after the maid died.
- Melee hits and projectiles originating from a maid entity are now cancelled when the victim is that maid's owner, together with a short action-bar notice.
- The new check is still gated by `preventFriendlyFire`, so packs that intentionally allow friendly fire keep the previous behaviour.

### Notes

- No configuration keys or network protocol changes in this release; existing `2.1.2` worlds and clients only need the mod jar updated.
- Server and client must still run the same version because the mod declares `displayTest="MATCH_VERSION"`.

## 2.1.2

Addon accessory compatibility.

### Added

- Added a compatibility layer that runs Touhou Little Maid bauble hooks for player maids. Touhou Little Maid only invokes `IMaidBauble` on `EntityMaid` instances, so accessories registered by addon mods could be placed in a player maid's bauble slots but their behaviour never executed. The new layer dispatches `onTick`, `onInjured`, `onDeath`, `onPutOn`, and `onTakeOff` for any bauble resolved through `BaubleManager.getBauble(ItemStack)`, which covers every addon that registers through the official TLM API.
- Hook dispatch runs against a detached `EntityMaid` proxy that mirrors the player maid's position, rotation, tick count, health, potion effects, and burning state. Potion effects and burning applied by a hook are copied back to the real player, so accessories that buff the wearer actually take effect.
- Bauble equipment changes are tracked per slot, firing `onTakeOff` for the removed item and `onPutOn` for the new one in the same order the vanilla implementation uses.
- `onInjured` receives a mutable damage value, so damage-modifying accessories work and cancelling accessories still cancel the hit. `onDeath` can veto a lethal hit.
- Verified coexistence with the accessory registrations of Heart Pact (`GrowthPauseHairpinBauble`), Maid Beacon (`BeaconEffectGeneratorBauble`), and SolMaid.
- Added an `addonBaubleCompat` option (default `true`) to disable the layer for packs that prefer the previous behaviour.

### Compatibility notes

- Baubles in the `touhou_little_maid` namespace are skipped by the new layer. Touhou Little Maid's own accessories remain handled by the existing native implementation, so no effect runs twice.
- The layer is applied per bauble with isolated error handling; a misbehaving addon accessory logs a single warning instead of interrupting the server tick.
- Mirrored proxies are released when the owner logs out or when the player maid is cloned on death.
- Addons that query their own state through helpers such as `isWornBy(EntityMaid)` from their own event handlers only ever see `EntityMaid` instances. Their TLM bauble hooks now run, but those self-queries cannot observe a player maid without changes on their side.

## 2.1.1

### 修复内容（中文）

- 修复 TACZ 枪械无法自动射击。此前每 tick 都会重新调用举枪动作，导致 `shoot` 始终返回 `IS_DRAWING`，女仆一直停在等待状态，同时 TACZ 也会拒绝玩家的手动射击。现在每轮部署只举枪一次，射击的俯仰角与偏航角参数也改为与 TACZ 签名一致的顺序。
- 新增每名女仆独立的 TACZ 自动开火开关。玩家女仆可用「切换 TACZ 枪械自动开火」按键（默认 `V`）在自动与手动之间切换，原生面板的配置页也提供同一开关。
- 修复兼容模组物品被原生女仆面板抢占。手持心契同眠的工具（眷恋测试工具、女仆成长工具、花卉测试工具等）右键时不再只弹出女仆界面，而是交回对应模组处理，好感度可以正常增加。
- 修复原生面板开启期间的物品复制与槽位覆盖。背包回写不再每 tick 执行，手持物品改为写回读取时的那个槽位，而不再写入当前选中的快捷栏槽位，滚轮切换物品不会再复制或覆盖其它物品。
- 修复友伤被拦截时女仆仍然累积怒气的问题。怒气现在只在实际造成伤害时累积。
- 新增 `strikeEnabled` 选项（默认 `true`）。设为 `false` 后情绪值仍会正常显示，但罢工不会再中断自动工作。
- 修复玩家女仆射出的投射物在关系无法解析后仍然伤害主人，包括箭矢从主人身上反弹后击中女仆本人的情况。投射物现在会在发射时记录主人，友伤保护不再依赖飞行途中的实时关系。
- 自动远程攻击在主人位于弹道范围内时会跳过本次射击，避免箭矢擦过主人。
- 修复原生面板的代理实体被渲染到世界中，导致主人打开界面时同时看到两名女仆。
- 修复玩家女仆无法驾驶车万女仆扫帚飞行的问题。扫帚要求第二名乘客必须是 `EntityMaid`，而玩家女仆永远不满足该条件。
- 在原生面板中开放活动范围设置，可调小、调大或设为无限制；女仆本人也可用「循环切换活动范围」按键调整。
- 更新女仆界面网络协议版本，并补齐中英文按键与按钮文案。

### Bug Fixes (English)

- Fixed TACZ firearms never firing automatically. The gun draw animation was restarted every tick, so `shoot` permanently reported `IS_DRAWING` and the maid stayed in a waiting state while the weapon also refused manual fire. The draw animation is now performed once per deployment and the shot pitch/yaw arguments follow the TACZ signature order.
- Added a per-maid TACZ auto-fire toggle. Player maids can switch between automatic and manual fire with the `Toggle TACZ Auto Fire` key (default `V`); the native GUI config tab exposes the same switch.
- Fixed compatibility addon items being hijacked by the native maid panel. Right-clicking with a Heart Pact tool (longing tester, growth tool, flower test kit, and similar) now reaches that mod instead of only opening the maid GUI, so favorability gains apply again.
- Fixed item duplication and slot overwriting while the native maid panel was open. Inventory write-back no longer runs every tick, and the held item is written back to the slot it was read from instead of the currently selected hotbar slot, so scrolling no longer copies or overwrites items.
- Fixed the maid gaining anger while friendly fire was blocked. Anger now only accumulates when damage is actually applied.
- Added a `strikeEnabled` option (default `true`). Setting it to `false` keeps the mood value visible but stops strikes from interrupting automatic work.
- Fixed player-maid projectiles harming their owner after the relationship was no longer resolvable, including arrows that bounced off the owner and hit the maid. Projectiles record their owner when fired, so the protection no longer depends on live relationship state.
- Automatic ranged attacks now skip firing when the owner stands within the projectile path.
- Fixed the native maid panel proxy entity being rendered in the world, which made the owner see two maids at once.
- Fixed player maids being unable to fly the Touhou Little Maid broom. The broom required an `EntityMaid` companion passenger, which a player maid never satisfies.
- Exposed the maid activity range in the native panel. Range can be stepped down, stepped up, or set to unlimited, and a `Cycle Activity Range` key is available for the maid's own client.
- Updated the maid GUI network protocol version and added Chinese/English key and button labels.

### 兼容与升级说明（中文）

- 新增服务端配置：`addonCompat.littleMadMood.strikeEnabled`（默认 `true`）与 `combat.taczAutoFireDefault`（默认 `true`）。
- 女仆界面网络协议版本由 3 升至 4，服务端与客户端需同时更新。
- 车万女仆扫帚兼容位于 `playermaid.touhou.mixins.json`（`required=false`），未安装车万女仆时会被自动跳过。
- 旧存档首次加载会提示 `playermaid (version 2.1.0 -> 2.1.1)`，属正常的版本升级通知。

### Compatibility and Upgrade Notes (English)

- New server options: `addonCompat.littleMadMood.strikeEnabled` (default `true`) and `combat.taczAutoFireDefault` (default `true`).
- The maid GUI network protocol moved from version 3 to 4; update server and client together.
- The Touhou Little Maid broom integration lives in `playermaid.touhou.mixins.json` (`required=false`) and is skipped automatically when Touhou Little Maid is absent.
- Existing worlds show `playermaid (version 2.1.0 -> 2.1.1)` on first load. This is the normal version-change notice.

## 2.1.0

### Highlights

- Added functional automatic combat executors for bows, crossbows, tridents, Touhou danmaku, TACZ firearms, and Iron's Spells 'n Spellbooks.
- Expanded player maid work modes with real crop planting, crop harvesting, sugar cane planting, and sugar cane harvesting.
- Improved projectile aiming with low-arc ballistic compensation so arrows reliably hit targets at medium and long range.
- Fixed Loyalty-enchanted tridents duplicating when thrown and returned.
- Improved native Maid GUI task synchronization and automatic work activation.

### Ranged Combat

- Added real arrow projectiles for automatic bow and crossbow attacks.
- Added real thrown trident entities with enchantment and durability handling.
- Added targeted Touhou danmaku attacks using the equipped Hakurei Gohei.
- Bows, crossbows, tridents, and gohei must now be equipped in the main hand instead of being activated remotely from the inventory.
- Automatic ranged attacks pause while the player is actively using an item.
- Melee and ranged attacks now share the player's attack-strength cadence, preventing simultaneous sword, bow, and crossbow attack loops.
- Crossbows now perform an initial loading phase and respect the Quick Charge enchantment.
- Added low-trajectory ballistic aiming based on projectile speed, distance, target height, and gravity.
- Improved target filtering to require a living, visible, hostile entity within the configured combat range.

### Trident Fixes

- Fixed Loyalty tridents leaving the original item in the main hand while also returning the thrown copy.
- A Loyalty trident is now removed from the main hand after a successful throw and returned by its projectile entity.
- Durability loss is applied to the returning trident stack.
- Non-Loyalty tridents retain the existing automatic-throw behavior and consume durability normally.
- Riptide tridents remain excluded from automatic ranged throwing.

### Farming and Gathering

- Normal farming now harvests mature vanilla crops and restores them to their initial growth state.
- Player maids can now plant compatible Forge crop seeds on nearby empty farmland.
- Basic planting and harvesting are no longer completely blocked when no hoe is present.
- Sugar cane work no longer incorrectly requires a hoe.
- Sugar cane harvesting now skips isolated one-block plants and selects a plant with a harvestable upper segment.
- Harvesting preserves the bottom sugar cane block so the plant can regrow.
- Player maids can plant sugar cane from their inventory on suitable soil next to water.
- Added functional executors for melon and pumpkin harvesting, mature cocoa harvesting, honey collection, grass clearing, snow clearing, sheep shearing, and cow milking.
- Added clearer work-status messages when a required item or valid nearby target is missing.

### Mod Compatibility

- Added optional automatic firearm support for Timeless and Classics Zero (TACZ).
- TACZ attacks use a valid gun in the main hand, respect available ammunition, attempt reloading, and report missing or failed compatibility states.
- Added optional spell-combat support for Iron's Spells 'n Spellbooks.
- Spell tasks use the currently selected spell and respect mana, cast conditions, cast time, and cooldowns.
- Added mappings for the `maidspell:spell_combat_far` and `maidspell:spell_combat_melee` task IDs.
- Improved optional Curios integration and inventory-screen compatibility.

### Task and GUI Synchronization

- Selecting a supported task through the native Touhou Maid GUI now enables automatic work for that task.
- Updated work descriptions to state main-hand weapon, ammunition, seed, farmland, and planting requirements more clearly.
- Combat modes now run every tick while still respecting weapon, item-use, attack-strength, loading, and cooldown restrictions.
- Non-combat work continues to respect the configured automatic work interval and schedule.
- Fishing and board-game modes retain their task mapping and display an execution notice when manual interaction is required.

### Reliability and Testing

- Added unit coverage for spell task mapping, combat executor registration, work hints, and low-arc projectile compensation.
- Added a comprehensive 2.0/2.1 integration test checklist and a dedicated test-server launcher.
- Added mod icon and updated Forge metadata for the 2.1.0 release.
- Verified compilation, unit tests, reobfuscation, and the final Forge build on Minecraft 1.20.1.

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
