package com.narrafork.playermaid;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 主人向指定女仆发起的临时工作请求。 */
public final class WorkRequests {
    public record Request(UUID maidUuid, UUID ownerUuid, String ownerName, WorkMode mode, long expiresAt) {
        public boolean expired(long gameTime) {
            return gameTime >= expiresAt;
        }
    }

    private static final Map<UUID, Request> REQUESTS = new ConcurrentHashMap<>();

    public static Request create(ServerPlayer owner, ServerPlayer maid, WorkMode mode) {
        return create(owner, maid, mode, WorkConstants.REQUEST_TIMEOUT_TICKS);
    }

    public static Request create(ServerPlayer owner, ServerPlayer maid, WorkMode mode, long timeoutTicks) {
        long expiresAt = maid.serverLevel().getGameTime() + Math.max(1L, timeoutTicks);
        Request request = new Request(maid.getUUID(), owner.getUUID(), owner.getGameProfile().getName(), mode, expiresAt);
        REQUESTS.put(maid.getUUID(), request);
        return request;
    }

    public static Optional<Request> get(ServerPlayer maid) {
        return get(maid.getUUID(), maid.serverLevel().getGameTime());
    }

    public static Optional<Request> get(UUID maidUuid, long gameTime) {
        Request request = REQUESTS.get(maidUuid);
        if (request == null) return Optional.empty();
        if (request.expired(gameTime)) {
            REQUESTS.remove(maidUuid, request);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public static Optional<Request> remove(ServerPlayer maid) {
        return remove(maid.getUUID());
    }

    public static Optional<Request> remove(UUID maidUuid) {
        return Optional.ofNullable(REQUESTS.remove(maidUuid));
    }

    public static void cleanup(MinecraftServer server) {
        cleanup(server.overworld().getGameTime());
    }

    public static void cleanup(long gameTime) {
        REQUESTS.entrySet().removeIf(entry -> entry.getValue().expired(gameTime));
    }

    private WorkRequests() {
    }
}
