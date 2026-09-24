package dev.muon.irons_apothic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.muon.irons_apothic.util.SpellCastUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellMixin {

    @WrapMethod(method = "castSpell")
    private void irons_apothic$markCasterResolving(Level world, int spellLevel, ServerPlayer serverPlayer,
                                                   CastSource castSource, boolean triggerCooldown,
                                                   Operation<Void> original) {
        SpellCastUtil.resolve(serverPlayer, () -> original.call(world, spellLevel, serverPlayer, castSource, triggerCooldown));
    }
}
