package com.cmtech.android.ble.core;

import com.cmtech.android.ble.R;

/**
  *
  * ClassName:      DeviceState
  * Description:    connCallback state
  * Author:         chenm
  * CreateDate:     2018/4/21 下午4:47
  * UpdateUser:     chenm
  * UpdateDate:     2019/4/19 下午4:47
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class DeviceState {
    private static final int CLOSED_CODE = 0x00; // closed
    private static final int CONNECT_CODE = 0x01; // connected
    private static final int FAILURE_CODE = 0x02;// connect failure
    private static final int DISCONNECT_CODE = 0x03; // disconnect
    private static final int CONNECTING_CODE = 0x04; // connecting
    private static final int DISCONNECTING_CODE = 0x05; // disconnecting

    public static final DeviceState CLOSED = new DeviceState(CLOSED_CODE, "已关闭", R.mipmap.ic_disconnect_32px);
    public static final DeviceState CONNECT = new DeviceState(CONNECT_CODE, "已连接", R.mipmap.ic_connected_32px);
    public static final DeviceState FAILURE = new DeviceState(FAILURE_CODE, "连接失败", R.mipmap.ic_disconnect_32px);
    public static final DeviceState DISCONNECT = new DeviceState(DISCONNECT_CODE, "未连接", R.mipmap.ic_disconnect_32px);
    public static final DeviceState CONNECTING = new DeviceState(CONNECTING_CODE, "连接中", R.drawable.connecting);
    public static final DeviceState DISCONNECTING = new DeviceState(DISCONNECTING_CODE, "断开中", R.drawable.connecting);

    private final int code; // code
    private String description; // state description
    private int icon; // state icon

    private DeviceState(int code, String description, int icon) {
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
