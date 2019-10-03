package com.cmtech.android.ble.core;


/**
 *
 * ClassName:      OnBleDeviceUpdatedListener
 * Description:    Ble设备更新监听器
 * Author:         chenm
 * CreateDate:     2018-03-12 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-05 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public interface OnBleDeviceUpdatedListener {
    void onConnectStateUpdated(final BleDevice device); // 连接状态更新
    void onBleErrorNotified(final BleDevice device, boolean warn); // BLE错误通知
    void onBatteryUpdated(final BleDevice device); // 电池电量更新
}
