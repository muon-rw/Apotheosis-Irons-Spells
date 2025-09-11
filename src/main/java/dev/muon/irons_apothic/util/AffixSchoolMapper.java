package dev.muon.irons_apothic.util;

import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AffixSchoolMapper {

    public static Set<SchoolType> getSpellSchoolsFromGear(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        
        schools.addAll(getVanillaSlotAttributes(stack));
        schools.addAll(getCurioAttributes(stack));
        schools.addAll(getUpgradeDataAttributes(stack));
        return schools;
    }
    
    private static Set<SchoolType> getVanillaSlotAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();

        ItemAttributeModifiers componentModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> allModifierEntries = componentModifiers.modifiers();

        for (ItemAttributeModifiers.Entry entry : allModifierEntries) {
            Holder<Attribute> attributeHolder = entry.attribute();
            if (!attributeHolder.isBound()) continue;

            Attribute attribute = attributeHolder.value();
            SchoolType school = getSchoolFromAttribute(attribute);
            if (school != null) {
                schools.add(school);
            }
        }

        return schools;
    }

    private static Set<SchoolType> getUpgradeDataAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        UpgradeData upgradeData = UpgradeData.getUpgradeData(stack);
        
        for (Holder<UpgradeOrbType> upgradeHolder : upgradeData.upgrades().keySet()) {
            if (!upgradeHolder.isBound()) continue;
            
            UpgradeOrbType upgradeOrb = upgradeHolder.value();
            Holder<Attribute> attributeHolder = upgradeOrb.attribute();
            
            if (!attributeHolder.isBound()) continue;
            
            Attribute attribute = attributeHolder.value();
            SchoolType school = getSchoolFromAttribute(attribute);
            if (school != null) {
                schools.add(school);
            }
        }
        return schools;
    }

    private static Set<SchoolType> getCurioAttributes(ItemStack stack) {
        Set<SchoolType> schools = new HashSet<>();
        
        if (!(stack.getItem() instanceof ICurioItem curio)) {
            return schools;
        }

        var slotTypes = CuriosApi.getItemStackSlots(stack, false);
        
        for (String slotId : slotTypes.keySet()) {
            SlotContext context = new SlotContext(slotId, null, -1, false, true);
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("irons_apothic", "school_check");
            Multimap<Holder<Attribute>, AttributeModifier> attributes = curio.getAttributeModifiers(context, modifierId, stack);
            
            for (Holder<Attribute> attributeHolder : attributes.keySet()) {
                if (!attributeHolder.isBound()) continue;

                Attribute attribute = attributeHolder.value();
                SchoolType school = getSchoolFromAttribute(attribute);
                if (school != null) {
                    schools.add(school);
                }
            }
        }
        
        return schools;
    }
    
    private static SchoolType getSchoolFromAttribute(Attribute attribute) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        
        if (attrId != null && attrId.getPath().endsWith("_spell_power")) {
            String schoolName = attrId.getPath().replace("_spell_power", "");
            ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(attrId.getNamespace(), schoolName);
            return SchoolRegistry.REGISTRY.get(schoolResource);
        }
        
        return null;
    }
}