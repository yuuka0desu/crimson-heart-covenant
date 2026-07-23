package com.narrafork.playermaid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** TACZ 可选兼容层；射速、弹药、枪栓、换弹、过热和伤害全部交还 TACZ 处理。 */
public final class TaczGunCompat {
    public enum Result {
        FIRED, NO_TARGET, NO_GUN, NO_AMMO, WAITING, MISSING_MOD, FAILED
    }

    public static Result tick(ServerPlayer maid) {
        if (!ModList.get().isLoaded("tacz")) return Result.MISSING_MOD;
        LivingEntity target = RangedCombatExecutor.findTarget(maid, WorkConstants.GUN_RANGE);
        if (target == null) return Result.NO_TARGET;
        ItemStack gunStack = maid.getMainHandItem();
        try {
            Class<?> gunApi = Class.forName("com.tacz.guns.api.item.IGun");
            Method getGun = gunApi.getMethod("getIGunOrNull", ItemStack.class);
            if (getGun.invoke(null, gunStack) == null) return Result.NO_GUN;

            RangedCombatExecutor.lookAt(maid, target);
            Class<?> operatorApi = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            Object operator = operatorApi.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, maid);
            if (operator == null) return Result.FAILED;

            Supplier<ItemStack> currentGun = maid::getMainHandItem;
            operatorApi.getMethod("draw", Supplier.class).invoke(operator, currentGun);
            @SuppressWarnings("unchecked")
            Supplier<Float> yaw = () -> maid.getYRot();
            @SuppressWarnings("unchecked")
            Supplier<Float> pitch = () -> maid.getXRot();
            Object raw = operatorApi.getMethod("shoot", Supplier.class, Supplier.class).invoke(operator, yaw, pitch);
            String result = raw instanceof Enum<?> value ? value.name() : String.valueOf(raw);
            return switch (result) {
                case "SUCCESS" -> Result.FIRED;
                case "NO_AMMO" -> {
                    operatorApi.getMethod("reload").invoke(operator);
                    yield Result.NO_AMMO;
                }
                case "NEED_BOLT" -> {
                    operatorApi.getMethod("bolt").invoke(operator);
                    yield Result.WAITING;
                }
                case "NOT_DRAW", "COOL_DOWN", "IS_RELOADING", "IS_DRAWING", "IS_BOLTING",
                        "IS_SPRINTING", "OVERHEATED", "NETWORK_FAIL" -> Result.WAITING;
                case "NOT_GUN", "ID_NOT_EXIST" -> Result.NO_GUN;
                default -> Result.FAILED;
            };
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("TACZ 自动射击桥接失败", exception);
            return Result.FAILED;
        }
    }

    private TaczGunCompat() {
    }
}
