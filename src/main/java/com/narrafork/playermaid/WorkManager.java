package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;
import java.util.List;

/**
 * 玩家女仆工作状态、增益与轻量自动行为入口。
 * 工作数据暂存在 PERSISTED_NBT_TAG/PlayerMaidWork，死亡克隆后会随现有持久数据保留。
 */
public final class WorkManager {
    public static final String ROOT = "PlayerMaidWork";
    private static final String MODE = "Mode";
    private static final String AUTO = "Auto";

    public static void tick(ServerPlayer maid) {
        if (!MaidData.isMaid(maid) || !maid.isAlive()) return;

        WorkMode mode = getMode(maid);
        LittleMadCompat.tick(maid);
        TouhouMaidAffectionCompat.tick(maid);
        if (maid.tickCount % WorkConstants.EFFECT_REFRESH_INTERVAL_TICKS == 0) {
            refreshEffects(maid, mode);
            LittleMadCompat.applyCombatEffects(maid);
        }
        if (maid.tickCount % MaidConfig.AUTO_WORK_INTERVAL.get() == 0 && scheduleAllowsWork(maid)) {
            if (MaidData.pickup(maid)) collectNearbyItems(maid);
            if (!isAuto(maid)) return;
            if (LittleMadCompat.isStriking(maid)) {
                if (maid.tickCount % 200 == 0) maid.displayClientMessage(Component.literal("情绪过差，自动工作暂时罢工。")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            runAutomaticAction(maid, mode);
        }
    }

    public static boolean scheduleAllowsWork(ServerPlayer maid) {
        return switch (MaidData.schedule(maid)) {
            case ALL -> true;
            case DAY -> maid.serverLevel().isDay();
            case NIGHT -> maid.serverLevel().isNight();
        };
    }

    private static void collectNearbyItems(ServerPlayer maid) {
        double range = Math.min(MaidConfig.MAID_UI_PICKUP_RANGE.get(), MaidData.workSearchRange(maid));
        List<ItemEntity> items = maid.serverLevel().getEntitiesOfClass(ItemEntity.class,
                maid.getBoundingBox().inflate(range), item -> item.isAlive() && !item.getItem().isEmpty());
        items.sort(Comparator.comparingDouble(maid::distanceToSqr));
        for (ItemEntity item : items) {
            ItemStack remaining = item.getItem().copy();
            if (maid.getInventory().add(remaining)) {
                item.discard();
            } else if (remaining.getCount() != item.getItem().getCount()) {
                item.setItem(remaining);
            }
        }
    }

    public static void applyDamageBonus(LivingHurtEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer maid)
                || !MaidData.isMaid(maid) || !getMode(maid).isCombat()) {
            return;
        }
        event.setAmount((float) (event.getAmount() * (1.0D + MaidConfig.WORK_DAMAGE_BONUS.get())));
    }

    public static void setMode(Player maid, WorkMode mode) {
        writableRoot(maid).putString(MODE, mode.id());
    }

    public static WorkMode getMode(Player maid) {
        return WorkMode.byId(root(maid).getString(MODE)).orElse(WorkMode.IDLE);
    }

    public static void setAuto(Player maid, boolean enabled) {
        writableRoot(maid).putBoolean(AUTO, enabled);
    }

    public static boolean isAuto(Player maid) {
        return root(maid).getBoolean(AUTO);
    }

    public static void clear(Player maid) {
        maid.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove(ROOT);
    }

    public static Component status(ServerPlayer maid) {
        WorkMode mode = getMode(maid);
        String autoText = isAuto(maid) ? "开启" : "关闭";
        String ownerText = MaidData.ownerName(maid);
        if (ownerText.isBlank()) ownerText = "未绑定";
        return Component.literal("女仆工作 | 主人：" + ownerText
                        + " | 任务：" + mode.chineseName()
                        + "（" + mode.category().chineseName() + "）"
                        + " | 自动：" + autoText)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public static String statusText(ServerPlayer maid) {
        WorkMode mode = getMode(maid);
        return "任务：" + mode.chineseName() + " | 分类：" + mode.category().chineseName()
                + " | 自动：" + (isAuto(maid) ? "开启" : "关闭")
                + " | 活动范围：" + MaidData.activityRangeText(maid);
    }

    private static void refreshEffects(ServerPlayer maid, WorkMode mode) {
        switch (mode.category()) {
            case COMBAT -> {
                addEffect(maid, MobEffects.DAMAGE_BOOST, 0);
                addEffect(maid, MobEffects.DAMAGE_RESISTANCE, 0);
            }
            case GATHERING -> {
                addEffect(maid, MobEffects.DIG_SPEED, 0);
                addEffect(maid, MobEffects.MOVEMENT_SPEED, 0);
                addEffect(maid, MobEffects.LUCK, 0);
            }
            case FEEDING -> {
                addEffect(maid, MobEffects.MOVEMENT_SPEED, 0);
                addEffect(maid, MobEffects.REGENERATION, 0);
            }
            case TORCH -> addEffect(maid, MobEffects.NIGHT_VISION, 0);
            case EXTINGUISHING -> addEffect(maid, MobEffects.FIRE_RESISTANCE, 0);
            case GENERAL -> {
                addEffect(maid, MobEffects.MOVEMENT_SPEED, 0);
                addEffect(maid, MobEffects.LUCK, 0);
            }
        }
    }

    private static void addEffect(ServerPlayer maid, MobEffect effect, int amplifier) {
        maid.addEffect(new MobEffectInstance(effect, WorkConstants.EFFECT_DURATION_TICKS,
                amplifier, false, false, true));
    }

    private static void runAutomaticAction(ServerPlayer maid, WorkMode mode) {
        if (mode.isCombat()) {
            attackNearestMonster(maid);
            return;
        }
        switch (mode) {
            case FEED -> feedOwner(maid);
            case FEED_ANIMAL -> feedAnimal(maid);
            case EXTINGUISHING -> extinguish(maid);
            case TORCH -> placeTorch(maid);
            default -> {
                // 农耕和采集模式只提供增益，刻意不自动修改世界方块。
            }
        }
    }

    private static void attackNearestMonster(ServerPlayer maid) {
        double range = MaidData.workSearchRange(maid);
        List<Monster> targets = maid.serverLevel().getEntitiesOfClass(Monster.class,
                maid.getBoundingBox().inflate(range), target -> target.isAlive()
                        && !target.isAlliedTo(maid) && !maid.isAlliedTo(target)
                        && maid.hasLineOfSight(target));
        targets.stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr))
                .ifPresent(maid::attack);
    }

    private static void feedOwner(ServerPlayer maid) {
        ServerPlayer owner = MaidData.ownerUuid(maid)
                .map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        double range = MaidData.workSearchRange(maid);
        if (owner == null || owner.serverLevel() != maid.serverLevel() || maid.distanceToSqr(owner) > range * range
                || (!owner.getFoodData().needsFood() && owner.getHealth() >= owner.getMaxHealth())) {
            return;
        }

        Inventory inventory = maid.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.isEdible()) {
                owner.getFoodData().eat(stack.getItem(), stack);
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                consumeOne(maid, stack);
                return;
            }
        }
    }

    private static void feedAnimal(ServerPlayer maid) {
        double range = MaidData.workSearchRange(maid);
        List<Animal> animals = maid.serverLevel().getEntitiesOfClass(Animal.class,
                maid.getBoundingBox().inflate(range), animal -> animal.isAlive()
                        && !animal.isBaby() && animal.canFallInLove());
        animals.sort(Comparator.comparingDouble(maid::distanceToSqr));

        Inventory inventory = maid.getInventory();
        for (Animal animal : animals) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && animal.isFood(stack)) {
                    animal.setInLove(maid);
                    consumeOne(maid, stack);
                    return;
                }
            }
        }
    }

    private static void extinguish(ServerPlayer maid) {
        ServerLevel level = maid.serverLevel();
        maid.setRemainingFireTicks(0);
        MaidData.ownerUuid(maid)
                .map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid))
                .filter(owner -> owner.serverLevel() == level)
                .ifPresent(owner -> owner.setRemainingFireTicks(0));

        AABB area = maid.getBoundingBox().inflate(MaidData.workSearchRange(maid));
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            entity.setRemainingFireTicks(0);
        }

        int radius = Math.max(1, (int) Math.floor(Math.min(MaidData.workSearchRange(maid), 8.0D)));
        for (BlockPos pos : BlockPos.betweenClosed(maid.blockPosition().offset(-radius, -radius, -radius),
                maid.blockPosition().offset(radius, radius, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof FireBlock) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static void placeTorch(ServerPlayer maid) {
        ItemStack torches = findTorches(maid.getInventory());
        if (torches.isEmpty()) return;

        ServerLevel level = maid.serverLevel();
        BlockPos origin = maid.blockPosition();
        int radius = Math.max(1, (int) Math.floor(Math.min(MaidData.workSearchRange(maid), 8.0D)));
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius),
                origin.offset(radius, 1, radius))) {
            BlockState current = level.getBlockState(candidate);
            if (!current.canBeReplaced() || level.getMaxLocalRawBrightness(candidate) > WorkConstants.TORCH_MAX_LIGHT
                    || !Block.canSupportCenter(level, candidate.below(), Direction.UP)
                    || !torch.canSurvive(level, candidate)) {
                continue;
            }
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                best = candidate.immutable();
                bestDistance = distance;
            }
        }

        if (best != null && level.setBlock(best, torch, Block.UPDATE_ALL)) {
            consumeOne(maid, torches);
        }
    }

    private static ItemStack findTorches(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.TORCH)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void consumeOne(ServerPlayer maid, ItemStack stack) {
        if (!maid.getAbilities().instabuild) stack.shrink(1);
    }

    private static CompoundTag root(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(ROOT);
    }

    private static CompoundTag writableRoot(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) {
            persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        }
        CompoundTag work = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, work);
        }
        return work;
    }

    private WorkManager() {
    }
}
