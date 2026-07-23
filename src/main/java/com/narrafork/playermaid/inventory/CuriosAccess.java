package com.narrafork.playermaid.inventory;

import com.narrafork.playermaid.PlayerMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Curios 的唯一接触点。这里使用反射，使没有安装 Curios 的服务端也可加载其余库存代码。
 */
public final class CuriosAccess {
    public static List<IItemHandler> stacks(LivingEntity player) {
        if (!ModList.get().isLoaded("curios")) {
            return List.of();
        }

        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getInventory = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
            Object lazyResult = getInventory.invoke(null, player);
            Object inventory = resolve(lazyResult);
            if (inventory == null) {
                return List.of();
            }

            Class<?> inventoryApi = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Object rawMap = inventoryApi.getMethod("getCurios").invoke(inventory);
            if (!(rawMap instanceof Map<?, ?> map)) {
                return List.of();
            }

            Class<?> stacksApi = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            Method getStacks = stacksApi.getMethod("getStacks");
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));
            List<IItemHandler> result = new ArrayList<>();
            for (Map.Entry<?, ?> entry : entries) {
                Object handler = entry.getValue();
                if (!stacksApi.isInstance(handler)) continue;
                Object stacks = getStacks.invoke(handler);
                if (stacks instanceof IItemHandler itemHandler && itemHandler.getSlots() > 0) {
                    result.add(itemHandler);
                }
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("读取 {} 的 Curios 槽位失败，组合库存将仅显示原版槽位", player.getName().getString(), exception);
            return List.of();
        }
    }

    private static Object resolve(Object value) throws ReflectiveOperationException {
        if (value instanceof LazyOptional<?> optional) {
            return optional.resolve().orElse(null);
        }
        if (value instanceof java.util.Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private CuriosAccess() {
    }
}
