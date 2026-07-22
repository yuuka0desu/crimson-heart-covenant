package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 关系核心的统一读写入口，同时维护玩家女仆 NBT 与世界 SavedData。 */
public final class RelationshipService {
    public static RelationshipSavedData data(MinecraftServer server) {
        return RelationshipSavedData.get(server);
    }

    public static RelationshipGraph graph(MinecraftServer server) {
        return data(server).graph();
    }

    public static Optional<RelationshipNode> directManager(MinecraftServer server, RelationshipNode node) {
        return data(server).directManager(node);
    }

    public static boolean isDirectManager(MinecraftServer server, RelationshipNode manager, RelationshipNode node) {
        return graph(server).isDirectManager(manager, node);
    }

    public static boolean canManage(MinecraftServer server, RelationshipNode manager, RelationshipNode node) {
        return MaidConfig.INHERIT_MANAGEMENT.get()
                ? graph(server).canManage(manager, node)
                : graph(server).isDirectManager(manager, node);
    }

    public static List<RelationshipNode> ancestors(MinecraftServer server, RelationshipNode node) {
        return graph(server).ancestors(node);
    }

    public static Set<RelationshipNode> descendantPlayers(MinecraftServer server, RelationshipNode manager) {
        return graph(server).descendantPlayers(manager);
    }

    public static boolean hasCycle(MinecraftServer server) {
        return graph(server).hasCycle();
    }

    public static boolean bindToPlayer(ServerPlayer playerMaid, ServerPlayer manager) {
        RelationshipNode node = RelationshipNode.player(playerMaid.getUUID());
        RelationshipNode managerNode = RelationshipNode.player(manager.getUUID());
        RelationshipSavedData data = data(playerMaid.getServer());
        if (!data.bind(node, managerNode)) return false;
        MaidData.writeManager(playerMaid, managerNode, manager.getGameProfile().getName());
        return true;
    }

    public static boolean bindToMaid(ServerPlayer playerMaid, EntityMaid manager) {
        RelationshipNode node = RelationshipNode.player(playerMaid.getUUID());
        RelationshipNode managerNode = RelationshipNode.tlmMaid(manager.getUUID());
        RelationshipSavedData data = data(playerMaid.getServer());
        UUID tlmOwner = manager.getOwnerUUID();
        if (tlmOwner != null && !data.bind(managerNode, RelationshipNode.player(tlmOwner))) return false;
        if (!data.bind(node, managerNode)) return false;
        MaidData.writeManager(playerMaid, managerNode, manager.getName().getString());
        return true;
    }

    /** 供离线迁移或其他关系核心调用；不会访问在线实体 NBT。 */
    public static boolean bind(MinecraftServer server, RelationshipNode node, RelationshipNode manager) {
        return data(server).bind(node, manager);
    }

    public static boolean clear(ServerPlayer playerMaid) {
        boolean changed = data(playerMaid.getServer()).clear(RelationshipNode.player(playerMaid.getUUID()));
        MaidData.clearLocal(playerMaid);
        return changed;
    }

    public static boolean clear(MinecraftServer server, RelationshipNode node) {
        return data(server).clear(node);
    }

    public static boolean canManage(ServerPlayer manager, ServerPlayer playerMaid) {
        return canManage(manager.getServer(), RelationshipNode.player(manager.getUUID()),
                RelationshipNode.player(playerMaid.getUUID()));
    }

    public static boolean isDirectManager(ServerPlayer manager, ServerPlayer playerMaid) {
        return isDirectManager(manager.getServer(), RelationshipNode.player(manager.getUUID()),
                RelationshipNode.player(playerMaid.getUUID()));
    }

    public static Optional<LivingEntity> resolveDirectManager(ServerPlayer playerMaid) {
        Optional<RelationshipNode> manager = MaidData.directManager(playerMaid);
        if (manager.isEmpty()) return Optional.empty();
        RelationshipNode node = manager.get();
        if (node.type() == RelationshipNodeType.PLAYER) {
            return Optional.ofNullable(playerMaid.getServer().getPlayerList().getPlayer(node.uuid()));
        }
        for (var level : playerMaid.getServer().getAllLevels()) {
            Entity entity = level.getEntity(node.uuid());
            if (entity instanceof LivingEntity living) return Optional.of(living);
        }
        return Optional.empty();
    }

    public static Optional<ServerPlayer> topPlayerManager(ServerPlayer playerMaid) {
        ServerPlayer result = null;
        for (RelationshipNode node : ancestors(playerMaid.getServer(), RelationshipNode.player(playerMaid.getUUID()))) {
            if (node.type() == RelationshipNodeType.PLAYER) {
                ServerPlayer online = playerMaid.getServer().getPlayerList().getPlayer(node.uuid());
                if (online != null) result = online;
            }
        }
        return Optional.ofNullable(result);
    }

    public static Set<UUID> descendantPlayerUuids(MinecraftServer server, RelationshipNode manager) {
        return descendantPlayers(server, manager).stream().map(RelationshipNode::uuid)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private RelationshipService() {
    }
}
