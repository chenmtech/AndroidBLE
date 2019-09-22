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

import java.util.ArrayList;
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
    private static final List<ScanCallbackAdapter> callbackList = new ArrayList<>(); // 所有BLE扫描回调


    private BleDeviceScanner() {

    }

    // 开始扫描
    public static boolean startScan(ScanFilter scanFilter, IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new IllegalArgumentException("IBleScanCallback can't be null");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            if(scanner != null) {
                ScanCallbackAdapter scanCallback = null;

                for(ScanCallbackAdapter callback : callbackList) {
                    if(callback.bleScanCallback == bleScanCallback) {
                        scanCallback = callback;
                        break;
                    }
                }

                if(scanCallback == null) {
                    scanCallback = new ScanCallbackAdapter(bleScanCallback);

                    callbackList.add(scanCallback);
                }

                ScanSettings.Builder settingsBuilder = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY);

                scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);

                ViseLog.e("Start scanning");

                return true;
            }
        }

        return false;
    }

    // 停止扫描
    public static boolean stopScan(IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new IllegalArgumentException("IBleScanCallback can't be null.");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

            if(scanner != null) {
                ScanCallbackAdapter scanCallback = null;

                for(ScanCallbackAdapter callback : callbackList) {
                    if(callback.bleScanCallback == bleScanCallback) {
                        scanCallback = callback;
                        break;
                    }
                }

                if(scanCallback != null) {
                    callbackList.remove(scanCallback);

                    scanner.stopScan(scanCallback);

                    ViseLog.e("Scan stopped");

                    return true;
                }
            }
        }

        return false;
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
    }

}
