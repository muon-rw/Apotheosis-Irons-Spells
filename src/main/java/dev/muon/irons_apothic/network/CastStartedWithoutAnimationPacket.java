package dev.muon.irons_apothic.network;

import dev.muon.irons_apothic.IronsApothic;
import dev.muon.irons_apothic.client.CastAnimationSuppression;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record CastStartedWithoutAnimationPacket(UUID casterId, String spellId, int spellLevel) implements CustomPacketPayload {

    public static final Type<CastStartedWithoutAnimationPacket> TYPE = new Type<>(IronsApothic.loc("cast_started_without_animation"));
    public static final StreamCodec<ByteBuf, CastStartedWithoutAnimationPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CastStartedWithoutAnimationPacket::casterId,
            ByteBufCodecs.STRING_UTF8, CastStartedWithoutAnimationPacket::spellId,
            ByteBufCodecs.VAR_INT, CastStartedWithoutAnimationPacket::spellLevel,
            CastStartedWithoutAnimationPacket::new);

    public static void handle(CastStartedWithoutAnimationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> CastAnimationSuppression.run(() ->
                ClientSpellCastHelper.handleClientBoundOnCastStarted(packet.casterId, packet.spellId, packet.spellLevel)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
