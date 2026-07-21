package com.narrafork.playermaid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;

import java.util.Set;

/** Touhou Little Maid: Heart Pact 的玩家实体关系与物品桥。 */
public final class MaidMarriageCompat {
    private static final ResourceLocation PROPOSAL_RING = new ResourceLocation("maidmarriage", "proposal_ring");
    private static final ResourceLocation RAINBOW_BOUQUET = new ResourceLocation("maidmarriage", "rainbow_bouquet");
    private static final Set<Item> SWEETS = Set.of(Items.COOKIE, Items.CAKE, Items.PUMPKIN_PIE, Items.HONEY_BOTTLE,
            Items.SWEET_BERRIES, Items.APPLE, Items.GOLDEN_APPLE);
    private static final Set<Item> MEALS = Set.of(Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_COD,
            Items.COOKED_SALMON, Items.COOKED_MUTTON, Items.COOKED_PORKCHOP, Items.RABBIT_STEW, Items.MUSHROOM_STEW);
    private static final Set<Item> VALUABLES = Set.of(Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.AMETHYST_SHARD, Items.QUARTZ, Items.LAPIS_LAZULI, Items.NETHERITE_SCRAP);
    private static final Set<Item> OFFENSIVE = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE,
            Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.BONE);

    public enum GiftCategory { FLOWER, RAINBOW, SWEET, MEAL, VALUABLE, OFFENSIVE, OTHER }

    public static boolean loaded() { return ModList.get().isLoaded("maidmarriage"); }
    public static boolean isProposalRing(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(PROPOSAL_RING);
    }
    public static boolean isGift(ItemStack stack) {
        if (stack.isEmpty() || isProposalRing(stack)) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("maidmarriage") && Set.of("heart_pact_guide", "yes_pillow", "sunflower_hairpin",
                "marriage_consent_form", "growth_tool", "birth_tool", "pregnancy_test_tool", "pregnancy_settlement_tool",
                "family_tree_tool", "longing_tester", "flower_test_kit").contains(id.getPath())) return false;
        return category(stack) != GiftCategory.OTHER || stack.isEdible();
    }

    public static GiftCategory category(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(RAINBOW_BOUQUET)) return GiftCategory.RAINBOW;
        if (stack.is(ItemTags.FLOWERS)) return GiftCategory.FLOWER;
        if (OFFENSIVE.contains(stack.getItem())) return GiftCategory.OFFENSIVE;
        if (SWEETS.contains(stack.getItem())) return GiftCategory.SWEET;
        if (MEALS.contains(stack.getItem())) return GiftCategory.MEAL;
        if (VALUABLES.contains(stack.getItem())) return GiftCategory.VALUABLE;
        return GiftCategory.OTHER;
    }

    public static boolean giveHeldGift(ServerPlayer owner, ServerPlayer maid) {
        ItemStack stack = owner.getMainHandItem();
        if (!isGift(stack)) return false;
        long day = owner.serverLevel().getDayTime() / 24000L;
        if (AddonRelationshipData.getLong(maid, "GiftDay") != day) {
            AddonRelationshipData.putLong(maid, "GiftDay", day);
            AddonRelationshipData.putInt(maid, "GiftCount", 0);
        }
        if (AddonRelationshipData.getInt(maid, "GiftCount") >= 2) {
            owner.sendSystemMessage(Component.literal("今天已经送过两次礼物了。").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        GiftCategory category = category(stack);
        int favor;
        int mood;
        switch (category) {
            case RAINBOW -> { favor = 16; mood = -20; }
            case FLOWER -> { favor = 8; mood = -10; }
            case SWEET -> { favor = 3; mood = -15; }
            case MEAL -> { favor = AddonRelationshipData.favorability(maid) >= 128 ? 2 : 1; mood = -10; }
            case VALUABLE -> { favor = AddonRelationshipData.favorability(maid) >= 64 ? 6 : 2; mood = -5; }
            case OFFENSIVE -> { favor = -8; mood = 25; }
            default -> { favor = 1; mood = -3; }
        }
        String itemName = stack.getHoverName().getString();
        if (!owner.isCreative()) stack.shrink(1);
        AddonRelationshipData.putInt(maid, "GiftCount", AddonRelationshipData.getInt(maid, "GiftCount") + 1);
        int actual = AddonRelationshipData.addFavorability(maid, favor);
        LittleMadCompat.changeMood(maid, mood, "收到礼物");
        owner.sendSystemMessage(Component.literal("你把 " + itemName + " 送给了 " + maid.getGameProfile().getName()
                + "，好感 " + signed(actual) + "。").withStyle(actual >= 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        maid.sendSystemMessage(Component.literal("你接受了 " + owner.getGameProfile().getName() + " 送来的 " + itemName + "。"));
        return true;
    }

    public static boolean hasTwoRings(ServerPlayer owner) {
        return isProposalRing(owner.getMainHandItem()) && isProposalRing(owner.getOffhandItem())
                && !owner.getMainHandItem().getOrCreateTag().getBoolean("PlayerMaidVow")
                && !owner.getOffhandItem().getOrCreateTag().getBoolean("PlayerMaidVow");
    }

    public static void completeMarriage(ServerPlayer owner, ServerPlayer maid) {
        ItemStack ownerRing = owner.getMainHandItem().copy();
        ItemStack maidRing = owner.getOffhandItem().copy();
        ownerRing.setCount(1);
        maidRing.setCount(1);
        engrave(ownerRing, owner, maid);
        engrave(maidRing, owner, maid);
        if (!owner.isCreative()) {
            owner.getMainHandItem().shrink(1);
            owner.getOffhandItem().shrink(1);
        }
        if (!owner.addItem(ownerRing)) owner.drop(ownerRing, false);
        if (!maid.addItem(maidRing)) maid.drop(maidRing, false);
        AddonRelationshipData.marry(maid, owner);
    }

    private static void engrave(ItemStack ring, ServerPlayer owner, ServerPlayer maid) {
        var tag = ring.getOrCreateTag();
        tag.putBoolean("PlayerMaidVow", true);
        tag.putUUID("VowOwner", owner.getUUID());
        tag.putUUID("VowMaid", maid.getUUID());
        tag.putString("VowOwnerName", owner.getGameProfile().getName());
        tag.putString("VowMaidName", maid.getGameProfile().getName());
    }

    private static String signed(int value) { return value >= 0 ? "+" + value : Integer.toString(value); }
    private MaidMarriageCompat() { }
}
