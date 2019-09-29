package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.core.BleDeviceGatt;
import com.cmtech.android.ble.exception.BleException;

/**
 *
 * ClassName:      IBleConnectCallback
 * Description:    连接回调接口
 * Author:         chenm
 * CreateDate:     2019-09-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-09-19 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public interface IBleConnectCallback {
    //连接成功
    void onConnectSuccess(BleDeviceGatt bleDeviceGatt);

    //连接失败
    void onConnectFailure(BleException exception);

    //连接断开
    void onDisconnect();

}
