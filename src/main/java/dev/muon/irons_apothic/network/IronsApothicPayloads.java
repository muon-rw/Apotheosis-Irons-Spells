package dev.muon.irons_apothic.network;

import dev.muon.irons_apothic.IronsApothic;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class IronsApothicPayloads {

    private IronsApothicPayloads() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(IronsApothicPayloads::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(IronsApothic.MODID).versioned("1");
        registrar.playToClient(CastStartedWithoutAnimationPacket.TYPE, CastStartedWithoutAnimationPacket.STREAM_CODEC,
                CastStartedWithoutAnimationPacket::handle);
        registrar.playToClient(CastFinishedWithoutAnimationPacket.TYPE, CastFinishedWithoutAnimationPacket.STREAM_CODEC,
                CastFinishedWithoutAnimationPacket::handle);
    }
}
