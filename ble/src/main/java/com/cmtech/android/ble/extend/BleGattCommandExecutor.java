package com.cmtech.android.ble.extend;

import android.os.Looper;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.bledevice.core
 * ClassName:      BleGattCommandExecutor
 * Description:    Gatt Command执行器
 * Author:         作者名
 * CreateDate:     2019-05-02 15:11
 * UpdateUser:     更新者
 * UpdateDate:     2019-05-02 15:11
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
class BleGattCommandExecutor {

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
                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        stop();
                    }
                });
            }
        }
    }

    private Thread executeThread; // 执行命令的线程

    private final BleGattCommandManager commandManager;

    BleGattCommandExecutor(DeviceMirror deviceMirror) {
        commandManager = new BleGattCommandManager(deviceMirror);

    }

    // 开始执行命令
    void start() {
        executeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ViseLog.i("Start Command Execution Thread: "+Thread.currentThread().getName());
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
    }

    // 停止执行命令
    void stop() {
        executeThread.interrupt();

        try {
            executeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViseLog.e("Command Executor is stopped");
    }

    void addReadCommand(BleGattElement element, IBleCallback dataOpCallback) {
        commandManager.addReadCommand(element, new BleSerialCommandCallback(dataOpCallback));
    }

    void addWriteCommand(BleGattElement element, byte[] data, IBleCallback dataOpCallback) {
        commandManager.addWriteCommand(element, data, new BleSerialCommandCallback(dataOpCallback));
    }

    void addWriteCommand(BleGattElement element, byte data, IBleCallback dataOpCallback) {
        addWriteCommand(element, new byte[]{data}, dataOpCallback);
    }

    void addNotifyCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback notifyOpCallback) {
        commandManager.addNotifyCommand(element, enable, new BleSerialCommandCallback(dataOpCallback), notifyOpCallback);
    }

    void addIndicateCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback indicateOpCallback) {
        commandManager.addIndicateCommand(element, enable, new BleSerialCommandCallback(dataOpCallback), indicateOpCallback);
    }

    void addInstantCommand(IBleCallback dataOpCallback) {
        commandManager.addInstantCommand(dataOpCallback);
    }

                         // 命令执行器是否存活
    boolean isAlive() {
        return ((executeThread != null) && executeThread.isAlive());
    }
}
