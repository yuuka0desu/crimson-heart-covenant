package com.narrafork.playermaid.mixin;

import com.narrafork.playermaid.MaidMessages;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", remap = false),
            remap = false, require = 0)
    private void playermaid$joinDev(PlayerList list, Component original, boolean overlay,
                                    Connection connection, ServerPlayer player) {
        list.broadcastSystemMessage(MaidMessages.joined(player, original), overlay);
    }

    @Redirect(method = "m_11261_(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V", remap = false),
            remap = false, require = 0)
    private void playermaid$joinSrg(PlayerList list, Component original, boolean overlay,
                                    Connection connection, ServerPlayer player) {
        list.broadcastSystemMessage(MaidMessages.joined(player, original), overlay);
    }

    @Redirect(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", remap = false),
            remap = false, require = 0)
    private void playermaid$quitDev(PlayerList list, Component original, boolean overlay,
                                    ServerPlayer player) {
        list.broadcastSystemMessage(MaidMessages.left(player, original), overlay);
    }

    @Redirect(method = "m_11286_(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;m_240416_(Lnet/minecraft/network/chat/Component;Z)V", remap = false),
            remap = false, require = 0)
    private void playermaid$quitSrg(PlayerList list, Component original, boolean overlay,
                                    ServerPlayer player) {
        list.broadcastSystemMessage(MaidMessages.left(player, original), overlay);
    }
}
