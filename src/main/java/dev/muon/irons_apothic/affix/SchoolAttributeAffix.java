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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SchoolAttributeAffix extends AttributeAffix {

    public static final Codec<SchoolAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(a -> a.attribute),
                    PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> a.schoolId)
            ).apply(inst, SchoolAttributeAffix::new));

    protected final Optional<ResourceLocation> schoolId;
    protected final Optional<SchoolType> school;

    public SchoolAttributeAffix(AffixDefinition def, Holder<Attribute> attr, Operation op, Map<LootRarity, StepFunction> values, Set<LootCategory> categories, Optional<ResourceLocation> schoolId) {
        super(def, attr, op, values, categories);
        this.schoolId = schoolId;
        this.school = schoolId.flatMap(id -> {
            SchoolType s = SchoolRegistry.REGISTRY.get(id);
            if (s == null) {
                Apotheosis.LOGGER.warn("Unknown school ID {} provided for SchoolAttributeAffix, affix may not apply correctly until school is registered.", id);
                return Optional.empty();
            }
            return Optional.of(s);
        });
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!super.canApplyTo(stack, cat, rarity)) {
            return false;
        }
        if (!cat.isNone()) {
            Set<SchoolType> gearSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack);

            if (this.school.isPresent()) {
                SchoolType requiredSchool = this.school.get();
                return gearSchools.contains(requiredSchool);
            } else {
                return gearSchools.isEmpty();
            }
        }
        return false; 
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
} 