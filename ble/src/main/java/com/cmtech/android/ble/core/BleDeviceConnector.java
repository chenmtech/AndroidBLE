package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;
import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.OtherException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;

/**
 * ClassName:      BleDeviceConnector
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

public class BleDeviceConnector extends AbstractDeviceConnector {
    private Context context; // 上下文，用于启动蓝牙连接。当调用open()打开设备时赋值
    private BleGatt bleGatt; // Gatt，连接成功后赋值，完成连接状态改变处理以及数据通信功能
    private BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。连接成功后启动，连接失败或者断开时停止

    // connection callback
    private final IBleConnectCallback connectCallback = new IBleConnectCallback() {
        // connection success
        @Override
        public void onConnectSuccess(final BleGatt bleGatt) {
            processConnectSuccess(bleGatt);
        }

        // connection failure
        @Override
        public void onConnectFailure(final BleException exception) {
            processConnectFailure(exception);
        }

        // disconnection
        @Override
        public void onDisconnect() {
            processDisconnect();
        }
    };

    public BleDeviceConnector(IDevice device) {
        super(device);
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

    // 打开设备
    @Override
    public void open(Context context) {
        if (state != CLOSED) {
            ViseLog.e("The device is opened.");
            return;
        }
        if (context == null) {
            throw new NullPointerException("The context is null.");
        }

        ViseLog.e("BleDeviceConnector.open()");
        this.context = context;
        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        setState(DISCONNECT);
        if (device.autoConnect()) {
            connect();
        }
    }

    // 切换状态
    @Override
    public void switchState() {
        ViseLog.e("BleDeviceConnector.switchState()");
        if (isDisconnected()) {
            connect();
        } else if (isConnected()) {
            disconnect(true);
        } else { // 无效操作
            device.handleException(new OtherException(context.getString(R.string.invalid_operation)));
        }
    }

    // 强制断开
    @Override
    public void disconnect(boolean forever) {
        ViseLog.e("BleDeviceConnector.disconnect(): " + (forever ? "forever" : ""));
        if (bleGatt != null) {
            bleGatt.disconnect(forever);
        }
    }

    // 关闭设备
    @Override
    public void close() {
        ViseLog.e("BleDeviceConnector.close()");
        if(bleGatt != null)
            bleGatt.clear();
        setState(BleDeviceState.CLOSED);

        gattCmdExecutor = null;
        bleGatt = null;
        context = null;
    }

    @Override
    public void clear() {
        if (bleGatt != null) {
            bleGatt.clear();
        }
    }

    private void connect() {
        new BleGatt(context, device, connectCallback).connect();
    }

    // 处理连接成功
    private void processConnectSuccess(BleGatt bleGatt) {
        // 防止重复连接成功
        if (isConnected()) {
            ViseLog.e("Connect Success Again!!!");
            return;
        }
        if (state == CLOSED) { // 设备已经关闭了，强行清除
            clear();
            return;
        }

        ViseLog.e("Process connect success: " + bleGatt);

        this.bleGatt = bleGatt;
        gattCmdExecutor.start();

        if (!device.onConnectSuccess()) {
            disconnect(false);
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        if (state != FAILURE) {
            ViseLog.e("Process connect failure: " + bleException);

            gattCmdExecutor.stop();
            bleGatt = null;
            device.onConnectFailure();
        }
    }

    // 处理连接断开
    private void processDisconnect() {
        if (state != DISCONNECT) {
            ViseLog.e("Process disconnect.");

            gattCmdExecutor.stop();
            bleGatt = null;
            device.onDisconnect();
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
