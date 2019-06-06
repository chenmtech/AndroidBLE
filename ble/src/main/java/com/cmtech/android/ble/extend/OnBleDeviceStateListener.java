package com.cmtech.android.ble.extend;

/**
 * OnBleDeviceStateListener: 设备状态观察者接口
 * Created by bme on 2018/3/12.
 */

public interface OnBleDeviceStateListener {
    void onDeviceConnectStateUpdated(final BleDevice device);
    void onReconnectFailureNotify(final BleDevice device, boolean warn);
    void onDeviceBatteryUpdated(final BleDevice device);

}
