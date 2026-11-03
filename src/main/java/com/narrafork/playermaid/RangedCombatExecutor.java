package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.projectile.DanmakuShoot;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.function.Predicate;

/** 使用真实投射物执行弓、弩、三叉戟和 TLM 弹幕攻击。 */
public final class RangedCombatExecutor {
    private static final String AUTO_CROSSBOW_CHARGE_UNTIL = "PlayerMaidAutoCrossbowChargeUntil";
    /** 投射物上记录的主人 UUID，用于脱离实时关系的友伤判定。 */
    public static final String OWNER_TAG = "PlayerMaidOwner";

    public enum Result {
        FIRED, NO_TARGET, NO_WEAPON, NO_AMMO, COOLING_DOWN, FRIENDLY_BLOCKED
    }

    public static Result tick(ServerPlayer maid, WorkMode mode) {
        if (maid.isUsingItem()
                || maid.getAttackStrengthScale(0.5F) < WorkConstants.COMBAT_READY_STRENGTH) {
            return Result.COOLING_DOWN;
        }
        double range = switch (mode) {
            case TRIDENT_ATTACK -> WorkConstants.TRIDENT_RANGE;
            case DANMAKU_ATTACK -> WorkConstants.DANMAKU_RANGE;
            default -> WorkConstants.RANGED_RANGE;
        };
        Monster target = findTarget(maid, range);
        if (target == null) return Result.NO_TARGET;
        if (isOwnerInLineOfFire(maid, target)) return Result.FRIENDLY_BLOCKED;
        Result result = switch (mode) {
            case RANGED_ATTACK -> shootBow(maid, target);
            case CROSSBOW_ATTACK -> shootCrossbow(maid, target);
            case TRIDENT_ATTACK -> throwTrident(maid, target);
            case DANMAKU_ATTACK -> shootDanmaku(maid, target);
            default -> Result.NO_WEAPON;
        };
        if (result == Result.FIRED) maid.resetAttackStrengthTicker();
        return result;
    }

    /**
     * 主人是否位于女仆与目标的弹道附近。命中时跳过本次射击，
     * 避免箭矢擦过主人后反弹回女仆自身。
     */
    public static boolean isOwnerInLineOfFire(ServerPlayer maid, LivingEntity target) {
        LivingEntity owner = RelationshipService.resolveDirectManager(maid).orElse(null);
        if (owner == null || owner == maid || owner == target || owner.level() != maid.level()) return false;
        Vec3 start = maid.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 ownerCenter = new Vec3(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ());
        ProjectileAim.ClosestPoint closest = ProjectileAim.closestOnSegment(
                start.x, start.y, start.z, end.x, end.y, end.z,
                ownerCenter.x, ownerCenter.y, ownerCenter.z);
        if (!closest.insideSegment()) return false;
        double clearance = WorkConstants.FRIENDLY_FIRE_CLEARANCE + owner.getBbWidth() * 0.5D;
        return closest.distance() <= clearance;
    }

    /** 把主人 UUID 写入投射物，使友伤保护不再依赖飞行途中的实时关系查询。 */
    public static void tagOwner(net.minecraft.world.entity.Entity projectile, ServerPlayer maid) {
        projectile.getPersistentData().putUUID(OWNER_TAG, maid.getUUID());
    }

    /** 读取投射物上记录的主人 UUID。 */
    public static java.util.Optional<java.util.UUID> taggedOwner(net.minecraft.world.entity.Entity projectile) {
        var tag = projectile.getPersistentData();
        return tag.hasUUID(OWNER_TAG) ? java.util.Optional.of(tag.getUUID(OWNER_TAG)) : java.util.Optional.empty();
    }

    public static Monster findTarget(ServerPlayer maid, double range) {
        return maid.serverLevel().getEntitiesOfClass(Monster.class, maid.getBoundingBox().inflate(range),
                        target -> target.isAlive() && maid.distanceToSqr(target) <= range * range
                                && !target.isAlliedTo(maid) && !maid.isAlliedTo(target)
                                && maid.hasLineOfSight(target))
                .stream().min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
    }

    private static Result shootBow(ServerPlayer maid, LivingEntity target) {
        ItemStack bow = mainHandItem(maid, stack -> stack.getItem() instanceof BowItem);
        if (bow.isEmpty()) return Result.NO_WEAPON;
        if (maid.getCooldowns().isOnCooldown(bow.getItem())) return Result.COOLING_DOWN;
        ItemStack ammo = findArrow(maid.getInventory());
        boolean infinite = maid.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
        if (ammo.isEmpty() && !infinite) return Result.NO_AMMO;
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);

        AbstractArrow arrow = ((ArrowItem) ammo.getItem()).createArrow(maid.level(), ammo, maid);
        arrow.setCritArrow(true);
        arrow.setEnchantmentEffectsFromEntity(maid, 1.0F);
        int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        if (power > 0) arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5D + 0.5D);
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (punch > 0) arrow.setKnockback(punch);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) arrow.setSecondsOnFire(100);
        aim(arrow, maid, target, 3.0F, 1.0F);
        arrow.pickup = infinite ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        tagOwner(arrow, maid);
        maid.serverLevel().addFreshEntity(arrow);
        maid.serverLevel().playSound(null, maid.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        maid.swing(InteractionHand.MAIN_HAND, true);
        if (!infinite) ammo.shrink(1);
        damage(maid, bow);
        maid.getCooldowns().addCooldown(bow.getItem(), WorkConstants.BOW_COOLDOWN_TICKS);
        return Result.FIRED;
    }

    private static Result shootCrossbow(ServerPlayer maid, LivingEntity target) {
        ItemStack crossbow = mainHandItem(maid, stack -> stack.getItem() instanceof CrossbowItem);
        if (crossbow.isEmpty()) return Result.NO_WEAPON;
        if (maid.getCooldowns().isOnCooldown(crossbow.getItem())) return Result.COOLING_DOWN;
        int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, crossbow);
        int chargeTicks = Math.max(8, WorkConstants.CROSSBOW_COOLDOWN_TICKS - quickCharge * 5);
        long now = maid.level().getGameTime();
        long chargeUntil = crossbow.getOrCreateTag().getLong(AUTO_CROSSBOW_CHARGE_UNTIL);
        if (chargeUntil <= 0L) {
            crossbow.getOrCreateTag().putLong(AUTO_CROSSBOW_CHARGE_UNTIL, now + chargeTicks);
            return Result.COOLING_DOWN;
        }
        if (now < chargeUntil) return Result.COOLING_DOWN;
        crossbow.getOrCreateTag().remove(AUTO_CROSSBOW_CHARGE_UNTIL);
        ItemStack ammo = findArrow(maid.getInventory());
        if (ammo.isEmpty() && !maid.getAbilities().instabuild) return Result.NO_AMMO;
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);

        AbstractArrow arrow = ((ArrowItem) ammo.getItem()).createArrow(maid.level(), ammo, maid);
        arrow.setShotFromCrossbow(true);
        arrow.setPierceLevel((byte) EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, crossbow));
        arrow.setEnchantmentEffectsFromEntity(maid, 1.0F);
        aim(arrow, maid, target, 3.15F, 0.75F);
        arrow.pickup = maid.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        tagOwner(arrow, maid);
        maid.serverLevel().addFreshEntity(arrow);
        maid.serverLevel().playSound(null, maid.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        maid.swing(InteractionHand.MAIN_HAND, true);
        if (!maid.getAbilities().instabuild) ammo.shrink(1);
        damage(maid, crossbow);
        maid.getCooldowns().addCooldown(crossbow.getItem(), chargeTicks);
        return Result.FIRED;
    }

    private static Result throwTrident(ServerPlayer maid, LivingEntity target) {
        ItemStack trident = mainHandItem(maid, stack -> stack.is(Items.TRIDENT));
        if (trident.isEmpty()) return Result.NO_WEAPON;
        if (maid.getCooldowns().isOnCooldown(trident.getItem())) return Result.COOLING_DOWN;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.RIPTIDE, trident) > 0) return Result.NO_WEAPON;

        int loyalty = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.LOYALTY, trident);
        ItemStack projectileStack = trident.copy();
        projectileStack.setCount(1);
        damage(maid, projectileStack);
        ThrownTrident projectile = new ThrownTrident(maid.level(), maid, projectileStack);
        projectile.pickup = AbstractArrow.Pickup.DISALLOWED;
        aim(projectile, maid, target, 2.5F, 1.0F);
        tagOwner(projectile, maid);
        maid.serverLevel().addFreshEntity(projectile);
        maid.serverLevel().playSound(null, maid.blockPosition(), SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        maid.swing(InteractionHand.MAIN_HAND, true);
        if (loyalty > 0) {
            // 忠诚三叉戟的投射物就是返回物，发射后移除主手原物，避免复制。
            trident.shrink(1);
        } else {
            damage(maid, trident);
        }
        maid.getCooldowns().addCooldown(trident.getItem(), WorkConstants.TRIDENT_COOLDOWN_TICKS);
        return Result.FIRED;
    }

    private static Result shootDanmaku(ServerPlayer maid, LivingEntity target) {
        ItemStack gohei = mainHandItem(maid, ItemHakureiGohei::isGohei);
        if (gohei.isEmpty()) return Result.NO_WEAPON;
        if (maid.getCooldowns().isOnCooldown(gohei.getItem())) return Result.COOLING_DOWN;

        DanmakuShoot.create().setWorld(maid.level()).setThrower(maid).setTarget(target)
                .setRandomColor().setRandomType().setDamage(WorkConstants.DANMAKU_DAMAGE)
                .setGravity(0.0F).setVelocity(WorkConstants.DANMAKU_VELOCITY)
                .setInaccuracy(0.1F).aimedShot();
        maid.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, maid.getX(), maid.getEyeY(), maid.getZ(),
                5, 0.2D, 0.2D, 0.2D, 0.05D);
        maid.swing(InteractionHand.MAIN_HAND, true);
        damage(maid, gohei);
        maid.getCooldowns().addCooldown(gohei.getItem(), WorkConstants.DANMAKU_COOLDOWN_TICKS);
        return Result.FIRED;
    }

    private static void aim(AbstractArrow projectile, ServerPlayer maid, LivingEntity target,
                            float velocity, float inaccuracy) {
        Vec3 start = maid.getEyePosition();
        Vec3 destination = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        double dx = destination.x - start.x;
        double dz = destination.z - start.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = ProjectileAim.verticalDelta(projectile.getY(), destination.y, horizontal, velocity);
        projectile.shoot(dx, dy, dz, velocity, inaccuracy);
        lookAt(maid, target);
    }

    public static void lookAt(ServerPlayer maid, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(maid.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        maid.setYRot((float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F);
        maid.setXRot((float) -(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG));
        maid.setYHeadRot(maid.getYRot());
    }

    private static ItemStack mainHandItem(ServerPlayer maid, Predicate<ItemStack> predicate) {
        ItemStack stack = maid.getMainHandItem();
        return !stack.isEmpty() && predicate.test(stack) ? stack : ItemStack.EMPTY;
    }

    private static ItemStack findArrow(Inventory inventory) {
        return findItem(inventory, stack -> stack.getItem() instanceof ArrowItem);
    }

    private static ItemStack findItem(Inventory inventory, Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void damage(ServerPlayer maid, ItemStack stack) {
        if (!maid.getAbilities().instabuild) stack.hurtAndBreak(1, maid, player -> { });
    }

    private RangedCombatExecutor() {
    }
}
