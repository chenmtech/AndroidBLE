package com.cmtech.android.ble.core;

import com.cmtech.android.ble.exception.BleException;

public interface IDevice extends IConnector {
    int INVALID_BATTERY = -1; // invalid battery level
    DeviceRegisterInfo getRegisterInfo();
    void updateRegisterInfo(DeviceRegisterInfo registerInfo);
    boolean isLocal(); // is local device
    String getAddress();
    String getName();
    void setName(String name);
    String getUuidString();
    String getImagePath();
    boolean isAutoConnect();
    int getBattery();
    void setBattery(final int battery);
    void updateState();
    void addListener(OnDeviceListener listener);
    void removeListener(OnDeviceListener listener);
    void handleException(BleException ex); // handle exception
    boolean onConnectSuccess(); // operate when connection success
    void onConnectFailure(); // operate when connection failure
    void onDisconnect(); // operate when disconnection

    // device listener interface
    interface OnDeviceListener {
        void onStateUpdated(final IDevice device); // state updated
        void onExceptionNotified(final IDevice device, BleException ex); // exception notified
        void onBatteryUpdated(final IDevice device); // battery level updated
    }
}
