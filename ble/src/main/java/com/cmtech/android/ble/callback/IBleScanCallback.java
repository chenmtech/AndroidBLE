package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.core.BleDeviceDetailInfo;

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
    int CODE_ALREADY_STARTED = 1; // 扫描已经开始
    int CODE_BLE_CLOSED = 2; // 蓝牙已关闭
    int CODE_BLE_INNER_ERROR = 3; // 蓝牙内部错误

    void onDeviceFound(BleDeviceDetailInfo bleDeviceDetailInfo); // 发现设备
    void onScanFailed(int errorCode); // 扫描失败
}
