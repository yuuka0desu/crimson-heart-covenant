package com.narrafork.playermaid;

/**
 * TACZ 自动开火的纯状态机，便于单元测试。
 *
 * <p>核心约束：举枪动画窗口内绝不可重复调用 draw()，否则动画会被反复重置，
 * shoot() 将长期返回 IS_DRAWING，表现为「枪永远不开火」。</p>
 */
public final class TaczFireGate {
    public enum Step {
        /** 本 tick 不动作：未开启自动开火、正在手动使用物品、没有目标，或仍在举枪窗口内。 */
        HOLD,
        /** 需要调用一次 draw() 并进入举枪窗口。 */
        DRAW,
        /** 可以尝试 shoot()。 */
        SHOOT
    }

    public static Step next(long now, long drawUntil, boolean autoFire, boolean usingItem, boolean hasTarget) {
        if (!autoFire || usingItem || !hasTarget) return Step.HOLD;
        if (drawUntil <= 0L) return Step.DRAW;
        if (now < drawUntil) return Step.HOLD;
        return Step.SHOOT;
    }

    /** 记录举枪窗口结束时刻。 */
    public static long drawWindowEnd(long now, int ticks) {
        return now + Math.max(0, ticks);
    }

    /** shoot() 返回 NOT_DRAW 时重置，使下一次 tick 重新举枪。 */
    public static long resetAfterNotDrawn() {
        return 0L;
    }

    /** 未举枪时驱动扫射的可读标签。 */
    public static String describe(Step step) {
        return switch (step) {
            case HOLD -> "等待";
            case DRAW -> "举枪";
            case SHOOT -> "射击";
        };
    }

    private TaczFireGate() {
    }
}
