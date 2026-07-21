package com.narrafork.playermaid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 持久化玩家女仆的关系、情绪和羁绊兼容数据。 */
public final class AddonRelationshipData {
    public static final String ROOT = "PlayerMaidAddonCompat";
    private static final int DATA_VERSION = 1;

    public enum RelationStage {
        INITIAL("初识"), WARM("熟络"), CLOSE("暧昧"), DATING("恋人"), MARRIAGE("婚姻");
        private final String display;
        RelationStage(String display) { this.display = display; }
        public String display() { return display; }
    }

    public enum MoodLevel {
        GOOD("开心"), NORMAL("正常"), BAD("生气"), MAD("疯狂");
        private final String display;
        MoodLevel(String display) { this.display = display; }
        public String display() { return display; }
    }

    public static CompoundTag root(Player maid) {
        return maid.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT);
    }

    public static CompoundTag writable(Player maid) {
        CompoundTag persistent = maid.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag root = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) persisted.put(ROOT, root);
        root.putInt("DataVersion", DATA_VERSION);
        return root;
    }

    public static int favorability(Player maid) { return clamp(root(maid).getInt("Favorability"), 0, 256); }
    public static void setFavorability(Player maid, int value) { writable(maid).putInt("Favorability", clamp(value, 0, 256)); }
    public static int addFavorability(ServerPlayer maid, int raw) {
        int adjusted = raw;
        if (raw > 0) adjusted = Math.round(raw * LittleMadCompat.positiveFavorabilityMultiplier(maid));
        setFavorability(maid, favorability(maid) + adjusted);
        syncBondLevel(maid);
        writable(maid).putLong("LastInteraction", maid.serverLevel().getGameTime());
        return adjusted;
    }

    public static RelationStage relationStage(Player maid) {
        if (isMarried(maid)) return RelationStage.MARRIAGE;
        if (isConfessed(maid)) return RelationStage.DATING;
        int value = favorability(maid);
        if (value >= 64) return RelationStage.CLOSE;
        if (value >= 32) return RelationStage.WARM;
        return RelationStage.INITIAL;
    }

    public static boolean isConfessed(Player maid) { return root(maid).getBoolean("Confessed"); }
    public static void setConfessed(Player maid, boolean value) { writable(maid).putBoolean("Confessed", value); }
    public static boolean isMarried(Player maid) { return root(maid).getBoolean("Married"); }
    public static Optional<UUID> spouse(Player maid) {
        CompoundTag tag = root(maid);
        return tag.hasUUID("Spouse") ? Optional.of(tag.getUUID("Spouse")) : Optional.empty();
    }
    public static void marry(ServerPlayer maid, ServerPlayer owner) {
        CompoundTag tag = writable(maid);
        tag.putBoolean("Confessed", true);
        tag.putBoolean("Married", true);
        tag.putUUID("Spouse", owner.getUUID());
        tag.putLong("MarriedAt", maid.serverLevel().getGameTime());
        syncBondLevel(maid);
    }

    public static int mood(Player maid) { return clamp(root(maid).getInt("MoodAnger"), 0, 180); }
    public static void setMood(Player maid, int value) { writable(maid).putInt("MoodAnger", clamp(value, 0, 180)); }
    public static MoodLevel moodLevel(Player maid) {
        int value = mood(maid);
        if (value >= 180) return MoodLevel.MAD;
        if (value >= 120) return MoodLevel.BAD;
        if (value >= 60) return MoodLevel.NORMAL;
        return MoodLevel.GOOD;
    }

    public static long getLong(Player maid, String key) { return root(maid).getLong(key); }
    public static void putLong(Player maid, String key, long value) { writable(maid).putLong(key, value); }
    public static int getInt(Player maid, String key) { return root(maid).getInt(key); }
    public static void putInt(Player maid, String key, int value) { writable(maid).putInt(key, value); }
    public static boolean getBoolean(Player maid, String key) { return root(maid).getBoolean(key); }
    public static void putBoolean(Player maid, String key, boolean value) { writable(maid).putBoolean(key, value); }

    public static int bondLevel(Player maid) { return Math.min(3, favorability(maid) / 64); }
    public static int availableBondPoints(Player maid) {
        int earned = favorability(maid) / 4;
        int spent = 0;
        for (String id : abilities(maid)) spent += TouhouMaidAffectionCompat.cost(id);
        return Math.max(0, earned - spent);
    }
    public static Set<String> abilities(Player maid) {
        Set<String> result = new LinkedHashSet<>();
        ListTag list = root(maid).getList("BondAbilities", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }
    public static boolean hasAbility(Player maid, String id) { return abilities(maid).contains(id); }
    public static void unlockAbility(Player maid, String id) {
        Set<String> values = abilities(maid);
        values.add(id);
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        writable(maid).put("BondAbilities", list);
    }

    public static void syncBondLevel(ServerPlayer maid) {
        ServerPlayer owner = MaidData.ownerUuid(maid)
                .map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        if (owner != null) TouhouMaidAffectionCompat.sync(owner, maid);
    }

    public static void clear(Player maid) {
        maid.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove(ROOT);
    }

    public static String status(Player maid) {
        return "关系：" + relationStage(maid).display() + "（好感 " + favorability(maid) + "/256）"
                + " | 情绪：" + moodLevel(maid).display() + " | 羁绊：" + bondLevel(maid)
                + " | P点：" + availableBondPoints(maid);
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private AddonRelationshipData() { }
}
