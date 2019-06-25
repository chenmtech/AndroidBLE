package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.model.BluetoothLeDevice;

/**
 * IGattDataCallback: Gatt数据操作回调接口
 * Created by bme on 2018/3/1.
 */

public interface IGattDataCallback {
    void onSuccess(byte[] data); // 数据操作成功

    void onFailure(GattDataException exception); // 数据操作失败
}
