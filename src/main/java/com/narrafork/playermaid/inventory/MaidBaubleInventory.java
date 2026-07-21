package com.narrafork.playermaid.inventory;

import com.narrafork.playermaid.MaidData;
import com.narrafork.playermaid.PlayerMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Method;

/** 玩家女仆独立的九格车万女仆专属饰品栏。 */
public final class MaidBaubleInventory extends ItemStackHandler {
    public static final int SIZE = 9;
    private static final String ROOT = "PlayerMaidBaubles";
    private final ServerPlayer maid;
    private boolean loading;

    public MaidBaubleInventory(ServerPlayer maid) {
        super(SIZE);
        this.maid = maid;
        loading = true;
        CompoundTag saved = persisted(maid).getCompound(ROOT);
        if (!saved.isEmpty()) deserializeNBT(saved);
        loading = false;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isMaidBauble(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (!loading) save();
    }

    public void save() {
        persisted(maid).put(ROOT, serializeNBT());
    }

    public int find(String namespace, String path) {
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!stack.isEmpty() && namespace.equals(key.getNamespace()) && path.equals(key.getPath())) return i;
        }
        return -1;
    }

    public static boolean isMaidBauble(ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded("touhou_little_maid")) return false;
        try {
            Class<?> manager = Class.forName("com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager");
            Method method = manager.getMethod("getBauble", ItemStack.class);
            return method.invoke(null, stack) != null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("检测车万女仆专属饰品失败: {}", stack, exception);
            return false;
        }
    }

    public static void returnAll(ServerPlayer maid) {
        CompoundTag persistent = persisted(maid);
        if (!persistent.contains(ROOT)) return;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.extractItem(slot, 64, false);
            if (!stack.isEmpty() && !maid.getInventory().add(stack)) maid.drop(stack, false);
        }
        persistent.remove(ROOT);
    }

    public static boolean hasData(Player player) {
        return persisted(player).contains(ROOT);
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(Player.PERSISTED_NBT_TAG)) persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }
}
