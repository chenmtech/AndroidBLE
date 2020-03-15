package com.cmtech.android.ble.core;

import android.content.Context;

public interface IConnector {
    boolean open(Context context); // open connector
    void close(); // close connector
    void connect(); // connect
    void disconnect(boolean forever); // disconnect. if forever=true, no reconnection occurred, otherwise reconnect it.
    DeviceState getState();

    interface IConnectorCallback {
        boolean onConnectSuccess(); // operate when connection success
        void onConnectFailure(); // operate when connection failure
        void onDisconnect(); // operate when disconnection
        void onConnectorStateUpdated();
    }
}
