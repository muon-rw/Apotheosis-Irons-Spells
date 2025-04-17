package dev.muon.irons_apothic.mixin.apoth;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_apothic.affix.AffixSchoolMapper;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.stream.Stream;

@Mixin(value = LootController.class, remap = false)
public class LootControllerMixin {
    @ModifyReturnValue(method = "getAvailableAffixes", at = @At("RETURN"))
    private static Stream<DynamicHolder<Affix>> filterAffixes(Stream<DynamicHolder<Affix>> originalAffixes, ItemStack stack, LootRarity rarity, AffixType type) {
        Set<SchoolType> gearSpellSchools = AffixSchoolMapper.getSpellSchoolsFromWeapon(stack);

        return originalAffixes
                .filter(a -> {
                    Affix affix = a.get();
                    String affixId = AffixRegistry.INSTANCE.getKey(affix).toString();

                    if (AffixSchoolMapper.isElementalAffix(affixId)) {
                        SchoolType affixSpellSchool = AffixSchoolMapper.getSpellSchoolForAffix(affixId);
                        if (affixSpellSchool != null) {
                            return gearSpellSchools.contains(affixSpellSchool);
                        }
                    }
                    if (AffixSchoolMapper.isGenericSpellAffix(affixId)) {
                        return gearSpellSchools.isEmpty();
                    }
                    return true;
                });
    }
}