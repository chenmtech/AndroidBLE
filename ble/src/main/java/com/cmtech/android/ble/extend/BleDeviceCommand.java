package com.cmtech.android.ble.extend;

import android.os.Handler;

import com.vise.log.ViseLog;

/**
  *
  * ClassName:      BleDeviceCommand
  * Description:    表示BLE设备的操作命令
  * Author:         chenm
  * CreateDate:     2019-06-25 17:54
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 17:54
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

abstract class BleDeviceCommand {
    static volatile boolean waitingResponse = false; // 是否在等待响应

    private static int curReconnectTimes = 0; // 重连次数

    final BleDevice device;

    BleDeviceCommand(BleDevice device) {
        this.device = device;
    }

    abstract void execute(Handler handler);

    static synchronized void resetReconnectTimes() {
        curReconnectTimes = 0;
    }

    static synchronized void addReconnectTimes() {
        curReconnectTimes++;
    }

    synchronized void reconnect() {
        int totalReconnectTimes = device.getReconnectTimes();

        if(totalReconnectTimes != -1 && curReconnectTimes >= totalReconnectTimes) {
            device.notifyReconnectFailure();

            device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

            curReconnectTimes = 0;
        } else {
            device.startScan();
            ViseLog.e("reconnect times: " + curReconnectTimes);
        }
    }
}
