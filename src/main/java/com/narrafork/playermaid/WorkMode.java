package com.narrafork.playermaid;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 玩家女仆支持的 TLM 工作模式。
 * id 与 TLM 的任务 id 保持一致，便于以后直接接入任务注册表。
 */
public enum WorkMode {
    IDLE("idle", "待命", Category.GENERAL),
    ATTACK("attack", "近战攻击", Category.COMBAT),
    RANGED_ATTACK("ranged_attack", "远程攻击", Category.COMBAT),
    CROSSBOW_ATTACK("crossbow_attack", "弩箭攻击", Category.COMBAT),
    DANMAKU_ATTACK("danmaku_attack", "弹幕攻击", Category.COMBAT),
    TRIDENT_ATTACK("trident_attack", "三叉戟攻击", Category.COMBAT),
    GUN_ATTACK("gun_attack", "枪械攻击", Category.COMBAT),
    FARM("farm", "耕作", Category.GATHERING),
    SUGAR_CANE("sugar_cane", "甘蔗收割", Category.GATHERING),
    MELON("melon", "瓜果收割", Category.GATHERING),
    COCOA("cocoa", "可可采集", Category.GATHERING),
    HONEY("honey", "蜂蜜采集", Category.GATHERING),
    GRASS("grass", "除草", Category.GATHERING),
    SNOW("snow", "除雪", Category.GATHERING),
    FEED("feed", "照料主人", Category.FEEDING),
    SHEARS("shears", "剪毛", Category.GATHERING),
    MILK("milk", "挤奶", Category.GATHERING),
    TORCH("torch", "插火把", Category.TORCH),
    FEED_ANIMAL("feed_animal", "喂养动物", Category.FEEDING),
    FISHING("fishing", "钓鱼", Category.GENERAL),
    EXTINGUISHING("extinguishing", "灭火", Category.EXTINGUISHING),
    BOARD_GAMES("board_games", "桌游陪伴", Category.GENERAL);

    public enum Category {
        COMBAT("战斗"),
        GATHERING("农耕采集"),
        FEEDING("喂食照料"),
        TORCH("照明"),
        EXTINGUISHING("消防"),
        GENERAL("通用");

        private final String chineseName;

        Category(String chineseName) {
            this.chineseName = chineseName;
        }

        public String chineseName() {
            return chineseName;
        }
    }

    private final String id;
    private final String chineseName;
    private final Category category;

    WorkMode(String id, String chineseName, Category category) {
        this.id = id;
        this.chineseName = chineseName;
        this.category = category;
    }

    public String id() {
        return id;
    }

    public String chineseName() {
        return chineseName;
    }

    public Category category() {
        return category;
    }

    public boolean isCombat() {
        return category == Category.COMBAT;
    }

    public static Optional<WorkMode> byId(String id) {
        if (id == null) return Optional.empty();
        String normalized = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(mode -> mode.id.equals(normalized)).findFirst();
    }

    public static Optional<WorkMode> byTaskUid(ResourceLocation uid) {
        if (uid == null || !("touhou_little_maid".equals(uid.getNamespace())
                || PlayerMaid.MOD_ID.equals(uid.getNamespace()))) return Optional.empty();
        return byId(uid.getPath());
    }

    public ResourceLocation taskUid() {
        return new ResourceLocation("touhou_little_maid", id);
    }
}
