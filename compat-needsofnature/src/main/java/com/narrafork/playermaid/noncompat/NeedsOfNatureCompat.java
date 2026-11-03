package com.narrafork.playermaid.noncompat;

import com.afwid.api.AfwAnimationApi;
import com.afwid.api.AfwDamageBehavior;
import com.narrafork.playermaid.AddonInteractions;
import com.narrafork.playermaid.MaidData;
import com.nonid.NeedsOfNature;
import com.nonid.api.NonChangeSource;
import com.nonid.api.NonGender;
import com.nonid.api.NonPlayerApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class NeedsOfNatureCompat {
    private static final String ROOT = "PlayerMaidNeedsOfNatureCompat";
    private static final String GENDER_INITIALIZED = "GenderInitialized";

    public static boolean isBusy(ServerPlayer player) {
        if (player == null || player.isRemoved()) return false;
        return NeedsOfNature.isActorPendingOrActive(player.serverLevel(), player.getUUID())
                || AfwAnimationApi.isActorPendingOrActive(player.serverLevel(), player.getUUID());
    }

    public static boolean shouldPauseWork(ServerPlayer player) {
        return CompatRules.shouldBlockAutomaticAction(CompatConfig.PAUSE_AUTOMATIC_WORK.get(),
                MaidData.isMaid(player), isBusy(player));
    }

    public static boolean shouldBlockTeleport(ServerPlayer player) {
        return CompatRules.shouldBlockAutomaticAction(CompatConfig.BLOCK_TELEPORTS.get(),
                MaidData.isMaid(player), isBusy(player));
    }

    public static boolean shouldBlockGui(ServerPlayer owner, ServerPlayer target) {
        return CompatConfig.BLOCK_NATIVE_GUI.get() && (isBusy(owner) || isBusy(target));
    }

    public static void initializeGenderIfNeeded(ServerPlayer player) {
        if (!MaidData.isMaid(player)) return;
        CompoundTag root = writableRoot(player);
        boolean processed = root.getBoolean(GENDER_INITIALIZED);
        NonGender gender = NonPlayerApi.getGender(player);
        if (processed) return;
        if (gender != NonGender.NONE) {
            root.putBoolean(GENDER_INITIALIZED, true);
            return;
        }
        if (!CompatRules.shouldInitializeFemale(false, true, gender,
                CompatConfig.INITIALIZE_UNSET_GENDER_AS_FEMALE.get())) return;
        if (NonPlayerApi.canChangeGender(player, NonChangeSource.SYSTEM)
                && NonPlayerApi.setGender(player, NonGender.FEMALE, NonChangeSource.SYSTEM)) {
            root.putBoolean(GENDER_INITIALIZED, true);
            PlayerMaidNonCompat.LOGGER.info("Initialized unset Needs of Nature gender for player maid {}",
                    player.getGameProfile().getName());
        }
    }

    /**
     * Consumes an empty-hand interaction when the target owns an active NoN animation instance.
     */
    public static boolean tryJoinTargetAnimation(ServerPlayer joiningPlayer, ServerPlayer target) {
        if (!CompatConfig.PRIORITIZE_ANIMATION_JOIN.get() || !isBusy(target)) return false;
        UUID instanceId = NeedsOfNature.getActivePlayerInstance(target);
        if (instanceId == null) return true;
        boolean joined = NeedsOfNature.joinAnimationNow(joiningPlayer.serverLevel(), instanceId,
                List.of(joiningPlayer), joiningPlayer);
        if (!joined) {
            PlayerMaidNonCompat.LOGGER.debug("NoN animation join was consumed but no expanded match started: player={} target={} instance={}",
                    joiningPlayer.getGameProfile().getName(), target.getGameProfile().getName(), instanceId);
        }
        return true;
    }

    public static boolean tryPlayInteraction(ServerPlayer owner, ServerPlayer maid,
                                             AddonInteractions.Action action) {
        if (!CompatConfig.USE_NON_INTERACTION_ANIMATIONS.get()
                || owner == null || maid == null || action == null
                || owner.serverLevel() != maid.serverLevel() || isBusy(owner) || isBusy(maid)) {
            return false;
        }
        String tag = CompatRules.interactionTag(CompatConfig.INTERACTION_TAG_PREFIX.get(), action);
        List<ServerPlayer> actors = List.of(owner, maid);
        NeedsOfNature.AfwMatchedAnimation match = NeedsOfNature.findAnimationForActors(owner.serverLevel(), actors,
                Set.of(tag), maid, true, null, Set.of());
        if (match == null) return false;
        UUID instanceId = NeedsOfNature.startAnimationNowWithMetadata(owner.serverLevel(), match.animationId(),
                actors, match.actorKeys(), match.stages(), AfwDamageBehavior.STOP_ON_DAMAGE,
                false, maid, NeedsOfNature.buildModeMetadata(false), owner, false);
        if (instanceId == null) return false;
        PlayerMaidNonCompat.LOGGER.debug("Started NoN interaction animation {} for {} and {} using tag {}",
                match.animationId(), owner.getGameProfile().getName(), maid.getGameProfile().getName(), tag);
        return true;
    }

    public static boolean shouldFallbackToYsm() {
        return CompatConfig.FALLBACK_TO_YSM.get();
    }

    private static CompoundTag writableRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        CompoundTag root = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) persisted.put(ROOT, root);
        return root;
    }

    private NeedsOfNatureCompat() {
    }
}
