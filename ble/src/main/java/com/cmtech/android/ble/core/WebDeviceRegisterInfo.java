package com.cmtech.android.ble.core;

public class WebDeviceRegisterInfo extends DeviceRegisterInfo {
    private final String broadcastId;

    public WebDeviceRegisterInfo(String macAddress, String uuidStr, String broadcastId) {
        super(macAddress, uuidStr);
        this.broadcastId = broadcastId;
    }

    public String getBroadcastId() {
        return broadcastId;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
