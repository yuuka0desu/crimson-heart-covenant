package com.narrafork.playermaid.noncompat.mixin;

import com.narrafork.playermaid.nativegui.NativeMaidGuiBridge;
import com.narrafork.playermaid.noncompat.NeedsOfNatureCompat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NativeMaidGuiBridge.class)
public abstract class NativeMaidGuiBridgeMixin {
    @Inject(method = "open", at = @At("HEAD"), cancellable = true, remap = false)
    private static void playermaidNonCompat$blockBusyGui(ServerPlayer owner, ServerPlayer target,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (NeedsOfNatureCompat.shouldBlockGui(owner, target)) cir.setReturnValue(false);
    }
}
