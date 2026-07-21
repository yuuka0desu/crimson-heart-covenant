package com.narrafork.playermaid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

/**
 * YSM 2.6.5 可选兼容桥。全部发布版 YSM 符号都通过反射访问，避免通用端在未安装
 * YSM 时链接其类，也避免把玩家女仆误走 EntityMaid 能力链。
 */
public final class YsmCompat {
    public static final String MOD_ID = "yes_steve_model";
    private static final String ROOT = "com.elfmcys.yesstevemodel.";
    private static final String SWITCH_PACKET = ROOT + "O0Oo0oooOoOOO0oOoOOoOooo";
    private static final String MODEL_CAP_PROVIDER = ROOT + "Oo00o0OooOOo0ooOoo0oO0o0";
    private static final String MODEL_CAP = ROOT + "oooooooOOoOOoO00OooOo00O";
    private static final String OBF = "Oo0Oo0o00O00Oo0OOoOOoooo";
    private static final Map<AddonInteractions.Action, String> DEFAULT_ANIMATIONS = new EnumMap<>(AddonInteractions.Action.class);

    static {
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.PET, "animation.player.pet");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.HUG, "animation.player.hug");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.KISS, "animation.player.kiss");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.MORNING_KISS, "animation.player.morning_kiss");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.LAP_PILLOW, "animation.player.lap_pillow");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.CONFESS, "animation.player.confess");
        DEFAULT_ANIMATIONS.put(AddonInteractions.Action.MARRY, "animation.player.marry");
    }

    public static boolean available() {
        return MaidConfig.YSM_COMPAT.get() && ModList.get().isLoaded(MOD_ID);
    }

    public record ModelSelection(String modelId, String textureId) { }

    /** 读取普通玩家当前 YSM 模型，供车万原生 GUI 代理同步预览。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ModelSelection currentModel(ServerPlayer player) {
        if (!available()) return null;
        try {
            Class<?> providerClass = Class.forName(MODEL_CAP_PROVIDER);
            Capability capability = (Capability) providerClass.getField(OBF).get(null);
            Object modelInfo = player.getCapability(capability).orElse(null);
            if (modelInfo == null) return null;
            Class<?> capClass = Class.forName(MODEL_CAP);
            String model = (String) capClass.getMethod("o0OOooo0o0OO00OoOOOo0o0O").invoke(modelInfo);
            String texture = (String) capClass.getMethod("O00OOOooOoooOoo0o0o0oO0O").invoke(modelInfo);
            return new ModelSelection(model, texture);
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("读取玩家 YSM 模型失败：{}", player.getGameProfile().getName(), exception);
            return null;
        }
    }

    /** 让 YSM 自己完成模型存在性、纹理和授权模型校验，但把能力目标绑定到玩家女仆。 */
    public static boolean switchTargetModel(ServerPlayer target, String modelId, String textureId) {
        if (!available() || modelId == null || textureId == null || modelId.length() > 256 || textureId.length() > 256) return false;
        try {
            Class<?> packetClass = Class.forName(SWITCH_PACKET);
            Constructor<?> constructor = packetClass.getConstructor(String.class, String.class);
            Object packet = constructor.newInstance(modelId, textureId);
            Method handler = packetClass.getDeclaredMethod(OBF, packetClass, ServerPlayer.class);
            handler.setAccessible(true);
            handler.invoke(null, packet, target);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("YSM 目标模型切换失败：{} / {}", modelId, textureId, exception);
            return false;
        }
    }

    /** 在互动服务端确认成功后，直接调用目标普通玩家的 YSM ModelInfoCapability 动画同步。 */
    public static boolean playInteraction(ServerPlayer owner, ServerPlayer maid, AddonInteractions.Action action) {
        if (!available() || !MaidConfig.YSM_INTERACTION_ANIMATIONS.get() || action == AddonInteractions.Action.GIFT) return false;
        String animation = animationFor(action);
        if (animation.isBlank()) return false;
        boolean maidPlayed = play(maid, animation);
        if (MaidConfig.YSM_ANIMATE_OWNER.get()) play(owner, animation + ".owner");
        return maidPlayed;
    }

    private static String animationFor(AddonInteractions.Action action) {
        String configured = switch (action) {
            case PET -> MaidConfig.YSM_ANIMATION_PET.get();
            case HUG -> MaidConfig.YSM_ANIMATION_HUG.get();
            case KISS -> MaidConfig.YSM_ANIMATION_KISS.get();
            case MORNING_KISS -> MaidConfig.YSM_ANIMATION_MORNING_KISS.get();
            case LAP_PILLOW -> MaidConfig.YSM_ANIMATION_LAP_PILLOW.get();
            case CONFESS -> MaidConfig.YSM_ANIMATION_CONFESS.get();
            case MARRY -> MaidConfig.YSM_ANIMATION_MARRY.get();
            case GIFT -> "";
        };
        return configured.isBlank() ? DEFAULT_ANIMATIONS.getOrDefault(action, "") : configured;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean play(ServerPlayer player, String animation) {
        try {
            Class<?> providerClass = Class.forName(MODEL_CAP_PROVIDER);
            Field field = providerClass.getField(OBF);
            Capability capability = (Capability) field.get(null);
            LazyOptional<?> optional = player.getCapability(capability);
            Object modelInfo = optional.orElse(null);
            if (modelInfo == null) return false;
            Method method = Class.forName(MODEL_CAP).getMethod(OBF, ServerPlayer.class, String.class);
            method.invoke(modelInfo, player, animation);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("YSM 互动动画触发失败：{}", animation, exception);
            return false;
        }
    }

    private YsmCompat() { }
}
