package com.narrafork.playermaid.inventory;

import com.narrafork.playermaid.MaidConfig;
import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.MaidNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/** 固定 256x256 的玩家女仆主菜单及库存分页容器。 */
public final class MaidInventoryMenu extends AbstractContainerMenu {
    public static final int IMAGE_SIZE = 256;
    private static final int SLOT = 18;
    private static final int OWNER_X = 47;
    private static final int OWNER_Y = 154;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player owner;
    private final UUID maidId;
    private final ViewType viewType;
    private final int targetSlotCount;
    private final int maidBaubleSlotCount;
    private final int curiosSlotCount;

    public MaidInventoryMenu(int id, Inventory ownerInventory, FriendlyByteBuf data) {
        this(id, ownerInventory, read(data));
    }

    private MaidInventoryMenu(int id, Inventory ownerInventory, ClientData data) {
        super(menuType(), id);
        owner = ownerInventory.player;
        maidId = data.maidId;
        viewType = data.view;
        maidBaubleSlotCount = data.baubles;
        curiosSlotCount = data.curios;
        targetSlotCount = data.targetSlots;
        addClientTargetSlots(new ItemStackHandler(targetSlotCount));
        if (hasOwnerInventory(viewType)) addOwnerSlots(ownerInventory);
    }

    private MaidInventoryMenu(int id, Inventory ownerInventory, ServerPlayer maid, ViewType view, List<IItemHandler> curios) {
        super(menuType(), id);
        owner = ownerInventory.player;
        maidId = maid.getUUID();
        viewType = view;
        maidBaubleSlotCount = view == ViewType.MAID_BAUBLES ? MaidBaubleInventory.SIZE : 0;
        curiosSlotCount = view == ViewType.CURIOS ? curios.stream().mapToInt(IItemHandler::getSlots).sum() : 0;
        targetSlotCount = targetCount(view, curiosSlotCount);

        switch (view) {
            case COMBINED -> addMaidInventorySlots(maid.getInventory());
            case MAID_BAUBLES -> addHandlerSlots(new MaidBaubleInventory(maid), 0, MaidBaubleInventory.SIZE, 47, 48);
            case CURIOS -> {
                int offset = 0;
                for (IItemHandler handler : curios) {
                    addHandlerSlots(handler, 0, handler.getSlots(), 47, 48, offset);
                    offset += handler.getSlots();
                }
            }
            case ENDER_CHEST -> addContainerSlots(maid.getEnderChestInventory(), 27, 47, 48);
            default -> { }
        }
        if (hasOwnerInventory(view)) addOwnerSlots(ownerInventory);
    }

    public static void open(ServerPlayer owner, ServerPlayer maid, ViewType view) {
        boolean selfBaubles = view == ViewType.MAID_BAUBLES && owner == maid && MaidData.isMaid(maid);
        double distance = MaidConfig.GUI_INTERACTION_DISTANCE.get();
        if (!MaidConfig.GUI_ENABLED.get() || !online(owner, maid) || (!selfBaubles && !com.narrafork.playermaid.RelationshipService.canManage(owner, maid))
                || owner.serverLevel() != maid.serverLevel() || (!selfBaubles && owner.distanceToSqr(maid) > distance * distance)) return;
        List<IItemHandler> curios = view == ViewType.CURIOS ? CuriosAccess.stacks(maid) : List.of();
        int curiosCount = curios.stream().mapToInt(IItemHandler::getSlots).sum();
        int targetCount = targetCount(view, curiosCount);
        int baubles = view == ViewType.MAID_BAUBLES ? MaidBaubleInventory.SIZE : 0;
        NetworkHooks.openScreen(owner, new net.minecraft.world.MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("screen.playermaid.title", maid.getGameProfile().getName()); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new MaidInventoryMenu(id, inventory, maid, view, curios);
            }
        }, buffer -> {
            buffer.writeUUID(maid.getUUID()); buffer.writeEnum(view); buffer.writeVarInt(targetCount);
            buffer.writeVarInt(baubles); buffer.writeVarInt(curiosCount);
        });
        MaidNetwork.sendSnapshot(owner, maid);
    }

    public static void openSelfBaubles(ServerPlayer maid) { if (MaidData.isMaid(maid)) open(maid, maid, ViewType.MAID_BAUBLES); }

    @Override public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverOwner)) return true;
        ServerPlayer maid = serverOwner.getServer().getPlayerList().getPlayer(maidId);
        if (maid == null || !online(serverOwner, maid) || serverOwner.serverLevel() != maid.serverLevel()) return false;
        if (viewType == ViewType.MAID_BAUBLES && maid == serverOwner) return MaidData.isMaid(maid);
        double distance = MaidConfig.GUI_INTERACTION_DISTANCE.get();
        return com.narrafork.playermaid.RelationshipService.canManage(serverOwner, maid) && serverOwner.distanceToSqr(maid) <= distance * distance;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack copy = stack.copy();
        boolean moved = index < targetSlotCount
                ? moveItemStackTo(stack, targetSlotCount, slots.size(), false)
                : moveItemStackTo(stack, 0, targetSlotCount, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, stack);
        return copy;
    }

    public UUID getMaidId() { return maidId; }
    public ViewType getViewType() { return viewType; }
    public int getImageWidth() { return IMAGE_SIZE; }
    public int getImageHeight() { return IMAGE_SIZE; }
    public int getMaidBaubleSlotCount() { return maidBaubleSlotCount; }
    public int getCuriosSlotCount() { return curiosSlotCount; }

    private void addMaidInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 47 + col * SLOT, 39 + row * SLOT));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 47 + col * SLOT, 93));
        for (int i = 0; i < 4; i++) addSlot(new ArmorSlot(inventory, 39 - i, 47 + i * SLOT, 116, ARMOR[i]));
        addSlot(new Slot(inventory, 40, 119, 116));
    }

    private void addClientTargetSlots(ItemStackHandler dummy) {
        if (viewType == ViewType.COMBINED) {
            for (int i = 0; i < 36; i++) addSlot(new SlotItemHandler(dummy, i, 47 + i % 9 * SLOT, 39 + i / 9 * SLOT));
            for (int i = 0; i < 5; i++) addSlot(new SlotItemHandler(dummy, 36 + i, 47 + i * SLOT, 116));
        } else if (targetSlotCount > 0) addHandlerSlots(dummy, 0, targetSlotCount, 47, 48);
    }

    private void addHandlerSlots(IItemHandler handler, int first, int count, int x, int y) { addHandlerSlots(handler, first, count, x, y, 0); }
    private void addHandlerSlots(IItemHandler handler, int first, int count, int x, int y, int offset) {
        for (int i = 0; i < count; i++) {
            int p = offset + i;
            addSlot(new SlotItemHandler(handler, first + i, x + p % 9 * SLOT, y + p / 9 * SLOT));
        }
    }
    private void addContainerSlots(Container container, int count, int x, int y) {
        for (int i = 0; i < count; i++) addSlot(new Slot(container, i, x + i % 9 * SLOT, y + i / 9 * SLOT));
    }
    private void addOwnerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, OWNER_X + col * SLOT, OWNER_Y + row * SLOT));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, OWNER_X + col * SLOT, OWNER_Y + 58));
    }

    private static int targetCount(ViewType view, int curios) {
        return switch (view) {
            case COMBINED -> 41;
            case MAID_BAUBLES -> MaidBaubleInventory.SIZE;
            case CURIOS -> curios;
            case ENDER_CHEST -> 27;
            default -> 0;
        };
    }
    private static boolean hasOwnerInventory(ViewType view) {
        return view == ViewType.COMBINED || view == ViewType.MAID_BAUBLES || view == ViewType.CURIOS || view == ViewType.ENDER_CHEST;
    }
    private static boolean online(ServerPlayer owner, ServerPlayer maid) {
        return owner.getServer() == maid.getServer() && owner.getServer().getPlayerList().getPlayer(maid.getUUID()) == maid
                && maid.connection != null && maid.connection.isAcceptingMessages();
    }
    private static ClientData read(FriendlyByteBuf data) {
        UUID id = data.readUUID(); ViewType view = data.readEnum(ViewType.class);
        int targets = Math.max(0, data.readVarInt()); int baubles = Math.max(0, data.readVarInt()); int curios = Math.max(0, data.readVarInt());
        return new ClientData(id, view, targets, baubles, curios);
    }
    @SuppressWarnings("unchecked") private static MenuType<MaidInventoryMenu> menuType() { return ModMenus.MAID_INVENTORY.get(); }
    private record ClientData(UUID maidId, ViewType view, int targetSlots, int baubles, int curios) { }

    private static final class ArmorSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private ArmorSlot(Container container, int index, int x, int y, EquipmentSlot slot) { super(container, index, x, y); equipmentSlot = slot; }
        @Override public boolean mayPlace(ItemStack stack) { return Mob.getEquipmentSlotForItem(stack) == equipmentSlot; }
        @Override public int getMaxStackSize() { return 1; }
    }
}
