package com.cmtech.android.ble.core;

import com.cmtech.android.ble.R;

public interface IDevice extends IDeviceConnector{
    int INVALID_BATTERY = -1; // 无效电池电量
    int MSG_INVALID_OPERATION = R.string.invalid_operation;

    // 设备监听器接口
    interface OnDeviceListener {
        void onStateUpdated(final IDevice device); // 设备状态更新
        void onExceptionMsgNotified(final IDevice device, int msgId); // 异常消息通知
        void onBatteryUpdated(final IDevice device); // 电池电量更新
    }

    DeviceRegisterInfo getRegisterInfo();
    void updateRegisterInfo(DeviceRegisterInfo registerInfo);
    String getAddress();
    String getName();
    String getUuidString();
    String getImagePath();
    boolean autoConnect();
    boolean warnBleInnerError();
    void updateState();
    int getBattery();
    void setBattery(final int battery);
    void addListener(OnDeviceListener listener);
    void removeListener(OnDeviceListener listener);
    boolean onConnectSuccess(); // 连接成功后执行的操作
    void onConnectFailure(); // 连接失败后执行的操作
    void onDisconnect(); // 断开连接后执行的操作
}
