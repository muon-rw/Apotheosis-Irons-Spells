package dev.muon.irons_apothic.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.irons_apothic.util.BientitySpellCastAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Server-only synthetic facing for {@link BientitySpellCastAim} during affix-triggered bi-entity casts.
 * Each injector takes the fast-skip null path when no cast is active to keep cost on the per-tick rotation
 * getters near zero.
 */
@Mixin(Entity.class)
public class EntityMixin {

    @ModifyReturnValue(method = "getLookAngle", at = @At("RETURN"))
    private Vec3 irons_apothic$bientityLookAngle(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getForward", at = @At("RETURN"))
    private Vec3 irons_apothic$bientityForward(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getXRot", at = @At("RETURN"))
    private float irons_apothic$bientityXRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getYRot", at = @At("RETURN"))
    private float irons_apothic$bientityYRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }

    @ModifyReturnValue(method = "getRotationVector", at = @At("RETURN"))
    private Vec2 irons_apothic$bientityRotationVector(Vec2 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.rotationVector() : original;
    }

    @ModifyReturnValue(method = "getViewVector", at = @At("RETURN"))
    private Vec3 irons_apothic$bientityViewVector(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float irons_apothic$bientityViewXRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float irons_apothic$bientityViewYRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }
}
