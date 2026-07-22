package com.narrafork.playermaid;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MaidConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue REQUEST_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue STAY_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SHOW_STATUS_BAR;
    public static final ForgeConfigSpec.BooleanValue PREVENT_FRIENDLY_FIRE;
    public static final ForgeConfigSpec.BooleanValue CONSUME_CAKE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_OWNER_INVENTORY_MANAGEMENT;
    public static final ForgeConfigSpec.DoubleValue WORK_DAMAGE_BONUS;
    public static final ForgeConfigSpec.IntValue AUTO_WORK_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue AUTO_WORK_RANGE;
    public static final ForgeConfigSpec.BooleanValue TOUHOU_COMPAT;
    public static final ForgeConfigSpec.BooleanValue ENTITY_MAID_CONTRACTS;
    public static final ForgeConfigSpec.BooleanValue INHERIT_MANAGEMENT;
    public static final ForgeConfigSpec.BooleanValue SYNC_MAID_TASKS;
    public static final ForgeConfigSpec.BooleanValue CLEAR_CONTRACTS_ON_MAID_DEATH;
    public static final ForgeConfigSpec.IntValue MAX_RELATION_DEPTH;
    public static final ForgeConfigSpec.BooleanValue MAID_MARRIAGE_COMPAT;
    public static final ForgeConfigSpec.BooleanValue MAID_AFFECTION_COMPAT;
    public static final ForgeConfigSpec.BooleanValue LITTLE_MAD_COMPAT;
    public static final ForgeConfigSpec.BooleanValue YSM_COMPAT;
    public static final ForgeConfigSpec.BooleanValue YSM_INTERACTION_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue YSM_ANIMATE_OWNER;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_PET;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_HUG;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_KISS;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_MORNING_KISS;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_LAP_PILLOW;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_CONFESS;
    public static final ForgeConfigSpec.ConfigValue<String> YSM_ANIMATION_MARRY;
    public static final ForgeConfigSpec.IntValue MOOD_RECOVER_INTERVAL;
    public static final ForgeConfigSpec.IntValue MOOD_RECOVER_AMOUNT;
    public static final ForgeConfigSpec.IntValue MOOD_WORK_CHECK_INTERVAL;
    public static final ForgeConfigSpec.IntValue MOOD_WORK_ANGER;
    public static final ForgeConfigSpec.DoubleValue MOOD_WORK_ANGER_CHANCE;
    public static final ForgeConfigSpec.IntValue MOOD_HURT_BY_OWNER;
    public static final ForgeConfigSpec.IntValue MOOD_HURT_BY_OTHER;
    public static final ForgeConfigSpec.IntValue MOOD_DEATH_ANGER;
    public static final ForgeConfigSpec.IntValue MOOD_STRIKE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue MOOD_STRIKE_CHANCE;
    public static final ForgeConfigSpec.BooleanValue GUI_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GUI_INTERACTION_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue GUI_STATUS_ONLY_WHEN_INVENTORY_DISABLED;
    public static final ForgeConfigSpec.BooleanValue GUI_TASK_TAB;
    public static final ForgeConfigSpec.BooleanValue GUI_CONFIG_TAB;
    public static final ForgeConfigSpec.BooleanValue GUI_BAUBLE_TAB;
    public static final ForgeConfigSpec.BooleanValue GUI_CURIOS_TAB;
    public static final ForgeConfigSpec.BooleanValue GUI_ENDER_TAB;
    public static final ForgeConfigSpec.IntValue GUI_EFFECT_SYNC_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue MAID_UI_PICKUP_RANGE;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_ALLOW_OWNER_RIDE;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_ENABLE_DOOR_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_ENABLE_FENCE_GATE_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_ENABLE_ACTIVE_CLIMBING;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_PICKUP;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_RIDEABLE;
    public static final ForgeConfigSpec.ConfigValue<String> MAID_UI_DEFAULT_SCHEDULE;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_SHOW_BACKPACK;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_SHOW_BACK_ITEM;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_SHOW_CHAT_BUBBLE;
    public static final ForgeConfigSpec.IntValue MAID_UI_DEFAULT_SOUND_FREQ;
    public static final ForgeConfigSpec.IntValue MAID_UI_DEFAULT_PICK_TYPE;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_OPEN_DOOR;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_OPEN_FENCE_GATE;
    public static final ForgeConfigSpec.BooleanValue MAID_UI_DEFAULT_ACTIVE_CLIMBING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("gameplay");
        REQUEST_TIMEOUT_SECONDS = builder.comment("驯服请求有效时间（秒）")
                .defineInRange("requestTimeoutSeconds", 60, 10, 600);
        FOLLOW_TELEPORT_DISTANCE = builder.comment("旧版跟随传送距离（保留配置兼容，当前由每名女仆的活动范围替代）")
                .defineInRange("followTeleportDistance", 16.0, 4.0, 128.0);
        STAY_RADIUS = builder.comment("旧版待命半径（保留配置兼容，当前由每名女仆的活动范围替代）")
                .defineInRange("stayRadius", 3.0, 0.5, 32.0);
        SHOW_STATUS_BAR = builder.comment("显示女仆/主人状态栏")
                .define("showStatusBar", true);
        PREVENT_FRIENDLY_FIRE = builder.comment("阻止主人和自己的女仆互相伤害")
                .define("preventFriendlyFire", true);
        CONSUME_CAKE = builder.comment("请求被接受时消耗主人一个蛋糕")
                .define("consumeCake", true);
        ALLOW_OWNER_INVENTORY_MANAGEMENT = builder.comment("允许主人管理女仆物品栏、末影箱和饰品栏")
                .define("allowOwnerInventoryManagement", true);
        WORK_DAMAGE_BONUS = builder.comment("战斗工作模式的额外伤害比例")
                .defineInRange("workDamageBonus", 0.20, 0.0, 5.0);
        AUTO_WORK_INTERVAL = builder.comment("自动工作执行间隔（tick）")
                .defineInRange("autoWorkInterval", 20, 5, 200);
        AUTO_WORK_RANGE = builder.comment("新绑定玩家女仆的默认活动范围；自动工作搜索最多使用32格")
                .defineInRange("autoWorkRange", 8.0, 2.0, 32.0);
        TOUHOU_COMPAT = builder.comment("启用车万女仆床、家具和功能道具适配")
                .define("touhouCompat", true);
        ENTITY_MAID_CONTRACTS = builder.comment("允许玩家潜行手持蛋糕右键已驯服 Maid，成为该实体的玩家女仆")
                .define("entityMaidContracts", true);
        INHERIT_MANAGEMENT = builder.comment("允许上级主人继承管理所有玩家女仆后代")
                .define("inheritManagement", true);
        SYNC_MAID_TASKS = builder.comment("Maid 实体切换任务时自动同步给关系树中的玩家女仆并开启自动工作")
                .define("syncMaidTasks", true);
        CLEAR_CONTRACTS_ON_MAID_DEATH = builder.comment("Maid 实体死亡时解除其直接玩家女仆契约")
                .define("clearContractsOnMaidDeath", true);
        MAX_RELATION_DEPTH = builder.comment("从属关系最大解析深度，用于阻止环路和异常长关系链")
                .defineInRange("maxRelationDepth", 64, 2, 256);
        builder.pop();

        builder.push("addonCompat");
        MAID_MARRIAGE_COMPAT = builder.comment("启用心契同眠关系、礼物与结婚兼容")
                .define("maidMarriageCompat", true);
        MAID_AFFECTION_COMPAT = builder.comment("启用女仆亲亲羁绊能力兼容")
                .define("maidAffectionCompat", true);
        LITTLE_MAD_COMPAT = builder.comment("启用 Little Mad Forge 1.20.1 内置情绪层")
                .define("littleMadCompat", true);
        builder.push("ysm");
        YSM_COMPAT = builder.comment("启用 Yes Steve Model 2.6.5 玩家女仆模型、预览与动画适配")
                .define("enabled", true);
        YSM_INTERACTION_ANIMATIONS = builder.comment("亲密互动成功后触发玩家女仆的 YSM 动画")
                .define("interactionAnimations", true);
        YSM_ANIMATE_OWNER = builder.comment("双人互动时同时尝试播放主人侧的 .owner 动画")
                .define("animateOwner", true);
        YSM_ANIMATION_PET = builder.define("petAnimation", "animation.player.pet");
        YSM_ANIMATION_HUG = builder.define("hugAnimation", "animation.player.hug");
        YSM_ANIMATION_KISS = builder.define("kissAnimation", "animation.player.kiss");
        YSM_ANIMATION_MORNING_KISS = builder.define("morningKissAnimation", "animation.player.morning_kiss");
        YSM_ANIMATION_LAP_PILLOW = builder.define("lapPillowAnimation", "animation.player.lap_pillow");
        YSM_ANIMATION_CONFESS = builder.define("confessAnimation", "animation.player.confess");
        YSM_ANIMATION_MARRY = builder.define("marryAnimation", "animation.player.marry");
        builder.pop();
        builder.push("littleMadMood");
        MOOD_RECOVER_INTERVAL = builder.comment("空闲情绪恢复间隔（tick）")
                .defineInRange("recoverInterval", 200, 20, 72000);
        MOOD_RECOVER_AMOUNT = builder.comment("每次恢复降低的生气值")
                .defineInRange("recoverAmount", 1, 0, 180);
        MOOD_WORK_CHECK_INTERVAL = builder.comment("自动工作情绪判定间隔（tick）")
                .defineInRange("workCheckInterval", 20, 1, 1200);
        MOOD_WORK_ANGER = builder.comment("自动工作判定命中时增加的生气值")
                .defineInRange("workAnger", 1, 0, 180);
        MOOD_WORK_ANGER_CHANCE = builder.comment("每次工作判定增加情绪的概率")
                .defineInRange("workAngerChance", 0.01, 0.0, 1.0);
        MOOD_HURT_BY_OWNER = builder.comment("被主人攻击时增加的基础生气值")
                .defineInRange("hurtByOwner", 20, 0, 180);
        MOOD_HURT_BY_OTHER = builder.comment("被其他玩家攻击时增加的基础生气值")
                .defineInRange("hurtByOther", 1, 0, 180);
        MOOD_DEATH_ANGER = builder.comment("死亡时增加的生气值")
                .defineInRange("deathAnger", 50, 0, 180);
        MOOD_STRIKE_THRESHOLD = builder.comment("开始概率罢工的情绪值")
                .defineInRange("strikeThreshold", 90, 0, 180);
        MOOD_STRIKE_CHANCE = builder.comment("每次自动工作判定的罢工概率")
                .defineInRange("strikeChance", 0.01, 0.0, 1.0);
        builder.pop();

        builder.pop();

        builder.push("gui");
        GUI_ENABLED = builder.comment("主人右键玩家女仆时调用车万女仆本体 EntityMaid.openMaidGui 原生面板")
                .define("enableTouhouStyleGui", true);
        GUI_INTERACTION_DISTANCE = builder.comment("主人操作玩家女仆界面的最大距离")
                .defineInRange("openDistance", 5.0D, 2.0D, 32.0D);
        GUI_STATUS_ONLY_WHEN_INVENTORY_DISABLED = builder.comment("关闭库存管理时仍允许打开只读状态主界面")
                .define("allowStatusOnlyWhenInventoryDisabled", true);
        GUI_TASK_TAB = builder.define("enableTaskTab", true);
        GUI_CONFIG_TAB = builder.define("enableMaidConfigTab", true);
        GUI_BAUBLE_TAB = builder.define("enableBaubleTab", true);
        GUI_CURIOS_TAB = builder.define("enableCuriosTab", true);
        GUI_ENDER_TAB = builder.define("enableEnderChestTab", true);
        GUI_EFFECT_SYNC_INTERVAL = builder.comment("界面状态与效果刷新间隔（tick）")
                .defineInRange("effectSyncInterval", 20, 5, 200);
        builder.pop();

        builder.push("maidUi");
        MAID_UI_PICKUP_RANGE = builder.defineInRange("pickupRange", 3.0D, 1.0D, 16.0D);
        MAID_UI_ALLOW_OWNER_RIDE = builder.define("allowOwnerRide", true);
        MAID_UI_ENABLE_DOOR_INTERACTION = builder.define("enableDoorInteraction", true);
        MAID_UI_ENABLE_FENCE_GATE_INTERACTION = builder.define("enableFenceGateInteraction", true);
        MAID_UI_ENABLE_ACTIVE_CLIMBING = builder.define("enableActiveClimbing", true);
        MAID_UI_DEFAULT_PICKUP = builder.define("defaultPickup", true);
        MAID_UI_DEFAULT_RIDEABLE = builder.define("defaultRideable", false);
        MAID_UI_DEFAULT_SCHEDULE = builder.defineInList("defaultSchedule", "ALL", java.util.List.of("DAY", "NIGHT", "ALL"));
        MAID_UI_DEFAULT_SHOW_BACKPACK = builder.define("defaultShowBackpack", true);
        MAID_UI_DEFAULT_SHOW_BACK_ITEM = builder.define("defaultShowBackItem", true);
        MAID_UI_DEFAULT_SHOW_CHAT_BUBBLE = builder.define("defaultShowChatBubble", true);
        MAID_UI_DEFAULT_SOUND_FREQ = builder.defineInRange("defaultSoundFreq", 50, 0, 100);
        MAID_UI_DEFAULT_PICK_TYPE = builder.defineInRange("defaultPickType", 0, 0, 2);
        MAID_UI_DEFAULT_OPEN_DOOR = builder.define("defaultOpenDoor", true);
        MAID_UI_DEFAULT_OPEN_FENCE_GATE = builder.define("defaultOpenFenceGate", true);
        MAID_UI_DEFAULT_ACTIVE_CLIMBING = builder.define("defaultActiveClimbing", true);
        builder.pop();
        SPEC = builder.build();
    }

    private MaidConfig() {
    }
}
