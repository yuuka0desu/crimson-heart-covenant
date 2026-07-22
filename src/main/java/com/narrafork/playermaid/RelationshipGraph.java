package com.narrafork.playermaid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 不依赖 Minecraft 状态的关系图解析器。边由被管理节点指向其直接管理者。
 * 实例是输入关系的不可变快照，适合独立单元测试。
 */
public final class RelationshipGraph {
    public static final int DEFAULT_MAX_DEPTH = 32;

    private final Map<RelationshipNode, RelationshipNode> managers;
    private final int maxDepth;

    public RelationshipGraph(Map<RelationshipNode, RelationshipNode> managers) {
        this(managers, DEFAULT_MAX_DEPTH);
    }

    public RelationshipGraph(Map<RelationshipNode, RelationshipNode> managers, int maxDepth) {
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be positive");
        this.managers = Collections.unmodifiableMap(new HashMap<>(managers));
        this.maxDepth = maxDepth;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public Map<RelationshipNode, RelationshipNode> edges() {
        return managers;
    }

    public Optional<RelationshipNode> directManager(RelationshipNode node) {
        return Optional.ofNullable(managers.get(node));
    }

    public boolean isDirectManager(RelationshipNode manager, RelationshipNode node) {
        return manager.equals(managers.get(node));
    }

    public boolean canManage(RelationshipNode manager, RelationshipNode node) {
        return ancestors(node).contains(manager);
    }

    /** 从直接管理者到最上层管理者排序；损坏图遇环或超过最大深度时停止。 */
    public List<RelationshipNode> ancestors(RelationshipNode node) {
        List<RelationshipNode> result = new ArrayList<>();
        Set<RelationshipNode> visited = new HashSet<>();
        visited.add(node);
        RelationshipNode current = node;
        for (int depth = 0; depth < maxDepth; depth++) {
            RelationshipNode manager = managers.get(current);
            if (manager == null || !visited.add(manager)) break;
            result.add(manager);
            current = manager;
        }
        return List.copyOf(result);
    }

    /** 返回由 manager 直接或间接管理的 PLAYER 节点，不包含 manager 本身。 */
    public Set<RelationshipNode> descendantPlayers(RelationshipNode manager) {
        Set<RelationshipNode> result = new LinkedHashSet<>();
        for (RelationshipNode candidate : allNodes()) {
            if (!candidate.equals(manager) && candidate.type() == RelationshipNodeType.PLAYER
                    && canManage(manager, candidate)) {
                result.add(candidate);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public boolean hasCycle() {
        for (RelationshipNode node : allNodes()) {
            if (chainHasCycle(node)) return true;
        }
        return false;
    }

    public boolean wouldCreateCycle(RelationshipNode node, RelationshipNode manager) {
        if (node.equals(manager)) return true;
        Map<RelationshipNode, RelationshipNode> candidate = new HashMap<>(managers);
        candidate.put(node, manager);
        return new RelationshipGraph(candidate, maxDepth).chainHasCycle(node);
    }

    /** 新边是否合法：无环，且 node 到根的边数不超过最大深度。 */
    public boolean canBind(RelationshipNode node, RelationshipNode manager) {
        if (wouldCreateCycle(node, manager)) return false;
        Map<RelationshipNode, RelationshipNode> candidate = new HashMap<>(managers);
        candidate.put(node, manager);
        RelationshipGraph graph = new RelationshipGraph(candidate, maxDepth);
        return graph.depthFrom(node) <= maxDepth && !graph.chainTruncated(node);
    }

    private int depthFrom(RelationshipNode node) {
        int depth = 0;
        RelationshipNode current = node;
        Set<RelationshipNode> visited = new HashSet<>();
        while (managers.containsKey(current) && visited.add(current)) {
            depth++;
            current = managers.get(current);
        }
        return depth;
    }

    private boolean chainTruncated(RelationshipNode node) {
        RelationshipNode current = node;
        Set<RelationshipNode> visited = new HashSet<>();
        for (int depth = 0; depth < maxDepth; depth++) {
            if (!visited.add(current)) return false;
            RelationshipNode next = managers.get(current);
            if (next == null) return false;
            current = next;
        }
        return managers.containsKey(current);
    }

    private boolean chainHasCycle(RelationshipNode start) {
        Set<RelationshipNode> visited = new HashSet<>();
        RelationshipNode current = start;
        while (current != null) {
            if (!visited.add(current)) return true;
            current = managers.get(current);
        }
        return false;
    }

    private Set<RelationshipNode> allNodes() {
        Set<RelationshipNode> nodes = new LinkedHashSet<>(managers.keySet());
        nodes.addAll(managers.values());
        return nodes;
    }
}
