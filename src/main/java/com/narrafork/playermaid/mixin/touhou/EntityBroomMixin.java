package com.narrafork.playermaid.mixin.touhou;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom;
import com.github.tartaricacid.touhoulittlemaid.entity.item.control.PlayerBroomControl;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.nativegui.PlayerMaidGuiProxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 让玩家女仆也能作为车万女仆扫帚所需的「同伴」。
 *
 * <p>车万女仆的 {@code EntityBroom.travel} 要求第二个乘客是 {@code EntityMaid}，
 * 玩家女仆是 {@code ServerPlayer}，因此扫帚永远进不了飞行控制，只提示
 * 「你需要一个女仆也坐在扫帚上」。而 {@code PlayerBroomControl.travel} 完全不使用
 * 女仆参数，所以这里用一个脱管的代理 EntityMaid 占位即可。</p>
 *
 * <p>该 Mixin 位于 {@code playermaid.touhou.mixins.json}（required=false），
 * 车万女仆缺席时整份配置会被跳过，不影响主模组加载。</p>
 */
@Mixin(EntityBroom.class)
public abstract class EntityBroomMixin {
    @Unique
    private EntityMaid playermaid$dummyCompanion;
    @Unique
    private PlayerBroomControl playermaid$playerControl;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void playermaid$allowPlayerMaidFlightDev(Vec3 travelVector, CallbackInfo ci) {
        playermaid$allowPlayerMaidFlight(ci);
    }

    @Inject(method = "m_7023_(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void playermaid$allowPlayerMaidFlightSrg(Vec3 travelVector, CallbackInfo ci) {
        playermaid$allowPlayerMaidFlight(ci);
    }

    @Unique
    private void playermaid$allowPlayerMaidFlight(CallbackInfo ci) {
        EntityBroom broom = (EntityBroom) (Object) this;
        List<Entity> passengers = broom.getPassengers();
        if (passengers.size() < 2) return;

        LivingEntity controlling = broom.getControllingPassenger();
        if (!(controlling instanceof Player player)) return;
        // 已经有一名真实的 EntityMaid 同伴时走原版逻辑。
        if (passengers.get(1) instanceof EntityMaid) return;

        ServerPlayer companion = null;
        for (int i = 1; i < passengers.size(); i++) {
            if (passengers.get(i) instanceof ServerPlayer candidate
                    && candidate != player && MaidData.isMaid(candidate)) {
                companion = candidate;
                break;
            }
        }
        if (companion == null) return;

        PlayerBroomControl control = playermaid$playerControl;
        if (control == null) {
            control = new PlayerBroomControl(broom);
            playermaid$playerControl = control;
        }
        control.travel(player, playermaid$dummyCompanion(broom));
        broom.move(MoverType.SELF, broom.getDeltaMovement());
        ci.cancel();
    }

    @Unique
    private EntityMaid playermaid$dummyCompanion(EntityBroom broom) {
        EntityMaid dummy = playermaid$dummyCompanion;
        if (dummy == null || dummy.level() != broom.level()) {
            dummy = new PlayerMaidGuiProxy(broom.level());
            playermaid$dummyCompanion = dummy;
        }
        return dummy;
    }
}
