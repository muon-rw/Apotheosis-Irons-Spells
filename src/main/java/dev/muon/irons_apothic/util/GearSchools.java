package dev.muon.irons_apothic.util;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.mixin.SchoolTypeAccessor;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record GearSchools(Set<SchoolType> schools, boolean hasGenericSpellPower) {
    private static final String SPELL_POWER_SUFFIX = "_spell_power";
    private static final ResourceLocation CURIO_MODIFIER_ID = IronsApothic.loc("school_check");

    public static GearSchools of(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        boolean hasGenericSpellPower = false;
        for (Holder<Attribute> holder : collectAttributes(stack)) {
            if (!holder.isBound()) continue;
            Attribute attr = holder.value();

            SchoolType school = getSchoolFromAttribute(attr);
            if (school != null) {
                schools.add(school);
            } else if (isGenericSpellPower(attr)) {
                hasGenericSpellPower = true;
            }
        }
        return new GearSchools(schools, hasGenericSpellPower);
    }

    private static List<Holder<Attribute>> collectAttributes(ItemStack stack) {
        List<Holder<Attribute>> attributes = new ArrayList<>();

        ItemAttributeModifiers componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : componentModifiers.modifiers()) {
            attributes.add(entry.attribute());
        }

        if (stack.getItem() instanceof ICurioItem curio) {
            for (String slotId : CuriosApi.getItemStackSlots(stack, false).keySet()) {
                SlotContext context = new SlotContext(slotId, null, -1, false, true);
                attributes.addAll(curio.getAttributeModifiers(context, CURIO_MODIFIER_ID, stack).keySet());
            }
        }

        for (Holder<UpgradeOrbType> upgradeHolder : UpgradeData.getUpgradeData(stack).upgrades().keySet()) {
            if (upgradeHolder.isBound()) {
                attributes.add(upgradeHolder.value().attribute());
            }
        }
        return attributes;
    }

    private static boolean isGenericSpellPower(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        return attrId != null && attrId.getPath().equals("spell_power");
    }

    /**
     * Returns null for generic spell_power and non-spell-power attributes. Falls back to a reverse registry
     * lookup for schools whose power attribute path differs from the school path, e.g. {@code ess_requiem:blade}
     * uses {@code ess_requiem:spellblade_spell_power}.
     */
    private static SchoolType getSchoolFromAttribute(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if (attrId == null) return null;

        String path = attrId.getPath();
        if (!path.endsWith(SPELL_POWER_SUFFIX) || path.length() <= SPELL_POWER_SUFFIX.length()) {
            return null;
        }

        String schoolName = path.substring(0, path.length() - SPELL_POWER_SUFFIX.length());
        ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(attrId.getNamespace(), schoolName);
        SchoolType school = SchoolRegistry.REGISTRY.get(schoolResource);
        if (school != null) return school;

        for (SchoolType candidate : SchoolRegistry.REGISTRY) {
            Holder<Attribute> powerHolder = ((SchoolTypeAccessor) (Object) candidate).irons_apothic$getPowerAttribute();
            if (powerHolder != null && powerHolder.isBound() && powerHolder.value() == attribute) {
                return candidate;
            }
        }
        return null;
    }
}
