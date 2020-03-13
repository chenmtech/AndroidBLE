package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;
import com.cmtech.android.ble.exception.OtherException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;

public class WebConnector extends AbstractConnector {
    private Context context;

    public WebConnector(IDevice device) {
        super(device);
    }

    @Override
    public void open(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("The context is null.");
        }

        if (state != CLOSED) {
            ViseLog.e("The device is opened.");
            return;
        }

        ViseLog.e("WebConnector.open()");
        this.context = context;
        setState(DISCONNECT);
        if (device.isAutoConnect()) {
            connect();
        }
    }

    @Override
    public void connect() {
        setState(CONNECT);
        if (!device.onConnectSuccess())
            disconnect(true);
    }

    @Override
    public void switchState() {
        ViseLog.e("WebConnector.switchState()");
        if (state == FAILURE || state == DISCONNECT) {
            connect();
        } else if (state == CONNECT) {
            disconnect(true);
        } else { // 无效操作
            device.handleException(new OtherException(context.getString(R.string.invalid_operation)));
        }
    }

    @Override
    public void disconnect(boolean forever) {
        setState(DISCONNECT);
    }

    @Override
    public void close() {
        ViseLog.e("WebConnector.close()");

        setState(BleDeviceState.CLOSED);
    }

}
