package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.util.AffixSchoolMapper;
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

public class SpellLevelAffix extends Affix {
    public static final Codec<SpellLevelAffix> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Affix.affixDef(),
                    ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.school.getId()),
                    LootRarity.mapCodec(LevelData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes)
            ).apply(inst, SpellLevelAffix::new));

    protected final SchoolType school;
    protected final Map<LootRarity, LevelData> values;
    protected final Set<LootCategory> validTypes;

    public SpellLevelAffix(AffixDefinition definition, ResourceLocation schoolId, Map<LootRarity, LevelData> values, Set<LootCategory> types) {
        super(definition);
        this.school = SchoolRegistry.getSchool(schoolId);
        if (this.school == null) {
            throw new IllegalArgumentException("Invalid school ID provided for SpellLevelAffix: " + schoolId);
        }
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

        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();

        return Component.translatable("affix.irons_apothic.spell_level.desc",
                Component.translatable(schoolTranslationKey).withStyle(school.getDisplayName().getStyle()),
                bonus);
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        LevelData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        int currentBonus = data.level().getInt(inst.level());
        int minBonus = data.level().getInt(0);
        int maxBonus = data.level().getInt(1);

        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();
        MutableComponent comp = Component.translatable("affix.irons_apothic.spell_level.desc",
                Component.translatable(schoolTranslationKey).withStyle(school.getDisplayName().getStyle()),
                currentBonus);

        // Add min/max bounds if they differ
        if (minBonus != maxBonus) {
            Component minComp = Component.literal(String.valueOf(minBonus));
            Component maxComp = Component.literal(String.valueOf(maxBonus));
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        return comp;
    }

    public SchoolType getSchool() {
        return school;
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

        Set<SchoolType> gearSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack);

        return gearSchools.contains(this.school);
    }

    public record LevelData(StepFunction level) {
        private static final Codec<LevelData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(LevelData::level)
                ).apply(inst, LevelData::new));
    }
}