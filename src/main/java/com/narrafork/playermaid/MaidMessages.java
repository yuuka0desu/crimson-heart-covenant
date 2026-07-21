package com.narrafork.playermaid;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MaidMessages {
    public static Component joined(ServerPlayer player, Component original) {
        if (!MaidData.isMaid(player)) return original;
        return Component.literal(title(player) + "加入了游戏");
    }

    public static Component left(ServerPlayer player, Component original) {
        if (!MaidData.isMaid(player)) return original;
        return Component.literal(title(player) + "退出了游戏");
    }

    public static Component death(ServerPlayer player, Component original) {
        if (!MaidData.isMaid(player)) return original;
        String originalText = original.getString();
        String playerName = player.getGameProfile().getName();
        String replacement = title(player);
        if (originalText.contains(playerName)) {
            return Component.literal(originalText.replaceFirst(java.util.regex.Pattern.quote(playerName),
                    java.util.regex.Matcher.quoteReplacement(replacement)));
        }
        return Component.literal(replacement + " " + originalText);
    }

    public static String title(ServerPlayer player) {
        String owner = MaidData.ownerName(player);
        if (owner == null || owner.isBlank()) owner = "未知主人";
        return owner + "的女仆" + player.getGameProfile().getName();
    }

    private MaidMessages() {
    }
}
