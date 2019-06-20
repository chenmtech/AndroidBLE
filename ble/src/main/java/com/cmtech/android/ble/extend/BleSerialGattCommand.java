package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

/**
 *
 * ClassName:      BleSerialGattCommandExecutor
 * Description:    串行Gatt命令，所谓串行是指当命令发出后，并不立即执行下一条命令。
 *                 而是等待直到接收到蓝牙设备返回的响应后，才算执行完成，然后继续执行下一条命令
 * Author:         chenm
 * CreateDate:     2019-06-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-20 07:02
 * UpdateRemark:   无
 * Version:        1.0
 */

class BleSerialGattCommand extends BleGattCommand {
    private boolean done = true; // 标记命令是否执行完毕

    // IBleCallback的装饰类，在一般的回调任务完成后，执行串行命令所需动作
    private class BleCallbackDecorator implements IBleCallback {
        private IBleCallback bleCallback;

        BleCallbackDecorator(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            onCommandSuccess(bleCallback, data, bluetoothGattChannel, bluetoothLeDevice);
        }

        @Override
        public void onFailure(BleException exception) {
            onCommandFailure(bleCallback, exception);
        }
    }

    BleSerialGattCommand(BleGattCommand gattCommand) {
        super(gattCommand);

        dataOpCallback = new BleCallbackDecorator(dataOpCallback);
    }

    @Override
    synchronized boolean execute() throws InterruptedException{
        done = super.execute();

        while(!done) {
            wait();
        }

        return done;
    }

    private synchronized void onCommandSuccess(IBleCallback bleCallback, byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.d("Cmd Success: <" + this + "> The return data is " + HexUtil.encodeHexStr(data));

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(deviceMirror != null) {
            deviceMirror.removeBleCallback(getGattInfoKey());
        }

        if(bleCallback != null) {
            bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);
        }

        done = true;

        notifyAll();
    }

    private void onCommandFailure(IBleCallback bleCallback, BleException exception) {
        ViseLog.e("Cmd Failure: <" + this + "> Exception: " + exception);

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(deviceMirror != null) {
            deviceMirror.removeBleCallback(getGattInfoKey());
        }

        if(deviceMirror != null) deviceMirror.disconnect();

        if(bleCallback != null)
            bleCallback.onFailure(exception);
    }
}
