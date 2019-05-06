package com.cmtech.android.ble.core;

import com.cmtech.android.ble.common.ConnectState;

public interface OnDeviceMirrorStateChangedListener {
    void onUpdateDeviceStateAccordingMirrorState(ConnectState mirrorState); // 根据Mirror的状态来更新观察者的状态
}
