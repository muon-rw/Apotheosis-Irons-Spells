package dev.muon.irons_apothic.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.irons_apothic.client.CastSlowdownSuppression;
import io.redspace.ironsspellbooks.player.ClientPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientPlayerEvents.class, remap = false)
public abstract class ClientPlayerEventsMixin {

    @ModifyExpressionValue(method = "onCalculatePlayerSpeed",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;isCasting()Z"))
    private static boolean irons_apothic$skipSlowdownForAffixCast(boolean isCasting) {
        return isCasting && !CastSlowdownSuppression.isActive();
    }
}
