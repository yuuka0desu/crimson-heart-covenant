package com.narrafork.playermaid.nativegui;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.narrafork.playermaid.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 客户端复用本体 EntityMaidRenderer，并只在本体原生面板预览中显示代理。 */
public final class NativeMaidGuiClient {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        if (!ModEntities.GUI_MAID_PROXY.isPresent()) {
            throw new IllegalStateException("EntityType playermaid:gui_maid_proxy was not registered; check ModEntities.register(modBus)");
        }
        EntityRenderers.register(ModEntities.GUI_MAID_PROXY.get(), context ->
                (net.minecraft.client.renderer.entity.EntityRenderer) new EntityMaidRenderer(context));
        MinecraftForge.EVENT_BUS.register(new NativeMaidGuiClient());
    }

    @SubscribeEvent
    public void hideProxyOutsideNativeGui(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof PlayerMaidGuiProxy
                && !(Minecraft.getInstance().screen instanceof AbstractMaidContainerGui<?>)) {
            event.setCanceled(true);
        }
    }
}
