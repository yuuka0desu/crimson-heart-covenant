package com.narrafork.playermaid;

/**
 * 工作系统的临时公共参数。后续可由主线程把这些值迁移到服务端配置。
 */
public final class WorkConstants {
    public static final int EFFECT_REFRESH_INTERVAL_TICKS = 20;
    public static final int EFFECT_DURATION_TICKS = 60;
    public static final int AUTO_ACTION_INTERVAL_TICKS = 20;
    public static final int REQUEST_TIMEOUT_TICKS = 20 * 60;

    public static final double COMBAT_RANGE = 8.0D;
    public static final double ANIMAL_RANGE = 8.0D;
    public static final double EXTINGUISH_ENTITY_RANGE = 8.0D;
    public static final int EXTINGUISH_BLOCK_RANGE = 4;
    public static final int TORCH_SEARCH_RADIUS = 3;
    public static final int TORCH_MAX_LIGHT = 7;

    public static final float COMBAT_DAMAGE_MULTIPLIER = 1.35F;

    private WorkConstants() {
    }
}
