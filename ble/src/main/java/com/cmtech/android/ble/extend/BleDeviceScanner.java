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

/**
 *
 * ClassName:      BleDeviceScanner
 * Description:    低功耗蓝牙扫描仪类
 * Author:         chenm
 * CreateDate:     2019-09-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-09-19 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public class BleDeviceScanner {
    private final Context context;

    private ScanFilter scanFilter; // 扫描过滤器

    private IBleScanCallback bleScanCallback; // 扫描回调

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            byte[] recordBytes = (result.getScanRecord() == null) ? null : result.getScanRecord().getBytes();

            BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(result.getDevice(), result.getRssi(), recordBytes, result.getTimestampNanos());

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


    public BleDeviceScanner(Context context) {
        this.context = context;
    }

    public BleDeviceScanner(Context context, ScanFilter scanFilter) {
        this(context);

        this.scanFilter = scanFilter;
    }

    // 设备过滤器
    public BleDeviceScanner setScanFilter(ScanFilter scanFilter) {
        this.scanFilter = scanFilter;

        return this;
    }

    // 开始扫描
    public void startScan(IBleScanCallback bleScanCallback) {
        this.bleScanCallback = bleScanCallback;

        BluetoothAdapter adapter = getBluetoothAdapter();

        if (adapter != null) {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);

            ViseLog.e("Start scanning");
        }
    }

    // 停止扫描
    public void stopScan() {
        BluetoothAdapter adapter = getBluetoothAdapter();

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            scanner.stopScan(scanCallback);

            ViseLog.e("Scan stopped");
        }
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        return (bluetoothManager == null) ? null : bluetoothManager.getAdapter();
    }

}
