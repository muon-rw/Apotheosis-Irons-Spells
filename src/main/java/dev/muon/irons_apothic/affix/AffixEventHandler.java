package dev.muon.irons_apothic.affix;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;

public class AffixEventHandler {

    public static void register() {
        NeoForge.EVENT_BUS.register(new AffixEventHandler());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        DamageSource src = e.getSource();
        Entity directEntity = src.getDirectEntity();
        Entity causingEntity = src.getEntity();

        boolean canTeleport = false;
        Vec3 targetPos = null;
        ItemStack sourceWeapon;

        if (directEntity instanceof Projectile spell && spell.getOwner() != null) {
            sourceWeapon = AffixHelper.getSourceWeapon(spell);
            if (!sourceWeapon.isEmpty()) {
                canTeleport = AffixHelper.streamAffixes(sourceWeapon).anyMatch(AffixInstance::enablesTelepathy);
                if (canTeleport) {
                    targetPos = spell.getOwner().position();
                }
            }
        }

        if (!canTeleport && causingEntity instanceof LivingEntity living) {
            sourceWeapon = living.getMainHandItem();
            canTeleport = AffixHelper.streamAffixes(sourceWeapon).anyMatch(AffixInstance::enablesTelepathy);
            if (canTeleport) {
                targetPos = living.position();
            }
        }

        if (canTeleport && targetPos != null) {
            for (ItemEntity item : e.getDrops()) {
                item.setPos(targetPos.x, targetPos.y, targetPos.z);
                item.setPickUpDelay(0);
            }
        }
    }

    @SubscribeEvent
    public void hookSpellDamageAffix(SpellDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getSpellDamageSource().getEntity() instanceof LivingEntity living ? living : null;
        if (caster == null) return;

        for (ItemStack stack : caster.getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_TARGET) {
                        affix.applyEffectInternal(event.getEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_DAMAGE_SELF) {
                        affix.applyEffectInternal(caster, inst);
                    }
                } else if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_DAMAGE) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getEntity();
                    }).orElse(event.getEntity());
                    
                    affix.triggerSpell(caster, target, inst);
                }
            });
        }
    }

    @SubscribeEvent
    public void hookSpellHealAffix(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity caster = event.getEntity();
        
        for (ItemStack stack : caster.getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_TARGET) {
                        affix.applyEffectInternal(event.getTargetEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_SELF) {
                        affix.applyEffectInternal(caster, inst);
                    }
                } else if (inst.getAffix() instanceof SpellTriggerAffix affix && affix.trigger == SpellTriggerAffix.TriggerType.SPELL_HEAL) {
                    LivingEntity target = affix.target.map(targetType -> switch (targetType) {
                        case SELF -> caster;
                        case TARGET -> event.getTargetEntity();
                    }).orElse(event.getTargetEntity());
                    
                    affix.triggerSpell(caster, target, inst);
                }
            });
        }
    }

    @SubscribeEvent
    public void onChangeMana(ChangeManaEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        
        Player player = event.getEntity();
        MagicData magicData = event.getMagicData();
        SpellData castingSpell = magicData.getCastingSpell();
        
        if (castingSpell == null || event.getNewMana() >= event.getOldMana()) {
            return;
        }
        
        AbstractSpell spell = castingSpell.getSpell();
        if (spell == null) return;
        
        SchoolType spellSchool = spell.getSchoolType();
        
        // Only applies to STAFF/MELEE_WEAPON
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;
        
        float totalReduction = 0f;
        
        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(mainHand);
        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                if (affix instanceof ManaCostAffix manaCostAffix) {
                    if (manaCostAffix.getSchool() == spellSchool) {
                        float reduction = manaCostAffix.getReductionPercent(instance.getRarity(), instance.level());
                        totalReduction += reduction;
                    }
                }
            }
        }

        if (totalReduction > 0) {
            float manaCost = event.getOldMana() - event.getNewMana();
            float reducedCost = manaCost * (1 - Math.min(totalReduction, 0.9f)); // Cap at 90% reduction
            float newManaValue = event.getOldMana() - reducedCost;
            event.setNewMana(newManaValue);
        }
    }

    @SubscribeEvent
    public void onSpellLevel(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;
        AbstractSpell spell = event.getSpell();
        SchoolType school = spell.getSchoolType();

        int totalBonus = 0;

        for (ItemStack stack : livingEntity.getAllSlots()) {
            totalBonus += getSpellLevelBonus(stack, school);
        }

        int curiosBonus = CuriosApi.getCuriosInventory(livingEntity)
            .map(curiosHandler -> {
                int bonus = 0;
                for (var slotResult : curiosHandler.findCurios(stack -> !stack.isEmpty())) {
                    bonus += getSpellLevelBonus(slotResult.stack(), school);
                }
                return bonus;
            })
            .orElse(0);
        
        totalBonus += curiosBonus;

        if (totalBonus > 0) {
            event.addLevels(totalBonus);
        }
    }

    private int getSpellLevelBonus(ItemStack stack, SchoolType school) {
        int bonus = 0;
        Map<DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(stack);
        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                if (affix instanceof SpellLevelAffix spellLevelAffix) {
                    if (spellLevelAffix.getSchool() == school) {
                        bonus += spellLevelAffix.getBonusLevel(instance.getRarity(), instance.level());
                    }
                }
            }
        }
        return bonus;
    }
} 