package com.narrafork.playermaid;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipGraphTest {
    private static RelationshipNode player(int value) {
        return RelationshipNode.player(new UUID(0L, value));
    }

    private static RelationshipNode maid(int value) {
        return RelationshipNode.tlmMaid(new UUID(0L, value));
    }

    @Test
    void playerHierarchyInheritsManagement() {
        RelationshipNode a = player(1), b = player(2), c = player(3);
        RelationshipGraph graph = new RelationshipGraph(Map.of(b, a, c, b), 64);
        assertTrue(graph.isDirectManager(b, c));
        assertTrue(graph.canManage(a, c));
        assertEquals(java.util.List.of(b, a), graph.ancestors(c));
        assertEquals(java.util.Set.of(b, c), graph.descendantPlayers(a));
    }

    @Test
    void entityMaidBridgesNativeOwnerAndPlayerMaid() {
        RelationshipNode owner = player(1), entityMaid = maid(2), subordinate = player(3);
        RelationshipGraph graph = new RelationshipGraph(Map.of(entityMaid, owner, subordinate, entityMaid), 64);
        assertTrue(graph.canManage(owner, subordinate));
        assertTrue(graph.isDirectManager(entityMaid, subordinate));
        assertFalse(graph.isDirectManager(owner, subordinate));
    }

    @Test
    void cycleAndOverDepthAreRejected() {
        RelationshipNode a = player(1), b = player(2), c = player(3), d = player(4);
        RelationshipGraph graph = new RelationshipGraph(Map.of(b, a, c, b), 2);
        assertFalse(graph.canBind(a, c));
        assertTrue(graph.wouldCreateCycle(a, c));
        assertFalse(graph.canBind(d, c));
    }

    @Test
    void removingMiddleEdgeRevokesAncestorPermission() {
        RelationshipNode a = player(1), b = player(2), c = player(3);
        RelationshipGraph linked = new RelationshipGraph(Map.of(b, a, c, b), 64);
        RelationshipGraph detached = new RelationshipGraph(Map.of(c, b), 64);
        assertTrue(linked.canManage(a, c));
        assertFalse(detached.canManage(a, c));
        assertTrue(detached.canManage(b, c));
    }
}
