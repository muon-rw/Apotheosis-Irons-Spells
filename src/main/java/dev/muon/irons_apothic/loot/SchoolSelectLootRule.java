package dev.muon.irons_apothic.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.util.GearSchools;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.LootRule;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Runs {@code if_true} on gear with school-specific spell power, otherwise the optional {@code if_false}.
 */
public record SchoolSelectLootRule(LootRule ifTrue, Optional<LootRule> ifFalse) implements LootRule {

    public static final Codec<SchoolSelectLootRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LootRule.CODEC.fieldOf("if_true").forGetter(SchoolSelectLootRule::ifTrue),
            LootRule.CODEC.optionalFieldOf("if_false").forGetter(SchoolSelectLootRule::ifFalse)
    ).apply(inst, SchoolSelectLootRule::new));

    @Override
    public Codec<SchoolSelectLootRule> getCodec() {
        return CODEC;
    }

    @Override
    public void execute(ItemStack stack, LootRarity rarity, GenContext ctx) {
        if (!GearSchools.of(stack).schools().isEmpty()) {
            this.ifTrue.execute(stack, rarity, ctx);
        } else {
            this.ifFalse.ifPresent(rule -> rule.execute(stack, rarity, ctx));
        }
    }
}
