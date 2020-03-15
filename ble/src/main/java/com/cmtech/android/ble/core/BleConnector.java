package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.exception.BleException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.DeviceState.CLOSED;
import static com.cmtech.android.ble.core.DeviceState.CONNECT;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.DeviceState.FAILURE;

/**
 * ClassName:      BleConnector
 * Description:    低功耗蓝牙设备类
 * Author:         chenm
 * CreateDate:     2018-02-19 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-05 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 * UpdateUser:     chenm
 * UpdateDate:     2019-10-21 07:02
 * UpdateRemark:   优化代码，
 * Version:        1.1
 */

public class BleConnector extends AbstractConnector {
    private BleGatt bleGatt; // Gatt，连接成功后赋值，完成连接状态改变处理以及数据通信功能
    private BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。连接成功后启动，连接失败或者断开时停止

    // connection callback
    private final IBleConnectCallback connectCallback = new IBleConnectCallback() {
        // connection success
        @Override
        public void onConnectSuccess(final BleGatt bleGatt) {
            // 防止重复连接成功
            if (state != CONNECT) {
                if (state == CLOSED) { // 设备已经关闭了，强行清除
                    bleGatt.close();
                    return;
                }

                ViseLog.e("Connect success: " + bleGatt);

                BleConnector.this.bleGatt = bleGatt;
                gattCmdExecutor.start();

                if (!connCallback.onConnectSuccess()) {
                    disconnect(false);
                } else {
                    setState(CONNECT);
                }
            }
        }

        // connection failure
        @Override
        public void onConnectFailure(final BleException exception) {
            if (state != FAILURE) {
                ViseLog.e("Connect failure: " + exception);

                bleGatt = null;
                gattCmdExecutor.stop();
                setState(FAILURE);
                reconnect();
                connCallback.onConnectFailure();
            }
        }

        // disconnection
        @Override
        public void onDisconnect() {
            if (state != DISCONNECT) {
                ViseLog.e("Disconnect.");

                bleGatt = null;
                gattCmdExecutor.stop();
                setState(DISCONNECT);
                reconnect();
                connCallback.onDisconnect();
            }
        }
    };

    public BleConnector(String address, IConnectorCallback connectorCallback) {
        super(address, connectorCallback);
    }

    public BleGatt getBleGatt() {
        return bleGatt;
    }

    // 设备是否包含gatt elements
    public boolean containGattElements(BleGattElement[] elements) {
        for (BleGattElement element : elements) {
            if (element == null || element.transformToGattObject(this) == null)
                return false;
        }
        return true;
    }

    // 设备是否包含gatt element
    public boolean containGattElement(BleGattElement element) {
        return !(element == null || element.transformToGattObject(this) == null);
    }

    @Override
    public boolean open(Context context) {
        boolean success = super.open(context);
        if(success)
            gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        return success;
    }

    @Override
    public void close() {
        ViseLog.e("BleConnector.close()");
        if(bleGatt != null)
            bleGatt.close();

        gattCmdExecutor = null;
        bleGatt = null;

        super.close();
    }

    @Override
    public void connect() {
        super.connect();
        new BleGatt(context, address, connectCallback).connect();
    }

    // 强制断开
    @Override
    public void disconnect(boolean forever) {
        ViseLog.e("BleConnector.disconnect(): " + (forever ? "forever" : ""));
        super.disconnect(forever);
        if (bleGatt != null) {
            bleGatt.disconnect(forever);
        }
    }

    public boolean isGattExecutorAlive() {
        return gattCmdExecutor.isAlive();
    }

    public final void read(BleGattElement element, IBleDataCallback dataCallback) {
        gattCmdExecutor.read(element, dataCallback);
    }

    public final void write(BleGattElement element, byte[] data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    public final void write(BleGattElement element, byte data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    public final void notify(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.notify(element, enable, receiveCallback);
    }

    public final void indicate(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.indicate(element, enable, receiveCallback);
    }

    public final void runInstantly(IBleDataCallback callback) {
        gattCmdExecutor.runInstantly(callback);
    }
}
