package dev.muon.irons_apothic.affix;

import dev.muon.irons_apothic.util.GearSchools;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

public abstract class SchoolFilteredAffix extends Affix {

    /**
     * Shared re-entrancy guard for affix-driven spell casts. Set by both {@link SpellTriggerAffix} and
     * {@link ImbuedSpellTriggerAffix} during {@code triggerSpell} so a cast triggered by one cannot recursively
     * trigger the other (or itself) via downstream {@code SpellDamageEvent} / {@code SpellHealEvent} fan-out.
     */
    protected static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    public static boolean isCurrentlyTriggering() {
        return IS_TRIGGERING.get();
    }

    protected SchoolFilteredAffix(AffixDefinition definition) {
        super(definition);
    }

    /**
     * An absent filter matches all gear. An empty set matches gear with generic spell power or no school-specific
     * spell power. Otherwise the gear must have at least one of the given schools.
     */
    protected static boolean matchesSchools(ItemStack stack, Optional<Set<SchoolType>> schools) {
        if (schools.isEmpty()) {
            return true;
        }

        Set<SchoolType> allowedSchools = schools.get();
        GearSchools gear = GearSchools.of(stack);
        if (allowedSchools.isEmpty()) {
            return gear.hasGenericSpellPower() || gear.schools().isEmpty();
        }
        return gear.schools().stream().anyMatch(allowedSchools::contains);
    }

}

