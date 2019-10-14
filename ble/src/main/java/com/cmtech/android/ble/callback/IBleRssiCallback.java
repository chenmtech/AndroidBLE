package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.exception.BleException;

/**
 *
 * ClassName:      IBleRssiCallback
 * Description:    读取RSSI信号回调接口
 * Author:         chenm
 * CreateDate:     2019-09-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-09-19 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public interface IBleRssiCallback {
    void onSuccess(int rssi); // 读取成功
    void onFailure(BleException exception); // 读取失败
}
