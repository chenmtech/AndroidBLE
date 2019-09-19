package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.model.BluetoothLeDevice;

public interface IBleScanCallback {
    void onDeviceFound(BluetoothLeDevice bluetoothLeDevice);

    void onScanFailed(int errorCode);
}
