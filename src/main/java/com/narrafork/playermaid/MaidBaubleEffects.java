package com.narrafork.playermaid;

import com.narrafork.playermaid.inventory.MaidBaubleInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.lang.reflect.Method;
import java.util.List;

/** 将 EntityMaid 专用饰品效果等价应用到已绑定的玩家女仆。 */
public final class MaidBaubleEffects {
    private static final String TLM = "touhou_little_maid";

    public static void tick(ServerPlayer maid) {
        if (!MaidData.isMaid(maid) || maid.tickCount % 20 != 0) return;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        if (maid.tickCount % 60 == 0 && inventory.find(TLM, "item_magnet_bauble") >= 0) magnet(maid);
        if (maid.tickCount % 100 == 0) {
            int wireless = inventory.find(TLM, "wireless_io");
            if (wireless >= 0) wirelessTransfer(maid, inventory.getStackInSlot(wireless));
        }
        // 其它女仆附属注册的饰品钩子在这里代为执行。
        AddonBaubleCompat.tick(maid);
    }

    public static void onHurt(LivingHurtEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer maid) || !MaidData.isMaid(maid)) return;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);

        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            int nimble = inventory.find(TLM, "nimble_fabric");
            if (nimble >= 0 && teleportAway(maid)) {
                damageOne(inventory, nimble, maid);
                event.setCanceled(true);
                return;
            }
        }

        if (applyBuiltinProtection(event, maid, inventory)) return;

        MutableFloat amount = new MutableFloat(event.getAmount());
        if (AddonBaubleCompat.onInjured(maid, event.getSource(), amount)) {
            event.setCanceled(true);
            return;
        }
        if (amount.floatValue() != event.getAmount()) event.setAmount(amount.floatValue());
    }

    /** 车万女仆本体内置保护饰品；返回 true 表示已取消本次伤害。 */
    private static boolean applyBuiltinProtection(LivingHurtEvent event, ServerPlayer maid, MaidBaubleInventory inventory) {
        String protection = null;
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) protection = "explosion_protect_bauble";
        else if (event.getSource().is(DamageTypeTags.IS_FIRE)) protection = "fire_protect_bauble";
        else if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) protection = "projectile_protect_bauble";
        else if (event.getSource().is(DamageTypeTags.IS_FALL)) protection = "fall_protect_bauble";
        else if (event.getSource().is(DamageTypeTags.IS_DROWNING)) protection = "drown_protect_bauble";
        else if (event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) protection = "magic_protect_bauble";
        if (protection == null) return false;

        int slot = inventory.find(TLM, protection);
        if (slot < 0) return false;
        damageOne(inventory, slot, maid);
        if ("fire_protect_bauble".equals(protection)) {
            maid.clearFire();
            maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300));
        } else if ("drown_protect_bauble".equals(protection)) {
            maid.setAirSupply(maid.getMaxAirSupply());
        }
        event.setCanceled(true);
        maid.displayClientMessage(Component.literal("女仆保护饰品抵消了本次伤害").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer maid) || !MaidData.isMaid(maid)) return;
        MaidBaubleInventory inventory = new MaidBaubleInventory(maid);
        int slot = inventory.find(TLM, "ultramarine_orb_elixir");
        if (slot < 0) slot = inventory.find("minecraft", "totem_of_undying");
        if (slot >= 0) {
            ItemStack stack = inventory.getStackInSlot(slot);
            stack.shrink(1);
            inventory.setStackInSlot(slot, stack);
            event.setCanceled(true);
            maid.setHealth(Math.min(6.0F, maid.getMaxHealth()));
            maid.clearFire();
            maid.removeAllEffects();
            maid.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            maid.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            maid.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800));
            maid.level().broadcastEntityEvent(maid, (byte) 35);
            maid.sendSystemMessage(Component.literal("女仆专属饰品使你免于死亡！").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (AddonBaubleCompat.onDeath(maid, event.getSource())) event.setCanceled(true);
    }

    private static void magnet(ServerPlayer maid) {
        var area = maid.getBoundingBox().inflate(6.0D);
        List<ItemEntity> items = maid.serverLevel().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.isAlive() && !item.hasPickUpDelay());
        for (ItemEntity item : items) {
            item.setPos(maid.getX(), maid.getY(), maid.getZ());
            item.playerTouch(maid);
        }
        for (ExperienceOrb orb : maid.serverLevel().getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            orb.setPos(maid.getX(), maid.getY(), maid.getZ());
            orb.playerTouch(maid);
        }
    }

    private static boolean teleportAway(ServerPlayer maid) {
        BlockPos origin = maid.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = origin.getX() + maid.getRandom().nextInt(13) - 6;
            int y = origin.getY() + maid.getRandom().nextInt(5) - 2;
            int z = origin.getZ() + maid.getRandom().nextInt(13) - 6;
            BlockPos pos = new BlockPos(x, y, z);
            if (maid.serverLevel().getBlockState(pos).getCollisionShape(maid.serverLevel(), pos).isEmpty()
                    && maid.serverLevel().getBlockState(pos.above()).getCollisionShape(maid.serverLevel(), pos.above()).isEmpty()
                    && maid.serverLevel().getBlockState(pos.below()).isFaceSturdy(maid.serverLevel(), pos.below(), Direction.UP)) {
                maid.teleportTo(x + 0.5D, y, z + 0.5D);
                return true;
            }
        }
        return false;
    }

    private static void damageOne(MaidBaubleInventory inventory, int slot, ServerPlayer maid) {
        ItemStack stack = inventory.getStackInSlot(slot);
        stack.hurtAndBreak(1, maid, broken -> maid.sendSystemMessage(Component.literal("一件女仆饰品损坏了")));
        inventory.setStackInSlot(slot, stack);
    }

    private static void wirelessTransfer(ServerPlayer maid, ItemStack wireless) {
        try {
            Class<?> itemClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.item.ItemWirelessIO");
            Method getBindingPos = itemClass.getMethod("getBindingPos", ItemStack.class);
            Object value = getBindingPos.invoke(null, wireless);
            if (!(value instanceof BlockPos pos) || maid.blockPosition().distSqr(pos) > 256.0D) return;
            var blockEntity = maid.level().getBlockEntity(pos);
            if (blockEntity == null) return;
            IItemHandler chest = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            if (chest == null) return;
            boolean maidToChest = (boolean) itemClass.getMethod("isMaidToChest", ItemStack.class).invoke(null, wireless);
            if (maidToChest) moveOneToChest(maid, chest);
            else moveOneToMaid(maid, chest);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static void moveOneToChest(ServerPlayer maid, IItemHandler chest) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack source = maid.getInventory().getItem(slot);
            if (source.isEmpty() || MaidBaubleInventory.isMaidBauble(source)) continue;
            ItemStack one = source.copy();
            one.setCount(1);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(chest, one, false);
            if (remainder.isEmpty()) source.shrink(1);
            return;
        }
    }

    private static void moveOneToMaid(ServerPlayer maid, IItemHandler chest) {
        for (int slot = 0; slot < chest.getSlots(); slot++) {
            ItemStack simulated = chest.extractItem(slot, 1, true);
            if (simulated.isEmpty()) continue;
            ItemStack extracted = chest.extractItem(slot, 1, false);
            if (!maid.getInventory().add(extracted)) {
                ItemHandlerHelper.insertItemStacked(chest, extracted, false);
            }
            return;
        }
    }

    private MaidBaubleEffects() {
    }
}
