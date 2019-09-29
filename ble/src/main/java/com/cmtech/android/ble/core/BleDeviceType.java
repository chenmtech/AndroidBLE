package com.cmtech.android.ble.core;

import java.util.ArrayList;
import java.util.List;


/**
  *
  * ClassName:      BleDeviceType
  * Description:    Ble设备类型类
  * Author:         chenm
  * CreateDate:     2018-10-13 08:55
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-28 08:55
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class BleDeviceType {
    private final static List<BleDeviceType> SUPPORTED_DEVICE_TYPES = new ArrayList<>(); // 支持的设备类型数组

    private final String uuid; // 设备16位UUID字符串
    private final int defaultImage; // 缺省图标
    private final String defaultNickname; // 缺省设备名
    private final String factoryClassName; // 设备工厂类名

    public BleDeviceType(String uuid, int defaultImage, String defaultNickname, String factoryClassName) {
        this.uuid = uuid;
        this.defaultImage = defaultImage;
        this.defaultNickname = defaultNickname;
        this.factoryClassName = factoryClassName;
    }

    public static void addSupportedType(BleDeviceType deviceType) {
        if(!SUPPORTED_DEVICE_TYPES.contains(deviceType)) {
            SUPPORTED_DEVICE_TYPES.add(deviceType);
        }
    }

    // 通过UUID获取对应的设备类型
    public static BleDeviceType getFromUuid(String uuid) {
        for(BleDeviceType type : SUPPORTED_DEVICE_TYPES) {
            if(type.getUuid().equalsIgnoreCase(uuid)) {
                return type;
            }
        }
        return null;
    }

    public String getUuid() {
        return uuid;
    }
    public int getDefaultImage() {
        return defaultImage;
    }
    public String getDefaultNickname() {
        return defaultNickname;
    }
    public String getFactoryClassName() { return factoryClassName; }
}
