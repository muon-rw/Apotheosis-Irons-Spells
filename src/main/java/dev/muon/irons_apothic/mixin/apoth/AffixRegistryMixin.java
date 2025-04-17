package dev.muon.irons_apothic.mixin.apoth;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.affix.MagicTelepathicAffix;
import dev.muon.irons_apothic.affix.SpellEffectAffix;
import dev.muon.irons_apothic.affix.SpellLevelAffix;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AffixRegistry.class, remap = false)
public abstract class AffixRegistryMixin {

    @Inject(method = "registerBuiltinCodecs()V", at = @At("TAIL"))
    private void irons_apothic$registerAffixCodecs(CallbackInfo ci) {
        AffixRegistry self = (AffixRegistry) (Object) this;
        self.registerCodec(IronsApothic.loc("spell_effect"), SpellEffectAffix.CODEC);
        self.registerCodec(IronsApothic.loc("magic_telepathic"), MagicTelepathicAffix.CODEC);
        self.registerCodec(IronsApothic.loc("spell_level"), SpellLevelAffix.CODEC);
    }
}