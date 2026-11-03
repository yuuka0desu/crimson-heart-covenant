package com.narrafork.playermaid.inventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.narrafork.playermaid.MaidNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 玩家女仆侧按键。整类只允许在客户端加载：
 * 挂载监听器的代码位于 DistExecutor 的 CLIENT 分支内，
 * 专用服务端不会加载本类，也不会触碰任何 net.minecraft.client 类型。
 */
public final class MaidKeys {
    private static final String CATEGORY = "key.categories.playermaid";

    private static KeyMapping toggleGunMode;
    private static KeyMapping cycleRange;

    /** 仅在 RegisterKeyMappingsEvent 中调用。 */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleGunMode = new KeyMapping("key.playermaid.toggle_gun_mode",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
        cycleRange = new KeyMapping("key.playermaid.cycle_range",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
        event.register(toggleGunMode);
        event.register(cycleRange);
    }

    /** 仅在客户端 tick 事件中调用。 */
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (toggleGunMode != null) {
            while (toggleGunMode.consumeClick()) MaidNetwork.sendSelfAction(MaidNetwork.SelfAction.TOGGLE_GUN_AUTO_FIRE);
        }
        if (cycleRange != null) {
            while (cycleRange.consumeClick()) MaidNetwork.sendSelfAction(MaidNetwork.SelfAction.CYCLE_ACTIVITY_RANGE);
        }
    }

    private MaidKeys() {
    }
}
