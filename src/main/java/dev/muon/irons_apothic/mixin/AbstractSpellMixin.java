package dev.muon.irons_apothic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.irons_apothic.network.CastFinishedWithoutAnimationPacket;
import dev.muon.irons_apothic.util.SpellCastUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {

    @WrapMethod(method = "castSpell")
    private void irons_apothic$markCasterResolving(Level world, int spellLevel, ServerPlayer serverPlayer,
                                                   CastSource castSource, boolean triggerCooldown,
                                                   Operation<Void> original) {
        SpellCastUtil.resolve(serverPlayer, () -> original.call(world, spellLevel, serverPlayer, castSource, triggerCooldown));
    }

    @ModifyArg(method = "onServerCastComplete",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToPlayersTrackingEntityAndSelf(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"),
            index = 1)
    private CustomPacketPayload irons_apothic$skipFinishAnimationForAffixCast(CustomPacketPayload onCastFinished,
                                                                              @Local(argsOnly = true) LivingEntity entity,
                                                                              @Local(argsOnly = true) boolean cancelled) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        if (entity instanceof ServerPlayer serverPlayer && SpellCastUtil.consumeCastWithoutAnimation(serverPlayer, spell)) {
            return new CastFinishedWithoutAnimationPacket(serverPlayer.getUUID(), spell.getSpellId(), cancelled);
        }
        return onCastFinished;
    }
}
