package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.LinkedList;

/**
 * BleGattCommandManager: Ble Gatt命令执行器
 * Created by bme on 2018/3/2.
 *
 */

/**
  *
  * ClassName:      BleGattCommandManager
  * Description:    Ble Gatt命令管理器
  * Author:         chenm
  * CreateDate:     2018-03-02 11:16
  * UpdateUser:     chenm
  * UpdateDate:     2019-05-02 11:16
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleGattCommandManager {
    private final static int CMD_ERROR_RETRY_TIMES = 3;      // Gatt命令执行错误可重复的次数

    private final DeviceMirror deviceMirror; // 执行命令的设备镜像

    private final LinkedList<BleGattCommand> commandList = new LinkedList<>(); // 要执行的命令队列

    private BleGattCommand currentCommand; // 当前在执行的命令

    private boolean currentCommandDone = true; // 标记当前命令是否执行完毕

    private int cmdErrorTimes = 0; // 命令执行错误的次数

    // 构造器：指定设备镜像
    BleGattCommandManager(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
    }

    synchronized void processCommandSuccessCallback(IBleCallback bleCallback, byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.d("Success to execute: " + currentCommand + " The return data is " + HexUtil.encodeHexStr(data));

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(currentCommand != null && deviceMirror != null) {
            deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
        }

        if(bleCallback != null) {
            bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);
        }

        currentCommandDone = true;

        cmdErrorTimes = 0;
    }

    synchronized boolean processCommandFailureCallback(IBleCallback bleCallback, BleException exception) {
        boolean isStop = false;

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(currentCommand != null && deviceMirror != null) {
            deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
        }

        // 有错误，且次数小于指定次数，重新执行当前命令
        if(cmdErrorTimes < CMD_ERROR_RETRY_TIMES && currentCommand != null) {
            // 再次执行当前命令
            currentCommand.execute();

            cmdErrorTimes++;
            ViseLog.i("Command Retry: " + cmdErrorTimes);
        } else {
            // 错误次数大于指定次数
            cmdErrorTimes = 0;

            isStop = true;

            if(deviceMirror != null) deviceMirror.disconnect();

            if(bleCallback != null)
                bleCallback.onFailure(exception);
        }
        ViseLog.i(currentCommand + " is wrong: " + exception);

        return isStop;
    }

    synchronized void addReadCommand(BleGattElement element, BleGattCommandExecutor.BleSerialCommandCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataOpCallback(dataOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    synchronized void addWriteCommand(BleGattElement element, byte[] data, BleGattCommandExecutor.BleSerialCommandCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataOpCallback(dataOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    // 添加写单字节数据命令
    synchronized void addWriteCommand(BleGattElement element, byte data, BleGattCommandExecutor.BleSerialCommandCallback dataOpCallback) {
        addWriteCommand(element, new byte[]{data}, dataOpCallback);
    }

    synchronized void addNotifyCommand(BleGattElement element, boolean enable
            , BleGattCommandExecutor.BleSerialCommandCallback dataOpCallback, IBleCallback notifyOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(dataOpCallback)
                .setNotifyOpCallback(notifyOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    synchronized void addIndicateCommand(BleGattElement element, boolean enable
            , BleGattCommandExecutor.BleSerialCommandCallback dataOpCallback, IBleCallback indicateOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(dataOpCallback)
                .setNotifyOpCallback(indicateOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    // 添加Instant命令
    synchronized void addInstantCommand(IBleCallback dataOpCallback) {
        BleGattCommand command = new BleGattCommand.Builder().setDataOpCallback(dataOpCallback).setInstantCommand(true).build();
        if(command != null)
            addCommandToList(command);
    }

    private void addCommandToList(BleGattCommand command) {
        ViseLog.d("add command: " + command);

        if(!commandList.add(command))
            throw new IllegalStateException();

    }

    synchronized void executeNextCommand() {
        if(currentCommandDone && !commandList.isEmpty()) {
            // 取出一条命令执行
            currentCommand = commandList.remove();

            if(currentCommand != null) {
                currentCommand.execute();

                ViseLog.d("execute command: " + currentCommand);

                // 设置未完成标记
                if (!currentCommand.isInstantCommand())
                    currentCommandDone = false;
            }
        }
    }

    synchronized void resetCommandList() {
        commandList.clear();
        currentCommand = null;
        currentCommandDone = false;
        cmdErrorTimes = 0;
    }

}
