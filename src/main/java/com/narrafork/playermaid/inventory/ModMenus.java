package com.narrafork.playermaid.inventory;

import com.narrafork.playermaid.PlayerMaid;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PlayerMaid.MOD_ID);

    public static final RegistryObject<MenuType<MaidInventoryMenu>> MAID_INVENTORY =
            MENUS.register("maid_inventory", () -> IForgeMenuType.create(MaidInventoryMenu::new));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }

    private ModMenus() {
    }
}
