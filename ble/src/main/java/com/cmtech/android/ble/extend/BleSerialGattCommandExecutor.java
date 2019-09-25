package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleDataCallback;
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
        if(device == null) {
            throw new IllegalArgumentException("BleDevice is null");
        }

        this.device = device;
    }

    // 启动Gatt命令执行器
    final void start() {
        if(device.getBleDeviceGatt() == null || isAlive()) {
            return;
        }

        ViseLog.e("启动gattCmdExecutor");

        gattCmdService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "MT_Gatt_Cmd");
            }
        });
    }

    // 停止Gatt命令执行器
    final void stop() {
        if(isAlive()) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(gattCmdService);

            ViseLog.e("停止gattCmdExecutor");
        }
    }

    // 是否还在运行
    boolean isAlive() {
        return ((gattCmdService != null) && !gattCmdService.isShutdown());
    }

    // Gatt操作
    // 读
    final void read(BleGattElement element, IBleDataCallback dataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_READ,
                null, dataCallback, null);

        if(command != null)
            sendCommand(command);
    }

    // 写多字节
    final void write(BleGattElement element, byte[] data, IBleDataCallback dataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_WRITE,
                data, dataCallback, null);

        if(command != null)
            sendCommand(command);
    }

    // 写单字节
    final void write(BleGattElement element, byte data, IBleDataCallback dataCallback) {
        write(element, new byte[]{data}, dataCallback);
    }

    // Notify
    final void notify(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        notify(element, enable, null, receiveCallback);
    }

    private void notify(BleGattElement element, boolean enable
            , IBleDataCallback dataCallback, IBleDataCallback receiveCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_NOTIFY,
                (enable) ? new byte[]{0x01} : new byte[]{0x00}, dataCallback, receiveCallback);

        if(command != null)
            sendCommand(command);
    }

    // Indicate
    final void indicate(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        indicate(element, enable, null, receiveCallback);
    }

    private void indicate(BleGattElement element, boolean enable
            , IBleDataCallback dataCallback, IBleDataCallback receiveCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, element, PropertyType.PROPERTY_INDICATE,
                (enable) ? new byte[]{0x01} : new byte[]{0x00}, dataCallback, receiveCallback);

        if(command != null)
            sendCommand(command);
    }

    // 无需等待响应立刻执行完毕
    final void runInstantly(IBleDataCallback dataCallback) {
        BleSerialGattCommand command = BleSerialGattCommand.create(device, null, PropertyType.PROPERTY_INSTANTRUN,
                null, dataCallback, null);

        if(command != null)
            sendCommand(command);
    }

    private void sendCommand(final BleGattCommand command) {
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
