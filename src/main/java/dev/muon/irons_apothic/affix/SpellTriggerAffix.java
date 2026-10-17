package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.util.SpellCastInterruptMode;
import dev.muon.irons_apothic.util.SpellCastOptions;
import dev.muon.irons_apothic.util.SpellCastUtil;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apothic_attributes.api.AbilityCooldowns;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpellTriggerAffix extends SchoolFilteredAffix {

    // Reads both single "school" (backward compat) and "schools" array, but only writes "schools"
    public static final Codec<SpellTriggerAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    SpellRegistry.REGISTRY.holderByNameCodec().fieldOf("spell").forGetter(a -> a.spell),
                    TriggerType.CODEC.fieldOf("trigger").forGetter(a -> a.trigger),
                    LootRarity.mapCodec(TriggerData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    TargetType.CODEC.optionalFieldOf("target").forGetter(a -> a.target),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a -> Optional.empty()),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("schools").forGetter(a -> a.schoolIds),
                    SpellCastOptions.MAP_CODEC.forGetter(a -> a.castOptions))
            .apply(inst, (def, spell, trigger, values, types, target, singleSchool, schoolsArray, castOptions) -> {
                Optional<List<ResourceLocation>> schoolIds = schoolsArray.isPresent()
                    ? schoolsArray
                    : singleSchool.map(List::of);
                return new SpellTriggerAffix(def, spell, trigger, values, types, target, schoolIds, castOptions);
            }));

    protected final Holder<AbstractSpell> spell;
    protected final TriggerType trigger;
    protected final Map<LootRarity, TriggerData> values;
    protected final Set<LootCategory> types;
    protected final Optional<TargetType> target;
    protected final Optional<List<ResourceLocation>> schoolIds;
    protected final Optional<Set<SchoolType>> schools;
    protected final SpellCastOptions castOptions;

    public SpellTriggerAffix(AffixDefinition definition, Holder<AbstractSpell> spell, TriggerType trigger,
                             Map<LootRarity, TriggerData> values, Set<LootCategory> types,
                             Optional<TargetType> target, Optional<List<ResourceLocation>> schoolIds,
                             Optional<Integer> castTime) {
        this(definition, spell, trigger, values, types, target, schoolIds, SpellCastOptions.withCastTime(castTime));
    }

    public SpellTriggerAffix(AffixDefinition definition, Holder<AbstractSpell> spell, TriggerType trigger,
                             Map<LootRarity, TriggerData> values, Set<LootCategory> types,
                             Optional<TargetType> target, Optional<List<ResourceLocation>> schoolIds,
                             SpellCastOptions castOptions) {
        super(definition);
        this.spell = spell;
        this.trigger = trigger;
        this.values = values;
        this.types = types;
        this.target = target;
        this.schoolIds = schoolIds;
        this.castOptions = castOptions;
        this.schools = schoolIds.map(ids -> {
            Set<SchoolType> schoolSet = new HashSet<>();
            for (ResourceLocation id : ids) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) {
                    schoolSet.add(school);
                } else {
                    Apotheosis.LOGGER.warn("Unknown school ID {} provided for SpellTriggerAffix, ignoring.", id);
                }
            }
            return schoolSet;
        });
    }

    public void triggerSpell(LivingEntity caster, LivingEntity target, AffixInstance inst) {
        triggerSpell(caster, target, inst.rarity().get(), inst.level());
    }

    private void triggerSpell(LivingEntity caster, LivingEntity target, LootRarity rarity, float level) {
        // Prevent infinite recursion, since we don't have access to CastSource in the SpellDamageEvent/SpellHealEvent
        if (SpellCastUtil.isCastingForAffix()) {
            return;
        }

        TriggerData data = this.values.get(rarity);
        if (data == null || caster.level().isClientSide()) return;

        int spellLevel = data.level().getInt(level);
        AbstractSpell spellInstance = this.spell.value();
        String spellId = spellInstance.getSpellId();

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean hasActiveRecast = magicData.getPlayerRecasts().hasRecastForSpell(spellId);

        int cooldown = data.cooldown();
        if (!hasActiveRecast && cooldown != 0 && AbilityCooldowns.isOnCooldown(caster, this.id(), cooldown)) {
            return;
        }

        boolean cast = target == caster
                ? SpellCastUtil.castSelf(caster, spellInstance, spellLevel, this.castOptions)
                : SpellCastUtil.castWithTarget(caster, spellInstance, spellLevel, target, this.castOptions);

        if (cast && !hasActiveRecast && cooldown != 0) {
            AbilityCooldowns.startCooldown(caster, this.id());
        }
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!(this.types.isEmpty() || this.types.contains(cat)) || !this.values.containsKey(rarity)) {
            return false;
        }
        return matchesSchools(stack, this.schools);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        String triggerKey = "affix.irons_apothic.trigger." + this.trigger.name().toLowerCase();
        AbstractSpell spellInstance = this.spell.value();
        int spellLevel = data.level().getInt(inst.level());

        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + spellLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());

        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        int currentLevel = data.level().getInt(inst.level());
        AbstractSpell spellInstance = this.spell.value();

        Component coloredSpellName = spellInstance.getDisplayName(null).copy()
                .append(" ")
                .append(Component.translatable("enchantment.level." + currentLevel))
                .withStyle(spellInstance.getSchoolType().getDisplayName().getStyle());

        boolean isSelfCast = this.target.map(t -> t == TargetType.SELF).orElse(false);
        String triggerKey = "affix.irons_apothic.trigger." + this.trigger.name().toLowerCase();
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey, coloredSpellName);

        int minLevel = data.level().getInt(0);
        int maxLevel = data.level().getInt(1);
        if (minLevel != maxLevel) {
            Component minComp = Component.translatable("enchantment.level." + minLevel);
            Component maxComp = Component.translatable("enchantment.level." + maxLevel);
            comp.append(Affix.valueBounds(minComp, maxComp));
        }

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public void doPostAttack(AffixInstance inst, LivingEntity user, Entity target) {
        if (this.trigger == TriggerType.MELEE_HIT && target instanceof LivingEntity livingTarget) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> livingTarget;
            }).orElse(livingTarget);

            triggerSpell(user, actualTarget, inst);
        }
    }

    @Override
    public void doPostHurt(AffixInstance inst, LivingEntity user, DamageSource source) {
        if (this.trigger == TriggerType.HURT && source.getEntity() instanceof LivingEntity attacker) {
            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> user;
                case TARGET -> attacker;
            }).orElse(user);

            triggerSpell(user, actualTarget, inst);
        }
    }

    @Override
    public void onProjectileImpact(float level, LootRarity rarity, Projectile proj, HitResult res, HitResult.Type type) {
        if (this.trigger == TriggerType.PROJECTILE_HIT && type == HitResult.Type.ENTITY &&
                res instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity hitEntity &&
                proj.getOwner() instanceof LivingEntity owner) {

            LivingEntity actualTarget = this.target.map(targetType -> switch (targetType) {
                case SELF -> owner;
                case TARGET -> hitEntity;
            }).orElse(hitEntity);

            triggerSpell(owner, actualTarget, rarity, level);
        }
    }

    @Override
    public float onShieldBlock(AffixInstance inst, LivingEntity entity, DamageSource source, float amount) {
        if (this.trigger == TriggerType.SHIELD_BLOCK) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
            LivingEntity actualTarget = attacker != null
                    ? this.target.map(t -> t == TargetType.TARGET ? attacker : entity).orElse(attacker)
                    : entity;

            triggerSpell(entity, actualTarget, inst);
        }
        return amount;
    }

    public enum TriggerType {
        SPELL_DAMAGE,
        SPELL_HEAL,
        MELEE_HIT,
        PROJECTILE_HIT,
        HURT,
        SHIELD_BLOCK;
        public static final Codec<TriggerType> CODEC = PlaceboCodecs.enumCodec(TriggerType.class);
    }

    public enum TargetType {
        SELF,
        TARGET;

        public static final Codec<TargetType> CODEC = PlaceboCodecs.enumCodec(TargetType.class);
    }

    public record TriggerData(StepFunction level, int cooldown) {
        public static final Codec<TriggerData> CODEC = RecordCodecBuilder.create(inst -> inst
                .group(
                        StepFunction.CODEC.optionalFieldOf("level", StepFunction.constant(1)).forGetter(TriggerData::level),
                        Codec.INT.optionalFieldOf("cooldown", 0).forGetter(TriggerData::cooldown))
                .apply(inst, TriggerData::new));
    }

    @SuppressWarnings("unchecked")
    public static abstract class TriggerBuilder<T extends TriggerBuilder<T>> extends SchoolFilteredAffixBuilder<T, TriggerData> {
        protected final TriggerType trigger;
        @Nullable
        protected TargetType target;
        @Nullable
        protected Integer castTime;
        protected boolean playerAnimations = true;
        protected boolean castingSlowdown = true;
        protected SpellCastInterruptMode interruptMode = SpellCastInterruptMode.FORCE_COMPLETE;
        protected int delayTicks;

        protected TriggerBuilder(TriggerType trigger) {
            this.trigger = trigger;
        }

        public T target(TargetType target) {
            this.target = target;
            return (T) this;
        }

        public T castTime(int castTime) {
            this.castTime = castTime;
            return (T) this;
        }

        public T playerAnimations(boolean playerAnimations) {
            this.playerAnimations = playerAnimations;
            return (T) this;
        }

        public T castingSlowdown(boolean castingSlowdown) {
            this.castingSlowdown = castingSlowdown;
            return (T) this;
        }

        public T interruptMode(SpellCastInterruptMode interruptMode) {
            this.interruptMode = interruptMode;
            return (T) this;
        }

        public T delayTicks(int delayTicks) {
            this.delayTicks = delayTicks;
            return (T) this;
        }

        protected SpellCastOptions castOptions() {
            return new SpellCastOptions(Optional.ofNullable(this.castTime), this.playerAnimations, this.castingSlowdown,
                    this.interruptMode, this.delayTicks);
        }

        public T value(LootRarity rarity, int spellLevel, int cooldown) {
            return this.value(rarity, StepFunction.constant(spellLevel), cooldown);
        }

        public T value(LootRarity rarity, int minSpellLevel, int maxSpellLevel, int cooldown) {
            return this.value(rarity, StepFunction.fromBounds(minSpellLevel, maxSpellLevel, 1), cooldown);
        }

        public T value(LootRarity rarity, StepFunction spellLevel, int cooldown) {
            return this.value(rarity, new TriggerData(spellLevel, cooldown));
        }
    }

    public static class Builder extends TriggerBuilder<Builder> {
        protected final Holder<AbstractSpell> spell;

        public Builder(Holder<AbstractSpell> spell, TriggerType trigger) {
            super(trigger);
            this.spell = spell;
        }

        public Builder(AbstractSpell spell, TriggerType trigger) {
            this(SpellRegistry.REGISTRY.wrapAsHolder(spell), trigger);
        }

        public SpellTriggerAffix build() {
            this.validate();
            return new SpellTriggerAffix(this.definition, this.spell, this.trigger, this.values, this.categories,
                    Optional.ofNullable(this.target), this.schoolFilter(), this.castOptions());
        }
    }
}