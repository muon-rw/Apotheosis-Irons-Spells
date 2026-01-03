package dev.muon.irons_apothic.affix;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base class for affixes that filter by spell school.
 * Provides helper methods for checking school compatibility between affixes and gear.
 */
public abstract class SchoolFilteredAffix extends Affix {

    private static final String SPELL_POWER_SUFFIX = "_spell_power";

    protected SchoolFilteredAffix(AffixDefinition definition) {
        super(definition);
    }

    /**
     * Checks if the gear's schools match the filter requirements.
     * <ul>
     *   <li>If schools is Optional.empty(): not filtered, always returns true</li>
     *   <li>If schools is present but empty Set: matches gear with generic spell_power or no school-specific spell power</li>
     *   <li>If schools is present with values: matches gear that has at least one of those schools</li>
     * </ul>
     *
     * @param stack   The item stack to check
     * @param schools Optional set of allowed schools. Empty Optional = no filtering, empty Set = generic/none
     * @return true if the school filter requirement is met
     */
    protected static boolean matchesSchools(ItemStack stack, Optional<Set<SchoolType>> schools) {
        if (schools.isEmpty()) {
            return true;
        }

        Set<SchoolType> allowedSchools = schools.get();
        Set<SchoolType> gearSchools = new HashSet<>();
        boolean hasGenericSpellPower = false;

        // Check vanilla slot attributes
        ItemAttributeModifiers componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : componentModifiers.modifiers()) {
            Holder<Attribute> attributeHolder = entry.attribute();
            if (!attributeHolder.isBound()) continue;
            Attribute attr = attributeHolder.value();

            SchoolType school = getSchoolFromAttribute(attr);
            if (school != null) {
                gearSchools.add(school);
            } else if (isGenericSpellPower(attr)) {
                hasGenericSpellPower = true;
            }
        }

        // Check Curios attributes
        if (stack.getItem() instanceof ICurioItem curio) {
            var slotTypes = CuriosApi.getItemStackSlots(stack, false);
            for (String slotId : slotTypes.keySet()) {
                SlotContext context = new SlotContext(slotId, null, -1, false, true);
                ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("irons_apothic", "school_check");
                var attributes = curio.getAttributeModifiers(context, modifierId, stack);

                for (Holder<Attribute> attributeHolder : attributes.keySet()) {
                    if (!attributeHolder.isBound()) continue;
                    Attribute attr = attributeHolder.value();

                    SchoolType school = getSchoolFromAttribute(attr);
                    if (school != null) {
                        gearSchools.add(school);
                    } else if (isGenericSpellPower(attr)) {
                        hasGenericSpellPower = true;
                    }
                }
            }
        }

        // Check upgrade data
        UpgradeData upgradeData = UpgradeData.getUpgradeData(stack);
        for (Holder<UpgradeOrbType> upgradeHolder : upgradeData.upgrades().keySet()) {
            if (!upgradeHolder.isBound()) continue;

            UpgradeOrbType upgradeOrb = upgradeHolder.value();
            Holder<Attribute> attributeHolder = upgradeOrb.attribute();
            if (!attributeHolder.isBound()) continue;
            Attribute attr = attributeHolder.value();

            SchoolType school = getSchoolFromAttribute(attr);
            if (school != null) {
                gearSchools.add(school);
            } else if (isGenericSpellPower(attr)) {
                hasGenericSpellPower = true;
            }
        }

        // Apply filter logic
        if (allowedSchools.isEmpty()) {
            // Empty set = "generic" - matches gear with generic spell_power or no school-specific power
            return hasGenericSpellPower || gearSchools.isEmpty();
        } else {
            // Gear must contain at least one of the allowed schools
            for (SchoolType gearSchool : gearSchools) {
                if (allowedSchools.contains(gearSchool)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the attribute is generic spell_power (not school-specific).
     */
    private static boolean isGenericSpellPower(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        return attrId != null && attrId.getPath().equals("spell_power");
    }

    /**
     * Extracts school from a school-specific spell power attribute (e.g., fire_spell_power → fire).
     * Returns null for generic spell_power or non-spell-power attributes.
     */
    private static SchoolType getSchoolFromAttribute(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (attrId == null) return null;

        String path = attrId.getPath();
        if (path.endsWith(SPELL_POWER_SUFFIX) && path.length() > SPELL_POWER_SUFFIX.length()) {
            String schoolName = path.substring(0, path.length() - SPELL_POWER_SUFFIX.length());
            ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(attrId.getNamespace(), schoolName);
            return SchoolRegistry.REGISTRY.get(schoolResource);
        }
        return null;
    }
}

