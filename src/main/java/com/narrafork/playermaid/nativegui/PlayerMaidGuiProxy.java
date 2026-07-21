package com.narrafork.playermaid.nativegui;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.narrafork.playermaid.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 车万女仆原生菜单要求实体必须是 EntityMaid。本类是短生命周期 GUI 数据代理，
 * 不参与世界玩法；原生菜单和原生按钮仍直接操作这个真实 EntityMaid 子类。
 */
public final class PlayerMaidGuiProxy extends EntityMaid {
    @SuppressWarnings("unchecked")
    public PlayerMaidGuiProxy(EntityType<? extends PlayerMaidGuiProxy> type, Level level) {
        super((EntityType<EntityMaid>) (EntityType<?>) type, level);
        configure();
    }

    public PlayerMaidGuiProxy(Level level) {
        this(ModEntities.GUI_MAID_PROXY.get(), level);
    }

    private void configure() {
        setNoAi(true);
        setInvulnerable(true);
        setSilent(true);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
