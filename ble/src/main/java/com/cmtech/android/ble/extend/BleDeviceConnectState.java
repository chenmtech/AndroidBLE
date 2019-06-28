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

public class BleDeviceConnectState {
    static final BleDeviceConnectState CONNECT_CLOSED = new BleDeviceConnectState(0x00, "已关闭", R.mipmap.ic_disconnect_32px);

    static final BleDeviceConnectState CONNECT_SCANNING = new BleDeviceConnectState(0x01, "扫描中...", R.mipmap.ic_scanning_32px);

    static final BleDeviceConnectState CONNECT_CONNECTING = new BleDeviceConnectState(0x02, "连接中...", R.mipmap.ic_connecting_32px);

    static final BleDeviceConnectState CONNECT_SUCCESS = new BleDeviceConnectState(0x03, "已连接", R.mipmap.ic_connected_32px);

    static final BleDeviceConnectState CONNECT_FAILURE = new BleDeviceConnectState(0x04, "扫描连接失败", R.mipmap.ic_disconnect_32px);

    static final BleDeviceConnectState CONNECT_DISCONNECT = new BleDeviceConnectState(0x05, "已断开", R.mipmap.ic_disconnect_32px);

    private int code;

    private String description;

    private int icon;

    private BleDeviceConnectState(int code, String description, int icon) {
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

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return description;
    }
}
