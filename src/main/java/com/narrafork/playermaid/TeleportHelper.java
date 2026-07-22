package com.narrafork.playermaid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TeleportHelper {
    private static final int[][] OFFSETS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {2, 0}, {-2, 0}, {0, 2}, {0, -2},
            {2, 2}, {-2, 2}, {2, -2}, {-2, -2}
    };

    public static void near(ServerPlayer maid, LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        BlockPos origin = owner.blockPosition();
        for (int[] offset : OFFSETS) {
            BlockPos candidate = origin.offset(offset[0], 0, offset[1]);
            if (isSafe(level, candidate)) {
                maid.teleportTo(level, candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                        owner.getYRot(), maid.getXRot());
                return;
            }
        }
        maid.teleportTo(level, owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), maid.getXRot());
    }

    public static void toStayPoint(ServerPlayer maid, MaidData.StayPoint point) {
        ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(Registries.DIMENSION, point.dimension());
        ServerLevel level = maid.getServer().getLevel(key);
        if (level != null) {
            maid.teleportTo(level, point.x(), point.y(), point.z(), maid.getYRot(), maid.getXRot());
        }
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private TeleportHelper() {
    }
}
