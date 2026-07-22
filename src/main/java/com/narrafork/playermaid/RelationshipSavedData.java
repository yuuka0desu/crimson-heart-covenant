package com.narrafork.playermaid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 世界级关系边存储，实体离线后关系链仍然存在。 */
public final class RelationshipSavedData extends SavedData {
    public static final String DATA_NAME = PlayerMaid.MOD_ID + "_relationships";
    private static final String EDGES = "Edges";
    private static final String NODE_TYPE = "NodeType";
    private static final String NODE_UUID = "NodeUuid";
    private static final String MANAGER_TYPE = "ManagerType";
    private static final String MANAGER_UUID = "ManagerUuid";

    private final Map<RelationshipNode, RelationshipNode> managers = new HashMap<>();

    public static RelationshipSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RelationshipSavedData::load, RelationshipSavedData::new, DATA_NAME);
    }

    public static RelationshipSavedData load(CompoundTag tag) {
        RelationshipSavedData data = new RelationshipSavedData();
        ListTag edges = tag.getList(EDGES, Tag.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++) {
            CompoundTag edge = edges.getCompound(i);
            try {
                RelationshipNode node = new RelationshipNode(
                        RelationshipNodeType.valueOf(edge.getString(NODE_TYPE)), edge.getUUID(NODE_UUID));
                RelationshipNode manager = new RelationshipNode(
                        RelationshipNodeType.valueOf(edge.getString(MANAGER_TYPE)), edge.getUUID(MANAGER_UUID));
                data.managers.put(node, manager);
            } catch (IllegalArgumentException ignored) {
                // 跳过未知类型或损坏 UUID，保留其余离线关系。
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag edges = new ListTag();
        managers.forEach((node, manager) -> {
            CompoundTag edge = new CompoundTag();
            edge.putString(NODE_TYPE, node.type().name());
            edge.putUUID(NODE_UUID, node.uuid());
            edge.putString(MANAGER_TYPE, manager.type().name());
            edge.putUUID(MANAGER_UUID, manager.uuid());
            edges.add(edge);
        });
        tag.put(EDGES, edges);
        return tag;
    }

    public RelationshipGraph graph() {
        return new RelationshipGraph(managers, MaidConfig.MAX_RELATION_DEPTH.get());
    }

    public Optional<RelationshipNode> directManager(RelationshipNode node) {
        return Optional.ofNullable(managers.get(node));
    }

    public boolean bind(RelationshipNode node, RelationshipNode manager) {
        if (!graph().canBind(node, manager)) return false;
        if (!manager.equals(managers.put(node, manager))) setDirty();
        return true;
    }

    public boolean clear(RelationshipNode node) {
        if (managers.remove(node) == null) return false;
        setDirty();
        return true;
    }
}
