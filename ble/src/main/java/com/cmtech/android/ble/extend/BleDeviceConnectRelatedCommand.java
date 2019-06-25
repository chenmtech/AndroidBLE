package com.cmtech.android.ble.extend;

abstract class BleDeviceConnectRelatedCommand {
    boolean waitingResponse = false;

    final BleDevice device;

    BleDeviceConnectRelatedCommand(BleDevice device) {
        this.device = device;
    }

    abstract void execute() throws InterruptedException;
}
