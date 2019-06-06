package com.cmtech.android.ble.extend;

import java.util.ArrayList;
import java.util.List;

/**
 * BleDeviceType：Ble设备类型
 * Created by bme on 2018/10/13.
 */

public class BleDeviceType {
    private static List<BleDeviceType> supportedTypes = new ArrayList<>(); // 所有支持的设备类型

    private String uuid;                  // 设备16位UUID字符串
    private int defaultImage;             // 缺省图标
    private String defaultNickname;       // 缺省设备名
    private String factoryClassName;      // 设备工厂类名

    public BleDeviceType(String uuid, int defaultImage, String defaultNickname, String factoryClassName) {
        this.uuid = uuid;
        this.defaultImage = defaultImage;
        this.defaultNickname = defaultNickname;
        this.factoryClassName = factoryClassName;
    }

    public static void addSupportedType(BleDeviceType deviceType) {
        if(!supportedTypes.contains(deviceType)) {
            supportedTypes.add(deviceType);
        }
    }

    // 通过UUID获取对应的设备类型
    public static BleDeviceType getFromUuid(String uuid) {
        for(BleDeviceType type : supportedTypes) {
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

    // 创建设备类型对应的工厂
    AbstractBleDeviceFactory createDeviceFactory() {
        if(factoryClassName == null) return null;

        AbstractBleDeviceFactory factory;
        try {
            factory = (AbstractBleDeviceFactory) Class.forName(factoryClassName).newInstance();
        } catch (Exception e) {
            factory = null;
        }
        return factory;
    }
}
