package com.cmtech.android.ble.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.cmtech.android.ble.callback.ScanCallback;
import com.cmtech.android.ble.core.ViseBle;

/**
 * @Description: 蓝牙基础操作工具类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/8/5 20:43.
 */
public class BleUtil {
    public static void enableBluetooth(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    public static boolean isSupportBle(Context context) {
        if (context == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager.getAdapter() != null;
    }

    public static boolean isBleEnable(Context context) {
        if (!isSupportBle(context)) {
            return false;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return manager.getAdapter().isEnabled();
    }

    // 开始扫描
    public static void startScan(ScanCallback scanCallback) {
        ViseBle.getInstance().startScan(scanCallback);
    }

    // 停止扫描
    public static void stopScan(ScanCallback scanCallback) {
        ViseBle.getInstance().stopScan(scanCallback);
    }

    // 断开所有设备连接
    public static void disconnectAllDevice() {
        ViseBle.getInstance().disconnect();
    }

    // 清除所有设备资源
    public static void clearAllDevice() {
        ViseBle.getInstance().clear();
    }

}
