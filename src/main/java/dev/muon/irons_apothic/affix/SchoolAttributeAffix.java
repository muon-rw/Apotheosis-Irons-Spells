package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SchoolAttributeAffix extends AttributeAffix {

    // Codec that supports both single "school" (backward compat) and "schools" array
    public static final Codec<SchoolAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(a -> a.attribute),
                    PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> 
                        a.schoolIds.filter(list -> list.size() == 1).map(list -> list.get(0))),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("schools").forGetter(a -> a.schoolIds)
            ).apply(inst, (def, attr, op, values, categories, singleSchool, schoolsArray) -> {
                // Prefer "schools" array if present, otherwise use single "school"
                Optional<List<ResourceLocation>> schoolIds = schoolsArray.isPresent() 
                    ? schoolsArray 
                    : singleSchool.map(List::of);
                return new SchoolAttributeAffix(def, attr, op, values, categories, schoolIds);
            }));

    protected final Optional<List<ResourceLocation>> schoolIds;
    protected final Optional<Set<SchoolType>> schools;

    public SchoolAttributeAffix(AffixDefinition def, Holder<Attribute> attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories, Optional<List<ResourceLocation>> schoolIds) {
        super(def, attr, op, values, categories);
        this.schoolIds = schoolIds;
        this.schools = schoolIds.map(ids -> {
            Set<SchoolType> schoolSet = new HashSet<>();
            for (ResourceLocation id : ids) {
                SchoolType s = SchoolRegistry.REGISTRY.get(id);
                if (s != null) {
                    schoolSet.add(s);
                } else {
                    Apotheosis.LOGGER.warn("Unknown school ID {} provided for SchoolAttributeAffix, ignoring.", id);
                }
            }
            return schoolSet;
        });
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!super.canApplyTo(stack, cat, rarity)) {
            return false;
        }
        if (cat == null || cat.isNone()) {
             return false; 
        }

        return SchoolFilteredAffix.matchesSchools(stack, this.schools);
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
} 