package io.github.blaspat.global;


import java.util.concurrent.atomic.AtomicBoolean;

public class ClientStatus {
    private static final AtomicBoolean clientConnected = new AtomicBoolean(true);

    public static boolean isClientConnected() {
        return clientConnected.get();
    }

    public static void setClientConnected(boolean isConnected) {
        clientConnected.set(isConnected);
    }
}
