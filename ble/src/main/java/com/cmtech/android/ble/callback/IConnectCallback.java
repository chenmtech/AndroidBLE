package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;

/**
 * @Description: 连接设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 23:00.
 */
public interface IConnectCallback {
    //连接成功
    void onConnectSuccess(DeviceMirror deviceMirror);

    //连接失败
    void onConnectFailure(BleException exception);

    //连接断开
    void onDisconnect(boolean isActive);

}
