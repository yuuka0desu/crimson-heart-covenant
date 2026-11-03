package com.narrafork.playermaid;

/**
 * 工作系统的临时公共参数。后续可由主线程把这些值迁移到服务端配置。
 */
public final class WorkConstants {
    public static final int EFFECT_REFRESH_INTERVAL_TICKS = 20;
    public static final int EFFECT_DURATION_TICKS = 60;
    public static final int AUTO_ACTION_INTERVAL_TICKS = 20;
    public static final int REQUEST_TIMEOUT_TICKS = 20 * 60;

    /** 自动近战遵守原版玩家触及距离，不复用最高可达 32 格的工作搜索范围。 */
    public static final double COMBAT_RANGE = 3.0D;
    public static final float COMBAT_READY_STRENGTH = 0.95F;
    public static final double RANGED_RANGE = 24.0D;
    public static final double TRIDENT_RANGE = 16.0D;
    public static final double DANMAKU_RANGE = 24.0D;
    public static final double GUN_RANGE = 32.0D;
    public static final double SPELL_RANGE = 24.0D;
    public static final int BOW_COOLDOWN_TICKS = 20;
    public static final int CROSSBOW_COOLDOWN_TICKS = 25;
    public static final int TRIDENT_COOLDOWN_TICKS = 24;
    public static final int DANMAKU_COOLDOWN_TICKS = 20;
    /** TACZ 举枪动画的最短等待窗口；窗口内不得重复调用 draw，否则动画会被反复重置。 */
    public static final int TACZ_DRAW_TICKS = 12;
    /** 主人距离女仆与目标连线小于该值时跳过射击，避免箭矢擦过主人。 */
    public static final double FRIENDLY_FIRE_CLEARANCE = 1.5D;
    public static final float DANMAKU_DAMAGE = 4.0F;
    public static final float DANMAKU_VELOCITY = 1.8F;
    public static final double ANIMAL_RANGE = 8.0D;
    public static final double EXTINGUISH_ENTITY_RANGE = 8.0D;
    public static final int EXTINGUISH_BLOCK_RANGE = 4;
    public static final int TORCH_SEARCH_RADIUS = 3;
    public static final int TORCH_MAX_LIGHT = 7;

    public static final float COMBAT_DAMAGE_MULTIPLIER = 1.35F;

    private WorkConstants() {
    }
}
