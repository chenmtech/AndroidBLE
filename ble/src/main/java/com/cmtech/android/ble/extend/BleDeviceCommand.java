package com.cmtech.android.ble.extend;

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
    boolean waitingResponse = false;

    final BleDevice device;

    BleDeviceCommand(BleDevice device) {
        this.device = device;
    }

    abstract void execute() throws InterruptedException;
}
