package dev.muon.irons_apothic.affix;

import dev.muon.irons_apothic.util.GearSchools;
import dev.muon.irons_apothic.util.SpellCastUtil;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

public abstract class SchoolFilteredAffix extends Affix {

    public static boolean isCurrentlyTriggering() {
        return SpellCastUtil.isCastingForAffix();
    }

    protected SchoolFilteredAffix(AffixDefinition definition) {
        super(definition);
    }

    /**
     * An absent filter matches all gear. An empty set matches gear with generic spell power, or with no
     * school-specific spell power but some other non-school magic attribute such as mana or cooldown reduction.
     * Otherwise the gear must have at least one of the given schools.
     */
    protected static boolean matchesSchools(ItemStack stack, Optional<Set<SchoolType>> schools) {
        if (schools.isEmpty()) {
            return true;
        }

        Set<SchoolType> allowedSchools = schools.get();
        GearSchools gear = GearSchools.of(stack);
        if (allowedSchools.isEmpty()) {
            return gear.hasGenericSpellPower() || gear.schools().isEmpty() && gear.hasSchoollessMagicAttribute();
        }
        return gear.schools().stream().anyMatch(allowedSchools::contains);
    }

}

