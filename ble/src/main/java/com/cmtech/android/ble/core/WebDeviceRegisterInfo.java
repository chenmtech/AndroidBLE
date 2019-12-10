package com.cmtech.android.ble.core;

public class WebDeviceRegisterInfo extends DeviceRegisterInfo {
    private final String broadcastId; // 该网络设备的广播者ID
    private String broadcastName = "";

    public WebDeviceRegisterInfo(String macAddress, String uuidStr, String broadcastId) {
        super(macAddress, uuidStr);
        this.broadcastId = broadcastId;
    }

    public String getBroadcastId() {
        return broadcastId;
    }

    public String getBroadcastName() {
        return broadcastName;
    }

    public void setBroadcastName(String broadcastName) {
        this.broadcastName = broadcastName;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
