package dev.muon.irons_apothic.attribute;

import dev.muon.irons_apothic.IronsApothic;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;

public class AttributeEvents {

    @SubscribeEvent
    public void onSpellCast(SpellOnCastEvent event) {
        SchoolType school = event.getSchoolType();
        Holder<Attribute> attributeHolder = AttributeRegistry.getSchoolLevelAttribute(school);

        if (attributeHolder != null && attributeHolder.isBound()) {
            AttributeInstance levelAttr = event.getEntity().getAttribute(attributeHolder);

            if (levelAttr != null && levelAttr.getValue() > 0) {
                int baseLevel = event.getSpellLevel();
                int bonusLevel = (int) levelAttr.getValue();
                event.setSpellLevel(baseLevel + bonusLevel);
            }
        } else if (attributeHolder != null) {
             IronsApothic.LOGGER.warn("Unbound attribute holder found for school {} during SpellOnCastEvent.", school.getId());
        }
    }
}