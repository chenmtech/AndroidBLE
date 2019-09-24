package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.extend.BleDeviceDetailInfo;

/**
 *
 * ClassName:      IBleScanCallback
 * Description:    扫描回调接口
 * Author:         chenm
 * CreateDate:     2019-09-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-09-19 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public interface IBleScanCallback {
    int SCAN_FAILED_ALREADY_STARTED = 1; // 已经在扫描

    int SCAN_FAILED_BLE_DISABLE = 2; // 蓝牙未开启

    int SCAN_FAILED_BLE_INNER_ERROR = 3; // 蓝牙内部错误

    // 发现设备
    void onDeviceFound(BleDeviceDetailInfo bleDeviceDetailInfo);

    // 扫描失败
    void onScanFailed(int errorCode);
}
