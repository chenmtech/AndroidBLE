package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;

/**
 * MyScanCallback: 我的扫描回调类，负责扫描回调的处理
 * Created by bme on 2018/12/23.
 */

public class MyScanCallback implements IScanCallback {
    private final BleDevice device;

    MyScanCallback(BleDevice device) {
        this.device = device;
    }

    @Override
    public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

        if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            Toast.makeText(device.getContext(), "该设备未绑定，无法使用。", Toast.LENGTH_SHORT).show();

            device.processScanResult(false, null);

        } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            device.processScanResult(true, bluetoothLeDevice);

        }
    }

    @Override
    public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

    }

    @Override
    public void onScanTimeout() {
        device.processScanResult(false, null);
    }

}
