package com.narrafork.playermaid;

import java.util.Objects;
import java.util.UUID;

/** 关系图中的稳定身份；UUID 使节点在对应实体离线时仍可解析。 */
public record RelationshipNode(RelationshipNodeType type, UUID uuid) {
    public RelationshipNode {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(uuid, "uuid");
    }

    public static RelationshipNode player(UUID uuid) {
        return new RelationshipNode(RelationshipNodeType.PLAYER, uuid);
    }

    public static RelationshipNode tlmMaid(UUID uuid) {
        return new RelationshipNode(RelationshipNodeType.TLM_MAID, uuid);
    }
}
