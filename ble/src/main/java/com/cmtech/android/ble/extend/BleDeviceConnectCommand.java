package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.exception.BleException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SUCCESS;

/**
  *
  * ClassName:      BleDeviceConnectCommand
  * Description:    连接设备命令
  * Author:         chenm
  * CreateDate:     2019-06-25 18:50
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 18:50
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleDeviceConnectCommand extends BleDeviceCommand {

    private class MyConnectCallback implements IConnectCallback {
        MyConnectCallback() {
        }

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            processConnectSuccess(deviceMirror);
        }
        @Override
        public void onConnectFailure(final BleException exception) {
            processConnectFailure(exception);
        }
        @Override
        public void onDisconnect(final boolean isActive) {
            processDisconnect(isActive);
        }
    }

    BleDeviceConnectCommand(BleDevice device) {
        super(device);
    }

    @Override
    synchronized void execute() {
        ViseLog.e("startConnect...");

        MyConnectCallback connectCallback = new MyConnectCallback();

        ViseBle.getInstance().connect(device.getBluetoothLeDevice(), connectCallback);

        device.setConnectState(CONNECT_CONNECTING);

        waitingResponse = true;
    }

    // 处理连接成功回调
    private synchronized void processConnectSuccess(DeviceMirror mirror) {
        ViseLog.e("processConnectSuccess");

        device.setConnectState(CONNECT_SUCCESS);

        waitingResponse = false;

        // 设备执行连接后处理，如果出错则断开
        if (!device.executeAfterConnectSuccess()) {
            ViseLog.e("executeAfterConnectSuccess is wrong.");

            device.disconnect();
        }
    }

    // 处理连接错误
    private synchronized void processConnectFailure(final BleException bleException) {
        ViseLog.e("processConnectFailure with " + bleException );

        device.setConnectState(BleDeviceConnectState.CONNECT_FAILURE);

        waitingResponse = false;

        // 仍然有可能会连续执行两次下面语句
        device.executeAfterConnectFailure();

        device.reconnect();
    }

    // 处理连接断开
    private void processDisconnect(boolean isActive) {
        ViseLog.e("processDisconnect: " + isActive);

        device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

        waitingResponse = false;

        device.executeAfterDisconnect();
    }
}
