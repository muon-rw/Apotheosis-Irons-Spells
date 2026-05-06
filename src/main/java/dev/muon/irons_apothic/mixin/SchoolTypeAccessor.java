package dev.muon.irons_apothic.mixin;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SchoolType.class)
public interface SchoolTypeAccessor {
    @Accessor("powerAttribute")
    Holder<Attribute> irons_apothic$getPowerAttribute();
}
