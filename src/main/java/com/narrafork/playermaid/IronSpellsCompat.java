package com.narrafork.playermaid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Iron's Spells 'n Spellbooks 可选兼容层，施放玩家当前选择的真实法术。 */
public final class IronSpellsCompat {
    public enum Result {
        CAST_STARTED, CASTING, NO_TARGET, NO_SELECTION, UNAVAILABLE, MISSING_MOD, FAILED
    }

    public static Result tick(ServerPlayer maid) {
        if (!ModList.get().isLoaded("irons_spellbooks")) return Result.MISSING_MOD;
        LivingEntity target = RangedCombatExecutor.findTarget(maid, WorkConstants.SPELL_RANGE);
        if (target == null) return Result.NO_TARGET;
        RangedCombatExecutor.lookAt(maid, target);

        try {
            Class<?> magicDataApi = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            Object magicData = magicDataApi.getMethod("getPlayerMagicData", LivingEntity.class).invoke(null, maid);
            if (Boolean.TRUE.equals(magicDataApi.getMethod("isCasting").invoke(magicData))) {
                return Result.CASTING;
            }

            Class<?> managerApi = Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Object manager = managerApi.getConstructor(Player.class).newInstance(maid);
            Object spellData = managerApi.getMethod("getSelectedSpellData").invoke(manager);
            Object selection = managerApi.getMethod("getSelection").invoke(manager);
            if (spellData == null || selection == null) return Result.NO_SELECTION;

            Class<?> spellDataApi = Class.forName("io.redspace.ironsspellbooks.api.spells.SpellData");
            Object spell = spellDataApi.getMethod("getSpell").invoke(spellData);
            int level = (int) spellDataApi.getMethod("getLevel").invoke(spellData);
            if (spell == null || level <= 0) return Result.NO_SELECTION;

            Field slotField = selection.getClass().getField("slot");
            String slot = String.valueOf(slotField.get(selection));
            ItemStack castingStack = "offhand".equalsIgnoreCase(slot) ? maid.getOffhandItem() : maid.getMainHandItem();
            Object castSource = selection.getClass().getMethod("getCastSource").invoke(selection);
            Class<?> spellApi = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Class<?> castSourceApi = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
            Method initiate = spellApi.getMethod("attemptInitiateCast", ItemStack.class, int.class, Level.class,
                    Player.class, castSourceApi, boolean.class, String.class);
            boolean started = Boolean.TRUE.equals(initiate.invoke(spell, castingStack, level, maid.level(), maid,
                    castSource, false, slot));
            return started ? Result.CAST_STARTED : Result.UNAVAILABLE;
        } catch (ReflectiveOperationException | LinkageError exception) {
            PlayerMaid.LOGGER.warn("铁魔法自动施法桥接失败", exception);
            return Result.FAILED;
        }
    }

    private IronSpellsCompat() {
    }
}
