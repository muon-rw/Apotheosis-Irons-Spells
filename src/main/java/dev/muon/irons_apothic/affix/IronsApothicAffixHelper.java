package dev.muon.irons_apothic.affix;

import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Helper methods specific to Irons Apothic affix interactions, particularly with projectiles.
 */
public class IronsApothicAffixHelper {
    /**
     * Retrieves the Affix Instances associated with the weapon that fired a projectile.
     * Uses the source weapon data stored by Apotheosis.
     * @param projectile The projectile entity.
     * @return An immutable map of Affix Holders to Affix Instances from the source weapon, or an empty map.
     */
    public static Map<DynamicHolder<Affix>, AffixInstance> getAffixes(Projectile projectile) {
        // Retrieve the source weapon ItemStack stored by Apoth
        ItemStack sourceWeapon = AffixHelper.getSourceWeapon(projectile);

        // If a source weapon exists, get its affixes using the standard Apoth helper
        if (!sourceWeapon.isEmpty()) {
            return AffixHelper.getAffixes(sourceWeapon);
        }

        // Return empty if no source weapon data is found
        return Collections.emptyMap();
    }

    /**
     * Convenience method to stream the Affix Instances associated with the weapon that fired a projectile.
     * @param projectile The projectile entity.
     * @return A stream of Affix Instances from the source weapon.
     */
    public static Stream<AffixInstance> streamAffixes(Projectile projectile) {
        // Filters unbound/invalid instances implicitly via AffixHelper.getAffixes(ItemStack)
        return getAffixes(projectile).values().stream();
    }
}