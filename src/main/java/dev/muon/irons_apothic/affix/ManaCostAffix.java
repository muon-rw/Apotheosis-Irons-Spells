package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.category.LootCategories;
import dev.shadowsoffire.apotheosis.Apoth;
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

public class ManaCostAffix extends SchoolFilteredAffix {
    // Codec that supports both single "school" (backward compat) and "schools" array
    public static final Codec<ManaCostAffix> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Affix.affixDef(),
            ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> 
                a.schoolIds.filter(list -> list.size() == 1).map(list -> list.get(0))),
            ResourceLocation.CODEC.listOf().optionalFieldOf("schools").forGetter(a -> a.schoolIds),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories)
    ).apply(inst, (def, singleSchool, schoolsArray, values, categories) -> {
        // Prefer "schools" array if present, otherwise use single "school"
        Optional<List<ResourceLocation>> schoolIds = schoolsArray.isPresent() 
            ? schoolsArray 
            : singleSchool.map(List::of);
        return new ManaCostAffix(def, schoolIds, values, categories);
    }));

    protected final Optional<List<ResourceLocation>> schoolIds;
    protected final Optional<Set<SchoolType>> schools;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> categories;

    public ManaCostAffix(AffixDefinition definition, Optional<List<ResourceLocation>> schoolIds, Map<LootRarity, StepFunction> values, Set<LootCategory> categories) {
        super(definition);
        this.schoolIds = schoolIds;
        this.schools = schoolIds.map(ids -> {
            Set<SchoolType> schoolSet = new HashSet<>();
            for (ResourceLocation id : ids) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) {
                    schoolSet.add(school);
                } else {
                    Apotheosis.LOGGER.warn("Unknown school ID {} provided for ManaCostAffix, ignoring.", id);
                }
            }
            return schoolSet;
        });
        this.values = values;
        this.categories = categories;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat != LootCategories.STAFF && cat != Apoth.LootCategories.MELEE_WEAPON  && cat != LootCategories.SPELLBOOK) {
            return false;
        }
        
        if (!this.values.containsKey(rarity)) {
            return false;
        }
        
        if (!this.categories.isEmpty() && !this.categories.contains(cat)) {
            return false;
        }

        // Check if the gear matches the school filter
        return matchesSchools(stack, this.schools);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        float reduction = this.getReductionPercent(inst.getRarity(), inst.level());
        
        Component schoolComponent = getSchoolComponent();
        
        return Component.translatable("affix.irons_apothic.mana_cost.desc",
                schoolComponent,
                fmt(reduction * 100));
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
        MutableComponent comp = this.getDescription(inst, ctx);

        float minReduction = this.getReductionPercent(inst.getRarity(), 0);
        float maxReduction = this.getReductionPercent(inst.getRarity(), 1);
        
        Component minComp = Component.translatable("%s%%", fmt(minReduction * 100));
        Component maxComp = Component.translatable("%s%%", fmt(maxReduction * 100));
        
        return comp.append(valueBounds(minComp, maxComp));
    }

    public Optional<Set<SchoolType>> getSchools() {
        return schools;
    }

    public float getReductionPercent(LootRarity rarity, float level) {
        StepFunction func = this.values.get(rarity);
        return func != null ? func.get(level) : 0f;
    }
} 