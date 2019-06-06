package com.cmtech.android.ble.extend;

import android.content.Context;

/*
 * AbstractBleDeviceFactory：Ble设备抽象工厂
 * Created by bme on 2018/10/13.
 */

public abstract class AbstractBleDeviceFactory {
    // 工厂要用的设备基本信息对象
    protected BleDeviceBasicInfo basicInfo;

    // 获取BleDevice对应的抽象工厂
    public static AbstractBleDeviceFactory getBLEDeviceFactory(BleDevice device) {
        return (device == null) ? null : getBLEDeviceFactory(device.getBasicInfo());
    }

    // 获取BleDeviceBasicInfo对应的抽象工厂
    public static AbstractBleDeviceFactory getBLEDeviceFactory(BleDeviceBasicInfo basicInfo) {
        AbstractBleDeviceFactory factory = null;
        if(basicInfo != null) {
            factory = getBLEDeviceFactory(basicInfo.getUuidString());
        }
        if(factory != null) {
            factory.basicInfo = basicInfo;
        }
        return factory;
    }

    // 创建BleDevice
    public abstract BleDevice createDevice(Context context);

    // 创建Fragment
    public abstract BleDeviceFragment createFragment();

    private static AbstractBleDeviceFactory getBLEDeviceFactory(String uuidString) {
        return getBLEDeviceFactory(BleDeviceType.getFromUuid(uuidString));
    }

    private static AbstractBleDeviceFactory getBLEDeviceFactory(BleDeviceType deviceType) {
        return (deviceType == null) ? null : deviceType.createDeviceFactory();
    }
}
