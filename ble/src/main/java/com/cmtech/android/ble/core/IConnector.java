package com.cmtech.android.ble.core;

import android.content.Context;

public interface IConnector {
    void open(Context context); // open
    void connect(); // connect
    void disconnect(boolean forever); // disconnect. if forever=true, no reconnection occurred
    void close(); // close
    BleDeviceState getState(); // get state
    void setState(BleDeviceState state); // set state
    void switchState(); // switch state
}
