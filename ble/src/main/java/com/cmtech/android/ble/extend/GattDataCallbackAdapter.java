package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;

/**
 * GattDataCallbackAdapter: Ble数据操作回调适配器，将IGattDataOpCallback适配为ViseBle包中的IBleCallback
 * Created by bme on 2018/3/1.
 */

class GattDataCallbackAdapter implements IBleCallback {
    private IGattDataCallback dataOpCallback;

    GattDataCallbackAdapter(IGattDataCallback gattDataCallback) {
        if(gattDataCallback == null) {
            throw new IllegalArgumentException();
        }

        this.dataOpCallback = gattDataCallback;
    }

    @Override
    public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
        dataOpCallback.onSuccess(data, bluetoothLeDevice);
    }

    @Override
    public void onFailure(BleException exception) {
        dataOpCallback.onFailure(new GattDataException(exception));
    }
}
