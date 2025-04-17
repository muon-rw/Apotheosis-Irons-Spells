package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
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
                    LootRarity.mapCodec(Codec.INT).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.validTypes)
            ).apply(inst, SpellLevelAffix::new));

    protected final SchoolType school;
    protected final Map<LootRarity, Integer> values;
    protected final Set<LootCategory> validTypes;

    public SpellLevelAffix(AffixDefinition definition, ResourceLocation schoolId, Map<LootRarity, Integer> values, Set<LootCategory> types) {
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
        Integer bonus = this.values.get(inst.rarity().get());
        if (bonus == null) return Component.empty();

        String schoolTranslationKey = "school." + school.getId().getNamespace() + "." + school.getId().getPath();

        return Component.translatable("affix.irons_apothic.spell_level.desc",
                Component.translatable(schoolTranslationKey),
                bonus);
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        return this.getDescription(inst, ctx);
    }

    public SchoolType getSchool() {
        return school;
    }

    public int getBonusLevel(LootRarity rarity, float level) {
        return this.values.getOrDefault(rarity, 0);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat.isNone()) return false;
        return (this.validTypes.isEmpty() || this.validTypes.contains(cat))
                && this.values.containsKey(rarity);
    }
}