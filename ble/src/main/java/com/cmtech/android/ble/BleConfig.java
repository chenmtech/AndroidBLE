package com.cmtech.android.ble;


public class BleConfig {
    private static final int MIN_CONNECT_INTERVAL = 6000; // min connection interval, unit: millisecond

    private static BleConfig instance;
    private int connectInterval = MIN_CONNECT_INTERVAL; // 自动扫描时间间隔，单位：秒

    private BleConfig() {
    }

    public static BleConfig getInstance() {
        if (instance == null) {
            synchronized (BleConfig.class) {
                if (instance == null) {
                    instance = new BleConfig();
                }
            }
        }
        return instance;
    }

    public int getConnectInterval() {
        return connectInterval;
    }

    public void setConnectInterval(int connectInterval) {
        this.connectInterval = (connectInterval < MIN_CONNECT_INTERVAL) ? MIN_CONNECT_INTERVAL : connectInterval;
    }
}
