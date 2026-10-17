package dev.muon.irons_apothic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.muon.irons_apothic.network.CastFinishedWithoutAnimationPacket;
import dev.muon.irons_apothic.util.SpellCastOptions;
import dev.muon.irons_apothic.util.SpellCastUtil;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {

    @WrapMethod(method = "castSpell")
    private void irons_apothic$markCasterResolving(Level world, int spellLevel, ServerPlayer serverPlayer,
                                                   CastSource castSource, boolean triggerCooldown,
                                                   Operation<Void> original) {
        SpellCastUtil.resolve(serverPlayer, (AbstractSpell) (Object) this,
                () -> original.call(world, spellLevel, serverPlayer, castSource, triggerCooldown));
    }

    @Inject(method = "onServerCastComplete", at = @At("HEAD"))
    private void irons_apothic$untrackAffixCast(Level level, int spellLevel, LivingEntity entity,
                                                MagicData playerMagicData, boolean cancelled, CallbackInfo ci,
                                                @Share("affixCast") LocalRef<SpellCastOptions> affixCast) {
        if (entity instanceof ServerPlayer serverPlayer) {
            affixCast.set(SpellCastUtil.untrackAffixCast(serverPlayer, (AbstractSpell) (Object) this));
        }
    }

    @ModifyArg(method = "onServerCastComplete",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToPlayersTrackingEntityAndSelf(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"),
            index = 1)
    private CustomPacketPayload irons_apothic$skipFinishAnimationForAffixCast(CustomPacketPayload onCastFinished,
                                                                              @Local(argsOnly = true) LivingEntity entity,
                                                                              @Local(argsOnly = true) boolean cancelled,
                                                                              @Share("affixCast") LocalRef<SpellCastOptions> affixCast) {
        SpellCastOptions options = affixCast.get();
        if (options == null || options.playerAnimations()) {
            return onCastFinished;
        }
        return new CastFinishedWithoutAnimationPacket(entity.getUUID(), ((AbstractSpell) (Object) this).getSpellId(), cancelled);
    }
}
