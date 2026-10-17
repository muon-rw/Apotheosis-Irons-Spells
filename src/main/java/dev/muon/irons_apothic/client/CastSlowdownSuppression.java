package dev.muon.irons_apothic.client;

public final class CastSlowdownSuppression {

    // Client thread only. Cleared whenever ISS starts a new client-side cast (see ClientMagicDataMixin).
    private static boolean active;

    private CastSlowdownSuppression() {
    }

    public static void suppressCurrentCast() {
        active = true;
    }

    public static void clear() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }
}
