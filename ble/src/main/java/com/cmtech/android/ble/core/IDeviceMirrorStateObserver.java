package com.cmtech.android.ble.core;

import com.cmtech.android.ble.common.ConnectState;

public interface IDeviceMirrorStateObserver {
    void updateDeviceMirrorState(ConnectState mirrorState);
}
