package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.utils.ExecutorServiceUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *
 * ClassName:      BleSerialGattCommandExecutor
 * Description:    串行Gatt命令执行器
 * Author:         chenm
 * CreateDate:     2018-12-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-20 07:02
 * UpdateRemark:   采用Executor框架实现
 * Version:        1.0
 */

class BleSerialGattCommandExecutor {
    private final BleDevice device; // 设备

    private ExecutorService gattCmdService; // 命令执行Service

    BleSerialGattCommandExecutor(BleDevice device) {
        this.device = device;
    }

    // 启动Gatt命令执行器
    final void start() {
        if(device == null || device.getDeviceMirror() == null) {
            throw new NullPointerException("The device mirror of BleSerialGattCommandExecutor must not be null when it is started.");
        }

        if(gattCmdService != null && !gattCmdService.isShutdown()) return;

        gattCmdService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "MT_Gatt_Cmd");
            }
        });
    }

    // 停止Gatt命令执行器
    final void stop() {
        ExecutorServiceUtil.shutdownNowAndAwaitTerminate(gattCmdService);
    }

    // 是否还在运行
    final boolean isAlive() {
        return ((gattCmdService != null) && !gattCmdService.isShutdown());
    }

    // Gatt操作
    // 读
    final void read(BleGattElement element, IGattDataCallback gattDataCallback) {
        if(device.getDeviceMirror() == null) return;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataCallback(GattDataCallbackAdapter.create(gattDataCallback)).build();

        executeCommand(new BleSerialGattCommand(command));
    }

    // 写多字节
    final void write(BleGattElement element, byte[] data, IGattDataCallback gattDataCallback) {
        if(device.getDeviceMirror() == null) return;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataCallback(GattDataCallbackAdapter.create(gattDataCallback)).build();

        executeCommand(new BleSerialGattCommand(command));
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
        if(device.getDeviceMirror() == null) return;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(GattDataCallbackAdapter.create(gattDataCallback))
                .setNotifyOpCallback(GattDataCallbackAdapter.create(notifyOpCallback)).build();

        executeCommand(new BleSerialGattCommand(command));
    }

    // Indicate
    final void indicate(BleGattElement element, boolean enable, IGattDataCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    private void indicate(BleGattElement element, boolean enable
            , IGattDataCallback gattDataCallback, IGattDataCallback indicateOpCallback) {
        if(device.getDeviceMirror() == null) return;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(GattDataCallbackAdapter.create(gattDataCallback))
                .setNotifyOpCallback(GattDataCallbackAdapter.create(indicateOpCallback)).build();

        executeCommand(new BleSerialGattCommand(command));
    }

    // 无需蓝牙响应立刻执行
    final void runInstantly(IGattDataCallback gattDataCallback) {
        BleGattCommand command = new BleGattCommand.Builder().setDataCallback(GattDataCallbackAdapter.create(gattDataCallback))
                .setInstantCommand(true).build();

        executeCommand(new BleSerialGattCommand(command));
    }

    private void executeCommand(final BleGattCommand command) {
        if(command != null && isAlive()) {
            gattCmdService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.execute();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
