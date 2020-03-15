package com.cmtech.android.ble.core;

import android.content.Context;

import com.vise.log.ViseLog;

public class WebConnector extends AbstractConnector {
    public WebConnector(IDevice device) {
        super(device);
    }

    @Override
    public void open(Context context) {
        super.open(context);
    }

    @Override
    public void close() {
        ViseLog.e("WebConnector.close()");
        super.close();
    }

    @Override
    public void connect() {
        if (!device.onConnectSuccess())
            disconnect(true);
    }

    @Override
    public void disconnect(boolean forever) {

    }

}
