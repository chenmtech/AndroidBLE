package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.core.ViseBle;
import com.vise.log.ViseLog;

public class BleDeviceDisconnectCommand extends BleDeviceConnectRelatedCommand {

    BleDeviceDisconnectCommand(BleDevice device) {
        super(device);
    }

    @Override
    synchronized void execute() throws InterruptedException {
        ViseLog.e("disconnect...");

        ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getBluetoothLeDevice());

        Thread.sleep(1000);

        if(device.getConnectState() != BleDeviceConnectState.CONNECT_DISCONNECT) {
            device.executeAfterDisconnect();

            device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);
        }
    }
}
