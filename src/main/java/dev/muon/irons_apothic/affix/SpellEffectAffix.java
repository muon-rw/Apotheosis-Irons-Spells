package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Set;

public class SpellEffectAffix extends Affix {
    public static final Codec<SpellEffectAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("mob_effect").forGetter(a -> a.effect),
                    SpellTarget.CODEC.fieldOf("target").forGetter(a -> a.target),
                    LootRarity.mapCodec(EffectData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    Codec.BOOL.optionalFieldOf("stack_on_reapply", false).forGetter(a -> a.stackOnReapply),
                    Codec.intRange(1, 255).optionalFieldOf("stacking_limit", 255).forGetter(a -> a.stackingLimit))
            .apply(inst, SpellEffectAffix::new));

    protected final Holder<MobEffect> effect;
    protected final SpellTarget target;
    protected final Map<LootRarity, EffectData> values;
    protected final Set<LootCategory> types;
    protected final boolean stackOnReapply;
    protected final int stackingLimit;

    public void applyEffectInternal(LivingEntity target, AffixInstance inst) {
        EffectData data = this.values.get(inst.rarity().get());
        if (data == null || target.level().isClientSide()) return;

        int cooldown = data.cooldown();
        if (cooldown != 0 && Affix.isOnCooldown(this.id(), cooldown, target)) return;

        MobEffectInstance currentInst = target.getEffect(this.effect);
        MobEffectInstance newInst = data.build(this.effect, inst.level());

        if (this.stackOnReapply && currentInst != null) {
            int newAmp = Math.min(this.stackingLimit - 1, currentInst.getAmplifier() + newInst.getAmplifier() + 1);
            int newDur = Math.max(currentInst.getDuration(), newInst.getDuration());
            MobEffectInstance combinedInst = new MobEffectInstance(this.effect, newDur, newAmp, currentInst.isAmbient(), currentInst.isVisible(), currentInst.isVisible());

            target.removeEffect(this.effect);
            target.addEffect(combinedInst);

        } else {
            target.addEffect(newInst);
        }

        if (cooldown != 0) {
            Affix.startCooldown(this.id(), target);
        }
    }

    public SpellEffectAffix(AffixDefinition definition, Holder<MobEffect> effect, SpellTarget target, Map<LootRarity, EffectData> values, Set<LootCategory> types, boolean stackOnReapply, int stackingLimit) {
        super(definition);
        this.effect = effect;
        this.target = target;
        this.values = values;
        this.types = types;
        this.stackOnReapply = stackOnReapply;
        this.stackingLimit = stackingLimit;
    }

    public static enum SpellTarget {
        SPELL_DAMAGE_SELF,
        SPELL_DAMAGE_TARGET,
        SPELL_HEAL_SELF,
        SPELL_HEAL_TARGET;

        public static final Codec<SpellTarget> CODEC = PlaceboCodecs.enumCodec(SpellTarget.class);
    }

    private static Component toComponent(MobEffectInstance inst, float tickRate) {
        MutableComponent mutablecomponent = Component.translatable(inst.getDescriptionId());
        Holder<MobEffect> mobeffect = inst.getEffect();

        if (inst.getAmplifier() > 0) {
            mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent,
                    Component.translatable("potion.potency." + inst.getAmplifier()));
        }

        if (inst.getDuration() > 20) {
            mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent,
                    MobEffectUtil.formatDuration(inst, 1.0F, tickRate));
        }

        return mutablecomponent.withStyle(mobeffect.value().getCategory().getTooltipFormatting());
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        EffectData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();
        MobEffectInstance effectInst = data.build(this.effect, inst.level());
        MutableComponent comp = Component.translatable("affix.irons_apothic.target." + this.target.name().toLowerCase(), toComponent(effectInst, ctx.tickRate()));

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }
        if (this.stackOnReapply) {
            comp = comp.append(" ").append(Component.translatable("affix.apotheosis.stacking"));
        }
        return comp;
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        LootRarity rarity = inst.rarity().get();
        EffectData data = this.values.get(rarity);
        if (data == null) return Component.empty();

        MobEffectInstance currentEffect = data.build(this.effect, inst.level());
        MutableComponent comp = Component.translatable("affix.irons_apothic.target." + this.target.name().toLowerCase(), toComponent(currentEffect, ctx.tickRate()));

        MobEffectInstance min = data.build(this.effect, 0);
        MobEffectInstance max = data.build(this.effect, 1);

        if (min.getAmplifier() != max.getAmplifier()) {
            Component minComp = min.getAmplifier() == 0 ? Component.literal("I") : Component.translatable("potion.potency." + min.getAmplifier());
            Component maxComp = Component.translatable("potion.potency." + max.getAmplifier());
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        if (!this.effect.value().isInstantenous() && min.getDuration() != max.getDuration()) {
            Component minComp = MobEffectUtil.formatDuration(min, 1.0F, ctx.tickRate());
            Component maxComp = MobEffectUtil.formatDuration(max, 1.0F, ctx.tickRate());
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }
        if (this.stackOnReapply) {
            comp = comp.append(" ").append(Component.translatable("affix.apotheosis.stacking"));
        }

        return comp;
    }

    public static record EffectData(StepFunction duration, StepFunction amplifier, int cooldown) {
        private static Codec<EffectData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.fieldOf("duration").forGetter(EffectData::duration),
                        StepFunction.CODEC.fieldOf("amplifier").forGetter(EffectData::amplifier),
                        Codec.INT.optionalFieldOf("cooldown", 0).forGetter(EffectData::cooldown))
                .apply(inst, EffectData::new));

        public MobEffectInstance build(Holder<MobEffect> effect, float level) {
            float clampedLevel = Math.max(0F, level);
            return new MobEffectInstance(effect, this.duration.getInt(clampedLevel), this.amplifier.getInt(clampedLevel));
        }
    }
}