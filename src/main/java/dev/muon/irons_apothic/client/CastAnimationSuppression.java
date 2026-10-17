package dev.muon.irons_apothic.client;

public final class CastAnimationSuppression {

    // Client thread only
    private static boolean active;

    private CastAnimationSuppression() {
    }

    public static void run(Runnable castPacketHandler) {
        active = true;
        try {
            castPacketHandler.run();
        } finally {
            active = false;
        }
    }

    public static boolean isActive() {
        return active;
    }
}
