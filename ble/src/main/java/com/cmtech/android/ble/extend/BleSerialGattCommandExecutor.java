package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.utils.ExecutorUtil;
import com.vise.log.ViseLog;

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
            throw new NullPointerException("The device mirror must not be null when BleSerialGattCommandExecutor is started.");
        }

        if(isAlive()) return;

        gattCmdService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "MT_Gatt_Cmd");
            }
        });

        ViseLog.e("gattCmdExecutor starts.");
    }

    // 停止Gatt命令执行器
    final void stop() {
        if(isAlive()) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(gattCmdService);

            ViseLog.e("gattCmdExecutor stops.");
        }
    }

    // 是否还在运行
    boolean isAlive() {
        return ((gattCmdService != null) && !gattCmdService.isShutdown());
    }

    // Gatt操作
    // 读
    final void read(BleGattElement element, IGattDataCallback gattDataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_READ,
                null, gattDataCallback, null, false);

        if(command != null)
            executeCommand(command);
    }

    // 写多字节
    final void write(BleGattElement element, byte[] data, IGattDataCallback gattDataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_WRITE,
                data, gattDataCallback, null, false);

        if(command != null)
            executeCommand(command);
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
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_NOTIFY,
                (enable) ? new byte[]{0x01} : new byte[]{0x00}, gattDataCallback, notifyOpCallback, false);

        if(command != null)
            executeCommand(command);
    }

    // Indicate
    final void indicate(BleGattElement element, boolean enable, IGattDataCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    private void indicate(BleGattElement element, boolean enable
            , IGattDataCallback gattDataCallback, IGattDataCallback indicateOpCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_INDICATE,
                (enable) ? new byte[]{0x01} : new byte[]{0x00}, gattDataCallback, indicateOpCallback, false);

        if(command != null)
            executeCommand(command);
    }

    // 无需等待响应立刻执行完毕
    final void runInstantly(IGattDataCallback gattDataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, null, null,
                null, gattDataCallback, null, true);

        if(command != null)
            executeCommand(command);
    }

    private void executeCommand(final BleGattCommand command) {
        if(isAlive()) {
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
