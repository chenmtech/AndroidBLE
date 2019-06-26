package com.cmtech.android.ble.extend;

import android.os.Handler;

import com.cmtech.android.ble.core.ViseBle;
import com.vise.log.ViseLog;

/**
  *
  * ClassName:      BleDeviceDisconnectCommand
  * Description:    断开设备命令
  * Author:         chenm
  * CreateDate:     2019-06-25 18:48
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 18:48
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleDeviceDisconnectCommand extends BleDeviceCommand {

    BleDeviceDisconnectCommand(BleDevice device) {
        super(device);
    }

    @Override
    void execute(Handler handler) {
        if(handler == null)
            throw new NullPointerException();

        handler.post(new Runnable() {
            @Override
            public void run() {
                ViseLog.e("disconnect...");

                waitingResponse = true;

                ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getBluetoothLeDevice());

                ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getBluetoothLeDevice());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(device.getConnectState() != BleDeviceConnectState.CONNECT_DISCONNECT) {
                    device.executeAfterDisconnect();

                    device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);
                }

                resetReconnectTimes();

                waitingResponse = false;
            }
        });
    }
}
