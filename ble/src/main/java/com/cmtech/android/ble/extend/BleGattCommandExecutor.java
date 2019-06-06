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

public class BleGattCommandExecutor {


    // IBleCallback的装饰类，在一般的回调任务完成后，执行串行命令所需动作
    public class BleSerialCommandCallback implements IBleCallback {
        private IBleCallback bleCallback;

        BleSerialCommandCallback(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {

            commandManager.processCommandSuccessCallback(bleCallback, data, bluetoothGattChannel, bluetoothLeDevice);

        }

        @Override
        public void onFailure(BleException exception) {
            boolean isStop = commandManager.processCommandFailureCallback(bleCallback, exception);

            if(isStop) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        stop();
                    }
                });
            }
        }
    }

    private final BleDevice device; // BLE设备

    private Thread executeThread; // 执行命令的线程

    private final BleGattCommandManager commandManager = new BleGattCommandManager();

    BleGattCommandExecutor(BleDevice device) {
        this.device = device;
    }

    // 检测Gatt Element是否存在于device中
    public boolean checkElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if(BleDeviceUtil.getGattObject(device, element) == null) return false;
        }

        return true;
    }

    // 启动Gatt命令执行器
    public void start() {
        if(isAlive()) return;

        DeviceMirror deviceMirror = device.getDeviceMirror();

        if(deviceMirror == null) {
            throw new NullPointerException();
        }

        commandManager.setDeviceMirror(deviceMirror);

        executeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ViseLog.d("Start Command Execution Thread: "+Thread.currentThread().getName());
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        commandManager.executeNextCommand();
                    }
                } finally {
                    commandManager.resetCommandList();

                    ViseLog.e("executeThread finished!!!!!!");
                }
            }
        });

        executeThread.start();
        ViseLog.i("Success to create new GATT command executor.");
    }

    // 停止Gatt命令执行器
    public void stop() {
        if(isAlive()) {
            executeThread.interrupt();

            try {
                executeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ViseLog.e("Command Executor is stopped");
        }
    }

    public boolean isAlive() {
        return ((executeThread != null) && executeThread.isAlive());
    }

    // Gatt操作
    // 读
    public final void read(BleGattElement element, IGattDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandManager.addReadCommand(element, new BleSerialCommandCallback(callback));
    }

    // 写多字节
    public final void write(BleGattElement element, byte[] data, IGattDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandManager.addWriteCommand(element, data, new BleSerialCommandCallback(callback));
    }

    // 写单字节
    public final void write(BleGattElement element, byte data, IGattDataOpCallback dataOpCallback) {
        write(element, new byte[]{data}, dataOpCallback);
    }

    // Notify
    private void notify(BleGattElement element, boolean enable
            , IGattDataOpCallback dataOpCallback, IGattDataOpCallback notifyOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback notifyCallback = (notifyOpCallback == null) ? null : new BleDataOpCallbackAdapter(notifyOpCallback);
        commandManager.addNotifyCommand(element, enable, new BleSerialCommandCallback(dataCallback), notifyCallback);
    }

    // Notify
    public final void notify(BleGattElement element, boolean enable, IGattDataOpCallback notifyOpCallback) {
        notify(element, enable, null, notifyOpCallback);
    }

    // Indicate
    public final void indicate(BleGattElement element, boolean enable, IGattDataOpCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    // Indicate
    private void indicate(BleGattElement element, boolean enable
            , IGattDataOpCallback dataOpCallback, IGattDataOpCallback indicateOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback indicateCallback = (indicateOpCallback == null) ? null : new BleDataOpCallbackAdapter(indicateOpCallback);
        commandManager.addIndicateCommand(element, enable, new BleSerialCommandCallback(dataCallback), indicateCallback);
    }

    // 不需要蓝牙通信立刻执行
    public final void instExecute(IGattDataOpCallback dataOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandManager.addInstantCommand(dataCallback);
    }

}
