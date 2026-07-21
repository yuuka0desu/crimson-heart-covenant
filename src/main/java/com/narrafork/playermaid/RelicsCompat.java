package com.narrafork.playermaid;

import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class RelicsCompat {
    public static void applyHunterBelt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer maid)) return;
        ServerPlayer owner = MaidData.ownerUuid(maid)
                .map(uuid -> maid.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        if (owner == null) return;

        ItemStack belt = EntityUtils.findEquippedCurio(owner, ItemRegistry.HUNTER_BELT.get());
        if (belt.isEmpty() || !(belt.getItem() instanceof IRelicItem relic)) return;

        relic.spreadExperience(owner, belt, 1);
        double multiplier = relic.getAbilityValue(belt, "training", "damage");
        event.setAmount((float) (event.getAmount() * multiplier));
    }

    private RelicsCompat() {
    }
}
