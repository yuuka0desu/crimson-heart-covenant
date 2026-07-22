package com.narrafork.playermaid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.narrafork.playermaid.inventory.MaidBaubleInventory;
import com.narrafork.playermaid.inventory.MaidInventoryMenu;
import com.narrafork.playermaid.inventory.ViewType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class MaidCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maid")
                .then(Commands.literal("accept").executes(context -> accept(context.getSource())))
                .then(Commands.literal("reject").executes(context -> reject(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("baubles").executes(context -> openOwnBaubles(context.getSource())))
                .then(Commands.literal("leave").executes(context -> leave(context.getSource())))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("range")
                        .then(Commands.literal("self")
                                .executes(context -> showSelfRange(context.getSource()))
                                .then(Commands.literal("unlimited")
                                        .executes(context -> setSelfRangeUnlimited(context.getSource())))
                                .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(
                                                MaidData.MIN_ACTIVITY_RANGE, MaidData.MAX_ACTIVITY_RANGE))
                                        .executes(context -> setSelfRange(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "blocks")))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> showOtherRange(context.getSource(), EntityArgument.getPlayer(context, "player")))
                                .then(Commands.literal("unlimited")
                                        .executes(context -> setOtherRangeUnlimited(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))))
                                .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(
                                                MaidData.MIN_ACTIVITY_RANGE, MaidData.MAX_ACTIVITY_RANGE))
                                        .executes(context -> setOtherRange(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                DoubleArgumentType.getDouble(context, "blocks"))))))
                .then(Commands.literal("follow")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> follow(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("stay")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> stay(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("summon")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> summon(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("release")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> release(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("inventory")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> openInventory(context.getSource(), EntityArgument.getPlayer(context, "player"), ViewType.COMBINED))))
                .then(Commands.literal("enderchest")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> openInventory(context.getSource(), EntityArgument.getPlayer(context, "player"), ViewType.ENDER_CHEST))))
                .then(Commands.literal("curios")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> openInventory(context.getSource(), EntityArgument.getPlayer(context, "player"), ViewType.COMBINED))))
                .then(Commands.literal("work")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(WorkMode.values()).map(WorkMode::id), builder))
                                        .executes(context -> requestWork(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "mode"))))))
                .then(Commands.literal("workaccept").executes(context -> acceptWork(context.getSource())))
                .then(Commands.literal("workreject").executes(context -> rejectWork(context.getSource())))
                .then(Commands.literal("auto")
                        .then(Commands.literal("self")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setSelfAuto(context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setOtherAuto(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                BoolArgumentType.getBool(context, "enabled"))))))
                .then(Commands.literal("bed")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> bindBed(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("sleep")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> sendToBed(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("interact")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("action", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(AddonInteractions.Action.values()).map(AddonInteractions.Action::id), builder))
                                        .executes(context -> requestInteraction(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "action"))))))
                .then(Commands.literal("interactaccept").executes(context -> acceptInteraction(context.getSource())))
                .then(Commands.literal("interactreject").executes(context -> rejectInteraction(context.getSource())))
                .then(Commands.literal("relation")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> relation(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("bond")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> bond(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("bondunlock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TouhouMaidAffectionCompat.ABILITIES, builder))
                                        .executes(context -> unlockBond(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "ability"))))))
                .then(Commands.literal("bondauto")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setBondAuto(context.getSource(), BoolArgumentType.getBool(context, "enabled")))))
                .then(Commands.literal("forcebind").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("owner", EntityArgument.player())
                                .then(Commands.argument("maid", EntityArgument.player())
                                        .executes(context -> forceBind(context.getSource(),
                                                EntityArgument.getPlayer(context, "owner"),
                                                EntityArgument.getPlayer(context, "maid"))))))
                .then(Commands.literal("forceclear").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> forceClear(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
    }

    public static boolean bind(ServerPlayer owner, ServerPlayer maid, boolean consumeCake) {
        if (owner == maid || MaidData.isMaid(maid)) return false;
        if (consumeCake && MaidConfig.CONSUME_CAKE.get() && !owner.isCreative()) {
            int slot = findCake(owner);
            if (slot < 0) return false;
            owner.getInventory().getItem(slot).shrink(1);
        }
        if (!MaidData.bindToPlayer(maid, owner)) return false;
        AddonRelationshipData.clear(maid);
        AddonRelationshipData.setFavorability(maid, 0);
        AddonRelationshipData.setMood(maid, 0);
        WorkManager.clear(maid);
        WorkManager.setMode(maid, WorkMode.IDLE);
        WorkManager.setAuto(maid, false);
        MaidRequests.remove(maid);
        ServerLevel level = maid.serverLevel();
        level.sendParticles(ParticleTypes.HEART, maid.getX(), maid.getY() + 1.0, maid.getZ(), 12, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, maid.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.25F);
        owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName() + " 已成为你的女仆！").withStyle(ChatFormatting.LIGHT_PURPLE));
        maid.sendSystemMessage(Component.literal("你已成为 " + owner.getGameProfile().getName() + " 的女仆。使用 /maid status 查看状态。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static boolean hasCake(ServerPlayer player) {
        return player.isCreative() || findCake(player) >= 0;
    }

    private static int accept(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        MaidRequests.Request request = MaidRequests.get(maid).orElse(null);
        if (request == null) return fail(source, "当前没有有效的女仆请求。");
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(request.ownerUuid());
        if (owner == null) {
            MaidRequests.remove(maid);
            return fail(source, "请求者已离线，请让对方重新发起请求。");
        }
        if (!bind(owner, maid, true)) return fail(source, "绑定失败：你可能已有主人，或请求者已没有蛋糕。");
        return 1;
    }

    private static int reject(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        MaidRequests.Request request = MaidRequests.get(maid).orElse(null);
        if (request == null) return fail(source, "当前没有有效的女仆请求。");
        MaidRequests.remove(maid);
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(request.ownerUuid());
        if (owner != null) owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName() + " 拒绝了你的请求。"));
        source.sendSuccess(() -> Component.literal("已拒绝请求。"), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!MaidData.isMaid(player)) {
            source.sendSuccess(() -> Component.literal("你当前不是女仆。你拥有 " + onlineMaids(player).size() + " 名在线女仆。"), false);
            return 1;
        }
        String mode = MaidData.mode(player) == MaidData.Mode.FOLLOW ? "跟随" : "待命";
        String managerKind = MaidData.managerType(player).map(type -> type == RelationshipNodeType.TLM_MAID
                ? "Maid实体" : "玩家").orElse("未知");
        String topManager = RelationshipService.ancestors(player.getServer(), RelationshipNode.player(player.getUUID())).stream()
                .filter(node -> node.type() == RelationshipNodeType.PLAYER).reduce((first, second) -> second)
                .map(node -> player.getServer().getProfileCache().get(node.uuid()).map(profile -> profile.getName()).orElse(node.uuid().toString()))
                .orElse("无");
        Component message = Component.literal("直接主人：" + MaidData.ownerName(player) + "（" + managerKind + "），顶层主人："
                        + topManager + "，模式：" + mode
                        + "，范围：" + MaidData.activityRangeText(player)
                        + "，" + WorkManager.statusText(player)
                        + "，" + AddonRelationshipData.status(player)
                        + "，绑定床：" + (MaidData.bedPoint(player).isPresent() ? "已绑定" : "未绑定") + " ")
                .withStyle(ChatFormatting.AQUA)
                .append(rangeControls(player, true));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int showSelfRange(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        Component message = Component.literal("当前活动范围：" + MaidData.activityRangeText(maid) + " ")
                .withStyle(ChatFormatting.AQUA).append(rangeControls(maid, true));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int setSelfRange(CommandSourceStack source, double range)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        return applyRange(source, maid, range, false);
    }

    private static int setSelfRangeUnlimited(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        return applyRange(source, maid, MaidData.UNLIMITED_ACTIVITY_RANGE, false);
    }

    private static int showOtherRange(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        Component message = Component.literal(maid.getGameProfile().getName() + " 的活动范围："
                        + MaidData.activityRangeText(maid) + " ")
                .withStyle(ChatFormatting.AQUA).append(rangeControls(maid, false));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int setOtherRange(CommandSourceStack source, ServerPlayer maid, double range)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        return applyRange(source, maid, range, true);
    }

    private static int setOtherRangeUnlimited(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        return applyRange(source, maid, MaidData.UNLIMITED_ACTIVITY_RANGE, true);
    }

    private static int applyRange(CommandSourceStack source, ServerPlayer maid, double range, boolean changedByOwner) {
        if (range == MaidData.UNLIMITED_ACTIVITY_RANGE) MaidData.setActivityRangeUnlimited(maid);
        else MaidData.setActivityRange(maid, range);
        String text = MaidData.activityRangeText(maid);
        if (changedByOwner) {
            maid.sendSystemMessage(Component.literal("主人将你的活动范围设置为：" + text)
                    .withStyle(ChatFormatting.AQUA));
            source.sendSuccess(() -> Component.literal("已将 " + maid.getGameProfile().getName()
                    + " 的活动范围设置为：" + text), false);
        } else {
            source.sendSuccess(() -> Component.literal("活动范围已设置为：" + text), false);
            MaidData.ownerUuid(maid).map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid)).ifPresent(owner ->
                    owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName()
                            + " 将自己的活动范围设置为：" + text).withStyle(ChatFormatting.AQUA)));
        }
        return 1;
    }

    private static Component rangeControls(ServerPlayer maid, boolean self) {
        Component result = Component.literal("范围：").withStyle(ChatFormatting.GRAY);
        int[] presets = {4, 8, 16, 32, 64, 128};
        for (int value : presets) {
            String command = self ? "/maid range self " + value
                    : "/maid range " + maid.getGameProfile().getName() + " " + value;
            result = result.copy().append(Component.literal("[" + value + "]").withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("设置为 " + value + " 格"))))).append(Component.literal(" "));
        }
        String unlimitedCommand = self ? "/maid range self unlimited"
                : "/maid range " + maid.getGameProfile().getName() + " unlimited";
        return result.copy().append(Component.literal("[无限制]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, unlimitedCommand))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("取消活动位置限制")))));
    }

    private static int openOwnBaubles(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        MaidInventoryMenu.openSelfBaubles(maid);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前没有主人。");
        net.minecraft.world.entity.LivingEntity directManager = RelationshipService.resolveDirectManager(maid).orElse(null);
        MaidBaubleInventory.returnAll(maid);
        MaidData.clear(maid);
        AddonRelationshipData.clear(maid);
        WorkManager.clear(maid);
        source.sendSuccess(() -> Component.literal("你已解除女仆关系。"), false);
        if (directManager instanceof ServerPlayer owner) owner.sendSystemMessage(
                Component.literal(maid.getGameProfile().getName() + " 已解除女仆关系。"));
        return 1;
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        List<ServerPlayer> maids = onlineMaids(owner);
        if (maids.isEmpty()) return fail(source, "当前没有在线女仆。");
        List<String> names = new ArrayList<>();
        for (ServerPlayer maid : maids) {
            String level = RelationshipService.isDirectManager(owner, maid) ? "直接" : "继承";
            names.add(maid.getGameProfile().getName() + "[" + level + "，"
                    + (MaidData.mode(maid) == MaidData.Mode.FOLLOW ? "跟随" : "待命")
                    + "，范围" + MaidData.activityRangeText(maid) + "]");
        }
        source.sendSuccess(() -> Component.literal("在线女仆：" + String.join("、", names)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return maids.size();
    }

    private static int follow(CommandSourceStack source, ServerPlayer maid) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        MaidData.setFollow(maid);
        maid.sendSystemMessage(Component.literal("主人将你的模式设为：跟随").withStyle(ChatFormatting.AQUA));
        source.sendSuccess(() -> Component.literal("已让 " + maid.getGameProfile().getName() + " 跟随。"), false);
        return 1;
    }

    private static int stay(CommandSourceStack source, ServerPlayer maid) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        MaidData.setStay(maid);
        maid.sendSystemMessage(Component.literal("主人将你的模式设为：待命").withStyle(ChatFormatting.YELLOW));
        source.sendSuccess(() -> Component.literal("已让 " + maid.getGameProfile().getName() + " 在当前位置待命。"), false);
        return 1;
    }

    private static int summon(CommandSourceStack source, ServerPlayer maid) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        MaidData.setFollow(maid);
        TeleportHelper.near(maid, owner);
        maid.sendSystemMessage(Component.literal("主人召回了你。").withStyle(ChatFormatting.AQUA));
        source.sendSuccess(() -> Component.literal("已召回 " + maid.getGameProfile().getName() + "。"), false);
        return 1;
    }

    private static int release(CommandSourceStack source, ServerPlayer maid) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.isDirectManager(owner, maid)) return fail(source, "只有直接主人可以解除这段女仆关系。");
        MaidBaubleInventory.returnAll(maid);
        MaidData.clear(maid);
        AddonRelationshipData.clear(maid);
        WorkManager.clear(maid);
        maid.sendSystemMessage(Component.literal("主人解除了与你的女仆关系。"));
        source.sendSuccess(() -> Component.literal("已解除与 " + maid.getGameProfile().getName() + " 的关系。"), false);
        return 1;
    }

    private static int openInventory(CommandSourceStack source, ServerPlayer maid, ViewType viewType)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        if (!MaidConfig.ALLOW_OWNER_INVENTORY_MANAGEMENT.get()) return fail(source, "服务器已关闭库存管理功能。");
        MaidInventoryMenu.open(owner, maid, viewType);
        return 1;
    }

    private static int requestWork(CommandSourceStack source, ServerPlayer maid, String modeId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        WorkMode mode = WorkMode.byId(modeId).orElse(null);
        if (mode == null) return fail(source, "未知工作模式：" + modeId);
        WorkRequests.create(owner, maid, mode, MaidConfig.REQUEST_TIMEOUT_SECONDS.get() * 20L);

        Component accept = Component.literal("[接受工作]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid workaccept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("接受工作：" + mode.chineseName()))));
        Component reject = Component.literal("[拒绝]").withStyle(style -> style
                .withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid workreject")));
        maid.sendSystemMessage(Component.literal(owner.getGameProfile().getName() + " 要求你执行“" + mode.chineseName() + "”。 ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(accept).append(Component.literal(" ")).append(reject));
        source.sendSuccess(() -> Component.literal("已向 " + maid.getGameProfile().getName() + " 下达工作请求。"), false);
        return 1;
    }

    private static int acceptWork(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        WorkRequests.Request request = WorkRequests.get(maid).orElse(null);
        if (request == null) return fail(source, "当前没有有效的工作请求。");
        if (!RelationshipService.canManage(source.getServer(), RelationshipNode.player(request.ownerUuid()),
                RelationshipNode.player(maid.getUUID()))) {
            WorkRequests.remove(maid);
            return fail(source, "工作请求者当前已没有管理权限。");
        }
        WorkRequests.remove(maid);
        WorkManager.setMode(maid, request.mode());
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(request.ownerUuid());
        maid.sendSystemMessage(Component.literal("已接受工作：" + request.mode().chineseName()
                + "。自动模式当前" + (WorkManager.isAuto(maid) ? "开启" : "关闭") + "。")
                .withStyle(ChatFormatting.GREEN));
        if (owner != null) owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName()
                + " 已接受工作：“" + request.mode().chineseName() + "”。"));
        return 1;
    }

    private static int rejectWork(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        WorkRequests.Request request = WorkRequests.get(maid).orElse(null);
        if (request == null) return fail(source, "当前没有有效的工作请求。");
        WorkRequests.remove(maid);
        ServerPlayer owner = source.getServer().getPlayerList().getPlayer(request.ownerUuid());
        if (owner != null) owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName() + " 拒绝了工作请求。"));
        source.sendSuccess(() -> Component.literal("已拒绝工作请求。"), false);
        return 1;
    }

    private static int setSelfAuto(CommandSourceStack source, boolean enabled)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        WorkManager.setAuto(maid, enabled);
        source.sendSuccess(() -> Component.literal("混合自动工作已" + (enabled ? "开启" : "关闭") + "。"), false);
        return 1;
    }

    private static int setOtherAuto(CommandSourceStack source, ServerPlayer maid, boolean enabled)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        WorkManager.setAuto(maid, enabled);
        maid.sendSystemMessage(Component.literal("主人将你的混合自动工作设为：" + (enabled ? "开启" : "关闭")));
        source.sendSuccess(() -> Component.literal("已设置 " + maid.getGameProfile().getName() + " 的自动工作。"), false);
        return 1;
    }

    private static int bindBed(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        if (!TouhouMaidCompat.bindLookedAtBed(owner, maid)) return fail(source, "请站在女仆附近并看向 6 格内的车万女仆床。");
        maid.sendSystemMessage(Component.literal("主人为你绑定了一张车万女仆床。").withStyle(ChatFormatting.LIGHT_PURPLE));
        source.sendSuccess(() -> Component.literal("女仆床绑定完成。"), false);
        return 1;
    }

    private static int sendToBed(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid)) return fail(source, "该玩家不是你的女仆。");
        if (!TouhouMaidCompat.sendToBed(maid)) return fail(source, "该女仆还没有绑定有效的女仆床。");
        source.sendSuccess(() -> Component.literal("已让 " + maid.getGameProfile().getName() + " 返回绑定床休息。"), false);
        return 1;
    }

    private static int requestInteraction(CommandSourceStack source, ServerPlayer maid, String actionId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        AddonInteractions.Action action = AddonInteractions.Action.byId(actionId).orElse(null);
        if (action == null) return fail(source, "未知互动：" + actionId);
        return AddonInteractions.request(owner, maid, action) ? 1 : 0;
    }

    private static int acceptInteraction(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return AddonInteractions.accept(source.getPlayerOrException()) ? 1 : fail(source, "当前没有可接受的互动请求。");
    }

    private static int rejectInteraction(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return AddonInteractions.reject(source.getPlayerOrException()) ? 1 : fail(source, "当前没有可拒绝的互动请求。");
    }

    private static int relation(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.canManage(owner, maid) && maid != owner) return fail(source, "该玩家不是你的女仆。");
        source.sendSuccess(() -> Component.literal(maid.getGameProfile().getName() + " | "
                + AddonRelationshipData.status(maid)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int bond(CommandSourceStack source, ServerPlayer maid)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.isDirectManager(owner, maid)) return fail(source, "羁绊只对直接的玩家主从关系开放。");
        String unlocked = AddonRelationshipData.abilities(maid).stream()
                .map(TouhouMaidAffectionCompat::displayName).reduce((a, b) -> a + "、" + b).orElse("无");
        source.sendSuccess(() -> Component.literal("羁绊等级：" + AddonRelationshipData.bondLevel(maid)
                + "，可用 P 点：" + AddonRelationshipData.availableBondPoints(maid) + "，已解锁：" + unlocked)
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int unlockBond(CommandSourceStack source, ServerPlayer maid, String ability)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        if (!RelationshipService.isDirectManager(owner, maid)) return fail(source, "羁绊只对直接的玩家主从关系开放。");
        if (!TouhouMaidAffectionCompat.unlock(owner, maid, ability)) {
            return fail(source, "能力不存在、已经解锁，或羁绊等级/P点不足。需要 "
                    + TouhouMaidAffectionCompat.cost(ability) + " P点。");
        }
        maid.sendSystemMessage(Component.literal("已解锁羁绊能力：" + TouhouMaidAffectionCompat.displayName(ability))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        source.sendSuccess(() -> Component.literal("已为 " + maid.getGameProfile().getName() + " 解锁："
                + TouhouMaidAffectionCompat.displayName(ability)), false);
        return 1;
    }

    private static int setBondAuto(CommandSourceStack source, boolean enabled)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer maid = source.getPlayerOrException();
        if (!MaidData.isMaid(maid)) return fail(source, "你当前不是女仆。");
        AddonRelationshipData.putBoolean(maid, "AutoMorningKiss", enabled);
        source.sendSuccess(() -> Component.literal("早安吻自动互动已" + (enabled ? "开启" : "关闭") + "。"), false);
        return 1;
    }

    private static int forceBind(CommandSourceStack source, ServerPlayer owner, ServerPlayer maid) {
        if (owner == maid) return fail(source, "主人和女仆不能是同一玩家。");
        MaidBaubleInventory.returnAll(maid);
        MaidData.clear(maid);
        if (!bind(owner, maid, false)) return fail(source, "强制绑定失败。");
        source.sendSuccess(() -> Component.literal("管理员绑定完成。"), true);
        return 1;
    }

    private static int forceClear(CommandSourceStack source, ServerPlayer player) {
        MaidBaubleInventory.returnAll(player);
        MaidData.clear(player);
        AddonRelationshipData.clear(player);
        WorkManager.clear(player);
        MaidRequests.remove(player);
        source.sendSuccess(() -> Component.literal("已清除 " + player.getGameProfile().getName() + " 的女仆关系和待处理请求。"), true);
        return 1;
    }

    public static List<ServerPlayer> onlineMaids(ServerPlayer owner) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : owner.getServer().getPlayerList().getPlayers()) {
            if (RelationshipService.canManage(owner, player)) result.add(player);
        }
        return result;
    }

    private static int findCake(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.CAKE) && !stack.isEmpty()) return i;
        }
        return -1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }

    private MaidCommands() {
    }
}
