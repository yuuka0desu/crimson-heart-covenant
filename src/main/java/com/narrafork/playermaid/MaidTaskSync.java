package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 将真实 EntityMaid 的任务沿从属树向下同步给在线玩家女仆。 */
public final class MaidTaskSync {
    private static final Map<UUID, ResourceLocation> LAST_TASKS = new HashMap<>();

    public static void tick(EntityMaid maid) {
        if (maid.level().isClientSide() || maid.getServer() == null || maid.isRemoved()) return;
        refreshNativeOwnerLink(maid);
        if (!MaidConfig.SYNC_MAID_TASKS.get() || maid.getTask() == null) return;

        ResourceLocation uid = maid.getTask().getUid();
        ResourceLocation previous = LAST_TASKS.put(maid.getUUID(), uid);
        if (previous != null && previous.equals(uid)) return;
        syncNow(maid);
    }

    public static void syncNow(EntityMaid maid) {
        if (maid.getServer() == null || maid.getTask() == null || !MaidConfig.SYNC_MAID_TASKS.get()) return;
        refreshNativeOwnerLink(maid);
        ResourceLocation uid = maid.getTask().getUid();
        String taskName = uid.toString();
        MinecraftServer server = maid.getServer();
        for (UUID playerId : RelationshipService.descendantPlayerUuids(server,
                RelationshipNode.tlmMaid(maid.getUUID()))) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || !MaidData.isMaid(player)) continue;
            boolean supported = WorkManager.assignFromMaid(player, maid.getUUID(), uid, taskName,
                    maid.level().getGameTime());
            player.sendSystemMessage(Component.translatable(supported
                            ? "message.playermaid.task.synced" : "message.playermaid.task.unsupported",
                    maid.getName(), taskName).withStyle(supported ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
        }
    }

    public static void refreshNativeOwnerLink(EntityMaid maid) {
        if (maid.getServer() == null) return;
        RelationshipNode maidNode = RelationshipNode.tlmMaid(maid.getUUID());
        UUID owner = maid.getOwnerUUID();
        if (owner == null) {
            RelationshipService.clear(maid.getServer(), maidNode);
        } else {
            RelationshipService.bind(maid.getServer(), maidNode, RelationshipNode.player(owner));
        }
    }

    public static void onDeath(EntityMaid maid) {
        LAST_TASKS.remove(maid.getUUID());
        if (maid.getServer() == null || !MaidConfig.CLEAR_CONTRACTS_ON_MAID_DEATH.get()) return;
        MinecraftServer server = maid.getServer();
        RelationshipNode maidNode = RelationshipNode.tlmMaid(maid.getUUID());
        var edges = Map.copyOf(RelationshipService.graph(server).edges());
        edges.forEach((node, manager) -> {
            if (!manager.equals(maidNode) || node.type() != RelationshipNodeType.PLAYER) return;
            RelationshipService.clear(server, node);
            ServerPlayer player = server.getPlayerList().getPlayer(node.uuid());
            if (player != null) {
                MaidData.clearLocal(player);
                AddonRelationshipData.clear(player);
                WorkManager.clear(player);
                player.sendSystemMessage(Component.translatable("message.playermaid.entity_contract.maid_died")
                        .withStyle(ChatFormatting.YELLOW));
            }
        });
        RelationshipService.clear(server, maidNode);
    }

    public static void migratePlayer(ServerPlayer player) {
        MaidData.directManager(player).ifPresent(manager -> RelationshipService.bind(player.getServer(),
                RelationshipNode.player(player.getUUID()), manager));
    }

    private MaidTaskSync() {
    }
}
