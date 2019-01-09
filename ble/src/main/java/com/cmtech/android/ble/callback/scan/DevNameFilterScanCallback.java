package com.cmtech.android.ble.callback.scan;

import com.cmtech.android.ble.model.BluetoothLeDevice;

/**
 *  DevNameFilterScanCallback: 用指定的设备名作为过滤规则的扫描回调适配器
 * Created by bme on 2018/2/19.
 */

public class DevNameFilterScanCallback extends ScanCallback {
    private String deviceName;//指定设备名称

    public DevNameFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public ScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null) {
            if(deviceName.equals("") || deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
                return bluetoothLeDevice;
            }
        }
        return null;
    }
}
