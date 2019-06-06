package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.vise.log.ViseLog;

/**
 *
 * ClassName:      BleDeviceGattOperator
 * Description:    Gatt操作执行者
 * Author:         chenm
 * CreateDate:     2018-12-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-05 07:02
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public class BleDeviceGattOperator {

    private BleGattCommandExecutor commandExecutor; // Gatt命令执行器

    private final BleDevice device; // BLE设备

    BleDeviceGattOperator(BleDevice device) {
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

        commandExecutor = new BleGattCommandExecutor(deviceMirror);

        commandExecutor.start();
        ViseLog.i("Success to create new GATT command executor.");
    }

    // 停止Gatt命令执行器
    public void stop() {
        if(isAlive()) {
            commandExecutor.stop();
        }
    }

    public boolean isAlive() {
        return (commandExecutor != null && commandExecutor.isAlive());
    }

    // Gatt操作
    // 读
    public final void read(BleGattElement element, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandExecutor.addReadCommand(element, callback);
    }

    // 写多字节
    public final void write(BleGattElement element, byte[] data, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandExecutor.addWriteCommand(element, data, callback);
    }

    // 写单字节
    public final void write(BleGattElement element, byte data, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandExecutor.addWriteCommand(element, data, callback);
    }

    // Notify
    public final void notify(BleGattElement element, boolean enable
            , IBleDataOpCallback dataOpCallback, IBleDataOpCallback notifyOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback notifyCallback = (notifyOpCallback == null) ? null : new BleDataOpCallbackAdapter(notifyOpCallback);
        commandExecutor.addNotifyCommand(element, enable, dataCallback, notifyCallback);
    }

    // Notify
    public final void notify(BleGattElement element, boolean enable, IBleDataOpCallback notifyOpCallback) {
        notify(element, enable, null, notifyOpCallback);
    }

    // Indicate
    public final void indicate(BleGattElement element, boolean enable, IBleDataOpCallback indicateOpCallback) {
        indicate(element, enable, null, indicateOpCallback);
    }

    // Indicate
    public final void indicate(BleGattElement element, boolean enable
            , IBleDataOpCallback dataOpCallback, IBleDataOpCallback indicateOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback indicateCallback = (indicateOpCallback == null) ? null : new BleDataOpCallbackAdapter(indicateOpCallback);
        commandExecutor.addIndicateCommand(element, enable, dataCallback, indicateCallback);
    }

    // 不需要蓝牙通信立刻执行
    public final void instExecute(IBleDataOpCallback dataOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        commandExecutor.addInstantCommand(dataCallback);
    }

}
