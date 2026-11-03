package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 玩家女仆工作状态、增益与轻量自动行为入口。
 * 工作数据暂存在 PERSISTED_NBT_TAG/PlayerMaidWork，死亡克隆后会随现有持久数据保留。
 */
public final class WorkManager {
    public static final String ROOT = "PlayerMaidWork";
    private static final String MODE = "Mode";
    private static final String AUTO = "Auto";
    private static final String TASK_UID = "TaskUid";
    private static final String TASK_SOURCE = "TaskSourceMaid";
    private static final String TASK_NAME = "TaskDisplayName";
    private static final String TASK_SYNCED_AT = "TaskSyncedAt";
    private static final String LAST_WORK_NOTICE = "LastWorkNotice";

    public static void tick(ServerPlayer maid) {
        if (!MaidData.isMaid(maid) || !maid.isAlive()) return;

        WorkMode mode = getMode(maid);
        LittleMadCompat.tick(maid);
        TouhouMaidAffectionCompat.tick(maid);
        if (maid.tickCount % WorkConstants.EFFECT_REFRESH_INTERVAL_TICKS == 0) {
            refreshEffects(maid, mode);
            LittleMadCompat.applyCombatEffects(maid);
        }
        int interval = MaidConfig.AUTO_WORK_INTERVAL.get();
        boolean actionTick = maid.tickCount % interval == 0;
        boolean scheduleOpen = scheduleAllowsWork(maid);
        if (actionTick && scheduleOpen && MaidData.pickup(maid)) collectNearbyItems(maid);
        if (!scheduleOpen || !isAuto(maid)) return;
        if (LittleMadCompat.isStriking(maid)) {
            if (maid.tickCount % 200 == 0) maid.displayClientMessage(Component.literal("情绪过差，自动工作暂时罢工。")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (mode.isCombat() || actionTick) runAutomaticAction(maid, mode);
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
        CompoundTag tag = writableRoot(maid);
        tag.putString(MODE, mode.id());
        tag.putString(TASK_UID, mode.taskUid().toString());
        tag.remove(TASK_SOURCE);
        tag.putString(TASK_NAME, mode.chineseName());
    }

    public static boolean assignFromMaid(ServerPlayer player, UUID sourceMaid, ResourceLocation taskUid,
                                         String displayName, long gameTime) {
        CompoundTag tag = writableRoot(player);
        tag.putString(TASK_UID, taskUid.toString());
        tag.putUUID(TASK_SOURCE, sourceMaid);
        tag.putString(TASK_NAME, displayName == null || displayName.isBlank() ? taskUid.toString() : displayName);
        tag.putLong(TASK_SYNCED_AT, gameTime);
        WorkMode mode = WorkMode.byTaskUid(taskUid).orElse(WorkMode.IDLE);
        tag.putString(MODE, mode.id());
        tag.putBoolean(AUTO, true);
        return mode != WorkMode.IDLE || "idle".equals(taskUid.getPath());
    }

    public static WorkMode getMode(Player maid) {
        return WorkMode.byId(root(maid).getString(MODE)).orElse(WorkMode.IDLE);
    }

    public static Optional<ResourceLocation> taskUid(Player maid) {
        return Optional.ofNullable(ResourceLocation.tryParse(root(maid).getString(TASK_UID)));
    }

    public static Optional<UUID> taskSource(Player maid) {
        CompoundTag tag = root(maid);
        return tag.hasUUID(TASK_SOURCE) ? Optional.of(tag.getUUID(TASK_SOURCE)) : Optional.empty();
    }

    public static String taskDisplayName(Player maid) {
        String name = root(maid).getString(TASK_NAME);
        return name.isBlank() ? getMode(maid).chineseName() : name;
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
                        + " | 任务：" + taskDisplayName(maid)
                        + "（" + mode.category().chineseName() + "）"
                        + " | 自动：" + autoText)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public static String statusText(ServerPlayer maid) {
        WorkMode mode = getMode(maid);
        String uid = taskUid(maid).map(ResourceLocation::toString).orElse(mode.taskUid().toString());
        return "任务：" + taskDisplayName(maid) + " [" + uid + "] | 分类：" + mode.category().chineseName()
                + " | 自动：" + (isAuto(maid) ? "开启" : "关闭")
                + " | 执行说明：" + mode.executionHint()
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
            runCombatAction(maid, mode);
            return;
        }
        boolean acted = switch (mode) {
            case FARM -> harvestCrop(maid);
            case SUGAR_CANE -> harvestSugarCane(maid);
            case MELON -> harvestMelon(maid);
            case COCOA -> harvestCocoa(maid);
            case HONEY -> collectHoney(maid);
            case GRASS -> clearGrass(maid);
            case SNOW -> clearSnow(maid);
            case SHEARS -> shearSheep(maid);
            case MILK -> milkCow(maid);
            case FEED -> feedOwner(maid);
            case FEED_ANIMAL -> feedAnimal(maid);
            case EXTINGUISHING -> extinguish(maid);
            case TORCH -> placeTorch(maid);
            case FISHING, BOARD_GAMES -> {
                showWorkNotice(maid, mode.executionHint());
                yield false;
            }
            default -> false;
        };
        if (!acted && mode.hasAutomaticExecutor()) showMissingRequirement(maid, mode);
    }

    private static void runCombatAction(ServerPlayer maid, WorkMode mode) {
        switch (mode) {
            case ATTACK -> attackNearestMonster(maid);
            case RANGED_ATTACK, CROSSBOW_ATTACK, TRIDENT_ATTACK, DANMAKU_ATTACK -> {
                RangedCombatExecutor.Result result = RangedCombatExecutor.tick(maid, mode);
                if (result == RangedCombatExecutor.Result.NO_WEAPON) {
                    showWorkNotice(maid, mode.executionHint());
                } else if (result == RangedCombatExecutor.Result.NO_AMMO) {
                    showWorkNotice(maid, mode.chineseName() + "等待中：缺少弹药");
                }
            }
            case GUN_ATTACK -> {
                TaczGunCompat.Result result = TaczGunCompat.tick(maid);
                if (result == TaczGunCompat.Result.NO_GUN) {
                    showWorkNotice(maid, "枪械攻击等待中：请把有效 TACZ 枪械放在主手");
                } else if (result == TaczGunCompat.Result.NO_AMMO) {
                    showWorkNotice(maid, "枪械攻击等待中：弹匣和背包弹药不足，已尝试换弹");
                } else if (result == TaczGunCompat.Result.MANUAL) {
                    showWorkNotice(maid, "枪械自动开火已关闭，可按按键重新开启");
                } else if (result == TaczGunCompat.Result.MISSING_MOD) {
                    showWorkNotice(maid, "枪械攻击缺少 TACZ 模组");
                } else if (result == TaczGunCompat.Result.FAILED) {
                    showWorkNotice(maid, "TACZ 射击桥接发生错误，请检查日志");
                }
            }
            case SPELL_ATTACK -> {
                IronSpellsCompat.Result result = IronSpellsCompat.tick(maid);
                if (result == IronSpellsCompat.Result.NO_SELECTION) {
                    showWorkNotice(maid, "法术战斗等待中：请在铁魔法轮盘中选择一个法术");
                } else if (result == IronSpellsCompat.Result.UNAVAILABLE) {
                    showWorkNotice(maid, "法术暂时不可施放：请检查法力、法术冷却和施法条件");
                } else if (result == IronSpellsCompat.Result.MISSING_MOD) {
                    showWorkNotice(maid, "法术战斗缺少 Iron's Spells 'n Spellbooks 模组");
                } else if (result == IronSpellsCompat.Result.FAILED) {
                    showWorkNotice(maid, "铁魔法施法桥接发生错误，请检查日志");
                }
            }
            default -> { }
        }
    }

    private static void attackNearestMonster(ServerPlayer maid) {
        if (maid.getAttackStrengthScale(0.5F) < WorkConstants.COMBAT_READY_STRENGTH) return;
        double range = WorkConstants.COMBAT_RANGE;
        Monster target = maid.serverLevel().getEntitiesOfClass(Monster.class,
                        maid.getBoundingBox().inflate(range), value -> value.isAlive()
                                && maid.distanceToSqr(value) <= range * range
                                && !value.isAlliedTo(maid) && !maid.isAlliedTo(value)
                                && maid.hasLineOfSight(value))
                .stream().min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        if (target == null) return;
        maid.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        maid.attack(target);
        maid.resetAttackStrengthTicker();
    }

    private static boolean harvestCrop(ServerPlayer maid) {
        ServerLevel level = maid.serverLevel();
        ItemStack hoe = findItem(maid.getInventory(), stack -> stack.is(ItemTags.HOES));
        BlockPos mature = nearestBlock(maid, state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state));
        if (mature == null) return plantCrop(maid);

        BlockState state = level.getBlockState(mature);
        CropBlock crop = (CropBlock) state.getBlock();
        for (ItemStack drop : Block.getDrops(state, level, mature, level.getBlockEntity(mature), maid, hoe)) {
            giveOrDrop(maid, drop);
        }
        level.setBlock(mature, crop.getStateForAge(0), Block.UPDATE_ALL);
        damageTool(maid, hoe);
        return true;
    }

    private static boolean plantCrop(ServerPlayer maid) {
        ServerLevel level = maid.serverLevel();
        ItemStack seed = findItem(maid.getInventory(), WorkManager::isCropSeed);
        if (seed.isEmpty()) return false;
        IPlantable plantable = (IPlantable) ((ItemNameBlockItem) seed.getItem()).getBlock();
        BlockPos soil = nearestBlock(maid, (candidate, state) -> state.is(Blocks.FARMLAND)
                && level.getBlockState(candidate.above()).isAir()
                && state.canSustainPlant(level, candidate, Direction.UP, plantable));
        if (soil == null) return false;
        if (!level.setBlock(soil.above(), plantable.getPlant(level, soil), Block.UPDATE_ALL)) return false;
        consumeOne(maid, seed);
        return true;
    }

    private static boolean isCropSeed(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemNameBlockItem item)) return false;
        if (!(item.getBlock() instanceof IPlantable plantable)) return false;
        return plantable.getPlantType(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                == net.minecraftforge.common.PlantType.CROP;
    }

    private static boolean harvestSugarCane(ServerPlayer maid) {
        ServerLevel level = maid.serverLevel();
        BlockPos pos = nearestBlock(maid, (candidate, state) -> state.is(Blocks.SUGAR_CANE)
                && level.getBlockState(candidate.above()).is(Blocks.SUGAR_CANE));
        if (pos != null) {
            BlockPos top = pos;
            while (level.getBlockState(top.above()).is(Blocks.SUGAR_CANE)) top = top.above();
            return level.destroyBlock(top, true, maid);
        }
        return plantSugarCane(maid);
    }

    private static boolean plantSugarCane(ServerPlayer maid) {
        ItemStack cane = findItem(maid.getInventory(), stack -> stack.is(Items.SUGAR_CANE));
        if (cane.isEmpty()) return false;
        ServerLevel level = maid.serverLevel();
        BlockPos soil = nearestBlock(maid, (candidate, state) -> {
            if (!(state.is(BlockTags.DIRT) || state.is(BlockTags.SAND))
                    || !level.getBlockState(candidate.above()).isAir()) return false;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getFluidState(candidate.relative(direction)).is(net.minecraft.tags.FluidTags.WATER)
                        || level.getBlockState(candidate.relative(direction)).is(Blocks.FROSTED_ICE)) return true;
            }
            return false;
        });
        if (soil == null || !level.setBlock(soil.above(), Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        consumeOne(maid, cane);
        return true;
    }

    private static boolean harvestMelon(ServerPlayer maid) {
        ItemStack axe = findItem(maid.getInventory(), stack -> stack.is(ItemTags.AXES));
        if (axe.isEmpty()) return false;
        BlockPos pos = nearestBlock(maid, state -> state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN));
        if (pos == null) return false;
        boolean result = maid.serverLevel().destroyBlock(pos, true, maid);
        if (result) damageTool(maid, axe);
        return result;
    }

    private static boolean harvestCocoa(ServerPlayer maid) {
        ItemStack axe = findItem(maid.getInventory(), stack -> stack.is(ItemTags.AXES));
        if (axe.isEmpty()) return false;
        BlockPos pos = nearestBlock(maid, state -> state.getBlock() instanceof CocoaBlock
                && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE);
        if (pos == null) return false;
        ServerLevel level = maid.serverLevel();
        BlockState state = level.getBlockState(pos);
        for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), maid, axe)) {
            giveOrDrop(maid, drop);
        }
        level.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
        damageTool(maid, axe);
        return true;
    }

    private static boolean collectHoney(ServerPlayer maid) {
        ItemStack bottles = findItem(maid.getInventory(), stack -> stack.is(Items.GLASS_BOTTLE));
        if (bottles.isEmpty()) return false;
        BlockPos pos = nearestBlock(maid, state -> state.hasProperty(BeehiveBlock.HONEY_LEVEL)
                && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS);
        if (pos == null) return false;
        ServerLevel level = maid.serverLevel();
        level.setBlock(pos, level.getBlockState(pos).setValue(BeehiveBlock.HONEY_LEVEL, 0), Block.UPDATE_ALL);
        consumeOne(maid, bottles);
        giveOrDrop(maid, new ItemStack(Items.HONEY_BOTTLE));
        return true;
    }

    private static boolean clearGrass(ServerPlayer maid) {
        ItemStack hoe = findItem(maid.getInventory(), stack -> stack.is(ItemTags.HOES));
        if (hoe.isEmpty()) return false;
        BlockPos pos = nearestBlock(maid, state -> state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH));
        if (pos == null) return false;
        boolean result = maid.serverLevel().destroyBlock(pos, true, maid);
        if (result) damageTool(maid, hoe);
        return result;
    }

    private static boolean clearSnow(ServerPlayer maid) {
        ItemStack shovel = findItem(maid.getInventory(), stack -> stack.is(ItemTags.SHOVELS));
        if (shovel.isEmpty()) return false;
        BlockPos pos = nearestBlock(maid, state -> state.is(Blocks.SNOW));
        if (pos == null) return false;
        boolean result = maid.serverLevel().destroyBlock(pos, true, maid);
        if (result) damageTool(maid, shovel);
        return result;
    }

    private static boolean shearSheep(ServerPlayer maid) {
        ItemStack shears = findItem(maid.getInventory(), stack -> stack.is(Items.SHEARS));
        if (shears.isEmpty()) return false;
        double range = MaidData.workSearchRange(maid);
        Sheep sheep = maid.serverLevel().getEntitiesOfClass(Sheep.class, maid.getBoundingBox().inflate(range),
                        value -> value.isAlive() && value.readyForShearing()).stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        if (sheep == null) return false;
        sheep.shear(net.minecraft.sounds.SoundSource.PLAYERS);
        damageTool(maid, shears);
        return true;
    }

    private static boolean milkCow(ServerPlayer maid) {
        ItemStack bucket = findItem(maid.getInventory(), stack -> stack.is(Items.BUCKET));
        if (bucket.isEmpty()) return false;
        double range = MaidData.workSearchRange(maid);
        Cow cow = maid.serverLevel().getEntitiesOfClass(Cow.class, maid.getBoundingBox().inflate(range),
                        value -> value.isAlive() && !value.isBaby()).stream()
                .min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        if (cow == null) return false;
        consumeOne(maid, bucket);
        giveOrDrop(maid, new ItemStack(Items.MILK_BUCKET));
        return true;
    }

    private static boolean feedOwner(ServerPlayer maid) {
        LivingEntity owner = RelationshipService.resolveDirectManager(maid).orElse(null);
        double range = MaidData.workSearchRange(maid);
        if (owner == null || owner.level() != maid.serverLevel() || maid.distanceToSqr(owner) > range * range) return false;
        if (owner instanceof ServerPlayer playerOwner
                && !playerOwner.getFoodData().needsFood() && playerOwner.getHealth() >= playerOwner.getMaxHealth()) return false;
        if (!(owner instanceof ServerPlayer) && owner.getHealth() >= owner.getMaxHealth()) return false;

        Inventory inventory = maid.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.isEdible()) {
                if (owner instanceof ServerPlayer playerOwner) playerOwner.getFoodData().eat(stack.getItem(), stack);
                else owner.heal(4.0F);
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                consumeOne(maid, stack);
                return true;
            }
        }
        return false;
    }

    private static boolean feedAnimal(ServerPlayer maid) {
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
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean extinguish(ServerPlayer maid) {
        ServerLevel level = maid.serverLevel();
        boolean acted = maid.isOnFire();
        maid.setRemainingFireTicks(0);
        LivingEntity owner = RelationshipService.resolveDirectManager(maid)
                .filter(value -> value.level() == level).orElse(null);
        if (owner != null && owner.isOnFire()) {
            owner.setRemainingFireTicks(0);
            acted = true;
        }

        AABB area = maid.getBoundingBox().inflate(MaidData.workSearchRange(maid));
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity.isOnFire()) {
                entity.setRemainingFireTicks(0);
                acted = true;
            }
        }

        int radius = Math.max(1, (int) Math.floor(Math.min(MaidData.workSearchRange(maid), 8.0D)));
        for (BlockPos pos : BlockPos.betweenClosed(maid.blockPosition().offset(-radius, -radius, -radius),
                maid.blockPosition().offset(radius, radius, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof FireBlock && level.removeBlock(pos, false)) {
                acted = true;
            }
        }
        return acted;
    }

    private static boolean placeTorch(ServerPlayer maid) {
        ItemStack torches = findTorches(maid.getInventory());
        if (torches.isEmpty()) return false;

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
            return true;
        }
        return false;
    }

    private static ItemStack findTorches(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.TORCH)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static BlockPos nearestBlock(ServerPlayer maid, Predicate<BlockState> predicate) {
        return nearestBlock(maid, (candidate, state) -> predicate.test(state));
    }

    private static BlockPos nearestBlock(ServerPlayer maid, java.util.function.BiPredicate<BlockPos, BlockState> predicate) {
        int radius = Math.max(1, (int) Math.floor(Math.min(MaidData.workSearchRange(maid), 8.0D)));
        BlockPos origin = maid.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius),
                origin.offset(radius, 2, radius))) {
            BlockState state = maid.serverLevel().getBlockState(candidate);
            if (!predicate.test(candidate, state)) continue;
            double distance = candidate.distSqr(origin);
            if (distance < nearestDistance) {
                nearest = candidate.immutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static ItemStack findItem(Inventory inventory, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void giveOrDrop(ServerPlayer maid, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remaining = stack.copy();
        maid.getInventory().add(remaining);
        if (!remaining.isEmpty()) maid.drop(remaining, false);
    }

    private static void damageTool(ServerPlayer maid, ItemStack stack) {
        if (!maid.getAbilities().instabuild && stack.isDamageableItem()) {
            stack.hurtAndBreak(1, maid, player -> { });
        }
    }

    private static void showMissingRequirement(ServerPlayer maid, WorkMode mode) {
        showWorkNotice(maid, "工作等待中：附近没有可处理目标，或" + mode.executionHint());
    }

    private static void showWorkNotice(ServerPlayer maid, String message) {
        CompoundTag tag = writableRoot(maid);
        long now = maid.level().getGameTime();
        long last = tag.getLong(LAST_WORK_NOTICE);
        if (last > 0L && now - last < 200L) return;
        tag.putLong(LAST_WORK_NOTICE, now);
        maid.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW), true);
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
