package com.narrafork.playermaid;

/** 低抛物线投射物瞄准计算，参数单位与 Minecraft tick 物理一致。 */
public final class ProjectileAim {
    private static final double EPSILON = 1.0E-6D;

    /**
     * 返回从发射点到目标点所需的竖直分量。没有可行的低抛物线时直接瞄准目标高度。
     */
    public static double verticalDelta(double startY, double targetY, double horizontal, double velocity) {
        double direct = targetY - startY;
        if (horizontal < EPSILON || velocity <= EPSILON) return direct;

        double gravity = 0.05D;
        double velocitySquared = velocity * velocity;
        double discriminant = velocitySquared * velocitySquared
                - gravity * (gravity * horizontal * horizontal + 2.0D * direct * velocitySquared);
        if (discriminant < 0.0D) return direct;

        double tangent = (velocitySquared - Math.sqrt(discriminant)) / (gravity * horizontal);
        if (!Double.isFinite(tangent)) return direct;
        return tangent * horizontal;
    }

    private ProjectileAim() {
    }
}
