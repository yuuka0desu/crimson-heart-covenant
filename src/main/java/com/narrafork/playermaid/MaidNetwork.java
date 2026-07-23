package com.narrafork.playermaid;

import com.narrafork.playermaid.inventory.MaidInventoryMenu;
import com.narrafork.playermaid.inventory.ViewType;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 玩家女仆主界面的服务端权威操作与快照同步。 */
public final class MaidNetwork {
    private static final String VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(PlayerMaid.MOD_ID, "maid_ui"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();
    private static int id;

    public enum Action {
        OPEN_MAIN, OPEN_TASK, OPEN_CONFIG, OPEN_BACKPACK, OPEN_BAUBLES, OPEN_CURIOS, OPEN_ENDER,
        TOGGLE_HOME, TOGGLE_PICKUP, TOGGLE_RIDE, CYCLE_SCHEDULE, TOGGLE_AUTO, NEXT_TASK, SELECT_TASK,
        TOGGLE_SHOW_BACKPACK, TOGGLE_SHOW_BACK_ITEM, TOGGLE_CHAT_BUBBLE,
        SOUND_DOWN, SOUND_UP, CYCLE_PICK_TYPE, TOGGLE_DOOR, TOGGLE_GATE, TOGGLE_CLIMBING,
        REFRESH
    }

    public record Snapshot(UUID maidId, String maidName, boolean home, boolean pickup, boolean rideable,
                           MaidData.Schedule schedule, boolean autoWork, WorkMode workMode,
                           String taskUid, String taskName, String managerType,
                           boolean showBackpack, boolean showBackItem, boolean showChatBubble,
                           int soundFreq, int pickType, boolean openDoor, boolean openFenceGate,
                           boolean activeClimbing, float health, float maxHealth, int armor, int experienceLevel,
                           int favorability, int bondLevel, int bondPoints, AddonRelationshipData.RelationStage relation,
                           AddonRelationshipData.MoodLevel mood) {
        static Snapshot of(ServerPlayer maid) {
            return new Snapshot(maid.getUUID(), maid.getGameProfile().getName(),
                    MaidData.mode(maid) == MaidData.Mode.STAY, MaidData.pickup(maid), MaidData.rideable(maid),
                    MaidData.schedule(maid), WorkManager.isAuto(maid), WorkManager.getMode(maid),
                    WorkManager.taskUid(maid).map(ResourceLocation::toString).orElse(WorkManager.getMode(maid).taskUid().toString()),
                    WorkManager.taskDisplayName(maid), MaidData.managerType(maid).map(Enum::name).orElse("NONE"),
                    MaidData.showBackpack(maid), MaidData.showBackItem(maid), MaidData.showChatBubble(maid),
                    MaidData.soundFreq(maid), MaidData.pickType(maid), MaidData.openDoor(maid),
                    MaidData.openFenceGate(maid), MaidData.activeClimbing(maid), maid.getHealth(), maid.getMaxHealth(),
                    maid.getArmorValue(), maid.experienceLevel, AddonRelationshipData.favorability(maid),
                    AddonRelationshipData.bondLevel(maid), AddonRelationshipData.availableBondPoints(maid),
                    AddonRelationshipData.relationStage(maid), AddonRelationshipData.moodLevel(maid));
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(ActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionPacket::encode).decoder(ActionPacket::decode).consumerMainThread(ActionPacket::handle).add();
        CHANNEL.messageBuilder(SnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SnapshotPacket::encode).decoder(SnapshotPacket::decode).consumerMainThread(SnapshotPacket::handle).add();
    }

    public static void sendAction(UUID maidId, Action action) {
        sendAction(maidId, action, 0);
    }

    public static void sendAction(UUID maidId, Action action, int value) {
        CHANNEL.sendToServer(new ActionPacket(maidId, action, value));
    }

    public static void sendSnapshot(ServerPlayer owner, ServerPlayer maid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), new SnapshotPacket(Snapshot.of(maid)));
    }

    public static Snapshot clientSnapshot(UUID maidId) {
        return ClientSnapshots.SNAPSHOTS.get(maidId);
    }

    private record ActionPacket(UUID maidId, Action action, int value) {
        static void encode(ActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.maidId);
            buffer.writeEnum(packet.action);
            buffer.writeVarInt(packet.value);
        }
        static ActionPacket decode(FriendlyByteBuf buffer) {
            return new ActionPacket(buffer.readUUID(), buffer.readEnum(Action.class), buffer.readVarInt());
        }
        static void handle(ActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer owner = context.getSender();
            if (owner == null || !(owner.containerMenu instanceof MaidInventoryMenu menu)
                    || !menu.getMaidId().equals(packet.maidId)) return;
            ServerPlayer maid = owner.getServer().getPlayerList().getPlayer(packet.maidId);
            double range = MaidConfig.GUI_INTERACTION_DISTANCE.get();
            if (maid == null || !RelationshipService.canManage(owner, maid) || owner.serverLevel() != maid.serverLevel()
                    || owner.distanceToSqr(maid) > range * range || !menu.stillValid(owner)) return;

            switch (packet.action) {
                case OPEN_MAIN -> MaidInventoryMenu.open(owner, maid, ViewType.MAIN);
                case OPEN_TASK -> { if (MaidConfig.GUI_TASK_TAB.get()) MaidInventoryMenu.open(owner, maid, ViewType.TASK); }
                case OPEN_CONFIG -> { if (MaidConfig.GUI_CONFIG_TAB.get()) MaidInventoryMenu.open(owner, maid, ViewType.CONFIG); }
                case OPEN_BACKPACK -> { if (MaidConfig.ALLOW_OWNER_INVENTORY_MANAGEMENT.get()) MaidInventoryMenu.open(owner, maid, ViewType.COMBINED); }
                case OPEN_BAUBLES -> { if (MaidConfig.ALLOW_OWNER_INVENTORY_MANAGEMENT.get() && MaidConfig.GUI_BAUBLE_TAB.get()) MaidInventoryMenu.open(owner, maid, ViewType.MAID_BAUBLES); }
                case OPEN_CURIOS -> { if (MaidConfig.ALLOW_OWNER_INVENTORY_MANAGEMENT.get() && MaidConfig.GUI_CURIOS_TAB.get()) MaidInventoryMenu.open(owner, maid, ViewType.CURIOS); }
                case OPEN_ENDER -> { if (MaidConfig.ALLOW_OWNER_INVENTORY_MANAGEMENT.get() && MaidConfig.GUI_ENDER_TAB.get()) MaidInventoryMenu.open(owner, maid, ViewType.ENDER_CHEST); }
                case TOGGLE_HOME -> { if (MaidData.mode(maid) == MaidData.Mode.STAY) MaidData.setFollow(maid); else MaidData.setStay(maid); }
                case TOGGLE_PICKUP -> MaidData.setPickup(maid, !MaidData.pickup(maid));
                case TOGGLE_RIDE -> {
                    if (MaidConfig.MAID_UI_ALLOW_OWNER_RIDE.get()) {
                        boolean enabled = !MaidData.rideable(maid);
                        MaidData.setRideable(maid, enabled);
                        if (enabled) owner.startRiding(maid, true);
                        else if (owner.getVehicle() == maid) owner.stopRiding();
                    }
                }
                case CYCLE_SCHEDULE -> MaidData.setSchedule(maid, MaidData.schedule(maid).next());
                case TOGGLE_AUTO -> WorkManager.setAuto(maid, !WorkManager.isAuto(maid));
                case NEXT_TASK -> {
                    WorkMode[] modes = WorkMode.values();
                    WorkMode selected = modes[(WorkManager.getMode(maid).ordinal() + 1) % modes.length];
                    WorkManager.setMode(maid, selected);
                    sendWorkModeHint(owner, maid, selected);
                }
                case SELECT_TASK -> {
                    WorkMode[] modes = WorkMode.values();
                    if (packet.value >= 0 && packet.value < modes.length) {
                        WorkMode selected = modes[packet.value];
                        WorkManager.setMode(maid, selected);
                        sendWorkModeHint(owner, maid, selected);
                    }
                }
                case TOGGLE_SHOW_BACKPACK -> MaidData.setShowBackpack(maid, !MaidData.showBackpack(maid));
                case TOGGLE_SHOW_BACK_ITEM -> MaidData.setShowBackItem(maid, !MaidData.showBackItem(maid));
                case TOGGLE_CHAT_BUBBLE -> MaidData.setShowChatBubble(maid, !MaidData.showChatBubble(maid));
                case SOUND_DOWN -> MaidData.setSoundFreq(maid, MaidData.soundFreq(maid) - 10);
                case SOUND_UP -> MaidData.setSoundFreq(maid, MaidData.soundFreq(maid) + 10);
                case CYCLE_PICK_TYPE -> MaidData.setPickType(maid, MaidData.pickType(maid) + 1);
                case TOGGLE_DOOR -> { if (MaidConfig.MAID_UI_ENABLE_DOOR_INTERACTION.get()) MaidData.setOpenDoor(maid, !MaidData.openDoor(maid)); }
                case TOGGLE_GATE -> { if (MaidConfig.MAID_UI_ENABLE_FENCE_GATE_INTERACTION.get()) MaidData.setOpenFenceGate(maid, !MaidData.openFenceGate(maid)); }
                case TOGGLE_CLIMBING -> { if (MaidConfig.MAID_UI_ENABLE_ACTIVE_CLIMBING.get()) MaidData.setActiveClimbing(maid, !MaidData.activeClimbing(maid)); }
                case REFRESH -> { }
            }
            if (owner.containerMenu instanceof MaidInventoryMenu current && current.getMaidId().equals(maid.getUUID())) {
                current.broadcastChanges();
                sendSnapshot(owner, maid);
            }
        }
    }

    private static void sendWorkModeHint(ServerPlayer owner, ServerPlayer maid, WorkMode mode) {
        Component message = Component.literal(mode.chineseName() + "：" + mode.executionHint())
                .withStyle(mode.hasAutomaticExecutor() ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
        owner.sendSystemMessage(message);
        if (owner != maid) maid.sendSystemMessage(message);
    }

    private record SnapshotPacket(Snapshot snapshot) {
        static void encode(SnapshotPacket packet, FriendlyByteBuf b) {
            Snapshot s = packet.snapshot;
            b.writeUUID(s.maidId); b.writeUtf(s.maidName); b.writeBoolean(s.home); b.writeBoolean(s.pickup);
            b.writeBoolean(s.rideable); b.writeEnum(s.schedule); b.writeBoolean(s.autoWork); b.writeEnum(s.workMode);
            b.writeUtf(s.taskUid); b.writeUtf(s.taskName); b.writeUtf(s.managerType);
            b.writeBoolean(s.showBackpack); b.writeBoolean(s.showBackItem); b.writeBoolean(s.showChatBubble);
            b.writeVarInt(s.soundFreq); b.writeVarInt(s.pickType); b.writeBoolean(s.openDoor);
            b.writeBoolean(s.openFenceGate); b.writeBoolean(s.activeClimbing); b.writeFloat(s.health); b.writeFloat(s.maxHealth);
            b.writeVarInt(s.armor); b.writeVarInt(s.experienceLevel); b.writeVarInt(s.favorability); b.writeVarInt(s.bondLevel);
            b.writeVarInt(s.bondPoints); b.writeEnum(s.relation); b.writeEnum(s.mood);
        }
        static SnapshotPacket decode(FriendlyByteBuf b) {
            return new SnapshotPacket(new Snapshot(b.readUUID(), b.readUtf(64), b.readBoolean(), b.readBoolean(),
                    b.readBoolean(), b.readEnum(MaidData.Schedule.class), b.readBoolean(), b.readEnum(WorkMode.class),
                    b.readUtf(128), b.readUtf(128), b.readUtf(32),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readEnum(AddonRelationshipData.RelationStage.class),
                    b.readEnum(AddonRelationshipData.MoodLevel.class)));
        }
        static void handle(SnapshotPacket packet, Supplier<NetworkEvent.Context> supplier) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSnapshots.accept(packet.snapshot));
        }
    }

    private static final class ClientSnapshots {
        private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();
        private static void accept(Snapshot snapshot) {
            SNAPSHOTS.put(snapshot.maidId, snapshot);
            if (Minecraft.getInstance().screen instanceof com.narrafork.playermaid.inventory.client.MaidInventoryScreen screen) {
                screen.onSnapshot(snapshot);
            }
        }
    }

    private MaidNetwork() { }
}
