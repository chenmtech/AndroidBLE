package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
  *
  * ClassName:      BleGattCommandQueue
  * Description:    Ble Gatt命令队列
  * Author:         chenm
  * CreateDate:     2018-03-02 11:16
  * UpdateUser:     chenm
  * UpdateDate:     2019-05-02 11:16
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleGattCommandQueue {
    private final static int CMD_ERROR_RETRY_TIMES = 3;      // Gatt命令执行错误可重复的次数

    private final BlockingQueue<BleGattCommand> cmdQueue = new LinkedBlockingQueue<>(); // 要执行的命令队列

    private BleGattCommand command; // 当前在执行的命令

    private boolean done = true; // 标记当前命令是否执行完毕

    private int cmdErrorTimes = 0; // 命令执行错误的次数

    private DeviceMirror deviceMirror; // 执行命令的设备镜像


    BleGattCommandQueue() {

    }

    void setDeviceMirror(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
    }

    synchronized void onCommandSuccess(IBleCallback bleCallback, byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.d("Success to execute: " + command + " The return data is " + HexUtil.encodeHexStr(data));

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(command != null && deviceMirror != null) {
            deviceMirror.removeBleCallback(command.getGattInfoKey());
        }

        if(bleCallback != null) {
            bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);
        }

        done = true;

        cmdErrorTimes = 0;

        notifyAll();
    }

    synchronized boolean onCommandFailure(IBleCallback bleCallback, BleException exception) {
        boolean isStop = false;

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(command != null && deviceMirror != null) {
            deviceMirror.removeBleCallback(command.getGattInfoKey());
        }

        // 有错误，且次数小于指定次数，重新执行当前命令
        if(cmdErrorTimes < CMD_ERROR_RETRY_TIMES && command != null) {
            ViseLog.i("Retry Command Times: " + cmdErrorTimes);

            // 再次执行当前命令
            command.execute();

            cmdErrorTimes++;
        } else {
            // 错误次数大于指定次数
            cmdErrorTimes = 0;

            isStop = true;

            if(deviceMirror != null) deviceMirror.disconnect();

            if(bleCallback != null)
                bleCallback.onFailure(exception);
        }
        ViseLog.i(command + " is wrong: " + exception);

        return isStop;
    }

    void addReadCommand(BleGattElement element, BleGattCommandExecutor.BleCallbackDecorator dataCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataCallback(dataCallback).build();

        if(command != null)
            addCommandToList(command);
    }

    void addWriteCommand(BleGattElement element, byte[] data, BleGattCommandExecutor.BleCallbackDecorator dataCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataCallback(dataCallback).build();

        if(command != null)
            addCommandToList(command);
    }

    // 添加写单字节数据命令
    void addWriteCommand(BleGattElement element, byte data, BleGattCommandExecutor.BleCallbackDecorator dataCallback) {
        addWriteCommand(element, new byte[]{data}, dataCallback);
    }

    void addNotifyCommand(BleGattElement element, boolean enable
            , BleGattCommandExecutor.BleCallbackDecorator dataCallback, IBleCallback notifyCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(dataCallback)
                .setNotifyOpCallback(notifyCallback).build();

        if(command != null)
            addCommandToList(command);
    }

    void addIndicateCommand(BleGattElement element, boolean enable
            , BleGattCommandExecutor.BleCallbackDecorator dataCallback, IBleCallback indicateCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataCallback(dataCallback)
                .setNotifyOpCallback(indicateCallback).build();

        if(command != null)
            addCommandToList(command);
    }

    // 添加Instant命令
    void addInstantCommand(IBleCallback dataCallback) {
        BleGattCommand command = new BleGattCommand.Builder().setDataCallback(dataCallback).setInstantCommand(true).build();

        if(command != null)
            addCommandToList(command);
    }

    private synchronized void addCommandToList(BleGattCommand command) {
        ViseLog.d("add command: " + command);

        if(!cmdQueue.add(command))
            throw new IllegalStateException();

        notifyAll();
    }

    synchronized void executeNextCommand() throws InterruptedException{
        while (!done || cmdQueue.isEmpty()) {
            wait();
        }

        // 取出一条命令执行
        command = cmdQueue.remove();

        if(command != null) {
            ViseLog.d("Execute: " + command);

            command.execute();

            // 设置未完成标记
            if (!command.isInstantCommand())
                done = false;
        }

        notifyAll();
    }

    synchronized void reset() {
        cmdQueue.clear();

        command = null;

        done = true;

        cmdErrorTimes = 0;
    }

}
