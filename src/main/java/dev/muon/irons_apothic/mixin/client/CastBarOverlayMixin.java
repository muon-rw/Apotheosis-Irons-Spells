package dev.muon.irons_apothic.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.gui.overlays.CastBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CastBarOverlay.class, remap = false)
public abstract class CastBarOverlayMixin {

    // ISS hides the bar for INSTANT spells, which never have a cast duration of their own. Only a cast time override
    // (like our "cast_time") gives one a duration, and that cast does wait, so show the bar as for a LONG cast.
    @ModifyExpressionValue(method = "render",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;getCastType()Lio/redspace/ironsspellbooks/api/spells/CastType;", ordinal = 0))
    private CastType irons_apothic$showBarForInstantWithCastTime(CastType castType) {
        return castType == CastType.INSTANT && ClientMagicData.getCastDuration() > 0 ? CastType.LONG : castType;
    }
}
