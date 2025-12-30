package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpellLevelAffix extends SchoolFilteredAffix {
    // Codec that supports both single "school" (backward compat) and "schools" array
    public static final Codec<SpellLevelAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Affix.affixDef(),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> 
                        a.schoolIds.filter(list -> list.size() == 1).map(list -> list.get(0))),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("schools").forGetter(a -> a.schoolIds),
                    LootRarity.mapCodec(LevelData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes)
            ).apply(inst, (def, singleSchool, schoolsArray, values, types) -> {
                // Prefer "schools" array if present, otherwise use single "school"
                Optional<List<ResourceLocation>> schoolIds = schoolsArray.isPresent() 
                    ? schoolsArray 
                    : singleSchool.map(List::of);
                return new SpellLevelAffix(def, schoolIds, values, types);
            }));

    protected final Optional<List<ResourceLocation>> schoolIds;
    protected final Optional<Set<SchoolType>> schools;
    protected final Map<LootRarity, LevelData> values;
    protected final Set<LootCategory> validTypes;

    public SpellLevelAffix(AffixDefinition definition, Optional<List<ResourceLocation>> schoolIds, Map<LootRarity, LevelData> values, Set<LootCategory> types) {
        super(definition);
        this.schoolIds = schoolIds;
        this.schools = schoolIds.map(ids -> {
            Set<SchoolType> schoolSet = new HashSet<>();
            for (ResourceLocation id : ids) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) {
                    schoolSet.add(school);
                } else {
                    Apotheosis.LOGGER.warn("Unknown school ID {} provided for SpellLevelAffix, ignoring.", id);
                }
            }
            return schoolSet;
        });
        this.values = values;
        this.validTypes = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        LevelData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        int bonus = data.level().getInt(inst.level());
        Component schoolComponent = getSchoolComponent();
        String key = bonus == 1 
                ? "affix.irons_apothic.spell_level.desc.singular" 
                : "affix.irons_apothic.spell_level.desc";

        return Component.translatable(key, schoolComponent, bonus);
    }

    private Component getSchoolComponent() {
        if (schools.isEmpty()) {
            return Component.translatable("affix.irons_apothic.school.generic");
        }
        
        Set<SchoolType> schoolSet = schools.get();
        if (schoolSet.isEmpty()) {
            return Component.translatable("affix.irons_apothic.school.none");
        }
        
        SchoolType[] arr = schoolSet.toArray(new SchoolType[0]);
        
        if (arr.length == 1) {
            return formatSchool(arr[0]);
        }
        
        if (arr.length == 2) {
            // "Fire and Ice"
            return Component.empty()
                    .append(formatSchool(arr[0]))
                    .append(Component.literal(" and "))
                    .append(formatSchool(arr[1]));
        }
        
        if (arr.length == 3) {
            // "Fire, Ice, and Holy"
            return Component.empty()
                    .append(formatSchool(arr[0]))
                    .append(Component.literal(", "))
                    .append(formatSchool(arr[1]))
                    .append(Component.literal(", and "))
                    .append(formatSchool(arr[2]));
        }
        
        // 4 schools: "Fire, Ice, Holy, and Nature"
        return Component.empty()
                .append(formatSchool(arr[0]))
                .append(Component.literal(", "))
                .append(formatSchool(arr[1]))
                .append(Component.literal(", "))
                .append(formatSchool(arr[2]))
                .append(Component.literal(", and "))
                .append(formatSchool(arr[3]));
    }

    private static Component formatSchool(SchoolType school) {
        String key = "school." + school.getId().getNamespace() + "." + school.getId().getPath();
        return Component.translatable(key).withStyle(school.getDisplayName().getStyle());
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        LevelData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        int currentBonus = data.level().getInt(inst.level());
        int minBonus = data.level().getInt(0);
        int maxBonus = data.level().getInt(1);

        Component schoolComponent = getSchoolComponent();
        String key = currentBonus == 1 
                ? "affix.irons_apothic.spell_level.desc.singular" 
                : "affix.irons_apothic.spell_level.desc";
        MutableComponent comp = Component.translatable(key, schoolComponent, currentBonus);

        // Add min/max bounds if they differ
        if (minBonus != maxBonus) {
            Component minComp = Component.literal(String.valueOf(minBonus));
            Component maxComp = Component.literal(String.valueOf(maxBonus));
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        return comp;
    }

    public Optional<Set<SchoolType>> getSchools() {
        return schools;
    }

    public int getBonusLevel(LootRarity rarity, float level) {
        LevelData data = this.values.get(rarity);
        return data != null ? data.level().getInt(level) : 0;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat.isNone() || !this.values.containsKey(rarity)) {
            return false;
        }
        if (!this.validTypes.isEmpty() && !this.validTypes.contains(cat)) {
            return false;
        }

        return matchesSchools(stack, this.schools);
    }

    public record LevelData(StepFunction level) {
        private static final Codec<LevelData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(LevelData::level)
                ).apply(inst, LevelData::new));
    }
}