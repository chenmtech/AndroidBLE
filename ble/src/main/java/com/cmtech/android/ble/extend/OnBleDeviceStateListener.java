package com.cmtech.android.ble.extend;


/**
 *
 * ClassName:      OnBleDeviceStateListener
 * Description:    Ble设备状态监听器
 * Author:         chenm
 * CreateDate:     2018-03-12 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-05 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public interface OnBleDeviceStateListener {
    void onConnectStateUpdated(final BleDevice device); // 连接状态更新

    void onBleErrorNotified(final BleDevice device, boolean warn); // 发生BLE错误通知

    void onBatteryUpdated(final BleDevice device); // 电池电量更新

}
