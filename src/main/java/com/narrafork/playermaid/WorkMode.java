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
    IDLE("idle", "待命", Category.GENERAL, "不执行自动动作"),
    ATTACK("attack", "近战攻击", Category.COMBAT, "自动攻击附近敌怪"),
    RANGED_ATTACK("ranged_attack", "远程攻击", Category.COMBAT, "24 格内使用真实弓箭；主手需有弓，背包需有箭"),
    CROSSBOW_ATTACK("crossbow_attack", "弩箭攻击", Category.COMBAT, "24 格内使用真实弩箭；主手需有弩，背包需有箭"),
    DANMAKU_ATTACK("danmaku_attack", "弹幕攻击", Category.COMBAT, "24 格内发射 TLM 定向弹幕；背包需有御币"),
    TRIDENT_ATTACK("trident_attack", "三叉戟攻击", Category.COMBAT, "16 格内投掷真实三叉戟；激流三叉戟不适用"),
    GUN_ATTACK("gun_attack", "枪械攻击", Category.COMBAT, "32 格内调用 TACZ 真实射击；枪械必须放在主手并备有弹药"),
    SPELL_ATTACK("spell_attack", "法术战斗", Category.COMBAT, "24 格内施放铁魔法轮盘当前选中的法术；遵守法力、施法时间和冷却"),
    FARM("farm", "耕作", Category.GATHERING, "自动收获并补种作物；背包需有种子，附近需有耕地"),
    SUGAR_CANE("sugar_cane", "甘蔗收割", Category.GATHERING, "自动收割并补种甘蔗；背包需有甘蔗，附近需有水边土块或沙"),
    MELON("melon", "瓜果收割", Category.GATHERING, "自动收割西瓜和南瓜；背包需有斧"),
    COCOA("cocoa", "可可采集", Category.GATHERING, "自动收获成熟可可并保留植株；背包需有斧"),
    HONEY("honey", "蜂蜜采集", Category.GATHERING, "自动从满蜜蜂巢取蜜；背包需有玻璃瓶"),
    GRASS("grass", "除草", Category.GATHERING, "自动清除草、蕨和枯灌木；背包需有锄"),
    SNOW("snow", "除雪", Category.GATHERING, "自动清除雪层；背包需有锹"),
    FEED("feed", "照料主人", Category.FEEDING, "自动治疗或喂食直接主人；背包需有食物"),
    SHEARS("shears", "剪毛", Category.GATHERING, "自动给成年羊剪毛；背包需有剪刀"),
    MILK("milk", "挤奶", Category.GATHERING, "自动给成年牛挤奶；背包需有空桶"),
    TORCH("torch", "插火把", Category.TORCH, "自动在低亮度位置放置火把；背包需有火把"),
    FEED_ANIMAL("feed_animal", "喂养动物", Category.FEEDING, "自动喂养成年动物；背包需有对应食物"),
    FISHING("fishing", "钓鱼", Category.GENERAL, "需玩家手动使用钓鱼竿；当前暂无自动执行器"),
    EXTINGUISHING("extinguishing", "灭火", Category.EXTINGUISHING, "自动熄灭自身、主人、附近实体和火焰方块"),
    BOARD_GAMES("board_games", "桌游陪伴", Category.GENERAL, "需玩家手动与桌游家具交互；当前暂无自动执行器");

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
    private final String executionHint;

    WorkMode(String id, String chineseName, Category category, String executionHint) {
        this.id = id;
        this.chineseName = chineseName;
        this.category = category;
        this.executionHint = executionHint;
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

    public String executionHint() {
        return executionHint;
    }

    public boolean hasAutomaticExecutor() {
        return this != IDLE && this != FISHING && this != BOARD_GAMES;
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
        if (uid == null) return Optional.empty();
        if ("maidspell".equals(uid.getNamespace())
                && ("spell_combat_far".equals(uid.getPath()) || "spell_combat_melee".equals(uid.getPath()))) {
            return Optional.of(SPELL_ATTACK);
        }
        if (!("touhou_little_maid".equals(uid.getNamespace())
                || PlayerMaid.MOD_ID.equals(uid.getNamespace()))) return Optional.empty();
        return byId(uid.getPath());
    }

    public ResourceLocation taskUid() {
        return this == SPELL_ATTACK
                ? new ResourceLocation("maidspell", "spell_combat_far")
                : new ResourceLocation("touhou_little_maid", id);
    }
}
