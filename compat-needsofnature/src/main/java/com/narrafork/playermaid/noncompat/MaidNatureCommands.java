package com.narrafork.playermaid.noncompat;

import com.mojang.brigadier.CommandDispatcher;
import com.narrafork.playermaid.RelationshipService;
import com.nonid.api.NonPlayerApi;
import com.nonid.api.NonPlayerStateSnapshot;
import com.nonid.api.NonPregnancySnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MaidNatureCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maidnature")
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> showStatus(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))));
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer viewer = source.getPlayerOrException();
        if (viewer != target && !RelationshipService.canManage(viewer, target)) {
            source.sendFailure(Component.translatable("command.playermaid_non_compat.status.denied"));
            return 0;
        }
        NonPlayerStateSnapshot state = NonPlayerApi.getState(target);
        NonPregnancySnapshot pregnancy = state.pregnancy();
        String pregnancyText = pregnancy.active()
                ? pregnancy.entityTypeId() + " (" + Math.max(0L,
                (pregnancy.endTick() - target.serverLevel().getGameTime()) / 20L) + "s)"
                : "inactive";
        source.sendSuccess(() -> Component.translatable("command.playermaid_non_compat.status",
                        target.getGameProfile().getName(), state.gender().name(),
                        NeedsOfNatureCompat.isBusy(target),
                        state.liquidTank().storedMl(), state.liquidTank().capacityMl(),
                        state.mess().total(), state.destroyedSkin().stage(),
                        state.destroyedSkin().damage(), state.destroyedSkin().maxDamage(), pregnancyText)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private MaidNatureCommands() {
    }
}
