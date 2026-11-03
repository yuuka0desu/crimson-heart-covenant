package com.narrafork.playermaid.noncompat.mixin;

import com.narrafork.playermaid.MaidEvents;
import com.narrafork.playermaid.noncompat.NeedsOfNatureCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MaidEvents.class)
public abstract class MaidEventsMixin {
    @Inject(method = "enforceMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void playermaidNonCompat$pauseRangeEnforcement(ServerPlayer maid, LivingEntity owner, CallbackInfo ci) {
        if (NeedsOfNatureCompat.shouldBlockTeleport(maid)) ci.cancel();
    }
}
