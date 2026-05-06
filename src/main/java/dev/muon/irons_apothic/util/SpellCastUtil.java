package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Server-side cast pipeline for affix-triggered Iron's Spellbooks casts (CastSource.COMMAND).
 * <p>
 * Two entry points:
 * <ul>
 *   <li>{@link #castSelf} — caster-as-target / no-target (look-direction projectiles, self-AoE).</li>
 *   <li>{@link #castWithTarget} — bi-entity cast: aim is redirected to {@code target} via
 *       {@link BientitySpellCastAim} during {@code checkPreCastConditions} / {@code onServerPreCast} / {@code onCast},
 *       and any {@link TargetEntityCastData} or {@link TargetedTargetAreaCastData} the spell installs is overridden
 *       to point at {@code target}.</li>
 * </ul>
 * If the caster is already casting, the in-progress spell is force-completed (parity with the pre-refactor behavior;
 * affix triggers always interrupt).
 */
public final class SpellCastUtil {

    // ISS uses this slot id for /cast-style casts in MagicData.initiateCast and UpdateCastingStatePacket.
    private static final String COMMAND_CASTING_SLOT = "command";

    private SpellCastUtil() {
    }

    /** No target / caster-as-target. Skip aim redirect and any target injection. */
    public static void castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel) {
        castSelf(caster, spell, spellLevel, Optional.empty());
    }

    /**
     * Self-cast variant with a {@code castTime} override (player path only). When present, replaces the default
     * {@code 0} for INSTANT/LONG and the spell-derived value for CONTINUOUS.
     */
    public static void castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel, Optional<Integer> castTime) {
        if (caster.level().isClientSide()) {
            return;
        }
        castInternal(caster, spell, spellLevel, null, castTime);
    }

    /**
     * Cast {@code spell} from {@code caster} aimed at {@code target}. The caster's reported facing is steered toward
     * {@code target} during the cast setup window, and the spell's own {@link TargetEntityCastData} (or
     * {@link TargetedTargetAreaCastData}) is overridden to point at {@code target}.
     */
    public static void castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target) {
        castWithTarget(caster, spell, spellLevel, target, Optional.empty());
    }

    /**
     * Bi-entity variant with a {@code castTime} override (player path only).
     */
    public static void castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target,
                                      Optional<Integer> castTime) {
        if (caster.level().isClientSide()) {
            return;
        }
        castInternal(caster, spell, spellLevel, target == caster ? null : target, castTime);
    }

    private static void castInternal(LivingEntity caster, AbstractSpell spell, int spellLevel,
                                     @Nullable LivingEntity target, Optional<Integer> castTime) {
        Level world = caster.level();
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (magicData.isCasting()) {
            forceCompleteCurrentCast(caster, world, magicData);
        }

        if (target != null) {
            BientitySpellCastAim.push(caster, target);
        }
        try {
            if (caster instanceof ServerPlayer serverPlayer) {
                castForPlayer(serverPlayer, spell, spellLevel, magicData, target, castTime);
            } else if (caster instanceof IMagicEntity magicEntity) {
                // ISS mob casting; target / aim redirect aren't supported on this path.
                magicEntity.initiateCastSpell(spell, spellLevel);
            } else if (spell.checkPreCastConditions(world, spellLevel, caster, magicData)) {
                maybeOverrideTargetData(target, magicData);
                spell.onCast(world, spellLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, spellLevel, caster, magicData, false);
            }
        } finally {
            if (target != null) {
                BientitySpellCastAim.pop();
            }
        }
    }

    private static void castForPlayer(ServerPlayer serverPlayer, AbstractSpell spell, int spellLevel,
                                      MagicData magicData, @Nullable LivingEntity target, Optional<Integer> castTime) {
        Level world = serverPlayer.level();

        // Spells like RootSpell / HasteSpell install TargetEntityCastData here via Utils.preCastTargetHelper. The
        // bientity aim push redirects the raycast toward our target so the helper succeeds. Respect the return value:
        // if the spell aborts, so do we.
        if (!spell.checkPreCastConditions(world, spellLevel, serverPlayer, magicData)) {
            return;
        }

        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        int effectiveCastTime;
        if (castTime.isPresent()) {
            effectiveCastTime = Math.max(0, castTime.get());
        } else if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(spellLevel, serverPlayer);
        } else {
            effectiveCastTime = 0;
        }

        magicData.initiateCast(spell, spellLevel, effectiveCastTime, CastSource.COMMAND, COMMAND_CASTING_SLOT);
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(world, spellLevel, serverPlayer, magicData);

        maybeOverrideTargetData(target, magicData);

        if (effectiveCastTime > 0 && magicData.getAdditionalCastData() instanceof TargetEntityCastData boundData) {
            LivingEntity boundTarget = boundData.getTarget((ServerLevel) world);
            if (boundTarget != null) {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(boundTarget, spell));
            }
        }

        PacketDistributor.sendToPlayer(serverPlayer,
                new UpdateCastingStatePacket(spell.getSpellId(), spellLevel, effectiveCastTime, CastSource.COMMAND, COMMAND_CASTING_SLOT));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel));

        if (effectiveCastTime == 0) {
            spell.onCast(world, spellLevel, serverPlayer, CastSource.COMMAND, magicData);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OnClientCastPacket(spell.getSpellId(), spellLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
            spell.onServerCastComplete(world, spellLevel, serverPlayer, magicData, false);
        }
    }

    /**
     * If {@code target} is non-null and the spell installed a target-bearing cast data, swap our resolved target into
     * it. Strict class equality on {@link TargetEntityCastData} avoids stomping subclasses other than
     * {@link TargetedTargetAreaCastData}, which is handled explicitly.
     */
    private static void maybeOverrideTargetData(@Nullable LivingEntity target, MagicData magicData) {
        if (target == null) {
            return;
        }
        ICastData data = magicData.getAdditionalCastData();
        if (data == null) {
            return;
        }
        if (data.getClass() == TargetEntityCastData.class) {
            magicData.setAdditionalCastData(new TargetEntityCastData(target));
        } else if (data instanceof TargetedTargetAreaCastData targetedArea) {
            TargetedAreaEntity areaEntity = targetedArea.getAreaEntity();
            areaEntity.setOwner(target);
            areaEntity.setPos(target.position());
            magicData.setAdditionalCastData(new TargetedTargetAreaCastData(target, areaEntity));
        }
    }

    private static void forceCompleteCurrentCast(LivingEntity caster, Level world, MagicData magicData) {
        IronsApothic.LOGGER.debug("SpellCastUtil: force-completing in-progress cast {}", magicData.getCastingSpellId());
        AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
        int oldLevel = magicData.getCastingSpellLevel();
        CastSource oldSource = magicData.getCastSource();
        oldSpell.onCast(world, oldLevel, caster, oldSource, magicData);
        oldSpell.onServerCastComplete(world, oldLevel, caster, magicData, false);
        magicData.resetCastingState();
    }
}
