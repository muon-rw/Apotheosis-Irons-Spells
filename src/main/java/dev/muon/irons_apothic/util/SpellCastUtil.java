package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

public class SpellCastUtil {

    public static void castSpell(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target) {
        if (caster.level().isClientSide()) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (magicData.isCasting()) {
            IronsApothic.LOGGER.debug("SpellTriggerAffix: Entity is still casting {}, forcing spell completion", magicData.getCastingSpellId());
            AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
            oldSpell.onCast(caster.level(), magicData.getCastingSpellLevel(), caster, magicData.getCastSource(), magicData);
            oldSpell.onServerCastComplete(caster.level(), magicData.getCastingSpellLevel(), caster, magicData, false);
            magicData.resetCastingState();
            magicData = MagicData.getPlayerMagicData(caster);
        }

        IronsApothic.LOGGER.debug("SpellTriggerAffix: Merging target data, target: {}", target.getName().getString());
        updateTargetData(caster, target, magicData, spell, x -> true);

        if (caster instanceof ServerPlayer serverPlayer) {
            IronsApothic.LOGGER.debug("Casting SPELL FOR SERVERPLAYA");
            //TODO: Some of the manual targeting logic will need to be extracted in order to be used by non-players
            castSpellForPlayer(spell, spellLevel, serverPlayer, magicData);
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, spellLevel);
        } else if (caster instanceof LivingEntity) {
            if (spell.checkPreCastConditions(caster.level(), spellLevel, caster, magicData)) {
                spell.onCast(caster.level(), spellLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(caster.level(), spellLevel, caster, magicData, false);
            }
        }

    }

    private static void castSpellForPlayer(AbstractSpell spell, int spellLevel, ServerPlayer serverPlayer, MagicData magicData) {

        CastResult castResult = spell.canBeCastedBy(spellLevel, CastSource.COMMAND, magicData, serverPlayer);
        if (castResult.message != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        // Shouldn't happen
        if (magicData.isCasting()) {
            IronsApothic.LOGGER.warn("Attempted to trigger affix-cast while player was already casting");
            return;
        }

        // No precast conditions check here; we set a target manually, and ignore mana cost/cooldowns
        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        int effectiveCastTime = 0;
        // TODO: Test Continuous spells
        if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(spellLevel, serverPlayer);
        }
        magicData.initiateCast(spell, spellLevel, effectiveCastTime, CastSource.COMMAND, "command");
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(serverPlayer.level(), spellLevel, serverPlayer, magicData);

        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, effectiveCastTime, CastSource.COMMAND, "command"));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel));

        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetingData) {
            IronsApothic.LOGGER.debug("Casting Spell {} with target {}", magicData.getCastingSpellId(), targetingData.getTarget((ServerLevel) serverPlayer.level()).getName().getString());
        } else {
            IronsApothic.LOGGER.warn("Tried to merge Targeting Data but was overridden. Current cast data for spell {}: {}", magicData.getCastingSpellId(), magicData.getAdditionalCastData().getClass().getName());
        }

        // For instant cast spells, we need to execute them immediately
        // For spells with cast time > 0, the normal tick handler will execute them
        if (effectiveCastTime == 0) {
            spell.onCast(serverPlayer.level(), spellLevel, serverPlayer, CastSource.COMMAND, magicData);
            PacketDistributor.sendToPlayer(serverPlayer, new OnClientCastPacket(spell.getSpellId(), spellLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
        }
    }

    public static void updateTargetData(LivingEntity caster, Entity entityHit, MagicData playerMagicData, AbstractSpell spell, Predicate<LivingEntity> filter) {
        LivingEntity livingTarget = null;
        if (entityHit instanceof LivingEntity livingEntity && filter.test(livingEntity)) {
            livingTarget = livingEntity;
        } else if (entityHit instanceof PartEntity<?> partEntity && partEntity.getParent() instanceof LivingEntity livingParent && filter.test(livingParent)) {
            livingTarget = livingParent;
        }


        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));
            if (caster instanceof ServerPlayer serverPlayer) {
                if (spell.getCastType() != CastType.INSTANT) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.spell_target_success", livingTarget.getDisplayName().getString(), spell.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }
            if (livingTarget instanceof ServerPlayer serverPlayer) {
                Utils.sendTargetedNotification(serverPlayer, caster, spell);
            }
        } else if (caster instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
        }
    }

} 