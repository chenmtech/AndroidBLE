package com.cmtech.android.ble.callback.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.cmtech.android.ble.common.BleConfig;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public class ScanCallback implements BluetoothAdapter.LeScanCallback, IScanFilter {
    protected Handler handler = new Handler(Looper.myLooper());

    protected boolean isScan = true;//是否开始扫描

    protected boolean isScanning = false;//是否正在扫描

    protected BluetoothLeDeviceStore bluetoothLeDeviceStore;//用来存储扫描到的设备

    protected IScanCallback scanCallback;//扫描结果回调

    public ScanCallback(IScanCallback scanCallback) {
        this.scanCallback = scanCallback;

        if (scanCallback == null) {
            throw new NullPointerException("this scanCallback is null!");
        }

        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
    }

    public ScanCallback setScan(boolean scan) {
        isScan = scan;

        return this;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void scan() {
        BluetoothAdapter adapter = null;

        if (isScan) {
            if (isScanning) {
                return;
            }

            bluetoothLeDeviceStore.clear();

            if (BleConfig.getInstance().getScanTimeout() > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScanning = false;

                        if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                            ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
                        }

                        if (bluetoothLeDeviceStore.getDeviceMap() != null
                                && bluetoothLeDeviceStore.getDeviceMap().size() > 0) {
                            scanCallback.onScanFinish(bluetoothLeDeviceStore);
                        } else {
                            scanCallback.onScanTimeout();
                        }
                    }
                }, BleConfig.getInstance().getScanTimeout());
            }

            isScanning = true;

            adapter = ViseBle.getInstance().getBluetoothAdapter();

            if (adapter != null) {
                adapter.startLeScan(ScanCallback.this);
            }
        } else {
            isScanning = false;

            adapter = ViseBle.getInstance().getBluetoothAdapter();

            if (adapter != null) {
                adapter.stopLeScan(ScanCallback.this);
            }
        }
    }

    public ScanCallback removeHandlerMsg() {
        handler.removeCallbacksAndMessages(null);

        bluetoothLeDeviceStore.clear();

        return this;
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis());

        BluetoothLeDevice filterDevice = onFilter(bluetoothLeDevice);

        if (filterDevice != null) {
            bluetoothLeDeviceStore.addDevice(filterDevice);

            scanCallback.onDeviceFound(filterDevice);
        }
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        return bluetoothLeDevice;
    }
}
