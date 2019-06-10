package com.cmtech.android.ble.extend;

import android.app.Activity;
import android.content.Context;

import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.callback.scan.ScanCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.BleUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * BleDeviceUtil：Ble设备帮助类
 * Created by bme on 2018/10/13.
 */

public class BleDeviceUtil {
    // 使能蓝牙
    public static void enableBluetooth(Activity activity, int requestCode) {
        BleUtil.enableBluetooth(activity, requestCode);
    }

    // 是否支持BLE
    public static boolean isSupportBle(Context context) {
        return BleUtil.isSupportBle(context);
    }

    // 是否使能BLE
    public static boolean isBleEnable(Context context) {
        return BleUtil.isBleEnable(context);
    }

    // 开始扫描
    public static void startScan(ScanCallback scanCallback) {
        ViseBle.getInstance().startScan(scanCallback);
    }

    // 停止扫描
    public static void stopScan(ScanCallback scanCallback) {
        ViseBle.getInstance().stopScan(scanCallback);
    }

    // 连接设备
    public static void connect(BluetoothLeDevice bluetoothLeDevice, IConnectCallback connectCallback) {
        ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);
    }

    // 断开设备连接
    public static void disconnect(BluetoothLeDevice bluetoothLeDevice) {
        ViseBle.getInstance().getDeviceMirrorPool().disconnect(bluetoothLeDevice);
    }

    // 移除设备镜像
    public static void removeDeviceMirror(DeviceMirror deviceMirror) {
        ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(deviceMirror);
    }

    // 获取BluetoothLeDevice清单
    public static List<BluetoothLeDevice> getDeviceList() {
        return ViseBle.getInstance().getDeviceMirrorPool().getDeviceList();
    }

    // 获取BleDevice对应的设备镜像
    public static DeviceMirror getDeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        return ViseBle.getInstance().getDeviceMirrorPool().getDeviceMirror(bluetoothLeDevice);
    }

    // 断开所有设备连接
    public static void disconnectAllDevice() {
        ViseBle.getInstance().disconnect();
    }

    // 清除所有设备资源
    public static void clearAllDevice() {
        ViseBle.getInstance().clear();
    }

    // 列出目录中的所有.bme文件
    public static File[] listDirBmeFiles(File fileDir) {
        if(fileDir == null || !fileDir.exists()) return null;

        return fileDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".bme");
            }
        });
    }

    // 获取BleDevice设备上element对应的Gatt Object
    public static Object getGattObject(BleDevice device, BleGattElement element) {
        return (element == null) ? null : element.retrieveGattObject(device);
    }
}
