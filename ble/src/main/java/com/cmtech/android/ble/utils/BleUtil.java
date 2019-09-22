package com.cmtech.android.ble.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

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

    public static boolean isBleEnable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(adapter != null) {
            return adapter.isEnabled();
        }

        return false;
    }



}
