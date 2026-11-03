package com.narrafork.playermaid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 弹道几何回归：主人挡在弹道上时必须被识别。 */
class ProjectileAimTest {
    @Test
    void closestPointOnSegmentDetectsOwnerBetweenMaidAndTarget() {
        // 线段沿 X 轴从 (0,0,0) 到 (10,0,0)，主人在 (5,0.5,0)。
        ProjectileAim.ClosestPoint closest = ProjectileAim.closestOnSegment(
                0, 0, 0, 10, 0, 0, 5, 0.5, 0);
        assertTrue(closest.insideSegment());
        assertEquals(0.5, closest.distance(), 1.0E-6D);
    }

    @Test
    void ownerBehindShooterIsNotInsideSegment() {
        ProjectileAim.ClosestPoint closest = ProjectileAim.closestOnSegment(
                0, 0, 0, 10, 0, 0, -3, 0, 0);
        assertFalse(closest.insideSegment());
        assertEquals(3.0, closest.distance(), 1.0E-6D);
    }

    @Test
    void ownerBeyondTargetIsNotInsideSegment() {
        ProjectileAim.ClosestPoint closest = ProjectileAim.closestOnSegment(
                0, 0, 0, 10, 0, 0, 14, 0, 0);
        assertFalse(closest.insideSegment());
        assertEquals(4.0, closest.distance(), 1.0E-6D);
    }

    @Test
    void degenerateSegmentFallsBackToPointDistance() {
        ProjectileAim.ClosestPoint closest = ProjectileAim.closestOnSegment(
                2, 2, 2, 2, 2, 2, 2, 5, 2);
        assertFalse(closest.insideSegment());
        assertEquals(3.0, closest.distance(), 1.0E-6D);
    }

    @Test
    void verticalDeltaAimsAtTargetWhenNoLowArcExists() {
        // 目标高度远超低抛物线可达范围时直接瞄准目标高度。
        assertEquals(24.0D, ProjectileAim.verticalDelta(0.0D, 24.0D, 2.0D, 1.0D), 1.0E-6D);
    }

    @Test
    void verticalDeltaCompensatesForGravityOnFlatGround() {
        double delta = ProjectileAim.verticalDelta(0.0D, 0.0D, 20.0D, 3.0D);
        assertTrue(delta > 0.0D, "平地远距离射击需要向上的补偿量");
    }
}
