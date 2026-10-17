package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.network.CastStartedWithoutAnimationPacket;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side cast pipeline for affix-triggered Iron's Spellbooks casts (CastSource.COMMAND).
 * <ul>
 *   <li>{@link #castSelf}: caster-as-target or no target (look-direction projectiles, self-AoE).</li>
 *   <li>{@link #castWithTarget}: aim is redirected to {@code target} via {@link BientitySpellCastAim} during
 *       {@code checkPreCastConditions}, {@code onServerPreCast} and {@code onCast}, and any
 *       {@link TargetEntityCastData} or {@link TargetedTargetAreaCastData} the spell installs is overridden to point
 *       at {@code target}.</li>
 * </ul>
 * Affix triggers interrupt any in-progress cast by force-completing it, with two exceptions: a CONTINUOUS cast keeps
 * running and the trigger is skipped, and a spell that is resolving right now (the trigger came from its own damage
 * or healing) is not completed a second time. Both entry points return false when the trigger had no effect: it was
 * skipped, or nothing was interrupted and the spell's {@code checkPreCastConditions} failed.
 */
public final class SpellCastUtil {

    // ISS uses this slot id for /cast-style casts in MagicData.initiateCast and UpdateCastingStatePacket.
    private static final String COMMAND_CASTING_SLOT = "command";

    // Server thread only: AbstractSpell#castSpell takes a ServerPlayer.
    private static final Set<LivingEntity> RESOLVING = Collections.newSetFromMap(new IdentityHashMap<>());

    // Server thread only. Maps each caster to the spell ids of their in-progress casts that should finish without a
    // player animation. A set, because an affix cast can start and finish while another one is resolving. Weak keys
    // drop a disconnected player's entry; vanilla gives a respawned player the old entity id, so it keeps the entry.
    private static final Map<ServerPlayer, Set<String>> CASTS_WITHOUT_ANIMATION = new WeakHashMap<>();

    private SpellCastUtil() {
    }

    /** Wraps every {@code AbstractSpell#castSpell} (see {@code AbstractSpellMixin}). */
    public static void resolve(ServerPlayer caster, Runnable castSpell) {
        boolean outermost = RESOLVING.add(caster);
        try {
            castSpell.run();
        } finally {
            if (outermost) {
                RESOLVING.remove(caster);
            }
        }
    }

    /** Called from {@code AbstractSpellMixin} when {@code spell} finishes or is cancelled, and before animated casts. */
    public static boolean consumeCastWithoutAnimation(ServerPlayer caster, AbstractSpell spell) {
        Set<String> spellIds = CASTS_WITHOUT_ANIMATION.get(caster);
        if (spellIds == null || !spellIds.remove(spell.getSpellId())) {
            return false;
        }
        if (spellIds.isEmpty()) {
            CASTS_WITHOUT_ANIMATION.remove(caster);
        }
        return true;
    }

    /** No target / caster-as-target. Skip aim redirect and any target injection. */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel) {
        return castSelf(caster, spell, spellLevel, Optional.empty(), true);
    }

    /**
     * Self-cast variant with a {@code castTime} override (player path only). When present, replaces the default
     * {@code 0} for INSTANT/LONG and the spell-derived value for CONTINUOUS.
     */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel, Optional<Integer> castTime) {
        return castSelf(caster, spell, spellLevel, castTime, true);
    }

    /**
     * With {@code playerAnimations} false, ISS's start and finish animations are skipped while every other client-side
     * cast effect still plays (player path only).
     */
    public static boolean castSelf(LivingEntity caster, AbstractSpell spell, int spellLevel, Optional<Integer> castTime,
                                   boolean playerAnimations) {
        if (caster.level().isClientSide()) {
            return false;
        }
        return castInternal(caster, spell, spellLevel, null, castTime, playerAnimations);
    }

    /**
     * Cast {@code spell} from {@code caster} aimed at {@code target}. The caster's reported facing is steered toward
     * {@code target} during the cast setup window, and the spell's own {@link TargetEntityCastData} (or
     * {@link TargetedTargetAreaCastData}) is overridden to point at {@code target}.
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target) {
        return castWithTarget(caster, spell, spellLevel, target, Optional.empty(), true);
    }

    /**
     * Bi-entity variant with a {@code castTime} override (player path only).
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target,
                                         Optional<Integer> castTime) {
        return castWithTarget(caster, spell, spellLevel, target, castTime, true);
    }

    /**
     * Bi-entity variant with {@code castTime} and {@code playerAnimations} overrides (player path only).
     */
    public static boolean castWithTarget(LivingEntity caster, AbstractSpell spell, int spellLevel, LivingEntity target,
                                         Optional<Integer> castTime, boolean playerAnimations) {
        if (caster.level().isClientSide()) {
            return false;
        }
        return castInternal(caster, spell, spellLevel, target == caster ? null : target, castTime, playerAnimations);
    }

    private static boolean castInternal(LivingEntity caster, AbstractSpell spell, int spellLevel,
                                        @Nullable LivingEntity target, Optional<Integer> castTime,
                                        boolean playerAnimations) {
        Level world = caster.level();
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean interrupted = false;
        if (magicData.isCasting()) {
            if (magicData.getCastType() == CastType.CONTINUOUS) {
                return false;
            }
            if (!RESOLVING.contains(caster)) {
                forceCompleteCurrentCast(caster, world, magicData);
                interrupted = true;
            }
        }

        if (target != null) {
            BientitySpellCastAim.push(caster, target);
        }
        try {
            if (caster instanceof ServerPlayer serverPlayer) {
                return castForPlayer(serverPlayer, spell, spellLevel, magicData, target, castTime, playerAnimations)
                        || interrupted;
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
                                         MagicData magicData, @Nullable LivingEntity target, Optional<Integer> castTime,
                                         boolean playerAnimations) {
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
        if (playerAnimations) {
            // Drops a stale marker from an earlier affix cast of this spell that was wiped out before it could finish
            consumeCastWithoutAnimation(serverPlayer, spell);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                    new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel));
        } else {
            CASTS_WITHOUT_ANIMATION.computeIfAbsent(serverPlayer, player -> new HashSet<>()).add(spell.getSpellId());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                    new CastStartedWithoutAnimationPacket(serverPlayer.getUUID(), spell.getSpellId(), spellLevel));
        }

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
}
