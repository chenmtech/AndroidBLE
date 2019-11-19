package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;

public interface IDevice {
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
    BleDeviceState getState();
    boolean isScanning();
    boolean isConnected();
    boolean isDisconnected();
    void setState(BleDeviceState state);
    void updateState();
    int getBattery();
    void setBattery(final int battery);
    void addListener(OnDeviceListener listener);
    void removeListener(OnDeviceListener listener);

    void open(Context context); // 打开设备
    void switchState(); // 切换状态
    void callDisconnect(boolean stopAutoScan); // 请求断开
    void disconnect();
    boolean isStopped(); // 是否停止
    void close(); // 关闭设备
    void clear(); // 清除设备

    void setCallback(AbstractDevice.MyCallback myCallback);
/*
    boolean executeAfterConnectSuccess(); // 连接成功后执行的操作
    void executeAfterConnectFailure(); // 连接错误后执行的操作
    void executeAfterDisconnect(); // 断开连接后执行的操作*/
}
