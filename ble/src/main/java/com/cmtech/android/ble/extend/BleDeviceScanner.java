package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import com.cmtech.android.ble.callback.IBleScanCallback;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

import java.util.Collections;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class BleDeviceScanner {
    private ScanFilter scanFilter;

    private IBleScanCallback bleScanCallback;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothLeDevice bluetoothLeDevice;

            if(result.getScanRecord() == null) {
                bluetoothLeDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), null, result.getTimestampNanos());
            } else {
                bluetoothLeDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes(), result.getTimestampNanos());
            }

            if(bleScanCallback != null)
                bleScanCallback.onDeviceFound(bluetoothLeDevice);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            if(bleScanCallback != null)
                bleScanCallback.onScanFailed(errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            ViseLog.e("Batch scan result");
        }
    };


    public BleDeviceScanner() {
    }

    public BleDeviceScanner setScanFilter(ScanFilter scanFilter) {
        this.scanFilter = scanFilter;

        return this;
    }

    private static BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if(bluetoothManager == null) {
            return null;
        }

        return bluetoothManager.getAdapter();
    }

    public void startScan(Context context, IBleScanCallback bleScanCallback) {
        this.bleScanCallback = bleScanCallback;

        BluetoothAdapter adapter = getBluetoothAdapter(context);

        if (adapter != null) {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);

            ViseLog.e("Start scanning");
        }

    }

    public void stopScan(Context context) {
        BluetoothAdapter adapter = getBluetoothAdapter(context);

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.stopScan(scanCallback);

            ViseLog.e("Stop to scan");
        }
    }
}
