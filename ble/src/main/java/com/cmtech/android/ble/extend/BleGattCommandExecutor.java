package com.cmtech.android.ble.extend;

import android.os.Handler;
import android.os.Looper;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

/**
 *
 * ClassName:      BleGattCommandExecutor
 * Description:    Gatt命令执行器
 * Author:         chenm
 * CreateDate:     2018-12-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-05 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

class BleGattCommandExecutor {

    // IBleCallback的装饰类，在一般的回调任务完成后，执行串行命令所需动作
    class BleCallbackDecorator implements IBleCallback {
        private IBleCallback bleCallback;

        BleCallbackDecorator(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {

            commandQueue.onCommandSuccess(bleCallback, data, bluetoothGattChannel, bluetoothLeDevice);

        }

        @Override
        public void onFailure(BleException exception) {
            boolean isStop = commandQueue.onCommandFailure(bleCallback, exception);

            if(isStop) {
                new Handler(Looper.getMainLooper()).postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        device.disconnect();
                    }
                });
            }
        }
    }

    private final BleDevice device; // BLE设备

    private Thread executeThread; // 执行命令的线程

    private final BleGattCommandQueue commandQueue = new BleGattCommandQueue();


    BleGattCommandExecutor(BleDevice device) {
        this.device = device;
    }

    // 检测设备中是否包含Gatt Elements
    boolean checkElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if( element == null || element.retrieveGattObject(device) == null )
                return false;
        }

        return true;
    }

    // 启动Gatt命令执行器
    void start() {
        if(isAlive()) return;

        DeviceMirror deviceMirror = device.getDeviceMirror();

        if(deviceMirror == null) {
            stop();
            return;
        }

        commandQueue.setDeviceMirror(deviceMirror);

        commandQueue.reset();

        executeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ViseLog.d("Start Command Execution Thread: "+Thread.currentThread().getName());

                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        commandQueue.executeNextCommand();
                    }
                } finally {
                    ViseLog.e("The Command Execution Thread is finished!");
                }
            }
        });

        executeThread.start();
    }

    // 停止Gatt命令执行器
    void stop() {
        if(isAlive()) {
            executeThread.interrupt();

            try {
                executeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViseLog.e("The Command Executor is stopped");
        }
    }

    // 是否还在运行
    boolean isAlive() {
        return ((executeThread != null) && executeThread.isAlive());
    }

    // Gatt操作
    // 读
    final void read(BleGattElement element, IGattDataCallback gattDataCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        commandQueue.addReadCommand(element, new BleCallbackDecorator(dataCallback));
    }

    // 写多字节
    final void write(BleGattElement element, byte[] data, IGattDataCallback gattDataCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        commandQueue.addWriteCommand(element, data, new BleCallbackDecorator(dataCallback));
    }

    // 写单字节
    final void write(BleGattElement element, byte data, IGattDataCallback gattDataCallback) {
        write(element, new byte[]{data}, gattDataCallback);
    }

    // Notify
    final void notify(BleGattElement element, boolean enable, IGattDataCallback notifyOpCallback) {
        notify(element, enable, null, notifyOpCallback);
    }

    private void notify(BleGattElement element, boolean enable
            , IGattDataCallback gattDataCallback, IGattDataCallback notifyOpCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        IBleCallback notifyCallback = (notifyOpCallback == null) ? null : new GattDataCallbackAdapter(notifyOpCallback);

        commandQueue.addNotifyCommand(element, enable, new BleCallbackDecorator(dataCallback), notifyCallback);
    }

    // Indicate
    final void indicate(BleGattElement element, boolean enable, IGattDataCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    private void indicate(BleGattElement element, boolean enable
            , IGattDataCallback gattDataCallback, IGattDataCallback indicateOpCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        IBleCallback indicateCallback = (indicateOpCallback == null) ? null : new GattDataCallbackAdapter(indicateOpCallback);

        commandQueue.addIndicateCommand(element, enable, new BleCallbackDecorator(dataCallback), indicateCallback);
    }

    // 无需蓝牙通信立刻执行
    final void instExecute(IGattDataCallback gattDataCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        commandQueue.addInstantCommand(dataCallback);
    }

}
