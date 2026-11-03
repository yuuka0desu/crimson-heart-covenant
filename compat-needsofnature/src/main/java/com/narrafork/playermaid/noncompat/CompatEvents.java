package com.narrafork.playermaid.noncompat;

import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.RelationshipService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

public final class CompatEvents {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        NeedsOfNatureCompat.initializeGenderIfNeeded(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof ServerPlayer target) || !MaidData.isMaid(target)
                || !RelationshipService.canManage(player, target) || !player.getMainHandItem().isEmpty()) return;
        if (NeedsOfNatureCompat.isBusy(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (NeedsOfNatureCompat.tryJoinTargetAnimation(player, target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MaidNatureCommands.register(event.getDispatcher());
    }
}
