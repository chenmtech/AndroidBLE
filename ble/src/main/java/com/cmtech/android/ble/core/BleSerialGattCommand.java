package com.cmtech.android.ble.core;

import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

/**
 *
 * ClassName:      BleSerialGattCommand
 * Description:    表示串行Gatt命令，所谓串行命令是指当命令发出后，并不立即执行下一条命令。
 *                 而是等待接收到蓝牙设备返回的响应并执行回调后，才会继续执行下一条命令
 * Author:         chenm
 * CreateDate:     2019-06-20 07:02
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-20 07:02
 * UpdateRemark:   无
 * Version:        1.0
 */

class BleSerialGattCommand extends BleGattCommand {
    private volatile boolean finish = true; // 标记命令是否执行完毕

    // IBleCallback的装饰类，在一般的回调响应任务完成后，执行串行命令所需动作
    private class BleSerialCommandDataCallbackDecorator implements IBleDataCallback {
        private IBleDataCallback bleCallback;

        BleSerialCommandDataCallbackDecorator(IBleDataCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BleGattElement element) {
            onSerialCommandSuccess(bleCallback, data, element);
        }

        @Override
        public void onFailure(BleException exception) {
            onSerialCommandFailure(bleCallback, exception);
        }
    }

    private BleSerialGattCommand(BleGattCommand gattCommand) {
        super(gattCommand);
        dataCallback = new BleSerialCommandDataCallbackDecorator(dataCallback);
    }

    static BleSerialGattCommand create(BleDeviceConnector device, BleGattElement element, BleGattCmdType bleGattCmdType, byte[] data,
                                       IBleDataCallback dataCallback, IBleDataCallback receiveCallback) {
        if(device.getBleGatt() == null) return null;

        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDevice(device)
                .setBluetoothElement(element)
                .setBleGattCmdType(bleGattCmdType)
                .setData(data)
                .setDataCallback(dataCallback)
                .setReceiveCallback(receiveCallback).build();

        if(command == null) ViseLog.e("Gatt Command is error.");

        return new BleSerialGattCommand(command);
    }

    @Override
    synchronized boolean execute() throws InterruptedException{
        finish = super.execute();
        while(!finish) {
            wait();
        }
        return true;
    }

    private synchronized void onSerialCommandSuccess(IBleDataCallback bleCallback, byte[] data, BleGattElement bleGattElement) {
        if(data == null) {
            ViseLog.i("Command Success: " + this);
        } else {
            ViseLog.i("Command Success: " + this + " Return data: " + HexUtil.encodeHexStr(data));
        }
        if(bleCallback != null) {
            bleCallback.onSuccess(data, bleGattElement);
        }

        finish = true;
        notifyAll();
    }

    private synchronized void onSerialCommandFailure(IBleDataCallback bleCallback, BleException exception) {
        ViseLog.e("Command Failure: " + this + " Exception: " + exception);

        if(bleCallback != null)
            bleCallback.onFailure(exception);

        // 命令执行错误，请求断开连接
        if(getDevice() != null) {
            getDevice().forceDisconnect(false);
        }
    }
}
