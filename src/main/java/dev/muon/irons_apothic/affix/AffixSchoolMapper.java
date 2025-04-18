package dev.muon.irons_apothic.affix;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AffixSchoolMapper {

    public static Set<SchoolType> getSpellSchoolsFromGear(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        ItemAttributeModifiers componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> allModifierEntries = componentModifiers.modifiers();

        for (ItemAttributeModifiers.Entry entry : allModifierEntries) {
            Holder<Attribute> attributeHolder = entry.attribute();

            if (!attributeHolder.isBound()) continue;
            Attribute attribute = attributeHolder.value();
            ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);

            if (attrId != null && attrId.getPath().endsWith("_spell_power")) {
                String schoolName = attrId.getPath().replace("_spell_power", "");
                ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(attrId.getNamespace(), schoolName);
                SchoolType school = SchoolRegistry.REGISTRY.get(schoolResource);
                if (school != null) {
                    schools.add(school);
                }
            }
        }
        return schools;
    }
}