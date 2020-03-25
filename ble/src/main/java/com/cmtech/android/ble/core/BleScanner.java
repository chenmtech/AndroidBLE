package com.cmtech.android.ble.core;

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
import static com.cmtech.android.ble.callback.IBleScanCallback.CODE_BLE_CLOSED;
import static com.cmtech.android.ble.callback.IBleScanCallback.CODE_BLE_INNER_ERROR;

/**
 *
 * ClassName:      BleScanner
 * Description:    低功耗蓝牙扫描仪类
 * Author:         chenm
 * CreateDate:     2019-09-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-09-19 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public class BleScanner {
    private static final List<ScanCallbackAdapter> callbackList = new ArrayList<>(); // 所有扫描的回调
    private static volatile boolean bleInnerError = false; // 是否发生蓝牙内部错误，比如由于频繁扫描引起的错误

    // 开始扫描
    public static void startScan(ScanFilter scanFilter, final IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new NullPointerException("The IBleScanCallback is null");
        }

        ScanCallbackAdapter scanCallback = null;
        synchronized (BleScanner.class) {
            if (BleScanner.isBleDisabled()) {
                bleScanCallback.onScanFailed(CODE_BLE_CLOSED);
                return;
            }
            if (bleInnerError) {
                bleScanCallback.onScanFailed(CODE_BLE_INNER_ERROR);
                return;
            }
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
                bleScanCallback.onScanFailed(IBleScanCallback.CODE_ALREADY_STARTED);
                return;
            }
        }
        BluetoothLeScanner scanner;
        scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if(scanFilter == null) {
            scanner.startScan(scanCallback);
        } else {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
                    .setScanMode(SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L);
            scanner.startScan(Collections.singletonList(scanFilter), settingsBuilder.build(), scanCallback);
        }
    }

    // 停止扫描
    public static void stopScan(IBleScanCallback bleScanCallback) {
        if(bleScanCallback == null) {
            throw new NullPointerException("The IBleScanCallback is null.");
        }

        ScanCallbackAdapter scanCallback = null;
        synchronized (BleScanner.class) {
            if(isBleDisabled()) {
                return;
            }
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
        if(scanCallback != null) {
            BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            scanner.stopScan(scanCallback);
        }

        ViseLog.e("Scan stopped");
    }

    // 蓝牙是否关闭
    public static boolean isBleDisabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return (adapter == null || !adapter.isEnabled() || adapter.getBluetoothLeScanner() == null);
    }

    // 清除BLE内部错误标志
    public static void clearInnerError() {
        bleInnerError = false;
    }

    private static class ScanCallbackAdapter extends ScanCallback {
        private IBleScanCallback bleScanCallback;

        ScanCallbackAdapter(IBleScanCallback bleScanCallback) {
            if(bleScanCallback == null) {
                throw new IllegalArgumentException("The IBleScanCallback is null.");
            }
            this.bleScanCallback = bleScanCallback;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if(bleScanCallback != null) {
                byte[] recordBytes = (result.getScanRecord() == null) ? null : result.getScanRecord().getBytes();
                BleDeviceDetailInfo bleDeviceDetailInfo = new BleDeviceDetailInfo(result.getDevice(), result.getRssi(), recordBytes, result.getTimestampNanos());
                bleScanCallback.onDeviceFound(bleDeviceDetailInfo);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            bleInnerError = true;
            if(bleScanCallback != null)
                bleScanCallback.onScanFailed(CODE_BLE_INNER_ERROR);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            ViseLog.e("Batch scan result. Cannot be here.");
        }
    }

}
