package com.narrafork.playermaid;

import com.narrafork.playermaid.nativegui.PlayerMaidGuiProxy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 仅用于承载车万女仆本体原生 GUI 的临时实体类型。 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PlayerMaid.MOD_ID);

    public static final RegistryObject<EntityType<PlayerMaidGuiProxy>> GUI_MAID_PROXY = ENTITIES.register(
            "gui_maid_proxy", () -> EntityType.Builder.<PlayerMaidGuiProxy>of(PlayerMaidGuiProxy::new, MobCategory.MISC)
                    .sized(0.6F, 1.5F).clientTrackingRange(10).updateInterval(1)
                    .build(PlayerMaid.MOD_ID + ":gui_maid_proxy"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    private ModEntities() { }
}
