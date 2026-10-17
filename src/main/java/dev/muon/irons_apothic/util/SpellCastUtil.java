package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.network.CastStartedWithoutAnimationPacket;
import dev.muon.irons_apothic.network.CastWithoutSlowdownPacket;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.spells.TargetedTargetAreaCastData;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side cast pipeline for affix-triggered Iron's Spellbooks casts (CastSource.COMMAND).
 * <ul>
 *   <li>{@link #castSelf}: no target, so the spell uses its standard targeting (look-direction projectiles,
 *       self-AoE).</li>
 *   <li>{@link #castWithTarget}: aim is redirected to {@code target} via {@link BientitySpellCastAim} during
 *       {@code checkPreCastConditions}, {@code onServerPreCast} and {@code onCast}, and any
 *       {@link TargetEntityCastData} or {@link TargetedTargetAreaCastData} the spell installs is overridden to point
 *       at {@code target}.</li>
 * </ul>
 * We queue a cast in {@link PendingSpellCasts} when it has a delay, or when the caster is resolving a spell right now
 * (the trigger came from that spell's damage or healing). ISS keeps one cast per player, so starting ours
 * mid-resolution would overwrite the resolving spell, and ISS's cleanup for that spell would then wipe out ours. If a
 * queued cast's target is gone by the time it runs, the spell falls back to its standard targeting.
 * <p>
 * If the caster is already casting something else, {@link SpellCastInterruptMode} decides what happens to it. We can't
 * force-complete a CONTINUOUS cast, so FORCE_COMPLETE skips the trigger instead. Both entry points return false when
 * the trigger had no effect: it was skipped, or nothing was interrupted and the spell's {@code checkPreCastConditions}
 * failed. Queuing a cast counts as an effect.
 */
public final class SpellCastUtil {

    // ISS uses this slot id for /cast-style casts in MagicData.initiateCast and UpdateCastingStatePacket.
    private static final String COMMAND_CASTING_SLOT = "command";

    // Server thread only: AbstractSpell#castSpell takes a ServerPlayer.
    private static final Set<LivingEntity> RESOLVING = Collections.newSetFromMap(new IdentityHashMap<>());

    // Server thread only. In-progress player casts we started, by spell id, until ISS completes or cancels them. Weak
    // keys drop a disconnected player's entry; vanilla gives a respawned player the old entity id, so they keep it.
    private static final Map<ServerPlayer, Map<String, SpellCastOptions>> AFFIX_CASTS = new WeakHashMap<>();

    // Server thread only. Set while one of our casts runs or resolves, so its damage and healing can't trigger more
    // affixes. That includes casts with a cast time, which ISS resolves later from its own tick.
    private static boolean castingForAffix;

    private SpellCastUtil() {
    }

    public static boolean isCastingForAffix() {
        return castingForAffix;
    }

    /** Wraps every {@code AbstractSpell#castSpell} (see {@code AbstractSpellMixin}). */
    public static void resolve(ServerPlayer caster, AbstractSpell spell, Runnable castSpell) {
        boolean outermost = RESOLVING.add(caster);
        boolean wasCastingForAffix = castingForAffix;
        castingForAffix |= isAffixCast(caster, spell);
        try {
            castSpell.run();
        } finally {
            castingForAffix = wasCastingForAffix;
            if (outermost) {
                RESOLVING.remove(caster);
            }
        }
    }

    /** Called from {@code AbstractSpellMixin} when {@code spell} finishes or is cancelled. */
    @Nullable
    public static SpellCastOptions untrackAffixCast(ServerPlayer caster, AbstractSpell spell) {
        Map<String, SpellCastOptions> casts = AFFIX_CASTS.get(caster);
        if (casts == null) {
            return null;
        }
        SpellCastOptions options = casts.remove(spell.getSpellId());
        if (casts.isEmpty()) {
            AFFIX_CASTS.remove(caster);
        }
        return options;
    }

    private static boolean isAffixCast(ServerPlayer caster, AbstractSpell spell) {
        Map<String, SpellCastOptions> casts = AFFIX_CASTS.get(caster);
        return casts != null && casts.containsKey(spell.getSpellId());
    }

    /** No target / caster-as-target. Skip aim redirect and any target injection. */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel) {
        return castSelf(caster, spell, spellLevel, SpellCastOptions.DEFAULT);
    }

    /**
     * Self-cast variant with a {@code castTime} override (player path only). When present, replaces the default
     * {@code 0} for INSTANT/LONG and the spell-derived value for CONTINUOUS.
     */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel, Optional<Integer> castTime) {
        return castSelf(caster, spell, spellLevel, SpellCastOptions.withCastTime(castTime));
    }

    /**
     * {@code castTime}, {@code playerAnimations} and {@code castingSlowdown} apply to the player path only. With
     * {@code playerAnimations} false, ISS's start and finish animations are skipped while every other client-side cast
     * effect still plays. With {@code castingSlowdown} false, ISS doesn't slow the caster's movement during the cast.
     */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel, SpellCastOptions options) {
        if (caster.level().isClientSide()) {
            return false;
        }
        return castOrQueue(caster, spell, spellLevel, null, options);
    }

    /**
     * Cast {@code spell} from {@code caster} aimed at {@code target}. The caster's reported facing is steered toward
     * {@code target} during the cast setup window, and the spell's own {@link TargetEntityCastData} (or
     * {@link TargetedTargetAreaCastData}) is overridden to point at {@code target}.
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target) {
        return castWithTarget(caster, spell, spellLevel, target, SpellCastOptions.DEFAULT);
    }

    /**
     * Bi-entity variant with a {@code castTime} override (player path only).
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target,
                                         Optional<Integer> castTime) {
        return castWithTarget(caster, spell, spellLevel, target, SpellCastOptions.withCastTime(castTime));
    }

    /**
     * Bi-entity variant with cast options (see {@link #castSelf(LivingEntity, AbstractSpell, int, SpellCastOptions)}).
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target,
                                         SpellCastOptions options) {
        if (caster.level().isClientSide()) {
            return false;
        }
        return castOrQueue(caster, spell, spellLevel, target == caster ? null : target, options);
    }

    private static boolean castOrQueue(LivingEntity caster, AbstractSpell spell, int spellLevel,
                                       @Nullable LivingEntity target, SpellCastOptions options) {
        if (options.delayTicks() > 0 || RESOLVING.contains(caster)) {
            PendingSpellCasts.add(caster, spell, spellLevel, target, options);
            return true;
        }
        return castNow(caster, spell, spellLevel, target, options);
    }

    static boolean castNow(LivingEntity caster, AbstractSpell spell, int spellLevel, @Nullable LivingEntity target,
                           SpellCastOptions options) {
        boolean wasCastingForAffix = castingForAffix;
        castingForAffix = true;
        try {
            return castInternal(caster, spell, spellLevel, target, options);
        } finally {
            castingForAffix = wasCastingForAffix;
        }
    }

    private static boolean castInternal(LivingEntity caster, AbstractSpell spell, int spellLevel,
                                        @Nullable LivingEntity target, SpellCastOptions options) {
        Level world = caster.level();
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean interrupted = false;
        if (magicData.isCasting()) {
            switch (options.interruptMode()) {
                case FAIL -> {
                    return false;
                }
                case CANCEL -> cancelCurrentCast(caster, world, magicData);
                case FORCE_COMPLETE -> {
                    if (magicData.getCastType() == CastType.CONTINUOUS) {
                        return false;
                    }
                    forceCompleteCurrentCast(caster, world, magicData);
                }
            }
            interrupted = true;
        }

        if (target != null) {
            BientitySpellCastAim.push(caster, target);
        }
        try {
            if (caster instanceof ServerPlayer serverPlayer) {
                return castForPlayer(serverPlayer, spell, spellLevel, magicData, target, options) || interrupted;
            } else if (caster instanceof IMagicEntity magicEntity) {
                // ISS mob casting; target / aim redirect aren't supported on this path.
                magicEntity.initiateCastSpell(spell, spellLevel);
                return true;
            } else if (spell.checkPreCastConditions(world, spellLevel, caster, magicData)) {
                maybeOverrideTargetData(target, magicData);
                spell.onCast(world, spellLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, spellLevel, caster, magicData, false);
                return true;
            }
            return interrupted;
        } finally {
            if (target != null) {
                BientitySpellCastAim.pop();
            }
        }
    }

    private static boolean castForPlayer(ServerPlayer serverPlayer, AbstractSpell spell, int spellLevel,
                                         MagicData magicData, @Nullable LivingEntity target, SpellCastOptions options) {
        Level world = serverPlayer.level();

        // Spells like RootSpell / HasteSpell install TargetEntityCastData here via Utils.preCastTargetHelper. The
        // bientity aim push redirects the raycast toward our target so the helper succeeds. Respect the return value:
        // if the spell aborts, so do we.
        if (!spell.checkPreCastConditions(world, spellLevel, serverPlayer, magicData)) {
            return false;
        }

        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        int effectiveCastTime;
        if (options.castTime().isPresent()) {
            effectiveCastTime = Math.max(0, options.castTime().get());
        } else if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(spellLevel, serverPlayer);
        } else {
            effectiveCastTime = 0;
        }

        magicData.initiateCast(spell, spellLevel, effectiveCastTime, CastSource.COMMAND, COMMAND_CASTING_SLOT);
        magicData.setPlayerCastingItem(ItemStack.EMPTY);
        AFFIX_CASTS.computeIfAbsent(serverPlayer, player -> new HashMap<>()).put(spell.getSpellId(), options);

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
        if (!options.castingSlowdown()) {
            PacketDistributor.sendToPlayer(serverPlayer, CastWithoutSlowdownPacket.INSTANCE);
        }
        CustomPacketPayload castStarted = options.playerAnimations()
                ? new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel)
                : new CastStartedWithoutAnimationPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, castStarted);

        if (effectiveCastTime == 0) {
            spell.castSpell(world, spellLevel, serverPlayer, CastSource.COMMAND, false);
            spell.onServerCastComplete(world, spellLevel, serverPlayer, magicData, false);
        }
        return true;
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
        if (caster instanceof ServerPlayer serverPlayer) {
            oldSpell.castSpell(world, oldLevel, serverPlayer, oldSource, true);
            if (oldSource == CastSource.SCROLL) {
                Scroll.attemptRemoveScrollAfterCast(serverPlayer);
            }
        } else {
            oldSpell.onCast(world, oldLevel, caster, oldSource, magicData);
        }
        oldSpell.onServerCastComplete(world, oldLevel, caster, magicData, false);
        magicData.resetCastingState();
    }

    private static void cancelCurrentCast(LivingEntity caster, Level world, MagicData magicData) {
        IronsApothic.LOGGER.debug("SpellCastUtil: cancelling in-progress cast {}", magicData.getCastingSpellId());
        if (caster instanceof ServerPlayer serverPlayer) {
            Utils.serverSideCancelCast(serverPlayer);
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.cancelCast();
        } else {
            magicData.getCastingSpell().getSpell()
                    .onServerCastComplete(world, magicData.getCastingSpellLevel(), caster, magicData, true);
        }
    }
}
