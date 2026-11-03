package com.narrafork.playermaid.noncompat.mixin;

import com.narrafork.playermaid.WorkManager;
import com.narrafork.playermaid.noncompat.NeedsOfNatureCompat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorkManager.class)
public abstract class WorkManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void playermaidNonCompat$pauseAutomaticWork(ServerPlayer maid, CallbackInfo ci) {
        if (NeedsOfNatureCompat.shouldPauseWork(maid)) ci.cancel();
    }
}
