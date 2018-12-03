package com.cmtech.android.ble.core;

import com.cmtech.android.ble.common.ConnectState;

public interface IDeviceMirrorStateObserver {
    // 根据Mirror的状态来更新观察者的状态
    void updateDeviceStateAccordingMirror(ConnectState mirrorState);
}
