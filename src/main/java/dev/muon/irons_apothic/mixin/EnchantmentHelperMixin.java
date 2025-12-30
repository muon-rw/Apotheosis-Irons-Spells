package dev.muon.irons_apothic.mixin;

import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;

/**
 * Extends Apotheosis's affix hooks to also check Curios slots.
 * <p>
 * Apotheosis's EnchantmentHelperMixin only iterates {@link LivingEntity#getAllSlots()},
 * which doesn't include Curios. This mixin adds Curios support for doPostAttack (MELEE_HIT triggers).
 */
@Mixin(value = EnchantmentHelper.class, remap = false)
public class EnchantmentHelperMixin {

    /**
     * Injects at the end of doPostAttackEffectsWithItemSource to check Curios slots for affix triggers.
     * <p>
     * Only handles the attacker's curios for doPostAttack (MELEE_HIT).
     * Apotheosis already handles getAllSlots() - we only add Curios support here.
     */
    @Inject(at = @At("TAIL"), method = "doPostAttackEffectsWithItemSource(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;)V")
    private static void doPostAttackCurios(ServerLevel level, Entity target, DamageSource damageSource, @Nullable ItemStack itemSource, CallbackInfo ci) {
        if (damageSource.getEntity() instanceof LivingEntity user) {
            CuriosApi.getCuriosInventory(user).ifPresent(curiosHandler -> {
                for (var slotResult : curiosHandler.findCurios(stack -> !stack.isEmpty())) {
                    ItemStack stack = slotResult.stack();
                    var affixes = AffixHelper.getAffixes(stack);
                    for (AffixInstance inst : affixes.values()) {
                        int old = target.invulnerableTime;
                        target.invulnerableTime = 0;
                        inst.doPostAttack(user, target);
                        target.invulnerableTime = old;
                    }
                }
            });
        }
    }
}

