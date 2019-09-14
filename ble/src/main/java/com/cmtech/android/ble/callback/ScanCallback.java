package com.cmtech.android.ble.callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

import java.util.Collections;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public abstract class ScanCallback extends android.bluetooth.le.ScanCallback {
    private ScanFilter scanFilter;

    public ScanCallback() {
    }

    public ScanCallback setScanFilter(ScanFilter scanFilter) {
        this.scanFilter = scanFilter;

        return this;
    }

    public void startScan() {
        BluetoothAdapter adapter = ViseBle.getInstance().getBluetoothAdapter();

        if (adapter != null) {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), ScanCallback.this);

            ViseLog.e("Start scanning");
        }

    }

    public void stopScan() {
        BluetoothAdapter adapter = ViseBle.getInstance().getBluetoothAdapter();

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.stopScan(ScanCallback.this);

            ViseLog.e("Stop scan");
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes(), result.getTimestampNanos());

        ViseLog.e("Found device: device address = " + bluetoothLeDevice.getAddress());

        onScanFinish(bluetoothLeDevice);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);

        ViseLog.e("Scan Failed: errorCode = " + errorCode);

        onScanFinish(null);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);

        ViseLog.e("Batch scan result");
    }

    public abstract void onScanFinish(BluetoothLeDevice bluetoothLeDevice);
}
