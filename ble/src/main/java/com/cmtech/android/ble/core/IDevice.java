package com.cmtech.android.ble.core;

import com.cmtech.android.ble.exception.BleException;

public interface IDevice extends IDeviceConnector {
    int INVALID_BATTERY = -1; // 无效电池电量

    DeviceRegisterInfo getRegisterInfo();

    void updateRegisterInfo(DeviceRegisterInfo registerInfo);

    boolean isLocal();

    String getAddress();

    String getName();

    String getUuidString();

    String getImagePath();

    boolean autoConnect();

    boolean isWarnWhenBleInnerError();

    void updateState();

    int getBattery();

    void setBattery(final int battery);

    void addListener(OnDeviceListener listener);

    void removeListener(OnDeviceListener listener);

    boolean onConnectSuccess(); // 连接成功后执行的操作

    void onConnectFailure(); // 连接失败后执行的操作

    void onDisconnect(); // 断开连接后执行的操作

    void handleException(BleException ex); // 处理异常

    // 设备监听器接口
    interface OnDeviceListener {
        void onStateUpdated(final IDevice device); // 设备状态更新

        void onExceptionHandled(final IDevice device, BleException ex); // 异常处理

        void onBatteryUpdated(final IDevice device); // 电量更新
    }
}
