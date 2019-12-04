package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;
import com.cmtech.android.ble.exception.OtherException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;

public class WebDeviceConnector implements IDeviceConnector {
    private final IDevice device; // 设备
    private volatile BleDeviceState state = CLOSED; // 实时状态
    private Context context;

    public WebDeviceConnector(IDevice device) {
        this.device = device;
    }

    @Override
    public void open(Context context) {
        if (state != CLOSED) {
            ViseLog.e("The device is opened.");
            return;
        }
        if (context == null) {
            throw new NullPointerException("The context is null.");
        }

        ViseLog.e("WebDeviceConnector.open()");
        this.context = context;
        setState(DISCONNECT);
        if (device.autoConnect()) {
            connect();
        }
    }

    private void connect() {
        setState(CONNECT);
        if (!device.onConnectSuccess())
            forceDisconnect(true);
    }

    @Override
    public void switchState() {
        ViseLog.e("WebDeviceConnector.switchState()");
        if (isDisconnected()) {
            connect();
        } else if (isConnected()) {
            forceDisconnect(true);
        } else { // 无效操作
            device.handleException(new OtherException(context.getString(R.string.invalid_operation)));
        }
    }

    @Override
    public void forceDisconnect(boolean forever) {
        setState(DISCONNECT);
    }

    @Override
    public boolean isDisconnectedForever() {
        return isDisconnected();
    }

    @Override
    public void close() {
        if (!isDisconnectedForever()) {
            ViseLog.e("The device can't be closed currently.");
            return;
        }

        ViseLog.e("WebDeviceConnector.close()");

        setState(BleDeviceState.CLOSED);
    }

    @Override
    public void clear() {

    }

    @Override
    public BleDeviceState getState() {
        return state;
    }

    @Override
    public void setState(BleDeviceState state) {
        if (this.state != state) {
            ViseLog.e("The state of device " + device.getAddress() + " is " + state);
            this.state = state;
            device.updateState();
        }
    }

    @Override
    public boolean isConnected() {
        return state == CONNECT;
    }

    @Override
    public boolean isDisconnected() {
        return state == FAILURE || state == DISCONNECT;
    }

}
