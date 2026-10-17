package dev.muon.irons_apothic.util;

import com.mojang.serialization.Codec;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;

public enum SpellCastInterruptMode {
    FORCE_COMPLETE,
    CANCEL,
    FAIL;

    public static final Codec<SpellCastInterruptMode> CODEC = PlaceboCodecs.enumCodec(SpellCastInterruptMode.class);
}
