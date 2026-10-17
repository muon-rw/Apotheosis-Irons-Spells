package dev.muon.irons_apothic.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PendingSpellCasts {

    // Server thread only. Static state outlives an integrated server, so we clear it when the server stops.
    private static final List<PendingCast> PENDING = new ArrayList<>();

    private PendingSpellCasts() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PendingSpellCasts::castDue);
        NeoForge.EVENT_BUS.addListener(PendingSpellCasts::clear);
    }

    static void add(LivingEntity caster, AbstractSpell spell, int spellLevel, @Nullable LivingEntity target,
                    SpellCastOptions options) {
        int dueTick = ((ServerLevel) caster.level()).getServer().getTickCount() + options.delayTicks();
        PENDING.add(new PendingCast(caster, spell, spellLevel, target, options, dueTick));
    }

    // Runs after every level has ticked, so a cast queued mid-resolution with no delay still starts this tick,
    // after ISS has finished cleaning up the spell that triggered it.
    private static void castDue(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        int tick = event.getServer().getTickCount();
        List<PendingCast> due = new ArrayList<>();
        // Collect first: a cast can queue another one, which would break an open iterator.
        PENDING.removeIf(cast -> {
            if (!cast.caster.isAlive()) {
                return true;
            }
            if (cast.dueTick <= tick) {
                due.add(cast);
                return true;
            }
            return false;
        });
        for (PendingCast cast : due) {
            if (cast.caster.isAlive()) {
                SpellCastUtil.castNow(cast.caster, cast.spell, cast.spellLevel, cast.presentTarget(), cast.options);
            }
        }
    }

    private static void clear(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private record PendingCast(LivingEntity caster, AbstractSpell spell, int spellLevel, @Nullable LivingEntity target,
                               SpellCastOptions options, int dueTick) {

        // A target that died or left the caster's level falls back to the spell's standard targeting.
        @Nullable
        LivingEntity presentTarget() {
            return this.target != null && this.target.isAlive() && this.target.level() == this.caster.level()
                    ? this.target
                    : null;
        }
    }
}
