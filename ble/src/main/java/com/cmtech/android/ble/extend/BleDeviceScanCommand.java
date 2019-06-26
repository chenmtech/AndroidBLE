package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.callback.scan.ScanCallback;
import com.cmtech.android.ble.callback.scan.SingleFilterScanCallback;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_FAILURE;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SCANNING;

/**
 *
 * ClassName:      BleDeviceScanCommand
 * Description:    表示扫描BLE设备命令
 * Author:         chenm
 * CreateDate:     2019-06-25 17:54
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-25 17:54
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

class BleDeviceScanCommand extends BleDeviceCommand {

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

    void stop(Handler handler) {
        if(handler == null)
            throw new NullPointerException();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if(scanCallback != null && scanCallback.isScanning()) {
                    waitingResponse = true;

                    scanCallback.removeHandlerMsg();

                    scanCallback.setScan(false).scan();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                waitingResponse = false;
            }
        });
    }

    @Override
    void execute(Handler handler) {
        if(handler == null)
            throw new NullPointerException();

        handler.post(new Runnable() {
            @Override
            public void run() {
                scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(device.getMacAddress());

                scanCallback.setScan(true).scan();

                device.setConnectState(CONNECT_SCANNING);

                addReconnectTimes();

                waitingResponse = true;
            }
        });
    }

    // 处理扫描结果
    private void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.e("ProcessScanResult: " + canConnect);

        if (canConnect) {
            device.setBluetoothLeDevice(bluetoothLeDevice);

            device.startConnect(); // 扫描成功，启动连接
        } else {
            device.setConnectState(CONNECT_FAILURE);

            reconnect();
        }

        waitingResponse = false;
    }
}
