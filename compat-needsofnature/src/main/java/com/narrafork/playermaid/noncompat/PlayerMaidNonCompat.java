package com.narrafork.playermaid.noncompat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(PlayerMaidNonCompat.MOD_ID)
public final class PlayerMaidNonCompat {
    public static final String MOD_ID = "playermaid_non_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlayerMaidNonCompat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CompatConfig.SPEC,
                "player-maid-needs-of-nature-compat-server.toml");
        MinecraftForge.EVENT_BUS.register(new CompatEvents());
        LOGGER.info("Player Maid × Needs of Nature Compat 1.0.0 loaded");
    }
}
