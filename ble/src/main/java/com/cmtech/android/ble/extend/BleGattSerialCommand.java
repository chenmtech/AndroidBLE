package com.cmtech.android.ble.extend;

import android.os.Handler;
import android.os.Looper;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

public class BleGattSerialCommand extends BleGattCommand {
    private final static int CMD_ERROR_RETRY_TIMES = 3;      // Gatt命令执行错误可重复的次数

    private boolean done = true; // 标记当前命令是否执行完毕

    private int cmdErrorTimes = 0; // 命令执行错误的次数

    // IBleCallback的装饰类，在一般的回调任务完成后，执行串行命令所需动作
    class BleCallbackDecorator implements IBleCallback {
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
            boolean isStop = onCommandFailure(bleCallback, exception);

            if(isStop) {
                new Handler(Looper.getMainLooper()).postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        //device.disconnect();
                    }
                });
            }
        }
    }

    BleGattSerialCommand(BleGattCommand gattCommand) {
        super(gattCommand);
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
        ViseLog.d("Success to execute: " + this + " The return data is " + HexUtil.encodeHexStr(data));

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(deviceMirror != null) {
            deviceMirror.removeBleCallback(getGattInfoKey());
        }

        if(bleCallback != null) {
            bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);
        }

        done = true;

        cmdErrorTimes = 0;

        notifyAll();
    }

    private synchronized boolean onCommandFailure(IBleCallback bleCallback, BleException exception) {
        boolean isStop = false;

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(deviceMirror != null) {
            deviceMirror.removeBleCallback(getGattInfoKey());
        }

        // 有错误，且次数小于指定次数，重新执行当前命令
        if(cmdErrorTimes < CMD_ERROR_RETRY_TIMES) {
            ViseLog.i("Retry Command Times: " + cmdErrorTimes);

            // 再次执行当前命令
            try {
                execute();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cmdErrorTimes++;
        } else {
            // 错误次数大于指定次数
            cmdErrorTimes = 0;

            isStop = true;

            if(deviceMirror != null) deviceMirror.disconnect();

            if(bleCallback != null)
                bleCallback.onFailure(exception);
        }
        ViseLog.i(this + " is wrong: " + exception);

        return isStop;
    }
}
