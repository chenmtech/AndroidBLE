package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.vise.log.ViseLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
    private final BleDevice device;

    private ExecutorService gattCmdService;

    BleSerialGattCommandExecutor(BleDevice device) {
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
        if(device.getDeviceMirror() == null) return;

        if(gattCmdService != null && !gattCmdService.isTerminated()) return;

        if(device.getDeviceMirror() == null) {
            stop();
            return;
        }

        gattCmdService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "MT_Gatt_Execute");
            }
        });
    }

    // 停止Gatt命令执行器
    void stop() {
        if(gattCmdService != null) {
            gattCmdService.shutdownNow();

            try {
                boolean isTerminated = false;

                while(!isTerminated) {
                    isTerminated = gattCmdService.awaitTermination(1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 是否还在运行
    boolean isAlive() {
        return ((gattCmdService != null) && !gattCmdService.isShutdown());
    }

    // Gatt操作
    // 读
    final void read(BleGattElement element, IGattDataCallback gattDataCallback) {
        if(device.getDeviceMirror() == null) return;

        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(device.getDeviceMirror())
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataCallback(dataCallback).build();

        if(command != null) {
            executeCommand(new BleSerialGattCommand(command));
        }
    }

    // 写多字节
    final void write(BleGattElement element, byte[] data, IGattDataCallback gattDataCallback) {
        if(device.getDeviceMirror() == null) return;

        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(device.getDeviceMirror())
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataCallback(dataCallback).build();

        if(command != null) {
            executeCommand(new BleSerialGattCommand(command));
        }
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

        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        IBleCallback notifyCallback = (notifyOpCallback == null) ? null : new GattDataCallbackAdapter(notifyOpCallback);

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(device.getDeviceMirror())
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(dataCallback)
                .setNotifyOpCallback(notifyCallback).build();

        if(command != null) {
            executeCommand(new BleSerialGattCommand(command));
        }
    }

    // Indicate
    final void indicate(BleGattElement element, boolean enable, IGattDataCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    private void indicate(BleGattElement element, boolean enable
            , IGattDataCallback gattDataCallback, IGattDataCallback indicateOpCallback) {
        if(device.getDeviceMirror() == null) return;

        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        IBleCallback indicateCallback = (indicateOpCallback == null) ? null : new GattDataCallbackAdapter(indicateOpCallback);

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(device.getDeviceMirror())
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(dataCallback)
                .setNotifyOpCallback(indicateCallback).build();

        if(command != null)
            executeCommand(new BleSerialGattCommand(command));
    }

    // 无需蓝牙通信立刻执行
    final void instExecute(IGattDataCallback gattDataCallback) {
        IBleCallback dataCallback = (gattDataCallback == null) ? null : new GattDataCallbackAdapter(gattDataCallback);

        BleGattCommand command = new BleGattCommand.Builder().setDataCallback(dataCallback).setInstantCommand(true).build();

        if(command != null)
            executeCommand(command);
    }

    private void executeCommand(final BleGattCommand command) {
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
