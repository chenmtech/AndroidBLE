package com.cmtech.android.ble.extend;

/**
 * IGattDataOpCallback: Gatt数据操作回调接口
 * Created by bme on 2018/3/1.
 */

public interface IGattDataOpCallback {
    void onSuccess(byte[] data); // 数据操作成功

    void onFailure(GattDataOpException exception); // 数据操作失败
}
