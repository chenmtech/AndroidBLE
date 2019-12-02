package com.cmtech.android.ble.core;

import android.content.Context;

public interface IDeviceConnector {
    void open(Context context); // 打开设备
    void switchState(); // 切换状态
    void forceDisconnect(boolean forever); // 强制断开
    void close(); // 关闭设备
    void clear(); // 清除设备
    BleDeviceState getState(); // 获取状态
    void setState(BleDeviceState state); // 设置状态
    boolean isConnected(); // 是否已连接
    boolean isDisconnected(); // 是否已断开
    boolean isDisconnectedForever(); // 是否永远断开
    boolean isLocal(); // 是否本地连接
}
