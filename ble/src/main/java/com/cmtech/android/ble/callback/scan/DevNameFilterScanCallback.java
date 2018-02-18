package com.cmtech.android.ble.callback.scan;

import com.cmtech.android.ble.model.BluetoothLeDevice;

/**
 * Created by bme on 2018/2/19.
 */

public class DevNameFilterScanCallback extends NoFilterScanCallback {
    private String deviceName;//指定设备名称

    public DevNameFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public NoFilterScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null
                && deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
            return bluetoothLeDevice;
        } else
            return null;
    }
}
