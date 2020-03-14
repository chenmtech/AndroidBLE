package com.cmtech.android.ble.core;

import android.content.Context;

public interface IConnector {
    void open(Context context); // open connector
    void connect(); // connect
    void disconnect(boolean forever); // disconnect. if forever=true, no reconnection occurred, otherwise reconnect it.
    void close(); // close connector
}
