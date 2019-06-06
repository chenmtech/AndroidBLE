package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.R;

/**
  *
  * ClassName:      BleDeviceConnectState
  * Description:    BleDevice连接状态类型，在BLE包基础上增加了扫描和关闭两个状态
  * Author:         chenm
  * CreateDate:     2018/4/21 下午4:47
  * UpdateUser:     chenm
  * UpdateDate:     2019/4/19 下午4:47
  * UpdateRemark:   更新说明
  * Version:        1.0
 */
public enum BleDeviceConnectState {
    CONNECT_CLOSED(0x00, "设备关闭", R.mipmap.ic_disconnect_32px),

    CONNECT_SCANNING(0x01, "正在扫描", R.mipmap.ic_scanning_32px),

    CONNECT_CONNECTING(0x02, "正在连接", R.mipmap.ic_connecting_32px),

    CONNECT_SUCCESS(0x03, "连接成功", R.mipmap.ic_connected_32px),

    CONNECT_FAILURE(0x04, "连接失败", R.mipmap.ic_disconnect_32px),

    CONNECT_DISCONNECT(0x05, "连接断开", R.mipmap.ic_disconnect_32px);

    private int code;

    private String description;

    private int icon;

    BleDeviceConnectState(int code, String description, int icon) {
        this.code = code;

        this.description = description;

        this.icon = icon;
    }


    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getIcon() {
        return icon;
    }

    public static String getDescriptionFromCode(int code) {
        for(BleDeviceConnectState ele : BleDeviceConnectState.values()) {
            if(ele.code == code) {
                return ele.description;
            }
        }
        return "";
    }

    public static BleDeviceConnectState getFromCode(int code) {
        for(BleDeviceConnectState ele : BleDeviceConnectState.values()) {
            if(ele.code == code) {
                return ele;
            }
        }
        return null;
    }
}
