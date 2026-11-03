package com.narrafork.playermaid.noncompat.mixin;

import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.TeleportHelper;
import com.narrafork.playermaid.noncompat.NeedsOfNatureCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TeleportHelper.class)
public abstract class TeleportHelperMixin {
    @Inject(method = "near", at = @At("HEAD"), cancellable = true, remap = false)
    private static void playermaidNonCompat$blockNear(ServerPlayer maid, LivingEntity owner, CallbackInfo ci) {
        if (NeedsOfNatureCompat.shouldBlockTeleport(maid)) ci.cancel();
    }

    @Inject(method = "toStayPoint", at = @At("HEAD"), cancellable = true, remap = false)
    private static void playermaidNonCompat$blockStayPoint(ServerPlayer maid, MaidData.StayPoint point, CallbackInfo ci) {
        if (NeedsOfNatureCompat.shouldBlockTeleport(maid)) ci.cancel();
    }
}
