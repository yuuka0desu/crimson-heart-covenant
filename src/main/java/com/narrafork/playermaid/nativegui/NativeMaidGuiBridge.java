package com.narrafork.playermaid.nativegui;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BigBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.PickType;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.narrafork.playermaid.AddonRelationshipData;
import com.narrafork.playermaid.MaidConfig;
import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.PlayerMaid;
import com.narrafork.playermaid.WorkManager;
import com.narrafork.playermaid.WorkMode;
import com.narrafork.playermaid.YsmCompat;
import com.narrafork.playermaid.inventory.CuriosAccess;
import com.narrafork.playermaid.inventory.MaidBaubleInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 把玩家女仆映射为短生命周期 EntityMaid，使车万女仆 1.5.2 的真实菜单、Screen、
 * 标签按钮及网络消息原样运行。这里只同步数据，不复制本体 GUI 行为。
 */
public final class NativeMaidGuiBridge {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static boolean open(ServerPlayer owner, ServerPlayer target) {
        if (!MaidConfig.GUI_ENABLED.get() || !com.narrafork.playermaid.RelationshipService.canManage(owner, target)
                || owner.serverLevel() != target.serverLevel()) return false;
        double range = MaidConfig.GUI_INTERACTION_DISTANCE.get();
        if (owner.distanceToSqr(target) > range * range) return false;
        Session occupied = SESSIONS.values().stream()
                .filter(session -> session.targetId.equals(target.getUUID()) && !session.ownerId.equals(owner.getUUID()))
                .findFirst().orElse(null);
        if (occupied != null) {
            owner.sendSystemMessage(Component.translatable("message.playermaid.gui.target_busy",
                    target.getGameProfile().getName()));
            return false;
        }

        close(owner.getUUID());
        PlayerMaidGuiProxy proxy = new PlayerMaidGuiProxy(owner.serverLevel());
        proxy.tame(owner);
        proxy.setCustomName(Component.literal(target.getGameProfile().getName()));
        proxy.setCustomNameVisible(false);
        proxy.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        proxy.setMaidBackpackType(new BigBackpack());
        copyStateToProxy(target, proxy);
        copyInventoryToProxy(target, proxy);
        copyBaublesToProxy(target, proxy);
        copyCurios(target, proxy);
        owner.serverLevel().addFreshEntity(proxy);
        SESSIONS.put(owner.getUUID(), new Session(owner.getUUID(), target.getUUID(), proxy, 2));
        return true;
    }

    public static void tick(MinecraftServer server) {
        List<UUID> remove = new ArrayList<>();
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerId);
            ServerPlayer target = server.getPlayerList().getPlayer(session.targetId);
            if (!valid(owner, target, session.proxy)) {
                remove.add(session.ownerId);
                continue;
            }
            session.proxy.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            session.proxy.setHealth(Math.min(session.proxy.getMaxHealth(), target.getHealth()));

            if (session.openDelay > 0) {
                session.openDelay--;
                if (session.openDelay == 0) session.proxy.openMaidGui(owner);
                continue;
            }

            if (owner.containerMenu instanceof AbstractMaidContainer menu && menu.getMaid() == session.proxy) {
                session.absentTicks = 0;
                syncStateToTarget(session, target);
                syncInventoryToTarget(target, session.proxy);
                syncBaublesToTarget(target, session.proxy);
                syncCurios(session.proxy, target);
                syncYsmToTarget(session, target);
            } else if (++session.absentTicks > 10) {
                remove.add(session.ownerId);
            }
        }
        remove.forEach(NativeMaidGuiBridge::close);
    }

    public static void close(UUID ownerId) {
        Session session = SESSIONS.remove(ownerId);
        if (session == null) return;
        MinecraftServer server = session.proxy.getServer();
        ServerPlayer target = server == null ? null : server.getPlayerList().getPlayer(session.targetId);
        if (target != null) {
            syncStateToTarget(session, target);
            syncInventoryToTarget(target, session.proxy);
            syncBaublesToTarget(target, session.proxy);
            syncCurios(session.proxy, target);
            syncYsmToTarget(session, target);
        }
        session.proxy.discard();
    }

    public static void closeFor(UUID playerId) {
        close(playerId);
        List<UUID> owners = SESSIONS.values().stream().filter(s -> s.targetId.equals(playerId)).map(s -> s.ownerId).toList();
        owners.forEach(NativeMaidGuiBridge::close);
    }

    private static boolean valid(ServerPlayer owner, ServerPlayer target, PlayerMaidGuiProxy proxy) {
        if (owner == null || target == null || proxy.isRemoved() || !com.narrafork.playermaid.RelationshipService.canManage(owner, target)
                || owner.serverLevel() != target.serverLevel() || proxy.level() != target.level()) return false;
        double range = MaidConfig.GUI_INTERACTION_DISTANCE.get();
        return owner.distanceToSqr(target) <= range * range;
    }

    private static void copyStateToProxy(ServerPlayer target, PlayerMaidGuiProxy proxy) {
        proxy.setHealth(Math.min(proxy.getMaxHealth(), target.getHealth()));
        proxy.setExperience(target.experienceLevel);
        proxy.setFavorability(Math.max(0, AddonRelationshipData.favorability(target)));
        proxy.setHomeModeEnable(MaidData.mode(target) == MaidData.Mode.STAY);
        proxy.setPickup(MaidData.pickup(target));
        proxy.setRideable(MaidData.rideable(target));
        proxy.setSchedule(MaidSchedule.valueOf(MaidData.schedule(target).name()));
        var config = proxy.getConfigManager();
        config.setShowBackpack(MaidData.showBackpack(target));
        config.setShowBackItem(MaidData.showBackItem(target));
        config.setChatBubbleShow(MaidData.showChatBubble(target));
        config.setSoundFreq(MaidData.soundFreq(target));
        config.setPickupType(PickType.values()[Math.min(MaidData.pickType(target), PickType.values().length - 1)]);
        config.setOpenDoor(MaidData.openDoor(target));
        config.setOpenFenceGate(MaidData.openFenceGate(target));
        config.setActiveClimbing(MaidData.activeClimbing(target));
        findTask(WorkManager.getMode(target)).ifPresent(proxy::setTask);

        YsmCompat.ModelSelection model = YsmCompat.currentModel(target);
        if (model != null) {
            proxy.setIsYsmModel(true);
            proxy.setYsmModel(model.modelId(), model.textureId(), Component.literal(target.getGameProfile().getName()));
        }
    }

    private static void syncStateToTarget(Session session, ServerPlayer target) {
        boolean home = session.proxy.isHomeModeEnable();
        if (home != session.lastHome) {
            if (home) MaidData.setStay(target); else MaidData.setFollow(target);
            session.lastHome = home;
        }
        MaidData.setPickup(target, session.proxy.isPickup());
        MaidData.setRideable(target, session.proxy.isRideable());
        MaidData.setSchedule(target, MaidData.Schedule.valueOf(session.proxy.getSchedule().name()));
        var config = session.proxy.getConfigManager();
        MaidData.setShowBackpack(target, config.isShowBackpack());
        MaidData.setShowBackItem(target, config.isShowBackItem());
        MaidData.setShowChatBubble(target, config.isChatBubbleShow());
        MaidData.setSoundFreq(target, Math.round(config.getSoundFreq()));
        MaidData.setPickType(target, config.getPickupType().ordinal());
        MaidData.setOpenDoor(target, config.isOpenDoor());
        MaidData.setOpenFenceGate(target, config.isOpenFenceGate());
        MaidData.setActiveClimbing(target, config.isActiveClimbing());
        if (session.proxy.getTask() != null) {
            WorkMode.byTaskUid(session.proxy.getTask().getUid()).ifPresent(mode -> {
                WorkManager.setMode(target, mode);
                WorkManager.setAuto(target, mode != WorkMode.IDLE);
            });
        }
    }

    private static java.util.Optional<IMaidTask> findTask(WorkMode mode) {
        return TaskManager.getTaskMap().entrySet().stream()
                .filter(entry -> entry.getKey().equals(mode.taskUid())).map(Map.Entry::getValue).findFirst();
    }

    private static void copyInventoryToProxy(ServerPlayer target, PlayerMaidGuiProxy proxy) {
        for (int i = 0; i < 27; i++) proxy.getMaidInv().setStackInSlot(i, target.getInventory().getItem(i + 9).copy());
        proxy.setItemSlot(EquipmentSlot.MAINHAND, target.getMainHandItem().copy());
        proxy.setItemSlot(EquipmentSlot.OFFHAND, target.getOffhandItem().copy());
        for (EquipmentSlot slot : ARMOR) proxy.setItemSlot(slot, target.getItemBySlot(slot).copy());
    }

    private static void syncInventoryToTarget(ServerPlayer target, PlayerMaidGuiProxy proxy) {
        for (int i = 0; i < 27; i++) target.getInventory().setItem(i + 9, proxy.getMaidInv().getStackInSlot(i).copy());
        target.setItemSlot(EquipmentSlot.MAINHAND, proxy.getMainHandItem().copy());
        target.setItemSlot(EquipmentSlot.OFFHAND, proxy.getOffhandItem().copy());
        for (EquipmentSlot slot : ARMOR) target.setItemSlot(slot, proxy.getItemBySlot(slot).copy());
        target.getInventory().setChanged();
    }

    private static void copyBaublesToProxy(ServerPlayer target, PlayerMaidGuiProxy proxy) {
        MaidBaubleInventory source = new MaidBaubleInventory(target);
        for (int i = 0; i < Math.min(source.getSlots(), proxy.getMaidBauble().getSlots()); i++)
            proxy.getMaidBauble().setStackInSlot(i, source.getStackInSlot(i).copy());
    }

    private static void syncBaublesToTarget(ServerPlayer target, PlayerMaidGuiProxy proxy) {
        MaidBaubleInventory destination = new MaidBaubleInventory(target);
        for (int i = 0; i < Math.min(destination.getSlots(), proxy.getMaidBauble().getSlots()); i++)
            destination.setStackInSlot(i, proxy.getMaidBauble().getStackInSlot(i).copy());
        destination.save();
    }

    private static void copyCurios(ServerPlayer from, PlayerMaidGuiProxy to) {
        copyHandlers(CuriosAccess.stacks(from), CuriosAccess.stacks(to));
    }

    private static void syncCurios(PlayerMaidGuiProxy from, ServerPlayer to) {
        copyHandlers(CuriosAccess.stacks(from), CuriosAccess.stacks(to));
    }

    private static void copyHandlers(List<IItemHandler> source, List<IItemHandler> destination) {
        int groups = Math.min(source.size(), destination.size());
        for (int group = 0; group < groups; group++) {
            IItemHandler from = source.get(group), to = destination.get(group);
            if (!(to instanceof IItemHandlerModifiable modifiable)) continue;
            for (int slot = 0; slot < Math.min(from.getSlots(), to.getSlots()); slot++)
                modifiable.setStackInSlot(slot, from.getStackInSlot(slot).copy());
        }
    }

    private static void syncYsmToTarget(Session session, ServerPlayer target) {
        if (!session.proxy.isYsmModel()) return;
        String model = session.proxy.getYsmModelId();
        String texture = session.proxy.getYsmModelTexture();
        String key = model + "\u0000" + texture;
        if (!key.equals(session.lastYsm)) {
            if (YsmCompat.switchTargetModel(target, model, texture)) session.lastYsm = key;
        }
    }

    private static final class Session {
        final UUID ownerId;
        final UUID targetId;
        final PlayerMaidGuiProxy proxy;
        int openDelay;
        int absentTicks;
        boolean lastHome;
        String lastYsm;

        Session(UUID ownerId, UUID targetId, PlayerMaidGuiProxy proxy, int openDelay) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.proxy = proxy;
            this.openDelay = openDelay;
            this.lastHome = proxy.isHomeModeEnable();
            this.lastYsm = proxy.isYsmModel() ? proxy.getYsmModelId() + "\u0000" + proxy.getYsmModelTexture() : "";
        }
    }

    private NativeMaidGuiBridge() { }
}
