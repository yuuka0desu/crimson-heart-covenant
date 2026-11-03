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

    /** 点到线段最近点的结果；insideSegment 为 false 时最近点是线段端点。 */
    public record ClosestPoint(boolean insideSegment, double distance) { }

    /**
     * 计算点 P 到线段 AB 的最短距离。用于判断主人是否挡在女仆与目标之间的弹道上。
     * 线段退化为一点时按点距处理。
     */
    public static ClosestPoint closestOnSegment(double ax, double ay, double az,
                                                double bx, double by, double bz,
                                                double px, double py, double pz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double lengthSqr = dx * dx + dy * dy + dz * dz;
        if (lengthSqr < EPSILON) {
            return new ClosestPoint(false, Math.sqrt(sqr(px - ax) + sqr(py - ay) + sqr(pz - az)));
        }
        double t = ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / lengthSqr;
        boolean inside = t > 0.0D && t < 1.0D;
        double clamped = Math.max(0.0D, Math.min(1.0D, t));
        double cx = ax + dx * clamped;
        double cy = ay + dy * clamped;
        double cz = az + dz * clamped;
        return new ClosestPoint(inside, Math.sqrt(sqr(px - cx) + sqr(py - cy) + sqr(pz - cz)));
    }

    private static double sqr(double value) {
        return value * value;
    }

    private ProjectileAim() {
    }
}
