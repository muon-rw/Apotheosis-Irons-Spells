package dev.muon.irons_apothic.network;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.client.CastSlowdownSuppression;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// We send this to the caster right after ISS's UpdateCastingStatePacket, so it applies to the cast that packet started.
public record CastWithoutSlowdownPacket() implements CustomPacketPayload {

    public static final CastWithoutSlowdownPacket INSTANCE = new CastWithoutSlowdownPacket();
    public static final Type<CastWithoutSlowdownPacket> TYPE = new Type<>(IronsApothic.loc("cast_without_slowdown"));
    public static final StreamCodec<ByteBuf, CastWithoutSlowdownPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(CastWithoutSlowdownPacket packet, IPayloadContext context) {
        context.enqueueWork(CastSlowdownSuppression::suppressCurrentCast);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
