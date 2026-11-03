package com.narrafork.playermaid;

import com.mojang.logging.LogUtils;
import com.narrafork.playermaid.inventory.ModMenus;
import com.narrafork.playermaid.inventory.client.MaidInventoryClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PlayerMaid.MOD_ID)
public final class PlayerMaid {
    public static final String MOD_ID = "playermaid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerMaid() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModMenus.register(modBus);
        ModEntities.register(modBus);
        modBus.addListener(this::onEntityAttributes);
        MaidNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MaidConfig.SPEC, "player-maid-server.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::onClientSetup);
            // 按键与客户端 tick 监听器只在客户端挂载，专用服务端不会加载 MaidKeys。
            modBus.addListener(com.narrafork.playermaid.inventory.client.MaidKeys::registerKeyMappings);
            MinecraftForge.EVENT_BUS.addListener(com.narrafork.playermaid.inventory.client.MaidKeys::onClientTick);
        });
        MinecraftForge.EVENT_BUS.register(new MaidEvents());
        LOGGER.info("车万女仆：绯心待契 2.1.1 已加载");
    }

    private void onEntityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUI_MAID_PROXY.get(),
                com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid.createAttributes().build());
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MaidInventoryClient.registerScreens();
            com.narrafork.playermaid.nativegui.NativeMaidGuiClient.register();
        });
    }
}
