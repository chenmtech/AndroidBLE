package com.cmtech.android.ble.callback.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;

import com.cmtech.android.ble.common.BleConfig;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

import java.util.Collections;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public class ScanCallback extends android.bluetooth.le.ScanCallback implements IScanFilter {
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
                            BluetoothLeScanner scanner = ViseBle.getInstance().getBluetoothAdapter().getBluetoothLeScanner();
                            scanner.stopScan(ScanCallback.this);
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
                ScanFilter.Builder filterBuilder = new ScanFilter.Builder();

                filterBuilder.setDeviceName("CM1.0");

                ScanFilter filter = filterBuilder.build();

                ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

                BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

                scanner.startScan(Collections.singletonList(filter), settingsBuilder.build(), ScanCallback.this);

                ViseLog.e("start scann");
            }
        } else {
            isScanning = false;

            adapter = ViseBle.getInstance().getBluetoothAdapter();

            if (adapter != null) {
                BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

                scanner.stopScan(ScanCallback.this);

                ViseLog.e("stop scan");
            }
        }
    }

    public ScanCallback removeHandlerMsg() {
        handler.removeCallbacksAndMessages(null);

        bluetoothLeDeviceStore.clear();

        return this;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes(), result.getTimestampNanos());

        BluetoothLeDevice filterDevice = onFilter(bluetoothLeDevice);

        if (filterDevice != null) {
            ViseLog.e("found device");

            bluetoothLeDeviceStore.addDevice(filterDevice);

            scanCallback.onDeviceFound(filterDevice);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);

        scanCallback.onScanTimeout();
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);

        ViseLog.e("batch result");
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        return bluetoothLeDevice;
    }
}
