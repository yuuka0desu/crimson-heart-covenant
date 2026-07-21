package com.narrafork.playermaid;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MaidRequests {
    public record Request(UUID ownerUuid, String ownerName, long expiresAt) {
    }

    private static final Map<UUID, Request> REQUESTS = new HashMap<>();

    public static void create(ServerPlayer owner, ServerPlayer target) {
        long expiresAt = target.serverLevel().getGameTime() + MaidConfig.REQUEST_TIMEOUT_SECONDS.get() * 20L;
        REQUESTS.put(target.getUUID(), new Request(owner.getUUID(), owner.getGameProfile().getName(), expiresAt));
    }

    public static Optional<Request> get(ServerPlayer target) {
        Request request = REQUESTS.get(target.getUUID());
        if (request == null) return Optional.empty();
        if (request.expiresAt() < target.serverLevel().getGameTime()) {
            REQUESTS.remove(target.getUUID());
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public static void remove(ServerPlayer target) {
        REQUESTS.remove(target.getUUID());
    }

    public static void removeFor(UUID playerUuid) {
        REQUESTS.entrySet().removeIf(entry -> entry.getKey().equals(playerUuid) || entry.getValue().ownerUuid().equals(playerUuid));
    }

    public static void cleanup(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        REQUESTS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < gameTime);
    }

    private MaidRequests() {
    }
}
