package com.narrafork.playermaid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.narrafork.playermaid.inventory.MaidBaubleInventory;
import com.narrafork.playermaid.nativegui.PlayerMaidGuiProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 让其它女仆附属模组注册的 {@code IMaidBauble} 饰品钩子对玩家女仆生效。
 *
 * <p>车万女仆只会对 {@code EntityMaid} 调用饰品钩子，玩家女仆是 {@code ServerPlayer}，
 * 因此心契同眠的发夹、信标饰品、合成饰品等附属饰品虽然能被装进饰品栏，
 * 其 {@code onTick / onInjured / onDeath / onPutOn / onTakeOff} 却永远不会执行。
 * 这里用一个脱管的 {@link PlayerMaidGuiProxy}（{@code EntityMaid} 子类）镜像玩家女仆状态后
 * 代为调用，并把钩子产生的状态（药水效果、火焰）回写到真实玩家。</p>
 *
 * <p>车万女仆本体的饰品由 {@link MaidBaubleEffects} 原生实现，这里跳过
 * {@code touhou_little_maid} 命名空间，避免同一效果被执行两次。</p>
 */
public final class AddonBaubleCompat {
    private static final String TLM = "touhou_little_maid";
    private static final String BAUBLE_CLASS =
            "com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble";
    private static final String MANAGER_CLASS =
            "com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager";

    private static final Map<UUID, Holder> HOLDERS = new HashMap<>();
    private static Method onTick;
    private static Method onInjured;
    private static Method onDeath;
    private static Method onPutOn;
    private static Method onTakeOff;
    private static Method getBauble;
    private static boolean resolved;
    private static boolean unavailable;

    /** 每 20 tick 由 {@link MaidBaubleEffects} 调用。 */
    public static void tick(ServerPlayer maid) {
        if (!enabled() || !MaidData.isMaid(maid)) return;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        if (!resolve()) return;

        Holder holder = holderFor(maid);
        if (holder == null) return;
        EntityMaid proxy = holder.proxy;
        syncProxy(maid, proxy);

        // 先处理换装事件，保证钩子顺序与原版一致：先离开、后佩戴。
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack now = inventory.getStackInSlot(slot);
            ItemStack before = slot < holder.previous.length ? holder.previous[slot] : ItemStack.EMPTY;
            if (sameItem(before, now)) continue;
            if (!before.isEmpty()) invoke(onTakeOff, baubleOf(before), proxy, before);
            if (!now.isEmpty()) invoke(onPutOn, baubleOf(now), proxy, now);
            if (slot < holder.previous.length) holder.previous[slot] = now.copy();
        }

        boolean touched = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Object bauble = baubleOf(stack);
            if (bauble == null) continue;
            touched |= invoke(onTick, bauble, proxy, stack);
        }
        if (touched) mirrorBack(maid, proxy);
    }

    /** 由 {@link MaidBaubleEffects#onHurt} 调用；返回 true 表示已取消本次伤害。 */
    public static boolean onInjured(ServerPlayer maid, DamageSource source, MutableFloat amount) {
        if (!enabled() || !MaidData.isMaid(maid) || !resolve()) return false;
        Holder holder = holderFor(maid);
        if (holder == null) return false;
        syncProxy(maid, holder.proxy);
        return dispatchInjured(maid, holder, source, amount);
    }

    /** 由 {@link MaidBaubleEffects#onDeath} 调用；返回 true 表示已取消本次死亡。 */
    public static boolean onDeath(ServerPlayer maid, DamageSource source) {
        if (!enabled() || !MaidData.isMaid(maid) || !resolve()) return false;
        Holder holder = holderFor(maid);
        if (holder == null) return false;
        syncProxy(maid, holder.proxy);
        boolean saved = false;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Object bauble = baubleOf(stack);
            if (bauble == null || onDeath == null) continue;
            try {
                Object result = onDeath.invoke(bauble, holder.proxy, stack, source);
                if (result instanceof Boolean value && value) saved = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                warnOnce(exception);
            }
        }
        if (saved) mirrorBack(maid, holder.proxy);
        return saved;
    }

    /** 玩家女仆离线或解除契约时释放镜像实体。 */
    public static void forget(UUID maidId) {
        HOLDERS.remove(maidId);
    }

    private static boolean dispatchInjured(ServerPlayer maid, Holder holder,
                                           DamageSource source, MutableFloat amount) {
        if (onInjured == null) return false;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        boolean cancelled = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Object bauble = baubleOf(stack);
            if (bauble == null) continue;
            try {
                Object result = onInjured.invoke(bauble, holder.proxy, stack, source, amount);
                if (result instanceof Boolean value && value) cancelled = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                warnOnce(exception);
            }
        }
        if (cancelled) mirrorBack(maid, holder.proxy);
        return cancelled;
    }

    private static Holder holderFor(ServerPlayer maid) {
        Holder holder = HOLDERS.get(maid.getUUID());
        if (holder != null && holder.proxy.level() == maid.level()) return holder;
        int slots = new MaidBaubleInventory(maid).getSlots();
        PlayerMaidGuiProxy proxy = new PlayerMaidGuiProxy(maid.serverLevel());
        proxy.setNoAi(true);
        proxy.setInvulnerable(true);
        Player owner = ownerOf(maid);
        if (owner != null) proxy.tame(owner);
        String name = maid.getGameProfile().getName();
        if (name != null && !name.isBlank()) {
            proxy.setCustomName(net.minecraft.network.chat.Component.literal(name));
        }
        holder = new Holder(proxy, new ItemStack[slots]);
        HOLDERS.put(maid.getUUID(), holder);
        return holder;
    }

    private static Player ownerOf(ServerPlayer maid) {
        return RelationshipService.resolveDirectManager(maid)
                .filter(Player.class::isInstance).map(Player.class::cast).orElse(null);
    }

    /** 把真实玩家女仆的可观察状态镜像到脱管代理，供附属饰品钩子读取。 */
    private static void syncProxy(ServerPlayer maid, EntityMaid proxy) {
        proxy.moveTo(maid.getX(), maid.getY(), maid.getZ(), maid.getYRot(), maid.getXRot());
        proxy.tickCount = maid.tickCount;
        proxy.setHealth(Math.max(1.0F, Math.min(proxy.getMaxHealth(), maid.getHealth())));
        proxy.setYHeadRot(maid.getYHeadRot());
        proxy.removeAllEffects();
        for (MobEffectInstance effect : maid.getActiveEffects()) {
            proxy.addEffect(new MobEffectInstance(effect));
        }
        proxy.setRemainingFireTicks(maid.getRemainingFireTicks());
    }

    /** 把钩子施加在代理上的效果回写到真实玩家。 */
    private static void mirrorBack(ServerPlayer maid, EntityMaid proxy) {
        for (MobEffectInstance effect : proxy.getActiveEffects()) {
            if (!maid.hasEffect(effect.getEffect())) maid.addEffect(new MobEffectInstance(effect));
        }
        if (proxy.getRemainingFireTicks() > maid.getRemainingFireTicks()) {
            maid.setRemainingFireTicks(proxy.getRemainingFireTicks());
        }
    }

    private static Object baubleOf(ItemStack stack) {
        if (stack.isEmpty() || getBauble == null) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (TLM.equals(id.getNamespace())) return null;
        try {
            return getBauble.invoke(null, stack);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce(exception);
            return null;
        }
    }

    /** 反射调用钩子；返回 true 表示调用确实发生（用于决定是否需要回写状态）。 */
    private static boolean invoke(Method method, Object bauble, EntityMaid proxy, ItemStack stack) {
        if (method == null || bauble == null) return false;
        try {
            method.invoke(bauble, proxy, stack);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce(exception);
            return false;
        }
    }

    private static boolean enabled() {
        return MaidConfig.ADDON_BAUBLE_COMPAT.get();
    }

    private static boolean resolve() {
        if (resolved) return !unavailable;
        resolved = true;
        try {
            Class<?> baubleClass = Class.forName(BAUBLE_CLASS);
            Class<?> managerClass = Class.forName(MANAGER_CLASS);
            onTick = method(baubleClass, "onTick", EntityMaid.class, ItemStack.class);
            onInjured = method(baubleClass, "onInjured", EntityMaid.class, ItemStack.class,
                    DamageSource.class, MutableFloat.class);
            onDeath = method(baubleClass, "onDeath", EntityMaid.class, ItemStack.class, DamageSource.class);
            onPutOn = method(baubleClass, "onPutOn", EntityMaid.class, ItemStack.class);
            onTakeOff = method(baubleClass, "onTakeOff", EntityMaid.class, ItemStack.class);
            getBauble = managerClass.getMethod("getBauble", ItemStack.class);
            PlayerMaid.LOGGER.info("女仆附属饰品兼容层已启用：onTick/onInjured/onDeath/onPutOn/onTakeOff 将对玩家女仆生效");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
            PlayerMaid.LOGGER.warn("女仆附属饰品兼容层初始化失败，已跳过", exception);
            return false;
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return left.isEmpty() && right.isEmpty();
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameTags(left, right);
    }

    private static boolean warned;

    private static void warnOnce(Exception exception) {
        if (warned) return;
        warned = true;
        PlayerMaid.LOGGER.warn("女仆附属饰品钩子执行失败（后续同类错误不再重复输出）", exception);
    }

    private static final class Holder {
        final EntityMaid proxy;
        final ItemStack[] previous;

        Holder(EntityMaid proxy, ItemStack[] previous) {
            this.proxy = proxy;
            this.previous = previous;
        }
    }

    private AddonBaubleCompat() {
    }
}
