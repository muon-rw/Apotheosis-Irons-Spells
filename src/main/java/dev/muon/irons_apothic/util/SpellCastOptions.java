package dev.muon.irons_apothic.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record SpellCastOptions(Optional<Integer> castTime, boolean playerAnimations,
                               SpellCastInterruptMode interruptMode, int delayTicks) {

    public static final SpellCastOptions DEFAULT = withCastTime(Optional.empty());

    // Flattened into the owning affix's JSON object, so these read as top-level affix fields.
    public static final MapCodec<SpellCastOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst
            .group(
                    Codec.INT.optionalFieldOf("cast_time").forGetter(SpellCastOptions::castTime),
                    Codec.BOOL.optionalFieldOf("player_animations", true).forGetter(SpellCastOptions::playerAnimations),
                    SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.FORCE_COMPLETE)
                            .forGetter(SpellCastOptions::interruptMode),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", 0).forGetter(SpellCastOptions::delayTicks))
            .apply(inst, SpellCastOptions::new));

    public static SpellCastOptions withCastTime(Optional<Integer> castTime) {
        return new SpellCastOptions(castTime, true, SpellCastInterruptMode.FORCE_COMPLETE, 0);
    }
}
