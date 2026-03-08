package dev.muon.irons_apothic.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.irons_apothic.util.SpellCastUtil;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ImbuedSpellTriggerAffix extends SchoolFilteredAffix {
    private static final ThreadLocal<Boolean> IS_TRIGGERING = ThreadLocal.withInitial(() -> false);

    public static boolean isCurrentlyTriggering() {
        return IS_TRIGGERING.get();
    }

    public static final Codec<ImbuedSpellTriggerAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.affixDef(),
                    SpellTriggerAffix.TriggerType.CODEC.fieldOf("trigger").forGetter(a -> a.trigger),
                    LootRarity.mapCodec(SpellTriggerAffix.TriggerData.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types),
                    SpellTriggerAffix.TargetType.CODEC.optionalFieldOf("target").forGetter(a -> a.target),
                    ResourceLocation.CODEC.optionalFieldOf("school").forGetter(a ->
                            a.schoolIds.filter(list -> list.size() == 1).map(list -> list.get(0))),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("schools").forGetter(a -> a.schoolIds))
            .apply(inst, (def, trigger, values, types, target, singleSchool, schoolsArray) -> {
                Optional<List<ResourceLocation>> schoolIds = schoolsArray.isPresent()
                        ? schoolsArray
                        : singleSchool.map(List::of);
                return new ImbuedSpellTriggerAffix(def, trigger, values, types, target, schoolIds);
            }));

    protected final SpellTriggerAffix.TriggerType trigger;
    protected final Map<LootRarity, SpellTriggerAffix.TriggerData> values;
    protected final Set<LootCategory> types;
    protected final Optional<SpellTriggerAffix.TargetType> target;
    protected final Optional<List<ResourceLocation>> schoolIds;
    protected final Optional<Set<SchoolType>> schools;

    public ImbuedSpellTriggerAffix(AffixDefinition definition,
                                   SpellTriggerAffix.TriggerType trigger,
                                   Map<LootRarity, SpellTriggerAffix.TriggerData> values, Set<LootCategory> types,
                                   Optional<SpellTriggerAffix.TargetType> target,
                                   Optional<List<ResourceLocation>> schoolIds) {
        super(definition);
        this.trigger = trigger;
        this.values = values;
        this.types = types;
        this.target = target;
        this.schoolIds = schoolIds;
        this.schools = schoolIds.map(ids -> {
            Set<SchoolType> schoolSet = new HashSet<>();
            for (ResourceLocation id : ids) {
                SchoolType school = SchoolRegistry.REGISTRY.get(id);
                if (school != null) {
                    schoolSet.add(school);
                } else {
                    Apotheosis.LOGGER.warn("Unknown school ID {} provided for ImbuedSpellTriggerAffix, ignoring.", id);
                }
            }
            return schoolSet;
        });
    }

    /**
     * Resolves the imbued spell source stack, falling back to mainhand for non-imbuable items (e.g. shields).
     */
    private static ItemStack resolveSourceStack(ItemStack affixStack, LivingEntity entity) {
        if (ISpellContainer.isSpellContainer(affixStack) && !ISpellContainer.get(affixStack).isEmpty()) {
            return affixStack;
        }
        return entity.getMainHandItem();
    }

    public void triggerSpell(LivingEntity caster, LivingEntity target, AffixInstance inst) {
        triggerSpellInternal(caster, target, inst.rarity().get(), resolveSourceStack(inst.stack(), caster));
    }

    private void triggerSpellInternal(LivingEntity caster, LivingEntity target, LootRarity rarity, ItemStack sourceStack) {
        if (IS_TRIGGERING.get()) return;

        SpellTriggerAffix.TriggerData data = this.values.get(rarity);
        if (data == null || caster.level().isClientSide()) return;

        if (!ISpellContainer.isSpellContainer(sourceStack) || ISpellContainer.get(sourceStack).isEmpty()) return;

        var spellData = ISpellContainer.get(sourceStack).getSpellAtIndex(0);
        AbstractSpell spellInstance = spellData.getSpell();
        if (spellInstance == null) return;

        int spellLevel = spellData.getLevel();
        String spellId = spellInstance.getSpellId();

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        boolean hasActiveRecast = magicData.getPlayerRecasts().hasRecastForSpell(spellId);

        int cooldown = data.cooldown();
        if (!hasActiveRecast && cooldown != 0 && Affix.isOnCooldown(this.id(), cooldown, caster)) {
            return;
        }

        if (!matchesSchools(sourceStack, this.schools)) return;

        try {
            IS_TRIGGERING.set(true);

            SpellCastUtil.castSpell(caster, spellInstance, spellLevel, target);

            if (!hasActiveRecast && cooldown != 0) {
                Affix.startCooldown(this.id(), caster);
            }
        } finally {
            IS_TRIGGERING.set(false);
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
        // Require imbueable unless shield (spell from mainhand when blocking) or item already has a spell (e.g. MagicSwordItem/UniqueItem like Monstrous Flamberge)
        if (cat != Apoth.LootCategories.SHIELD) {
            if (!Utils.canImbue(stack) && !ISpellContainer.isSpellContainer(stack)) {
                return false;
            }
        }
        return matchesSchools(stack, this.schools);
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        SpellTriggerAffix.TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        String triggerKey = "affix.irons_apothic.trigger_imbued." + this.trigger.name().toLowerCase();
        boolean isSelfCast = this.target.map(t -> t == SpellTriggerAffix.TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey);

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        SpellTriggerAffix.TriggerData data = this.values.get(inst.rarity().get());
        if (data == null) return Component.empty();

        String triggerKey = "affix.irons_apothic.trigger_imbued." + this.trigger.name().toLowerCase();
        boolean isSelfCast = this.target.map(t -> t == SpellTriggerAffix.TargetType.SELF).orElse(false);
        String finalKey = isSelfCast ? triggerKey + ".self" : triggerKey;

        MutableComponent comp = Component.translatable(finalKey);

        int cooldown = data.cooldown();
        if (cooldown != 0) {
            Component cd = Component.translatable("affix.apotheosis.cooldown", StringUtil.formatTickDuration(cooldown, ctx.tickRate()));
            comp = comp.append(" ").append(cd);
        }

        return comp;
    }

    @Override
    public void doPostAttack(AffixInstance inst, LivingEntity user, Entity target) {
        if (this.trigger == SpellTriggerAffix.TriggerType.MELEE_HIT && target instanceof LivingEntity livingTarget) {
            LivingEntity actualTarget = this.target.map(t -> switch (t) {
                case SELF -> user;
                case TARGET -> livingTarget;
            }).orElse(livingTarget);

            triggerSpellInternal(user, actualTarget, inst.rarity().get(), resolveSourceStack(inst.stack(), user));
        }
    }

    @Override
    public void doPostHurt(AffixInstance inst, LivingEntity user, DamageSource source) {
        if (this.trigger == SpellTriggerAffix.TriggerType.HURT && source.getEntity() instanceof LivingEntity attacker) {
            LivingEntity actualTarget = this.target.map(t -> switch (t) {
                case SELF -> user;
                case TARGET -> attacker;
            }).orElse(user);

            triggerSpellInternal(user, actualTarget, inst.rarity().get(), resolveSourceStack(inst.stack(), user));
        }
    }

    @Override
    public void onProjectileImpact(float level, LootRarity rarity, Projectile proj, HitResult res, HitResult.Type type) {
        if (this.trigger == SpellTriggerAffix.TriggerType.PROJECTILE_HIT && type == HitResult.Type.ENTITY &&
                res instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity hitEntity &&
                proj.getOwner() instanceof LivingEntity owner) {

            LivingEntity actualTarget = this.target.map(t -> switch (t) {
                case SELF -> owner;
                case TARGET -> hitEntity;
            }).orElse(hitEntity);

            ItemStack sourceStack = AffixHelper.getSourceWeapon(proj);
            triggerSpellInternal(owner, actualTarget, rarity, sourceStack);
        }
    }

    @Override
    public float onShieldBlock(AffixInstance inst, LivingEntity entity, DamageSource source, float amount) {
        if (this.trigger == SpellTriggerAffix.TriggerType.SHIELD_BLOCK) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
            LivingEntity actualTarget = attacker != null
                    ? this.target.map(t -> t == SpellTriggerAffix.TargetType.TARGET ? attacker : entity).orElse(attacker)
                    : entity;

            triggerSpellInternal(entity, actualTarget, inst.rarity().get(), resolveSourceStack(inst.stack(), entity));
        }
        return amount;
    }
}
