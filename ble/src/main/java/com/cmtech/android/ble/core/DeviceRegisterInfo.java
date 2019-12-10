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
    public static final String DEFAULT_DEVICE_IMAGE_PATH = ""; // 缺省设备图标路径名
    public static final boolean DEFAULT_DEVICE_AUTO_CONNECT = true; // 设备打开时是否自动连接
    public static final boolean DEFAULT_WARN_WHEN_BLE_INNER_ERROR = true; // 缺省的蓝牙内部错误是否报警

    protected final String macAddress; // 设备mac地址
    protected final String uuidStr; // 设备广播Uuid16位字符串
    protected String name = DEFAULT_DEVICE_NAME; // 设备昵称
    protected String imagePath = DEFAULT_DEVICE_IMAGE_PATH; // 设备图标完整路径
    protected boolean autoConnect = DEFAULT_DEVICE_AUTO_CONNECT; // 设备打开后是否自动连接
    protected boolean warnWhenBleInnerError = DEFAULT_WARN_WHEN_BLE_INNER_ERROR; // 蓝牙内部错误是否报警

    public DeviceRegisterInfo(String macAddress, String uuidStr) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
    }

    protected DeviceRegisterInfo(String macAddress, String uuidStr, String name, String imagePath,
                                 boolean autoConnect, boolean warnWhenBleInnerError) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
        this.name = name;
        this.imagePath = imagePath;
        this.autoConnect = autoConnect;
        this.warnWhenBleInnerError = warnWhenBleInnerError;
    }

    public abstract boolean isLocal();
    public String getMacAddress() {
        return macAddress;
    }
    public String getUuidStr() {
        return uuidStr;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public boolean autoConnect() {
        return autoConnect;
    }
    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }
    public boolean isWarnWhenBleInnerError() {
        return warnWhenBleInnerError;
    }
    public void setWarnWhenBleInnerError(boolean warnWhenBleInnerError) {
        this.warnWhenBleInnerError = warnWhenBleInnerError;
    }

    public void update(DeviceRegisterInfo registerInfo) {
        if (macAddress.equalsIgnoreCase(registerInfo.macAddress) && uuidStr.equalsIgnoreCase(registerInfo.uuidStr)) {
            name = registerInfo.name;
            imagePath = registerInfo.imagePath;
            autoConnect = registerInfo.autoConnect;
            warnWhenBleInnerError = registerInfo.warnWhenBleInnerError;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceRegisterInfo that = (DeviceRegisterInfo) o;
        return macAddress.equalsIgnoreCase(that.macAddress) && isLocal() == that.isLocal();
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode() + (isLocal() ? 0 : 1);
    }
}
