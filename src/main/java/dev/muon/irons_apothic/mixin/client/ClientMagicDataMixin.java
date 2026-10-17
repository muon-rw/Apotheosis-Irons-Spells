package dev.muon.irons_apothic.mixin.client;

import dev.muon.irons_apothic.client.CastSlowdownSuppression;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMagicData.class, remap = false)
public abstract class ClientMagicDataMixin {

    // Not resetClientCastState: ISS calls it for every tracked player's finished cast, not just the local player's.
    @Inject(method = "setClientCastState", at = @At("HEAD"))
    private static void irons_apothic$clearSlowdownSuppression(CallbackInfo ci) {
        CastSlowdownSuppression.clear();
    }
}
