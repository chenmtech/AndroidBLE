package com.cmtech.android.ble.core;


import java.io.Serializable;


/**
 * ClassName:      DeviceRegisterInfo
 * Description:    设备注册信息，字段信息将保存在Preference中
 * Author:         chenm
 * CreateDate:     2018-06-27 08:56
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-28 08:56
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public abstract class DeviceRegisterInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_DEVICE_NAME = ""; // 缺省设备名
    public static final String DEFAULT_DEVICE_ICON = ""; // 缺省设备图标路径名
    public static final boolean DEFAULT_DEVICE_AUTO_CONNECT = true; // 设备打开时是否自动连接

    protected final String address; // mac address
    protected final String uuid; // uuid string
    protected String name = DEFAULT_DEVICE_NAME; // device name
    protected String icon = DEFAULT_DEVICE_ICON; // 设备图标完整路径
    protected boolean autoConnect = DEFAULT_DEVICE_AUTO_CONNECT; // 设备打开后是否自动连接

    public DeviceRegisterInfo(String address, String uuid) {
        this.address = address;
        this.uuid = uuid;
    }

    protected DeviceRegisterInfo(String address, String uuid, String name, String icon,
                                 boolean autoConnect) {
        this.address = address;
        this.uuid = uuid;
        this.name = name;
        this.icon = icon;
        this.autoConnect = autoConnect;
    }

    public abstract boolean isLocal();
    public String getAddress() {
        return address;
    }
    public String getUuid() {
        return uuid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
    public boolean isAutoConnect() {
        return autoConnect;
    }
    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void update(DeviceRegisterInfo registerInfo) {
        if (address.equalsIgnoreCase(registerInfo.address) && uuid.equalsIgnoreCase(registerInfo.uuid)) {
            name = registerInfo.name;
            icon = registerInfo.icon;
            autoConnect = registerInfo.autoConnect;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceRegisterInfo that = (DeviceRegisterInfo) o;
        return address.equalsIgnoreCase(that.address) && isLocal() == that.isLocal();
    }

    @Override
    public int hashCode() {
        return address.hashCode() + (isLocal() ? 0 : 1);
    }
}
