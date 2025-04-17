package dev.muon.irons_apothic.affix;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public class AffixEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void dropsLowest(LivingDropsEvent e) {
        MagicTelepathicAffix.drops(e);
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

}
