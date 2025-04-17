package dev.muon.irons_apothic.mixin.apoth;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_apothic.affix.AffixSchoolMapper;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apothic_attributes.modifiers.VanillaEquipmentSlot;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.apothic_attributes.modifiers.EntityEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {
    @ModifyReturnValue(method = "getAvailableAffixes", at = @At("RETURN"))
    private static Stream<DynamicHolder<Affix>> filterAffixes(Stream<DynamicHolder<Affix>> originalAffixes, ItemStack stack, LootRarity rarity, AffixType type) {
        LootCategory cat = LootCategory.forItem(stack);
        EquipmentSlot relevantSlot = EquipmentSlot.MAINHAND;

        EntitySlotGroup group = cat.getSlots();
        if (group != null) {
            Optional<Holder<EntityEquipmentSlot>> firstHolder = group.slots().stream().findFirst();
            if (firstHolder.isPresent()) {
                EntityEquipmentSlot entitySlot = firstHolder.get().value();
                if (entitySlot instanceof VanillaEquipmentSlot vanillaWrapper) {
                    relevantSlot = vanillaWrapper.slot();
                }
            }
        }

        Set<SchoolType> gearSpellSchools = AffixSchoolMapper.getSpellSchoolsFromGear(stack, relevantSlot);

        return originalAffixes
                .filter(a -> {
                    Affix affix = a.get();
                    String affixId = AffixRegistry.INSTANCE.getKey(affix).toString();

                    if (AffixSchoolMapper.isElementalAffix(affixId)) {
                        SchoolType affixSpellSchool = AffixSchoolMapper.getSpellSchoolForAffix(affixId);
                        if (affixSpellSchool != null) {
                            return gearSpellSchools.contains(affixSpellSchool);
                        }
                        return false;
                    }
                    if (AffixSchoolMapper.isGenericSpellAffix(affixId)) {
                        return gearSpellSchools.isEmpty();
                    }
                    return true;
                });
    }
}