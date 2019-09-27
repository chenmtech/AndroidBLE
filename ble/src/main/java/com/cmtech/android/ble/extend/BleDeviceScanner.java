package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import com.cmtech.android.ble.callback.IBleScanCallback;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static com.cmtech.android.ble.callback.IBleScanCallback.SCAN_FAILED_BLE_DISABLE;
import static com.cmtech.android.ble.callback.IBleScanCallback.SCAN_FAILED_BLE_INNER_ERROR;

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
    private static final List<ScanCallbackAdapter> callbackList = new ArrayList<>(); // 所有BLE扫描回调

    private static volatile boolean bleInnerError = false; // 蓝牙内部错误，比如由于频繁扫描引起的错误

    private static int scanTimes = 0;

    private BleDeviceScanner() {

    }

    // 开始扫描
    public static void startScan(ScanFilter scanFilter, final IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new IllegalArgumentException("IBleScanCallback is null");
        }

        BluetoothLeScanner scanner;
        ScanCallbackAdapter scanCallback = null;

        synchronized (BleDeviceScanner.class) {
            if (BleDeviceScanner.isBleDisabled()) {
                bleScanCallback.onScanFailed(SCAN_FAILED_BLE_DISABLE);
                return;
            }

            if (bleInnerError) {
                bleScanCallback.onScanFailed(SCAN_FAILED_BLE_INNER_ERROR);
                return;
            }

            scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

            for (ScanCallbackAdapter callback : callbackList) {
                if (callback.bleScanCallback == bleScanCallback) {
                    scanCallback = callback;
                    break;
                }
            }

            if (scanCallback == null) {
                scanCallback = new ScanCallbackAdapter(bleScanCallback);

                callbackList.add(scanCallback);
            } else {
                bleScanCallback.onScanFailed(IBleScanCallback.SCAN_FAILED_ALREADY_STARTED);
                return;
            }
        }

        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

        scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);

        scanTimes++;

        ViseLog.e("Start scanning, scanTimes = " + scanTimes);
    }

    // 停止扫描
    public static void stopScan(IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new IllegalArgumentException("IBleScanCallback is null.");
        }

        BluetoothLeScanner scanner;
        ScanCallbackAdapter scanCallback = null;
        synchronized (BleDeviceScanner.class) {
            if(isBleDisabled()) {
                return;
            }
            scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            for(ScanCallbackAdapter callback : callbackList) {
                if(callback.bleScanCallback == bleScanCallback) {
                    scanCallback = callback;
                    break;
                }
            }
            if(scanCallback != null) {
                callbackList.remove(scanCallback);
            }
        }
        if(scanCallback != null)
            scanner.stopScan(scanCallback);

        ViseLog.e("Scan stopped");
    }

    // 蓝牙是否已开启
    public static boolean isBleDisabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return (adapter == null || !adapter.isEnabled() || adapter.getBluetoothLeScanner() == null);
    }

    public static void clearInnerError() {
        bleInnerError = false;
        scanTimes = 0;
    }

    private static class ScanCallbackAdapter extends ScanCallback {
        private IBleScanCallback bleScanCallback;

        ScanCallbackAdapter(IBleScanCallback bleScanCallback) {
            if(bleScanCallback == null) {
                throw new IllegalArgumentException("IBleScanCallback can't be null.");
            }

            this.bleScanCallback = bleScanCallback;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            byte[] recordBytes = (result.getScanRecord() == null) ? null : result.getScanRecord().getBytes();

            BleDeviceDetailInfo bleDeviceDetailInfo = new BleDeviceDetailInfo(result.getDevice(), result.getRssi(), recordBytes, result.getTimestampNanos());

            if(bleScanCallback != null) {
                bleScanCallback.onDeviceFound(bleDeviceDetailInfo);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            bleInnerError = true;

            if(bleScanCallback != null)
                bleScanCallback.onScanFailed(SCAN_FAILED_BLE_INNER_ERROR);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            ViseLog.e("Batch scan result");
        }
    }

}
