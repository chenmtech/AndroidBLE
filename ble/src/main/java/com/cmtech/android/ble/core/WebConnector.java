package com.cmtech.android.ble.core;

import android.content.Context;

import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.DeviceState.CONNECT;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECT;

public class WebConnector extends AbstractConnector {
    public WebConnector(IDevice device) {
        super(device);
    }

    @Override
    public void open(Context context) {
        super.open(context);
    }

    @Override
    public void connect() {
        device.setState(CONNECT);
        if (!device.onConnectSuccess())
            disconnect(true);
    }

    @Override
    public void disconnect(boolean forever) {
        device.setState(DISCONNECT);
    }

    @Override
    public void close() {
        ViseLog.e("WebConnector.close()");
        super.close();
    }

}
