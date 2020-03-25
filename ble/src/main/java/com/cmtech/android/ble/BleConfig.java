package com.cmtech.android.ble;


public class BleConfig {
    private static final int MIN_RECONNECT_INTERVAL = 6000; // min reconnection interval, unit: millisecond.
    private static int reconnInterval = MIN_RECONNECT_INTERVAL; // when disconnect, device should wait this time to reconnect

    private BleConfig() {
    }

    public static int getReconnInterval() {
        return reconnInterval;
    }

    public static void setReconnInterval(int reconnInterval) {
        BleConfig.reconnInterval = Math.max(reconnInterval, MIN_RECONNECT_INTERVAL);
    }
}
