package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class MaidData {
    private static final String ROOT = "PlayerMaidData";
    private static final String OWNER_UUID = "OwnerUuid";
    private static final String OWNER_NAME = "OwnerName";
    private static final String MANAGER_TYPE = "ManagerType";
    private static final String MODE = "Mode";
    private static final String STAY_DIMENSION = "StayDimension";
    private static final String STAY_X = "StayX";
    private static final String STAY_Y = "StayY";
    private static final String STAY_Z = "StayZ";
    private static final String BED_DIMENSION = "BedDimension";
    private static final String BED_X = "BedX";
    private static final String BED_Y = "BedY";
    private static final String BED_Z = "BedZ";
    private static final String ACTIVITY_RANGE = "ActivityRange";
    private static final String PICKUP = "Pickup";
    private static final String RIDEABLE = "Rideable";
    private static final String SCHEDULE = "Schedule";
    private static final String SHOW_BACKPACK = "ShowBackpack";
    private static final String SHOW_BACK_ITEM = "ShowBackItem";
    private static final String SHOW_CHAT_BUBBLE = "ShowChatBubble";
    private static final String SOUND_FREQ = "SoundFreq";
    private static final String PICK_TYPE = "PickType";
    private static final String OPEN_DOOR = "OpenDoor";
    private static final String OPEN_FENCE_GATE = "OpenFenceGate";
    private static final String ACTIVE_CLIMBING = "ActiveClimbing";
    private static final String GUN_AUTO_FIRE = "GunAutoFire";
    public static final double UNLIMITED_ACTIVITY_RANGE = -1.0D;
    public static final double MIN_ACTIVITY_RANGE = 1.0D;
    public static final double MAX_ACTIVITY_RANGE = 128.0D;
    public static final double MAX_WORK_SEARCH_RANGE = 32.0D;
    /** 界面与按键循环使用的活动范围档位，最后一项为无限制。 */
    public static final double[] ACTIVITY_RANGE_STEPS = {8.0D, 16.0D, 32.0D, 64.0D, 128.0D, UNLIMITED_ACTIVITY_RANGE};

    public enum Mode {
        FOLLOW,
        STAY
    }

    public enum Schedule {
        DAY, NIGHT, ALL;

        public Schedule next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public record StayPoint(ResourceLocation dimension, double x, double y, double z) {
    }

    public static CompoundTag root(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT);
    }

    private static CompoundTag writableRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) {
            persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag root = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, root);
        }
        return root;
    }

    public static boolean isMaid(Player player) {
        return ownerUuid(player).isPresent();
    }

    public static Optional<UUID> ownerUuid(Player player) {
        String value = root(player).getString(OWNER_UUID);
        if (value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static String ownerName(Player player) {
        return root(player).getString(OWNER_NAME);
    }

    /** 旧存档没有 ManagerType 时按 PLAYER 解释。 */
    public static Optional<RelationshipNodeType> managerType(Player player) {
        if (ownerUuid(player).isEmpty()) return Optional.empty();
        String value = root(player).getString(MANAGER_TYPE);
        if (value.isBlank()) return Optional.of(RelationshipNodeType.PLAYER);
        try {
            return Optional.of(RelationshipNodeType.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.of(RelationshipNodeType.PLAYER);
        }
    }

    public static Optional<RelationshipNode> directManager(Player player) {
        Optional<UUID> uuid = ownerUuid(player);
        Optional<RelationshipNodeType> type = managerType(player);
        if (uuid.isEmpty() || type.isEmpty()) return Optional.empty();
        return Optional.of(new RelationshipNode(type.get(), uuid.get()));
    }

    public static Mode mode(Player player) {
        try {
            return Mode.valueOf(root(player).getString(MODE));
        } catch (IllegalArgumentException ignored) {
            return Mode.FOLLOW;
        }
    }

    /** 兼容旧调用；关系同时写入世界 SavedData。 */
    public static void bind(ServerPlayer maid, ServerPlayer owner) {
        bindToPlayer(maid, owner);
    }

    public static boolean bindToPlayer(ServerPlayer maid, ServerPlayer manager) {
        return RelationshipService.bindToPlayer(maid, manager);
    }

    public static boolean bindToMaid(ServerPlayer maid, EntityMaid manager) {
        return RelationshipService.bindToMaid(maid, manager);
    }

    static void writeManager(ServerPlayer maid, RelationshipNode manager, String managerName) {
        CompoundTag tag = writableRoot(maid);
        tag.putString(OWNER_UUID, manager.uuid().toString());
        tag.putString(OWNER_NAME, managerName);
        tag.putString(MANAGER_TYPE, manager.type().name());
        tag.putString(MODE, Mode.FOLLOW.name());
        if (!tag.contains(ACTIVITY_RANGE)) {
            tag.putDouble(ACTIVITY_RANGE, clampActivityRange(MaidConfig.AUTO_WORK_RANGE.get()));
        }
        tag.putBoolean(PICKUP, MaidConfig.MAID_UI_DEFAULT_PICKUP.get());
        tag.putBoolean(RIDEABLE, MaidConfig.MAID_UI_DEFAULT_RIDEABLE.get());
        tag.putString(SCHEDULE, MaidConfig.MAID_UI_DEFAULT_SCHEDULE.get());
        tag.putBoolean(SHOW_BACKPACK, MaidConfig.MAID_UI_DEFAULT_SHOW_BACKPACK.get());
        tag.putBoolean(SHOW_BACK_ITEM, MaidConfig.MAID_UI_DEFAULT_SHOW_BACK_ITEM.get());
        tag.putBoolean(SHOW_CHAT_BUBBLE, MaidConfig.MAID_UI_DEFAULT_SHOW_CHAT_BUBBLE.get());
        tag.putInt(SOUND_FREQ, MaidConfig.MAID_UI_DEFAULT_SOUND_FREQ.get());
        tag.putInt(PICK_TYPE, MaidConfig.MAID_UI_DEFAULT_PICK_TYPE.get());
        tag.putBoolean(OPEN_DOOR, MaidConfig.MAID_UI_DEFAULT_OPEN_DOOR.get());
        tag.putBoolean(OPEN_FENCE_GATE, MaidConfig.MAID_UI_DEFAULT_OPEN_FENCE_GATE.get());
        tag.putBoolean(ACTIVE_CLIMBING, MaidConfig.MAID_UI_DEFAULT_ACTIVE_CLIMBING.get());
        clearStayPoint(tag);
    }

    public static double activityRange(Player maid) {
        CompoundTag tag = root(maid);
        double value = tag.contains(ACTIVITY_RANGE) ? tag.getDouble(ACTIVITY_RANGE) : MaidConfig.AUTO_WORK_RANGE.get();
        if (value == UNLIMITED_ACTIVITY_RANGE) return UNLIMITED_ACTIVITY_RANGE;
        return clampActivityRange(value);
    }

    public static void setActivityRange(Player maid, double value) {
        writableRoot(maid).putDouble(ACTIVITY_RANGE, clampActivityRange(value));
    }

    public static void setActivityRangeUnlimited(Player maid) {
        writableRoot(maid).putDouble(ACTIVITY_RANGE, UNLIMITED_ACTIVITY_RANGE);
    }

    public static boolean isActivityRangeUnlimited(Player maid) {
        return activityRange(maid) == UNLIMITED_ACTIVITY_RANGE;
    }

    /** 按档位表切换活动范围，direction 为 +1 或 -1；当前值不在档位表内时归位到第一档。 */
    public static double stepActivityRange(Player maid, int direction) {
        double current = activityRange(maid);
        int step = direction < 0 ? -1 : 1;
        for (int i = 0; i < ACTIVITY_RANGE_STEPS.length; i++) {
            if (Math.abs(ACTIVITY_RANGE_STEPS[i] - current) < 1.0E-6D) {
                int next = Math.floorMod(i + step, ACTIVITY_RANGE_STEPS.length);
                return applyActivityRange(maid, ACTIVITY_RANGE_STEPS[next]);
            }
        }
        return applyActivityRange(maid, ACTIVITY_RANGE_STEPS[0]);
    }

    public static double applyActivityRange(Player maid, double value) {
        if (value == UNLIMITED_ACTIVITY_RANGE) setActivityRangeUnlimited(maid);
        else setActivityRange(maid, value);
        return activityRange(maid);
    }

    public static double workSearchRange(Player maid) {
        if (isActivityRangeUnlimited(maid)) return MAX_WORK_SEARCH_RANGE;
        return Math.min(activityRange(maid), MAX_WORK_SEARCH_RANGE);
    }

    public static String activityRangeText(Player maid) {
        if (isActivityRangeUnlimited(maid)) return "无限制";
        double value = activityRange(maid);
        return value == Math.rint(value) ? Integer.toString((int) value) + "格" : String.format(java.util.Locale.ROOT, "%.1f格", value);
    }

    private static double clampActivityRange(double value) {
        if (!Double.isFinite(value)) return Math.max(MIN_ACTIVITY_RANGE,
                Math.min(MAX_ACTIVITY_RANGE, MaidConfig.AUTO_WORK_RANGE.get()));
        return Math.max(MIN_ACTIVITY_RANGE, Math.min(MAX_ACTIVITY_RANGE, value));
    }

    public static boolean pickup(Player maid) { return bool(maid, PICKUP, MaidConfig.MAID_UI_DEFAULT_PICKUP.get()); }
    public static void setPickup(Player maid, boolean value) { writableRoot(maid).putBoolean(PICKUP, value); }
    public static boolean rideable(Player maid) { return bool(maid, RIDEABLE, MaidConfig.MAID_UI_DEFAULT_RIDEABLE.get()); }
    public static void setRideable(Player maid, boolean value) { writableRoot(maid).putBoolean(RIDEABLE, value); }
    public static boolean showBackpack(Player maid) { return bool(maid, SHOW_BACKPACK, MaidConfig.MAID_UI_DEFAULT_SHOW_BACKPACK.get()); }
    public static void setShowBackpack(Player maid, boolean value) { writableRoot(maid).putBoolean(SHOW_BACKPACK, value); }
    public static boolean showBackItem(Player maid) { return bool(maid, SHOW_BACK_ITEM, MaidConfig.MAID_UI_DEFAULT_SHOW_BACK_ITEM.get()); }
    public static void setShowBackItem(Player maid, boolean value) { writableRoot(maid).putBoolean(SHOW_BACK_ITEM, value); }
    public static boolean showChatBubble(Player maid) { return bool(maid, SHOW_CHAT_BUBBLE, MaidConfig.MAID_UI_DEFAULT_SHOW_CHAT_BUBBLE.get()); }
    public static void setShowChatBubble(Player maid, boolean value) { writableRoot(maid).putBoolean(SHOW_CHAT_BUBBLE, value); }
    public static boolean openDoor(Player maid) { return bool(maid, OPEN_DOOR, MaidConfig.MAID_UI_DEFAULT_OPEN_DOOR.get()); }
    public static void setOpenDoor(Player maid, boolean value) { writableRoot(maid).putBoolean(OPEN_DOOR, value); }
    public static boolean openFenceGate(Player maid) { return bool(maid, OPEN_FENCE_GATE, MaidConfig.MAID_UI_DEFAULT_OPEN_FENCE_GATE.get()); }
    public static void setOpenFenceGate(Player maid, boolean value) { writableRoot(maid).putBoolean(OPEN_FENCE_GATE, value); }
    public static boolean activeClimbing(Player maid) { return bool(maid, ACTIVE_CLIMBING, MaidConfig.MAID_UI_DEFAULT_ACTIVE_CLIMBING.get()); }
    public static void setActiveClimbing(Player maid, boolean value) { writableRoot(maid).putBoolean(ACTIVE_CLIMBING, value); }
    /** TACZ 自动开火开关；未设置时取配置默认值。 */
    public static boolean gunAutoFire(Player maid) { return bool(maid, GUN_AUTO_FIRE, MaidConfig.TACZ_AUTO_FIRE_DEFAULT.get()); }
    public static void setGunAutoFire(Player maid, boolean value) { writableRoot(maid).putBoolean(GUN_AUTO_FIRE, value); }

    public static Schedule schedule(Player maid) {
        try { return Schedule.valueOf(root(maid).getString(SCHEDULE)); }
        catch (IllegalArgumentException ignored) {
            try { return Schedule.valueOf(MaidConfig.MAID_UI_DEFAULT_SCHEDULE.get()); }
            catch (IllegalArgumentException ignoredAgain) { return Schedule.ALL; }
        }
    }
    public static void setSchedule(Player maid, Schedule value) { writableRoot(maid).putString(SCHEDULE, value.name()); }
    public static int soundFreq(Player maid) {
        CompoundTag tag = root(maid);
        return Math.max(0, Math.min(100, tag.contains(SOUND_FREQ) ? tag.getInt(SOUND_FREQ) : MaidConfig.MAID_UI_DEFAULT_SOUND_FREQ.get()));
    }
    public static void setSoundFreq(Player maid, int value) { writableRoot(maid).putInt(SOUND_FREQ, Math.max(0, Math.min(100, value))); }
    public static int pickType(Player maid) {
        CompoundTag tag = root(maid);
        return Math.max(0, Math.min(2, tag.contains(PICK_TYPE) ? tag.getInt(PICK_TYPE) : MaidConfig.MAID_UI_DEFAULT_PICK_TYPE.get()));
    }
    public static void setPickType(Player maid, int value) { writableRoot(maid).putInt(PICK_TYPE, Math.max(0, Math.min(2, value))); }

    private static boolean bool(Player maid, String key, boolean fallback) {
        CompoundTag tag = root(maid);
        return tag.contains(key) ? tag.getBoolean(key) : fallback;
    }

    /** 清理在线玩家女仆时同步移除世界关系边。 */
    public static void clear(Player maid) {
        if (maid instanceof ServerPlayer serverPlayer) {
            RelationshipService.clear(serverPlayer);
        } else {
            clearLocal(maid);
        }
    }

    static void clearLocal(Player maid) {
        CompoundTag persisted = maid.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(ROOT);
        persisted.remove(AddonRelationshipData.ROOT);
    }

    public static void setFollow(Player maid) {
        CompoundTag tag = writableRoot(maid);
        tag.putString(MODE, Mode.FOLLOW.name());
        clearStayPoint(tag);
    }

    public static void setStay(ServerPlayer maid) {
        CompoundTag tag = writableRoot(maid);
        tag.putString(MODE, Mode.STAY.name());
        tag.putString(STAY_DIMENSION, maid.level().dimension().location().toString());
        tag.putDouble(STAY_X, maid.getX());
        tag.putDouble(STAY_Y, maid.getY());
        tag.putDouble(STAY_Z, maid.getZ());
    }

    public static Optional<StayPoint> stayPoint(Player maid) {
        CompoundTag tag = root(maid);
        if (!tag.contains(STAY_DIMENSION)) return Optional.empty();
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(STAY_DIMENSION));
        if (dimension == null) return Optional.empty();
        return Optional.of(new StayPoint(dimension, tag.getDouble(STAY_X), tag.getDouble(STAY_Y), tag.getDouble(STAY_Z)));
    }

    public static void setBed(ServerPlayer maid, net.minecraft.core.BlockPos pos) {
        CompoundTag tag = writableRoot(maid);
        tag.putString(BED_DIMENSION, maid.level().dimension().location().toString());
        tag.putInt(BED_X, pos.getX());
        tag.putInt(BED_Y, pos.getY());
        tag.putInt(BED_Z, pos.getZ());
    }

    public static Optional<BedPoint> bedPoint(Player maid) {
        CompoundTag tag = root(maid);
        if (!tag.contains(BED_DIMENSION)) return Optional.empty();
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(BED_DIMENSION));
        if (dimension == null) return Optional.empty();
        return Optional.of(new BedPoint(dimension, new net.minecraft.core.BlockPos(
                tag.getInt(BED_X), tag.getInt(BED_Y), tag.getInt(BED_Z))));
    }

    public record BedPoint(ResourceLocation dimension, net.minecraft.core.BlockPos pos) {
    }

    public static boolean ownedBy(Player maid, Player owner) {
        return managerType(maid).filter(type -> type == RelationshipNodeType.PLAYER).isPresent()
                && ownerUuid(maid).map(owner.getUUID()::equals).orElse(false);
    }

    private static void clearStayPoint(CompoundTag tag) {
        tag.remove(STAY_DIMENSION);
        tag.remove(STAY_X);
        tag.remove(STAY_Y);
        tag.remove(STAY_Z);
    }

    private MaidData() {
    }
}
