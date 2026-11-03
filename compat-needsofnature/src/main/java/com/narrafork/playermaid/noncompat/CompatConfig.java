package com.narrafork.playermaid.noncompat;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CompatConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue INITIALIZE_UNSET_GENDER_AS_FEMALE;
    public static final ForgeConfigSpec.BooleanValue PAUSE_AUTOMATIC_WORK;
    public static final ForgeConfigSpec.BooleanValue BLOCK_TELEPORTS;
    public static final ForgeConfigSpec.BooleanValue BLOCK_NATIVE_GUI;
    public static final ForgeConfigSpec.BooleanValue PRIORITIZE_ANIMATION_JOIN;
    public static final ForgeConfigSpec.BooleanValue USE_NON_INTERACTION_ANIMATIONS;
    public static final ForgeConfigSpec.BooleanValue FALLBACK_TO_YSM;
    public static final ForgeConfigSpec.ConfigValue<String> INTERACTION_TAG_PREFIX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("needsOfNatureCompat");
        INITIALIZE_UNSET_GENDER_AS_FEMALE = builder
                .comment("Initialize a player maid as FEMALE only when Needs of Nature gender is NONE")
                .define("initializeUnsetGenderAsFemale", true);
        PAUSE_AUTOMATIC_WORK = builder
                .comment("Pause Player Maid automatic work while an AFW/NoN animation is pending or active")
                .define("pauseAutomaticWorkDuringAnimations", true);
        BLOCK_TELEPORTS = builder
                .comment("Block Player Maid automatic and command teleports during AFW/NoN animations")
                .define("blockTeleportsDuringAnimations", true);
        BLOCK_NATIVE_GUI = builder
                .comment("Block opening the native Maid GUI while either participant is in an animation")
                .define("blockNativeGuiDuringAnimations", true);
        PRIORITIZE_ANIMATION_JOIN = builder
                .comment("Let an empty-hand right click join a NoN animation before opening the Maid GUI")
                .define("prioritizeAnimationJoin", true);
        USE_NON_INTERACTION_ANIMATIONS = builder
                .comment("Try AFW animations tagged for accepted Player Maid interactions")
                .define("useNeedsOfNatureInteractionAnimations", true);
        FALLBACK_TO_YSM = builder
                .comment("Use the existing YSM interaction animation when no matching AFW animation starts")
                .define("fallbackToYsm", true);
        INTERACTION_TAG_PREFIX = builder
                .comment("AFW animation tag prefix; final tags are prefix + action id, for example playermaid_hug")
                .define("interactionTagPrefix", "playermaid_");
        builder.pop();
        SPEC = builder.build();
    }

    private CompatConfig() {
    }
}
