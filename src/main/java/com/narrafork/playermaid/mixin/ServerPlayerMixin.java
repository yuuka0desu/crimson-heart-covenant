package com.narrafork.playermaid.mixin;

import com.narrafork.playermaid.MaidMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;", remap = false),
            remap = false, require = 0)
    private Component playermaid$deathDev(CombatTracker tracker) {
        return MaidMessages.death((ServerPlayer) (Object) this, tracker.getDeathMessage());
    }

    @Redirect(method = "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;m_19293_()Lnet/minecraft/network/chat/Component;", remap = false),
            remap = false, require = 0)
    private Component playermaid$deathSrg(CombatTracker tracker) {
        return MaidMessages.death((ServerPlayer) (Object) this, tracker.getDeathMessage());
    }
}
