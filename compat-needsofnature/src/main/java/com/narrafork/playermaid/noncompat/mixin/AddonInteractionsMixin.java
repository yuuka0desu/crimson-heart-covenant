package com.narrafork.playermaid.noncompat.mixin;

import com.narrafork.playermaid.AddonInteractions;
import com.narrafork.playermaid.YsmCompat;
import com.narrafork.playermaid.noncompat.NeedsOfNatureCompat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AddonInteractions.class)
public abstract class AddonInteractionsMixin {
    @Redirect(method = "applyAccepted", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lcom/narrafork/playermaid/YsmCompat;playInteraction(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerPlayer;Lcom/narrafork/playermaid/AddonInteractions$Action;)Z"))
    private static boolean playermaidNonCompat$playNoNOrYsm(ServerPlayer owner, ServerPlayer maid,
                                                            AddonInteractions.Action action) {
        if (NeedsOfNatureCompat.tryPlayInteraction(owner, maid, action)) {
            return true;
        }
        return NeedsOfNatureCompat.shouldFallbackToYsm()
                && YsmCompat.playInteraction(owner, maid, action);
    }
}
