package com.narrafork.playermaid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TACZ 自动开火状态机回归：绘枪窗口内不得重复举枪。 */
class TaczFireGateTest {
    @Test
    void drawsFirstThenHoldsInsideWindow() {
        assertEquals(TaczFireGate.Step.DRAW,
                TaczFireGate.next(100L, 0L, true, false, true));

        long windowEnd = TaczFireGate.drawWindowEnd(100L, WorkConstants.TACZ_DRAW_TICKS);
        assertEquals(100L + WorkConstants.TACZ_DRAW_TICKS, windowEnd);

        // 窗口内每 tick 都只能是 HOLD，绝不能再次 DRAW，否则动画会被反复重置。
        for (long now = 100L; now < windowEnd; now++) {
            assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(now, windowEnd, true, false, true),
                    "窗口内不得重复举枪, now=" + now);
        }
    }

    @Test
    void shootsAfterWindowElapsed() {
        long windowEnd = TaczFireGate.drawWindowEnd(100L, WorkConstants.TACZ_DRAW_TICKS);
        assertEquals(TaczFireGate.Step.SHOOT,
                TaczFireGate.next(windowEnd, windowEnd, true, false, true));
        assertEquals(TaczFireGate.Step.SHOOT,
                TaczFireGate.next(windowEnd + 50L, windowEnd, true, false, true));
    }

    @Test
    void notDrawnResetsWindowSoNextTickDrawsAgain() {
        long reset = TaczFireGate.resetAfterNotDrawn();
        assertEquals(0L, reset);
        assertEquals(TaczFireGate.Step.DRAW, TaczFireGate.next(500L, reset, true, false, true));
    }

    @Test
    void holdsWhenAutoFireOffOrBusyOrNoTarget() {
        long windowEnd = TaczFireGate.drawWindowEnd(100L, WorkConstants.TACZ_DRAW_TICKS);
        assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(500L, windowEnd, false, false, true));
        assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(500L, windowEnd, true, true, true));
        assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(500L, windowEnd, true, false, false));
        // 关闭自动开火时即使窗口为空也不能自行举枪。
        assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(500L, 0L, false, false, true));
    }

    @Test
    void manualFireTakesPriorityOverWindow() {
        long windowEnd = TaczFireGate.drawWindowEnd(100L, WorkConstants.TACZ_DRAW_TICKS);
        assertEquals(TaczFireGate.Step.HOLD, TaczFireGate.next(windowEnd + 1L, windowEnd, true, true, true));
    }

    @Test
    void describeCoversAllSteps() {
        assertTrue(TaczFireGate.describe(TaczFireGate.Step.HOLD).length() > 0);
        assertTrue(TaczFireGate.describe(TaczFireGate.Step.DRAW).length() > 0);
        assertTrue(TaczFireGate.describe(TaczFireGate.Step.SHOOT).length() > 0);
        assertFalse(TaczFireGate.describe(TaczFireGate.Step.DRAW).equals(TaczFireGate.describe(TaczFireGate.Step.SHOOT)));
    }
}
