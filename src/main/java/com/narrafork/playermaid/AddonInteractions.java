package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 玩家之间需要确认的亲密交互请求。 */
public final class AddonInteractions {
    public enum Action {
        GIFT("gift", "赠送礼物"), PET("pet", "摸头"), HUG("hug", "拥抱"), KISS("kiss", "亲吻"),
        CONFESS("confess", "表白"), MARRY("marry", "结婚"), LAP_PILLOW("lap", "膝枕"), MORNING_KISS("morning_kiss", "早安吻");
        private final String id;
        private final String display;
        Action(String id, String display) { this.id = id; this.display = display; }
        public String id() { return id; }
        public String display() { return display; }
        public static Optional<Action> byId(String id) {
            for (Action action : values()) if (action.id.equals(id.toLowerCase(Locale.ROOT))) return Optional.of(action);
            return Optional.empty();
        }
    }

    public record Request(UUID ownerUuid, Action action, long expiresAt) { }
    private static final Map<UUID, Request> REQUESTS = new HashMap<>();

    public static boolean tryRightClick(PlayerInteractEvent.EntityInteract event, ServerPlayer owner, ServerPlayer maid) {
        if (!MaidData.ownedBy(maid, owner)) return false;
        if (MaidMarriageCompat.isProposalRing(owner.getMainHandItem())) {
            request(owner, maid, AddonRelationshipData.isConfessed(maid) ? Action.MARRY : Action.CONFESS);
            return true;
        }
        if (MaidMarriageCompat.isGift(owner.getMainHandItem())) {
            request(owner, maid, Action.GIFT);
            return true;
        }
        return false;
    }

    public static boolean request(ServerPlayer owner, ServerPlayer maid, Action action) {
        String failure = validate(owner, maid, action, false);
        if (failure != null) {
            owner.sendSystemMessage(Component.literal(failure).withStyle(ChatFormatting.RED));
            return false;
        }
        long expires = maid.serverLevel().getGameTime() + MaidConfig.REQUEST_TIMEOUT_SECONDS.get() * 20L;
        REQUESTS.put(maid.getUUID(), new Request(owner.getUUID(), action, expires));
        Component accept = Component.literal("[接受]").withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid interactaccept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("接受" + action.display()))));
        Component reject = Component.literal("[拒绝]").withStyle(style -> style.withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/maid interactreject")));
        maid.sendSystemMessage(Component.literal(owner.getGameProfile().getName() + " 向你发起“" + action.display() + "”互动。 ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(accept).append(Component.literal(" ")).append(reject));
        owner.sendSystemMessage(Component.literal("已向 " + maid.getGameProfile().getName() + " 发起“" + action.display() + "”，等待确认。"));
        return true;
    }

    public static boolean accept(ServerPlayer maid) {
        Request request = get(maid).orElse(null);
        if (request == null) return false;
        ServerPlayer owner = maid.getServer().getPlayerList().getPlayer(request.ownerUuid());
        REQUESTS.remove(maid.getUUID());
        if (owner == null) {
            maid.sendSystemMessage(Component.literal("互动发起者已离线。").withStyle(ChatFormatting.RED));
            return false;
        }
        String failure = validate(owner, maid, request.action(), true);
        if (failure != null) {
            maid.sendSystemMessage(Component.literal(failure).withStyle(ChatFormatting.RED));
            owner.sendSystemMessage(Component.literal(failure).withStyle(ChatFormatting.RED));
            return false;
        }
        return applyAccepted(owner, maid, request.action());
    }

    public static boolean reject(ServerPlayer maid) {
        Request request = get(maid).orElse(null);
        if (request == null) return false;
        REQUESTS.remove(maid.getUUID());
        ServerPlayer owner = maid.getServer().getPlayerList().getPlayer(request.ownerUuid());
        if (owner != null) owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName() + " 拒绝了“" + request.action().display() + "”。"));
        maid.sendSystemMessage(Component.literal("已拒绝互动。"));
        return true;
    }

    public static boolean applyAccepted(ServerPlayer owner, ServerPlayer maid, Action action) {
        int favor = 0;
        int mood = 0;
        switch (action) {
            case GIFT -> { return MaidMarriageCompat.giveHeldGift(owner, maid); }
            case PET -> { favor = 2; mood = -4; }
            case HUG -> { favor = 4; mood = -7; }
            case KISS, MORNING_KISS -> {
                favor = 3;
                mood = -8;
                TouhouMaidAffectionCompat.applyKissEffect(owner);
            }
            case CONFESS -> {
                AddonRelationshipData.setConfessed(maid, true);
                favor = 8;
                mood = -10;
            }
            case MARRY -> {
                MaidMarriageCompat.completeMarriage(owner, maid);
                favor = 16;
                mood = -15;
            }
            case LAP_PILLOW -> {
                favor = 3;
                mood = -8;
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            }
        }
        YsmCompat.playInteraction(owner, maid, action);
        int actual = AddonRelationshipData.addFavorability(maid, favor);
        LittleMadCompat.changeMood(maid, mood, action.display());
        maid.serverLevel().sendParticles(ParticleTypes.HEART,
                (owner.getX() + maid.getX()) / 2.0D, (owner.getY() + maid.getY()) / 2.0D + 1.2D,
                (owner.getZ() + maid.getZ()) / 2.0D, action == Action.MARRY ? 20 : 8, .4, .5, .4, .04);
        maid.serverLevel().playSound(null, maid.blockPosition(),
                action == Action.MARRY ? SoundEvents.PLAYER_LEVELUP : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, .8F, action == Action.MARRY ? 1.0F : 1.5F);
        String message = owner.getGameProfile().getName() + " 与 " + maid.getGameProfile().getName()
                + " 完成了“" + action.display() + "”互动，好感 +" + Math.max(0, actual) + "。";
        owner.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE));
        maid.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private static String validate(ServerPlayer owner, ServerPlayer maid, Action action, boolean accepting) {
        if (!MaidData.ownedBy(maid, owner)) return "当前主人—女仆关系已失效。";
        if (owner.serverLevel() != maid.serverLevel() || owner.distanceToSqr(maid) > 64.0D) return "双方需要在同一维度且相距不超过 8 格。";
        int favor = AddonRelationshipData.favorability(maid);
        return switch (action) {
            case GIFT -> MaidMarriageCompat.isGift(owner.getMainHandItem()) ? null : "请在主手持有可赠送物品。";
            case PET -> favor >= 32 ? null : "需要至少 32 好感才能摸头。";
            case HUG -> favor >= 64 ? null : "需要至少 64 好感才能拥抱。";
            case KISS -> favor >= 128 && AddonRelationshipData.isConfessed(maid) ? null : "亲吻需要 128 好感并完成表白。";
            case CONFESS -> favor >= 128 && !AddonRelationshipData.isConfessed(maid) ? null : "表白需要 128 好感且尚未进入恋人关系。";
            case MARRY -> {
                if (favor < 192 || !AddonRelationshipData.isConfessed(maid) || AddonRelationshipData.isMarried(maid)) yield "结婚需要 192 好感、已完成表白且当前未婚。";
                if (!MaidMarriageCompat.hasTwoRings(owner)) yield "结婚时主人主手和副手都需要持有未刻印的求婚戒指。";
                yield null;
            }
            case LAP_PILLOW -> favor >= 128 && AddonRelationshipData.isConfessed(maid)
                    && AddonRelationshipData.hasAbility(maid, "lap_pillow") ? null : "膝枕需要恋人关系并解锁休闲膝枕。";
            case MORNING_KISS -> AddonRelationshipData.hasAbility(maid, "morning_kiss") ? null : "尚未解锁早安吻。";
        };
    }

    public static Optional<Request> get(ServerPlayer maid) {
        Request request = REQUESTS.get(maid.getUUID());
        if (request == null) return Optional.empty();
        if (request.expiresAt() < maid.serverLevel().getGameTime()) {
            REQUESTS.remove(maid.getUUID());
            return Optional.empty();
        }
        return Optional.of(request);
    }
    public static void removeFor(UUID uuid) { REQUESTS.entrySet().removeIf(e -> e.getKey().equals(uuid) || e.getValue().ownerUuid().equals(uuid)); }
    public static void cleanup(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        REQUESTS.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    }
    private AddonInteractions() { }
}
