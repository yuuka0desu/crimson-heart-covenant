package com.narrafork.playermaid;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkModeTest {
    @Test
    void maidSpellCombatTasksMapToSpellExecutor() {
        assertEquals(WorkMode.SPELL_ATTACK,
                WorkMode.byTaskUid(new ResourceLocation("maidspell", "spell_combat_far")).orElseThrow());
        assertEquals(WorkMode.SPELL_ATTACK,
                WorkMode.byTaskUid(new ResourceLocation("maidspell", "spell_combat_melee")).orElseThrow());
        assertEquals(new ResourceLocation("maidspell", "spell_combat_far"), WorkMode.SPELL_ATTACK.taskUid());
    }

    @Test
    void lowArcAimAddsGravityCompensationWithoutOvershooting() {
        double compensation = ProjectileAim.verticalDelta(0.0D, 0.0D, 24.0D, 3.0D);
        assertTrue(compensation > 1.0D && compensation < 2.2D);
        assertEquals(0.0D, ProjectileAim.verticalDelta(4.0D, 4.0D, 0.0D, 3.0D), 1.0E-9D);
    }

    @Test
    void everyCombatModeHasAnAutomaticExecutorAndHint() {
        EnumSet<WorkMode> expected = EnumSet.of(WorkMode.ATTACK, WorkMode.RANGED_ATTACK,
                WorkMode.CROSSBOW_ATTACK, WorkMode.DANMAKU_ATTACK, WorkMode.TRIDENT_ATTACK,
                WorkMode.GUN_ATTACK, WorkMode.SPELL_ATTACK);
        EnumSet<WorkMode> actual = EnumSet.noneOf(WorkMode.class);
        for (WorkMode mode : WorkMode.values()) {
            if (mode.isCombat()) actual.add(mode);
            assertFalse(mode.executionHint().isBlank());
        }
        assertEquals(expected, actual);
        assertTrue(actual.stream().allMatch(WorkMode::hasAutomaticExecutor));
    }
}
