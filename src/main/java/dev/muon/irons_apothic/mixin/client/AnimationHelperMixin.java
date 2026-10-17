package dev.muon.irons_apothic.mixin.client;

import dev.muon.irons_apothic.client.CastAnimationSuppression;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Gating the callee rather than ClientSpellCastHelper's call sites, because the start animation is played from an
// ISS lambda whose synthetic name isn't stable across versions.
@Mixin(value = AnimationHelper.class, remap = false)
public abstract class AnimationHelperMixin {

    @Inject(method = {"animatePlayerStart", "cancelPlayerAnimation"}, at = @At("HEAD"), cancellable = true)
    private static void irons_apothic$skipAnimationForAffixCast(CallbackInfo ci) {
        if (CastAnimationSuppression.isActive()) {
            ci.cancel();
        }
    }
}
