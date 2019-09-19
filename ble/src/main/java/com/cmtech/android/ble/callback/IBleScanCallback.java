package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.model.BluetoothLeDevice;

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
    // 发现设备
    void onDeviceFound(BluetoothLeDevice bluetoothLeDevice);

    // 扫描失败
    void onScanFailed(int errorCode);
}
