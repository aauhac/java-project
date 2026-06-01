package com.tradeagent;

/**
 * Legacy DOC-API debug entrypoint.
 * Raw GKG flow replaced this path.
 */
@Deprecated(forRemoval = true)
public final class GdeltDebugMain {

    private GdeltDebugMain() {
    }

    public static void main(String[] args) {
        System.out.println("GdeltDebugMain is disabled. Use raw GKG refresh API instead.");
    }
}