package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Touhou Little Mad 0.1.8 的 Forge 1.20.1 玩家实体兼容实现。 */
public final class LittleMadCompat {
    public static void tick(ServerPlayer maid) {
        if (!MaidConfig.LITTLE_MAD_COMPAT.get() || !MaidData.isMaid(maid)) return;
        long now = maid.serverLevel().getGameTime();
        long lastRecover = AddonRelationshipData.getLong(maid, "MoodRecoverAt");
        if (now - lastRecover >= MaidConfig.MOOD_RECOVER_INTERVAL.get()) {
            AddonRelationshipData.putLong(maid, "MoodRecoverAt", now);
            if (WorkManager.getMode(maid) == WorkMode.IDLE || !WorkManager.isAuto(maid)) {
                changeMood(maid, -MaidConfig.MOOD_RECOVER_AMOUNT.get(), "休息恢复");
            }
        }
        if (WorkManager.isAuto(maid) && WorkManager.getMode(maid) != WorkMode.IDLE
                && now % MaidConfig.MOOD_WORK_CHECK_INTERVAL.get() == 0
                && maid.getRandom().nextDouble() < MaidConfig.MOOD_WORK_ANGER_CHANCE.get()) {
            changeMood(maid, MaidConfig.MOOD_WORK_ANGER.get(), "持续工作");
        }
        if (AddonRelationshipData.moodLevel(maid) == AddonRelationshipData.MoodLevel.GOOD
                && now - AddonRelationshipData.getLong(maid, "HappyFavorAt") >= 12000L) {
            AddonRelationshipData.putLong(maid, "HappyFavorAt", now);
            AddonRelationshipData.addFavorability(maid, 1);
        }
    }

    public static void onHurt(ServerPlayer maid, ServerPlayer attacker) {
        if (!MaidConfig.LITTLE_MAD_COMPAT.get() || !MaidData.isMaid(maid)) return;
        int amount = MaidData.ownedBy(maid, attacker)
                ? MaidConfig.MOOD_HURT_BY_OWNER.get() : MaidConfig.MOOD_HURT_BY_OTHER.get();
        changeMood(maid, amount, attacker.getGameProfile().getName() + "造成伤害");
    }

    public static void onDeath(ServerPlayer maid) {
        if (MaidConfig.LITTLE_MAD_COMPAT.get() && MaidData.isMaid(maid)) {
            AddonRelationshipData.setMood(maid, Math.min(110,
                    AddonRelationshipData.mood(maid) + MaidConfig.MOOD_DEATH_ANGER.get()));
        }
    }

    public static void onWake(ServerPlayer maid) {
        if (MaidConfig.LITTLE_MAD_COMPAT.get() && MaidData.isMaid(maid)) {
            changeMood(maid, -10, "睡眠恢复");
        }
    }

    public static int changeMood(ServerPlayer maid, int raw, String reason) {
        AddonRelationshipData.MoodLevel level = AddonRelationshipData.moodLevel(maid);
        double multiplier;
        if (raw > 0) {
            multiplier = switch (level) {
                case GOOD -> 1.0D;
                case NORMAL -> 1.5D;
                case BAD -> 2.0D;
                case MAD -> 0.0D;
            };
        } else {
            multiplier = switch (level) {
                case GOOD -> 1.0D;
                case NORMAL -> 0.75D;
                case BAD -> 0.5D;
                case MAD -> 0.0D;
            };
        }
        int change = (int) Math.round(raw * multiplier);
        AddonRelationshipData.MoodLevel before = level;
        AddonRelationshipData.setMood(maid, AddonRelationshipData.mood(maid) + change);
        AddonRelationshipData.MoodLevel after = AddonRelationshipData.moodLevel(maid);
        if (before != after) {
            maid.sendSystemMessage(Component.literal("情绪变为：" + after.display() + "（" + reason + "）")
                    .withStyle(after.ordinal() >= 2 ? ChatFormatting.RED : ChatFormatting.GREEN));
        }
        return change;
    }

    public static boolean isStriking(ServerPlayer maid) {
        if (!MaidConfig.LITTLE_MAD_COMPAT.get() || !MaidConfig.MOOD_STRIKE_ENABLED.get()) return false;
        int mood = AddonRelationshipData.mood(maid);
        if (mood >= 180) return true;
        return mood >= MaidConfig.MOOD_STRIKE_THRESHOLD.get()
                && maid.getRandom().nextDouble() < MaidConfig.MOOD_STRIKE_CHANCE.get();
    }

    public static float positiveFavorabilityMultiplier(ServerPlayer maid) {
        if (!MaidConfig.LITTLE_MAD_COMPAT.get()) return 1.0F;
        return switch (AddonRelationshipData.moodLevel(maid)) {
            case GOOD -> 2.0F;
            case NORMAL -> 1.0F;
            case BAD, MAD -> 0.0F;
        };
    }

    public static void applyCombatEffects(ServerPlayer maid) {
        if (!MaidConfig.LITTLE_MAD_COMPAT.get()) return;
        switch (AddonRelationshipData.moodLevel(maid)) {
            case GOOD -> maid.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 45, 0, false, false, true));
            case BAD -> maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 0, false, false, true));
            case MAD -> {
                maid.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 1, false, false, true));
                maid.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 0, false, false, true));
            }
            default -> { }
        }
    }

    public static void applyDamageMultiplier(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer maid) || !MaidData.isMaid(maid)
                || !MaidConfig.LITTLE_MAD_COMPAT.get()) return;
        double multiplier = switch (AddonRelationshipData.moodLevel(maid)) {
            case GOOD -> 0.9D;
            case NORMAL -> 1.0D;
            case BAD -> 1.15D;
            case MAD -> 1.35D;
        };
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    private LittleMadCompat() { }
}
