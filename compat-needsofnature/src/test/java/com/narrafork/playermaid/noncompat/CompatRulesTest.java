package com.narrafork.playermaid.noncompat;

import com.narrafork.playermaid.AddonInteractions;
import com.nonid.api.NonGender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatRulesTest {
    @Test
    void initializesOnlyUnprocessedPlayerMaidWithUnsetGender() {
        assertTrue(CompatRules.shouldInitializeFemale(false, true, NonGender.NONE, true));
        assertFalse(CompatRules.shouldInitializeFemale(true, true, NonGender.NONE, true));
        assertFalse(CompatRules.shouldInitializeFemale(false, false, NonGender.NONE, true));
        assertFalse(CompatRules.shouldInitializeFemale(false, true, NonGender.MALE, true));
        assertFalse(CompatRules.shouldInitializeFemale(false, true, NonGender.FEMALE, true));
        assertFalse(CompatRules.shouldInitializeFemale(false, true, NonGender.BOTH, true));
        assertFalse(CompatRules.shouldInitializeFemale(false, true, NonGender.NONE, false));
    }

    @Test
    void createsStableSanitizedInteractionTags() {
        assertEquals("playermaid_hug", CompatRules.interactionTag("playermaid_", AddonInteractions.Action.HUG));
        assertEquals("custom_prefix_morning_kiss",
                CompatRules.interactionTag(" Custom Prefix! ", AddonInteractions.Action.MORNING_KISS));
    }

    @Test
    void automaticActionsPauseOnlyForBusyPlayerMaids() {
        assertTrue(CompatRules.shouldBlockAutomaticAction(true, true, true));
        assertFalse(CompatRules.shouldBlockAutomaticAction(false, true, true));
        assertFalse(CompatRules.shouldBlockAutomaticAction(true, false, true));
        assertFalse(CompatRules.shouldBlockAutomaticAction(true, true, false));
    }
}
