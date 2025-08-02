package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.category.LootCategories;
import dev.shadowsoffire.apotheosis.Apoth;
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

import java.util.Map;
import java.util.Set;

public class ManaCostAffix extends Affix {
    public static final Codec<ManaCostAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Affix.affixDef(),
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()),
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories)
            ).apply(inst, ManaCostAffix::new));

    protected final SchoolType school;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> categories;

    public ManaCostAffix(AffixDefinition definition, ResourceLocation schoolId, Map<LootRarity, StepFunction> values, Set<LootCategory> categories) {
        super(definition);
        this.school = SchoolRegistry.getSchool(schoolId);
        if (this.school == null) {
            throw new IllegalArgumentException("Invalid school ID provided for ManaCostAffix: " + schoolId);
        }
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

        // Check if the gear has the matching school
        Set<SchoolType> gearSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack);
        return gearSchools.contains(this.school);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        float reduction = this.getReductionPercent(inst.getRarity(), inst.level());
        
        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();
        
        return Component.translatable("affix.irons_apothic.mana_cost.desc",
                Component.translatable(schoolTranslationKey).withStyle(school.getDisplayName().getStyle()),
                fmt(reduction * 100));
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

    public SchoolType getSchool() {
        return school;
    }

    public float getReductionPercent(LootRarity rarity, float level) {
        StepFunction func = this.values.get(rarity);
        return func != null ? func.get(level) : 0f;
    }
} 