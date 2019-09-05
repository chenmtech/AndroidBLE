package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

/**
 *
 * ClassName:      BleSerialGattCommandExecutor
 * Description:    串行Gatt命令，所谓串行是指当命令发出后，并不立即执行下一条命令。
 *                 而是等待接收到蓝牙设备返回的响应后，才继续执行下一条命令
 * Author:         chenm
 * CreateDate:     2019-06-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-20 07:02
 * UpdateRemark:   无
 * Version:        1.0
 */

class BleSerialGattCommand extends BleGattCommand {
    private final static int CMD_ERROR_RETRY_TIMES = 3; // Gatt命令执行错误可重试的次数

    private boolean done = true; // 标记命令是否执行完毕

    //private int cmdErrorTimes = 0; // 命令执行错误的次数

    // IBleCallback的装饰类，在一般的回调响应任务完成后，执行串行命令所需动作
    private class BleSerialCommandCallbackDecorator implements IBleCallback {
        private IBleCallback bleCallback;

        BleSerialCommandCallbackDecorator(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            onSerialCommandSuccess(bleCallback, data, bluetoothGattChannel, bluetoothLeDevice);
        }

        @Override
        public void onFailure(BleException exception) {
            onSerialCommandFailure(bleCallback, exception);
        }
    }

    private BleSerialGattCommand(BleGattCommand gattCommand) {
        super(gattCommand);

        dataOpCallback = new BleSerialCommandCallbackDecorator(dataOpCallback);
    }

    static BleSerialGattCommand create(BleDevice device, BleGattElement element, PropertyType propertyType, byte[] data,
                                IGattDataCallback dataCallback, IGattDataCallback notifyCallback, boolean isInstantCommand) {
        if(device.getDeviceMirror() == null) return null;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();

        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setPropertyType(propertyType)
                .setData(data)
                .setDataCallback(GattDataCallbackAdapter.create(dataCallback))
                .setNotifyOpCallback(GattDataCallbackAdapter.create(notifyCallback))
                .setInstantCommand(isInstantCommand).build();

        return new BleSerialGattCommand(command);
    }

    @Override
    synchronized boolean execute() throws InterruptedException{
        done = super.execute();

        while(!done) {
            wait();
        }

        return true;
    }

    private synchronized void onSerialCommandSuccess(IBleCallback bleCallback, byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
        if(data == null) {
            ViseLog.i("Command Success: <" + this + ">");
        } else {
            ViseLog.i("Command Success: <" + this + "> The return data is " + HexUtil.encodeHexStr(data));
        }

        removeBleCallback();

        if(bleCallback != null) {
            bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);
        }

        done = true;

        //cmdErrorTimes = 0;

        notifyAll();
    }

    private synchronized void onSerialCommandFailure(IBleCallback bleCallback, BleException exception) {
        ViseLog.e("Command Failure: <" + this + "> Exception: " + exception);

        removeBleCallback();

        if(bleCallback != null)
            bleCallback.onFailure(exception);

        /*if(device != null) {
            device.disconnect(true);
        }*/
    }
}
