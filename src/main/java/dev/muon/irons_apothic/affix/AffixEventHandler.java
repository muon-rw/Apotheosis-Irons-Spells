package dev.muon.irons_apothic.affix;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

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
        ItemStack sourceWeapon = ItemStack.EMPTY;

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
                }
            });
        }
    }

    @SubscribeEvent
    public void hookSpellHealAffix(SpellHealEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        for (ItemStack stack : event.getEntity().getAllSlots()) {
            AffixHelper.streamAffixes(stack).forEach(inst -> {
                if (inst.getAffix() instanceof SpellEffectAffix affix) {
                    if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_TARGET) {
                        affix.applyEffectInternal(event.getTargetEntity(), inst);
                    } else if (affix.target == SpellEffectAffix.SpellTarget.SPELL_HEAL_SELF) {
                        affix.applyEffectInternal(event.getEntity(), inst);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onSpellCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        SchoolType school = event.getSchoolType();
        CastSource source = event.getCastSource();

        ItemStack castingStack = ItemStack.EMPTY;
        if (source == CastSource.SPELLBOOK || source == CastSource.SWORD) {
            castingStack = player.getMainHandItem();
        }

        if (castingStack.isEmpty()) {
            return;
        }

        int totalBonus = 0;
        Map<dev.shadowsoffire.placebo.reload.DynamicHolder<Affix>, AffixInstance> affixes = AffixHelper.getAffixes(castingStack);

        for (AffixInstance instance : affixes.values()) {
            if (instance.isValid() && instance.affix().isBound()) {
                Affix affix = instance.getAffix();
                if (affix instanceof SpellLevelAffix spellLevelAffix) {
                    if (spellLevelAffix.getSchool() == school) {
                        int bonus = spellLevelAffix.getBonusLevel(instance.getRarity(), instance.level());
                        totalBonus += bonus;
                    }
                }
            }
        }

        if (totalBonus > 0) {
            event.setSpellLevel(event.getOriginalSpellLevel() + totalBonus);
        }
    }
} 