package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;

import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.callback.scan.ScanCallback;
import com.cmtech.android.ble.callback.scan.SingleFilterScanCallback;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SCANNING;

class BleDeviceScanCommand extends BleDeviceConnectRelatedCommand {

    private class MyScanCallback implements IScanCallback {
        MyScanCallback() {
        }

        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                processScanResult(false, null);

            } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                processScanResult(true, bluetoothLeDevice);

            }
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        @Override
        public void onScanTimeout() {
            processScanResult(false, null);
        }

    }

    private ScanCallback scanCallback;

    BleDeviceScanCommand(BleDevice device) {
        super(device);
    }

    synchronized void stop() {
        if(scanCallback != null && scanCallback.isScanning()) {
            scanCallback.removeHandlerMsg();

            scanCallback.setScan(false).scan();
        }

        waitingResponse = false;
    }

    @Override
    synchronized void execute() {
        scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(device.getMacAddress());

        scanCallback.setScan(true).scan();

        device.setConnectState(CONNECT_SCANNING);

        waitingResponse = true;
    }

    // 处理扫描结果
    private synchronized void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.e("ProcessScanResult: " + canConnect);

        if (canConnect) {
            device.setBluetoothLeDevice(bluetoothLeDevice);

            device.startConnect(); // 扫描成功，启动连接
        } else {
            device.reconnect();
        }

        waitingResponse = false;
    }
}
