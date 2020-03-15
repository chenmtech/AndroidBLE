package com.cmtech.android.ble.core;

import android.content.Context;

public interface IConnector {
    void open(Context context); // open connector
    void close(); // close connector
    void connect(); // connect
    void disconnect(boolean forever); // disconnect. if forever=true, no reconnection occurred, otherwise reconnect it.
}
