package dev.muon.irons_apothic.attribute;

import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

public class AttributeRegistry {
    private static final Map<SchoolType, Holder<Attribute>> SCHOOL_LEVEL_ATTRIBUTES = new HashMap<>();

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(AttributeRegistry::registerAttributes);
        modEventBus.addListener(AttributeRegistry::setAttributes);
    }

    private static void registerAttributes(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.ATTRIBUTE.key())) {
            SchoolRegistry.REGISTRY.forEach(school -> {
                String schoolPath = school.getId().getPath();
                ResourceLocation attributeId = IronsApothic.loc(schoolPath + "_spell_level");
                Attribute attribute = new RangedAttribute(
                        "attribute.irons_apothic." + schoolPath + "_spell_level",
                        0.0, 0.0, 5.0).setSyncable(true);

                event.register(BuiltInRegistries.ATTRIBUTE.key(), attributeId, () -> attribute);

                BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(BuiltInRegistries.ATTRIBUTE.key(), attributeId))
                        .ifPresent(holder -> SCHOOL_LEVEL_ATTRIBUTES.put(school, holder));
            });
        }
    }

    public static Holder<Attribute> getSchoolLevelAttribute(SchoolType school) {
        return SCHOOL_LEVEL_ATTRIBUTES.get(school);
    }

    @SubscribeEvent
    public static void setAttributes(EntityAttributeModificationEvent event) {
        SCHOOL_LEVEL_ATTRIBUTES.values().forEach(attributeHolder -> {
            if (attributeHolder != null && attributeHolder.isBound()) {
                event.add(EntityType.PLAYER, attributeHolder);
            } else {
                IronsApothic.LOGGER.warn("Attempted to add an unbound or null attribute holder to Player during EntityAttributeModificationEvent.");
            }
        });
    }
}