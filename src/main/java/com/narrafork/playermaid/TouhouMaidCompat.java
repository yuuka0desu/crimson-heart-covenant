package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.narrafork.playermaid.nativegui.PlayerMaidGuiProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Set;
import java.util.UUID;

public final class TouhouMaidCompat {
    private static final String TLM = "touhou_little_maid";
    private static final String SMART_SLAB_EMPTY = "smart_slab_empty";
    private static final String PLAYER_MAID_UUID = "PlayerMaidTargetUuid";
    private static final String PLAYER_MAID_NAME = "PlayerMaidTargetName";
    private static final String PLAYER_MAID_OWNER_UUID = "PlayerMaidOwnerUuid";
    private static final Set<String> FURNITURE = Set.of(
            "picnic_mat", "gomoku", "cchess", "wchess", "chair", "snack_cabinet",
            "computer", "keyboard", "bookshelf", "shrine", "maid_beacon", "model_switcher"
    );

    public static boolean tryContractWithEntityMaid(ServerPlayer player, EntityMaid maid) {
        if (!MaidConfig.ENTITY_MAID_CONTRACTS.get() || maid instanceof PlayerMaidGuiProxy
                || !player.isShiftKeyDown() || !player.getMainHandItem().is(net.minecraft.world.item.Items.CAKE)) {
            return false;
        }
        if (MaidData.isMaid(player)) {
            player.sendSystemMessage(Component.translatable("message.playermaid.entity_contract.already_bound")
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        if (!maid.isAlive() || !maid.isTame() || maid.getOwnerUUID() == null) {
            player.sendSystemMessage(Component.translatable("message.playermaid.entity_contract.untamed")
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        if (!MaidData.bindToMaid(player, maid)) {
            player.sendSystemMessage(Component.translatable("message.playermaid.entity_contract.cycle")
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        if (MaidConfig.CONSUME_CAKE.get() && !player.isCreative()) player.getMainHandItem().shrink(1);
        AddonRelationshipData.clear(player);
        AddonRelationshipData.setFavorability(player, 0);
        AddonRelationshipData.setMood(player, 0);
        WorkManager.clear(player);
        WorkManager.setMode(player, WorkMode.IDLE);
        WorkManager.setAuto(player, false);
        player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0,
                player.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8F, 1.25F);
        player.sendSystemMessage(Component.translatable("message.playermaid.entity_contract.success", maid.getName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        ServerPlayer topOwner = player.getServer().getPlayerList().getPlayer(maid.getOwnerUUID());
        if (topOwner != null) topOwner.sendSystemMessage(Component.translatable(
                        "message.playermaid.entity_contract.owner", player.getDisplayName(), maid.getName())
                .withStyle(ChatFormatting.AQUA));
        MaidTaskSync.syncNow(maid);
        return true;
    }

    public static boolean tryBindPlayerMaid(ServerPlayer owner, ServerPlayer target) {
        ItemStack stack = owner.getMainHandItem();
        if (!isEmptySmartSlab(stack)) return false;

        if (!RelationshipService.canManage(owner, target)) {
            owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.not_owned")
                    .withStyle(ChatFormatting.RED));
            return true;
        }

        var tag = stack.getOrCreateTag();
        tag.putUUID(PLAYER_MAID_UUID, target.getUUID());
        tag.putString(PLAYER_MAID_NAME, target.getGameProfile().getName());
        tag.putUUID(PLAYER_MAID_OWNER_UUID, owner.getUUID());
        owner.getCooldowns().addCooldown(stack.getItem(), 20);
        owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.bound",
                        target.getGameProfile().getName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        target.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.bound_target",
                        owner.getGameProfile().getName())
                .withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() == InteractionHand.MAIN_HAND && tryRecallPlayerMaid(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (!TLM.equals(id.getNamespace())) return;

        if ("maid_bed".equals(id.getPath()) && MaidData.isMaid(player)) {
            if (player.isShiftKeyDown()) {
                MaidData.setBed(player, event.getPos());
                player.sendSystemMessage(Component.literal("已绑定这张车万女仆床。").withStyle(ChatFormatting.LIGHT_PURPLE));
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            MaidData.bedPoint(player).filter(point -> point.dimension().equals(player.level().dimension().location()))
                    .filter(point -> point.pos().distManhattan(event.getPos()) <= 1)
                    .ifPresent(point -> restAtBed(player));
            return;
        }

        if (MaidData.isMaid(player) && FURNITURE.contains(id.getPath())) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 0, true, false));
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() == InteractionHand.MAIN_HAND && tryRecallPlayerMaid(player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!TLM.equals(id.getNamespace()) || !"servant_bell".equals(id.getPath())) return;

        int count = 0;
        for (ServerPlayer maid : MaidCommands.onlineMaids(player)) {
            MaidData.setFollow(maid);
            TeleportHelper.near(maid, player);
            count++;
        }
        if (count > 0) {
            int finalCount = count;
            player.sendSystemMessage(Component.literal("仆从铃召回了 " + finalCount + " 名玩家女仆。")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static boolean tryRecallPlayerMaid(ServerPlayer owner, ItemStack stack) {
        if (!isEmptySmartSlab(stack) || !stack.hasTag() || !stack.getTag().hasUUID(PLAYER_MAID_UUID)) {
            return false;
        }
        if (owner.getCooldowns().isOnCooldown(stack.getItem())) return true;

        var tag = stack.getTag();
        String targetName = tag.getString(PLAYER_MAID_NAME);
        if (targetName.isBlank()) targetName = tag.getUUID(PLAYER_MAID_UUID).toString();
        if (!tag.hasUUID(PLAYER_MAID_OWNER_UUID)
                || !owner.getUUID().equals(tag.getUUID(PLAYER_MAID_OWNER_UUID))) {
            owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.wrong_owner")
                    .withStyle(ChatFormatting.RED));
            return true;
        }

        UUID targetId = tag.getUUID(PLAYER_MAID_UUID);
        ServerPlayer maid = owner.getServer().getPlayerList().getPlayer(targetId);
        if (maid == null) {
            owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.offline", targetName)
                    .withStyle(ChatFormatting.YELLOW));
            owner.getCooldowns().addCooldown(stack.getItem(), 20);
            return true;
        }
        if (!RelationshipService.canManage(owner, maid)) {
            owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.relation_invalid", targetName)
                    .withStyle(ChatFormatting.RED));
            owner.getCooldowns().addCooldown(stack.getItem(), 20);
            return true;
        }

        TeleportHelper.near(maid, owner);
        owner.getCooldowns().addCooldown(stack.getItem(), 20);
        owner.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.recalled", targetName)
                .withStyle(ChatFormatting.AQUA));
        maid.sendSystemMessage(Component.translatable("message.playermaid.smart_slab.recalled_target",
                        owner.getGameProfile().getName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private static boolean isEmptySmartSlab(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return TLM.equals(id.getNamespace()) && SMART_SLAB_EMPTY.equals(id.getPath());
    }

    public static boolean bindLookedAtBed(ServerPlayer owner, ServerPlayer maid) {
        var hit = owner.pick(6.0, 0.0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return false;
        BlockPos pos = blockHit.getBlockPos();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(owner.level().getBlockState(pos).getBlock());
        if (!TLM.equals(id.getNamespace()) || !"maid_bed".equals(id.getPath())) return false;
        if (owner.serverLevel() != maid.serverLevel()) return false;
        MaidData.setBed(maid, pos);
        return true;
    }

    public static boolean sendToBed(ServerPlayer maid) {
        MaidData.BedPoint point = MaidData.bedPoint(maid).orElse(null);
        if (point == null) return false;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, point.dimension());
        var level = maid.getServer().getLevel(key);
        if (level == null) return false;
        BlockPos pos = point.pos();
        maid.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, maid.getYRot(), maid.getXRot());
        restAtBed(maid);
        return true;
    }

    private static void restAtBed(ServerPlayer maid) {
        if (maid.level().isNight()) {
            maid.heal(8.0F);
            maid.getFoodData().eat(4, 0.6F);
            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            maid.sendSystemMessage(Component.literal("你在绑定的女仆床上休息，恢复了体力。")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            maid.sendSystemMessage(Component.literal("已确认绑定床；夜间可在这里休息。"));
        }
    }

    private TouhouMaidCompat() {
    }
}
