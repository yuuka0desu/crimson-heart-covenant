package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Touhou Maid: Affection 的玩家女仆羁绊桥。 */
public final class TouhouMaidAffectionCompat {
    public static final Set<String> ABILITIES = Set.of("emergency_heal", "lap_pillow", "morning_kiss", "random_gift", "ysm_action");
    private static final Map<String, Integer> COSTS = Map.of(
            "emergency_heal", 3, "lap_pillow", 8, "morning_kiss", 12, "random_gift", 6, "ysm_action", 16);

    public static int cost(String id) { return COSTS.getOrDefault(id, 9999); }

    public static void sync(ServerPlayer owner, ServerPlayer maid) {
        if (!MaidConfig.MAID_AFFECTION_COMPAT.get() || !ModList.get().isLoaded("touhou_maid_affection")) return;
        try {
            Class<?> manager = Class.forName("com.github.touhoumaidaffection.bond.BondManager");
            Method setLevel = manager.getMethod("setBondLevel", ServerPlayer.class, java.util.UUID.class, int.class);
            setLevel.invoke(null, owner, maid.getUUID(), AddonRelationshipData.bondLevel(maid));
            Method unlock = manager.getMethod("unlockAbility", ServerPlayer.class, java.util.UUID.class, String.class);
            for (String id : AddonRelationshipData.abilities(maid)) unlock.invoke(null, owner, maid.getUUID(), id);
        } catch (ReflectiveOperationException | LinkageError error) {
            PlayerMaid.LOGGER.warn("同步 Touhou Maid Affection 羁绊数据失败", error);
        }
    }

    public static boolean unlock(ServerPlayer owner, ServerPlayer maid, String id) {
        if (!ABILITIES.contains(id) || AddonRelationshipData.hasAbility(maid, id)) return false;
        int requiredLevel = switch (id) {
            case "lap_pillow" -> 1;
            case "random_gift" -> 1;
            case "morning_kiss" -> 2;
            case "emergency_heal" -> 2;
            case "ysm_action" -> 3;
            default -> 4;
        };
        if (AddonRelationshipData.bondLevel(maid) < requiredLevel
                || AddonRelationshipData.availableBondPoints(maid) < cost(id)) return false;
        AddonRelationshipData.unlockAbility(maid, id);
        if (id.equals("random_gift")) AddonRelationshipData.putLong(maid, "NextRandomGift", System.currentTimeMillis() + 20L * 60_000L);
        if (id.equals("emergency_heal")) {
            AddonRelationshipData.putLong(maid, "RescueDay", maid.serverLevel().getDayTime() / 24000L);
            AddonRelationshipData.putInt(maid, "RescueCharges", 1);
        }
        sync(owner, maid);
        return true;
    }

    public static void tick(ServerPlayer maid) {
        if (!MaidConfig.MAID_AFFECTION_COMPAT.get() || !MaidData.isMaid(maid)) return;
        ServerPlayer owner = MaidData.ownerUuid(maid).map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        if (owner == null) return;

        if (AddonRelationshipData.hasAbility(maid, "random_gift") && maid.tickCount % 200 == 0) {
            long now = System.currentTimeMillis();
            long next = AddonRelationshipData.getLong(maid, "NextRandomGift");
            if (next == 0L) next = now + 20L * 60_000L;
            if (now >= next) {
                int queued = Math.min(7, AddonRelationshipData.getInt(maid, "QueuedGifts") + 1);
                AddonRelationshipData.putInt(maid, "QueuedGifts", queued);
                AddonRelationshipData.putLong(maid, "NextRandomGift", now + 20L * 60_000L);
            }
            if (AddonRelationshipData.getInt(maid, "QueuedGifts") > 0 && maid.serverLevel() == owner.serverLevel()
                    && maid.distanceToSqr(owner) <= 36.0D) deliverRandomGift(maid, owner);
        }

        if (AddonRelationshipData.hasAbility(maid, "morning_kiss")
                && AddonRelationshipData.getBoolean(maid, "AutoMorningKiss") && maid.tickCount % 200 == 0) {
            long day = maid.serverLevel().getDayTime() / 24000L;
            long time = maid.serverLevel().getDayTime() % 24000L;
            if (time >= 20L && time <= 1000L && AddonRelationshipData.getLong(maid, "MorningKissDay") != day
                    && maid.serverLevel() == owner.serverLevel() && maid.distanceToSqr(owner) <= 64.0D) {
                AddonRelationshipData.putLong(maid, "MorningKissDay", day);
                AddonInteractions.applyAccepted(owner, maid, AddonInteractions.Action.MORNING_KISS);
            }
        }
    }

    private static void deliverRandomGift(ServerPlayer maid, ServerPlayer owner) {
        List<ItemStack> pool = List.of(new ItemStack(Items.COOKIE, 3), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.EMERALD), new ItemStack(Items.AMETHYST_SHARD, 2),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 2));
        ItemStack gift = pool.get(maid.getRandom().nextInt(pool.size())).copy();
        if (!owner.addItem(gift)) owner.drop(gift, false);
        AddonRelationshipData.putInt(maid, "QueuedGifts", AddonRelationshipData.getInt(maid, "QueuedGifts") - 1);
        owner.sendSystemMessage(Component.literal(maid.getGameProfile().getName() + " 送给你 " + gift.getHoverName().getString()
                + " x" + gift.getCount()).withStyle(ChatFormatting.LIGHT_PURPLE));
        maid.serverLevel().sendParticles(ParticleTypes.HEART, maid.getX(), maid.getY() + 1.0D, maid.getZ(), 5, .3, .5, .3, .02);
    }

    public static void onOwnerHurt(LivingHurtEvent event) {
        if (!MaidConfig.MAID_AFFECTION_COMPAT.get() || !(event.getEntity() instanceof ServerPlayer owner)
                || event.getAmount() < owner.getHealth()) return;
        for (ServerPlayer maid : MaidCommands.onlineMaids(owner)) {
            if (!AddonRelationshipData.hasAbility(maid, "emergency_heal") || maid.serverLevel() != owner.serverLevel()
                    || maid.distanceToSqr(owner) > 24.0D * 24.0D) continue;
            long day = owner.serverLevel().getDayTime() / 24000L;
            if (AddonRelationshipData.getLong(maid, "RescueDay") != day) {
                AddonRelationshipData.putLong(maid, "RescueDay", day);
                AddonRelationshipData.putInt(maid, "RescueCharges", 1);
            }
            if (AddonRelationshipData.getInt(maid, "RescueCharges") <= 0) continue;
            AddonRelationshipData.putInt(maid, "RescueCharges", 0);
            event.setAmount(Math.max(0.0F, owner.getHealth() - 1.0F));
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            owner.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1, owner.getZ(), 30, .5, 1, .5, .1);
            owner.serverLevel().playSound(null, owner.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);
            owner.sendSystemMessage(Component.literal("残血救护触发！" + maid.getGameProfile().getName() + " 替你挡下了致命伤害。")
                    .withStyle(ChatFormatting.GOLD));
            maid.sendSystemMessage(Component.literal("你触发了对主人的残血救护。").withStyle(ChatFormatting.GOLD));
            break;
        }
    }

    public static void applyKissEffect(ServerPlayer owner) {
        MobEffect prayer = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("touhou_maid_affection", "maids_prayer"));
        owner.addEffect(new MobEffectInstance(prayer == null ? MobEffects.REGENERATION : prayer, 600, 0));
    }

    public static String displayName(String id) {
        return switch (id) {
            case "emergency_heal" -> "残血救护";
            case "lap_pillow" -> "休闲膝枕";
            case "morning_kiss" -> "早安吻";
            case "random_gift" -> "随机礼物";
            case "ysm_action" -> "YSM 动作联动";
            default -> id;
        };
    }

    private TouhouMaidAffectionCompat() { }
}
