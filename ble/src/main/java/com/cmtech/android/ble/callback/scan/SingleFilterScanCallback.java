package com.cmtech.android.ble.callback.scan;

import com.cmtech.android.ble.ViseBle;
import com.cmtech.android.ble.model.BluetoothLeDevice;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 设置扫描指定的单个设备，一般是设备名称和Mac地址
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/12 22:16.
 */
public class SingleFilterScanCallback extends FilterScanCallback {
    private AtomicBoolean hasFound = new AtomicBoolean(false);
    private String deviceName;//指定设备名称
    private String deviceMac;//指定设备Mac地址

    public SingleFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public FilterScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public FilterScanCallback setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        if (!hasFound.get()) {
            if (bluetoothLeDevice != null && bluetoothLeDevice.getAddress() != null && deviceMac != null
                    && deviceMac.equalsIgnoreCase(bluetoothLeDevice.getAddress().trim())) {
                hasFound.set(true);
                isScanning = false;
                removeHandlerMsg();
                ViseBle.getInstance().stopScan(SingleFilterScanCallback.this);
                tempDevice = bluetoothLeDevice;
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
                scanCallback.onScanFinish(bluetoothLeDeviceStore);
            } else if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null
                    && deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
                hasFound.set(true);
                isScanning = false;
                removeHandlerMsg();
                ViseBle.getInstance().stopScan(SingleFilterScanCallback.this);
                tempDevice = bluetoothLeDevice;
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
                scanCallback.onScanFinish(bluetoothLeDeviceStore);
            }
        }
        return tempDevice;
    }
}
