package com.cmtech.android.ble.core;

import com.cmtech.android.ble.R;

/**
  *
  * ClassName:      BleDeviceState
  * Description:    BleDevice设备状态类
  * Author:         chenm
  * CreateDate:     2018/4/21 下午4:47
  * UpdateUser:     chenm
  * UpdateDate:     2019/4/19 下午4:47
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class BleDeviceState {
    private static final int CLOSED_CODE = 0x00; // 已关闭
    private static final int SCANNING_CODE = 0x01; // 正在扫描
    private static final int CONNECTING_CODE = 0x02; // 正在连接
    private static final int DISCONNECTING_CODE = 0x03; // 正在断开
    private static final int CONNECT_SUCCESS_CODE = 0x04; // 连接成功
    private static final int CONNECT_FAILURE_CODE = 0x05;// 连接失败
    private static final int CONNECT_DISCONNECT_CODE = 0x06; // 连接断开

    public static final BleDeviceState CLOSED = new BleDeviceState(CLOSED_CODE, "已关闭", R.mipmap.ic_disconnect_32px);
    public static final BleDeviceState SCANNING = new BleDeviceState(SCANNING_CODE, "正在扫描", R.mipmap.ic_scanning_32px);
    public static final BleDeviceState CONNECTING = new BleDeviceState(CONNECTING_CODE, "连接中", R.mipmap.ic_connecting_32px);
    public static final BleDeviceState DISCONNECTING = new BleDeviceState(DISCONNECTING_CODE, "断开中", R.mipmap.ic_connecting_32px);
    public static final BleDeviceState CONNECT = new BleDeviceState(CONNECT_SUCCESS_CODE, "已连接", R.mipmap.ic_connected_32px);
    public static final BleDeviceState FAILURE = new BleDeviceState(CONNECT_FAILURE_CODE, "连接失败", R.mipmap.ic_disconnect_32px);
    public static final BleDeviceState DISCONNECT = new BleDeviceState(CONNECT_DISCONNECT_CODE, "未连接", R.mipmap.ic_disconnect_32px);

    private final int code; // 状态码
    private String description; // 状态描述
    private int icon; // 状态图标

    private BleDeviceState(int code, String description, int icon) {
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
