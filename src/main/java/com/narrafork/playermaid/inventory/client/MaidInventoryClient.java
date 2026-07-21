package com.narrafork.playermaid.inventory.client;

import com.narrafork.playermaid.inventory.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;

public final class MaidInventoryClient {
    /** 仅在 FMLClientSetupEvent.enqueueWork 中调用。 */
    public static void registerScreens() {
        MenuScreens.register(ModMenus.MAID_INVENTORY.get(), MaidInventoryScreen::new);
    }

    private MaidInventoryClient() {
    }
}
