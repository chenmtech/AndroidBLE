package com.cmtech.android.ble.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.widget.Toast;

import com.cmtech.android.ble.common.BleConfig;

/**
 * @Description: BLE设备操作入口
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:24.
 */
public class ViseBle {
    private Context context;//上下文
    private BluetoothManager bluetoothManager;//蓝牙管理
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private DeviceMirror lastDeviceMirror;//上次操作设备镜像

    private static ViseBle instance;//入口操作管理
    private static BleConfig bleConfig = BleConfig.getInstance();

    /**
     * 单例方式获取蓝牙通信入口
     *
     * @return 返回ViseBluetooth
     */
    public static ViseBle getInstance() {
        if (instance == null) {
            synchronized (ViseBle.class) {
                if (instance == null) {
                    instance = new ViseBle();
                }
            }
        }
        return instance;
    }

    private ViseBle() {
    }

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void init(Context context) {
        if (this.context == null && context != null) {
            this.context = context.getApplicationContext();
            bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager == null) {
                throw new IllegalStateException();
            }
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }


    /**
     * 获取蓝牙适配器
     *
     * @return 返回蓝牙适配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        if(bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);

            if(bluetoothManager == null) {
                Toast.makeText(context, "bluetoothManager is null", Toast.LENGTH_LONG).show();
                throw new IllegalStateException();
            }

            return bluetoothManager.getAdapter();
        } else
            return bluetoothAdapter;
    }



}
