package com.cmtech.android.ble.core;

import android.content.Context;

public interface IDeviceConnector {
    void open(Context context);
    void switchState();
    void callDisconnect(boolean stopAutoScan);
    void close();
    void clear();
    boolean isStopped();
    boolean isConnected();
    boolean isDisconnected();
    boolean isLocal();
}
