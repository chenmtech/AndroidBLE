package com.cmtech.android.ble.extend;

/**
 * OnBleDeviceListener: Ble设备监听器接口
 * Created by bme on 2018/3/12.
 */

public interface OnBleDeviceListener {
    void onConnectStateUpdated(final BleDevice device); // 连接状态改变

    void onReconnectFailureNotified(final BleDevice device, boolean warn); // 重连失败通知

    void onBatteryUpdated(final BleDevice device); // 电池电量改变

}
