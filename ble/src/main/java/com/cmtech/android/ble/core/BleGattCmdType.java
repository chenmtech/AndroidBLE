package com.cmtech.android.ble.core;

/**
 *
 * ClassName:      BleGattCmdType
 * Description:    Gatt命令类型
 * Author:         chenm
 * CreateDate:     2018-10-13 08:55
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-28 08:55
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public enum BleGattCmdType {
    GATT_CMD_READ(0x01),
    GATT_CMD_WRITE(0x02),
    GATT_CMD_NOTIFY(0x04),
    GATT_CMD_INDICATE(0x08),
    GATT_CMD_INSTANT_RUN(0x10); //即时执行命令属性，即不需要等待蓝牙响应，立即执行回调的命令

    private int code;

    BleGattCmdType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
