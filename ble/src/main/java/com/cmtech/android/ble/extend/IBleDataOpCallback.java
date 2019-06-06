package com.cmtech.android.ble.extend;

/**
 * IBleDataOpCallback: Ble数据操作回调接口
 * Created by bme on 2018/3/1.
 */

public interface IBleDataOpCallback {
    // 数据操作成功
    void onSuccess(byte[] data);
    // 数据操作失败
    void onFailure(BleDataOpException exception);
}
