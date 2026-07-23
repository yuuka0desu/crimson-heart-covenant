package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import com.narrafork.playermaid.inventory.MaidInventoryMenu;
import com.narrafork.playermaid.inventory.ViewType;
import com.narrafork.playermaid.nativegui.NativeMaidGuiBridge;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.List;

public final class MaidEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MaidCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer owner)) return;
        if (event.getTarget() instanceof EntityMaid entityMaid) {
            handleEntityMaidContract(event, owner, entityMaid);
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        if (MaidConfig.TOUHOU_COMPAT.get() && ModList.get().isLoaded("touhou_little_maid")
                && TouhouMaidCompat.tryBindPlayerMaid(owner, target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (AddonInteractions.tryRightClick(event, owner, target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (RelationshipService.canManage(owner, target)) {
            if (!NativeMaidGuiBridge.open(owner, target)) {
                owner.sendSystemMessage(Component.literal("当前不能打开玩家女仆原生面板。").withStyle(ChatFormatting.RED));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (!owner.getMainHandItem().is(Items.CAKE)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (owner == target) {
            owner.sendSystemMessage(Component.literal("你不能向自己发起女仆请求。").withStyle(ChatFormatting.RED));
            return;
        }
        if (MaidData.isMaid(target)) {
            owner.sendSystemMessage(Component.literal(target.getGameProfile().getName() + " 已经有主人了。").withStyle(ChatFormatting.RED));
            return;
        }
        if (!MaidCommands.hasCake(owner)) {
            owner.sendSystemMessage(Component.literal("需要至少一个蛋糕才能发起请求。").withStyle(ChatFormatting.RED));
            return;
        }

        MaidRequests.create(owner, target);
        Component accept = Component.literal("[接受]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("接受并成为女仆"))));
        Component reject = Component.literal("[拒绝]").withStyle(style -> style
                .withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid reject"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("拒绝本次请求"))));
        target.sendSystemMessage(Component.literal(owner.getGameProfile().getName() + " 邀请你成为女仆。 ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(accept).append(Component.literal(" ")).append(reject));
        owner.sendSystemMessage(Component.literal("已向 " + target.getGameProfile().getName() + " 发送女仆请求，请等待对方确认。")
                .withStyle(ChatFormatting.AQUA));
    }

    @SubscribeEvent
    public void onPlayerInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof EntityMaid maid)) return;
        handleEntityMaidContract(event, player, maid);
    }

    private static void handleEntityMaidContract(PlayerInteractEvent event, ServerPlayer player, EntityMaid maid) {
        if (TouhouMaidCompat.tryContractWithEntityMaid(player, maid)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof ServerPlayer owner)
                || !(event.getEntityBeingMounted() instanceof ServerPlayer maid) || owner == maid) return;
        if (!MaidConfig.MAID_UI_ALLOW_OWNER_RIDE.get() || !RelationshipService.canManage(owner, maid) || !MaidData.rideable(maid)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (MaidConfig.TOUHOU_COMPAT.get() && ModList.get().isLoaded("touhou_little_maid")) {
            TouhouMaidCompat.onRightClickBlock(event);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (MaidConfig.TOUHOU_COMPAT.get() && ModList.get().isLoaded("touhou_little_maid")) {
            TouhouMaidCompat.onRightClickItem(event);
        }
    }

    @SubscribeEvent
    public void onMaidTick(MaidTickEvent event) {
        MaidTaskSync.tick(event.getMaid());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        WorkManager.tick(player);
        MaidBaubleEffects.tick(player);
        if (player.tickCount % 20 != 0) return;

        if (MaidData.isMaid(player)) {
            LivingEntity owner = RelationshipService.resolveDirectManager(player).orElse(null);
            String modeName = MaidData.mode(player) == MaidData.Mode.FOLLOW ? "跟随" : "待命";
            if (MaidConfig.SHOW_STATUS_BAR.get()) {
                String ownerState = owner == null ? MaidData.ownerName(player) + "（离线）" : owner.getName().getString();
                player.displayClientMessage(Component.literal("女仆 | 主人：" + ownerState + " | 模式：" + modeName
                                + " | 工作：" + WorkManager.getMode(player).chineseName()
                                + " | 自动：" + (WorkManager.isAuto(player) ? "开" : "关")
                                + " | 范围：" + MaidData.activityRangeText(player)
                                + " | " + AddonRelationshipData.relationStage(player).display()
                                + " | 情绪：" + AddonRelationshipData.moodLevel(player).display()
                                + " | 羁绊：" + AddonRelationshipData.bondLevel(player))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            enforceMode(player, owner);
            return;
        }

        if (MaidConfig.SHOW_STATUS_BAR.get()) {
            List<ServerPlayer> maids = MaidCommands.onlineMaids(player);
            if (!maids.isEmpty()) {
                player.displayClientMessage(Component.literal("主人 | 在线女仆：" + maids.size())
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    private void enforceMode(ServerPlayer maid, LivingEntity owner) {
        if (MaidData.isActivityRangeUnlimited(maid)) return;
        double activityRange = MaidData.activityRange(maid);
        if (MaidData.mode(maid) == MaidData.Mode.FOLLOW) {
            if (owner == null) return;
            if (maid.serverLevel() != owner.level() || maid.distanceToSqr(owner) > activityRange * activityRange) {
                TeleportHelper.near(maid, owner);
            }
            return;
        }

        MaidData.stayPoint(maid).ifPresent(point -> {
            double radius = activityRange;
            boolean wrongDimension = !maid.level().dimension().location().equals(point.dimension());
            double dx = maid.getX() - point.x();
            double dy = maid.getY() - point.y();
            double dz = maid.getZ() - point.z();
            if (wrongDimension || dx * dx + dy * dy + dz * dz > radius * radius) {
                TeleportHelper.toStayPoint(maid, point);
            }
        });
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        MaidBaubleEffects.onHurt(event);
        if (event.isCanceled()) return;
        TouhouMaidAffectionCompat.onOwnerHurt(event);
        WorkManager.applyDamageBonus(event);
        LittleMadCompat.applyDamageMultiplier(event);
        if (ModList.get().isLoaded("relics")) {
            RelicsCompat.applyHunterBelt(event);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        MaidBaubleEffects.onDeath(event);
        if (event.getEntity() instanceof ServerPlayer maid) LittleMadCompat.onDeath(maid);
        if (event.getEntity() instanceof EntityMaid maid) MaidTaskSync.onDeath(maid);
    }

    @SubscribeEvent
    public void onPlayerWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer maid) LittleMadCompat.onWake(maid);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer maid && event.getSource().getEntity() instanceof ServerPlayer attacker
                && MaidData.isMaid(maid)) {
            LittleMadCompat.onHurt(maid, attacker);
        }
        if (!MaidConfig.PREVENT_FRIENDLY_FIRE.get() || !(event.getEntity() instanceof ServerPlayer victim)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (RelationshipService.canManage(attacker, victim) || RelationshipService.canManage(victim, attacker)) {
            event.setCanceled(true);
            attacker.displayClientMessage(Component.literal("主人与自己的女仆之间已关闭友伤。")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        CompoundTag oldPersisted = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!oldPersisted.isEmpty()) {
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, oldPersisted.copy());
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) MaidTaskSync.migratePlayer(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MaidRequests.removeFor(event.getEntity().getUUID());
        AddonInteractions.removeFor(event.getEntity().getUUID());
        NativeMaidGuiBridge.closeFor(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        NativeMaidGuiBridge.tick(event.getServer());
        if (event.getServer().getTickCount() % 200 == 0) {
            MaidRequests.cleanup(event.getServer());
            WorkRequests.cleanup(event.getServer());
            AddonInteractions.cleanup(event.getServer());
        }
    }
}
