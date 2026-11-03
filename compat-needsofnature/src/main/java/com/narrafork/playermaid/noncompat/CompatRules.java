package com.narrafork.playermaid.noncompat;

import com.narrafork.playermaid.AddonInteractions;
import com.nonid.api.NonGender;

import java.util.Locale;

public final class CompatRules {
    public static boolean shouldInitializeFemale(boolean alreadyProcessed, boolean isPlayerMaid,
                                                 NonGender currentGender, boolean enabled) {
        return enabled && !alreadyProcessed && isPlayerMaid && currentGender == NonGender.NONE;
    }

    public static String interactionTag(String prefix, AddonInteractions.Action action) {
        String safePrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
        return safePrefix + action.id().toLowerCase(Locale.ROOT);
    }

    public static boolean shouldBlockAutomaticAction(boolean enabled, boolean isPlayerMaid, boolean animationBusy) {
        return enabled && isPlayerMaid && animationBusy;
    }

    private CompatRules() {
    }
}
