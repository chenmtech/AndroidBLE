package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import com.cmtech.android.ble.callback.IBleScanCallback;
import com.cmtech.android.ble.model.BleDeviceDetailInfo;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
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
    private ScanFilter scanFilter; // 扫描过滤器

    private IBleScanCallback bleScanCallback; // 扫描回调

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            byte[] recordBytes = (result.getScanRecord() == null) ? null : result.getScanRecord().getBytes();

            BleDeviceDetailInfo bleDeviceDetailInfo = new BleDeviceDetailInfo(result.getDevice(), result.getRssi(), recordBytes, result.getTimestampNanos());

            if(bleScanCallback != null)
                bleScanCallback.onDeviceFound(bleDeviceDetailInfo);
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

    public BleDeviceScanner(ScanFilter scanFilter) {
        this();

        this.scanFilter = scanFilter;
    }

    // 设备过滤器
    public BleDeviceScanner setScanFilter(ScanFilter scanFilter) {
        this.scanFilter = scanFilter;

        return this;
    }

    // 开始扫描
    public boolean startScan(IBleScanCallback bleScanCallback) {
        this.bleScanCallback = bleScanCallback;

        BluetoothAdapter adapter = getBluetoothAdapter();

        if (adapter != null) {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            if(scanner != null) {
                scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);

                ViseLog.e("Start scanning");

                return true;
            }
        }

        return false;
    }

    // 停止扫描
    public void stopScan() {
        BluetoothAdapter adapter = getBluetoothAdapter();

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            if(scanner != null) {
                scanner.stopScan(scanCallback);

                ViseLog.e("Scan stopped");
            }
        }
    }

    private static BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static void cleanup() {
        BluetoothLeScanner scanner = getBluetoothAdapter().getBluetoothLeScanner();

        try {
            final Method cleanup = BluetoothLeScanner.class.getMethod("cleanup");
            if (scanner != null) {
                cleanup.invoke(scanner);
            }
        } catch (Exception e) {
            ViseLog.e("An exception occured while refreshing device" + e);
        }
    }

}
