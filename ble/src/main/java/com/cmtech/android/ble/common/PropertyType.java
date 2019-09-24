package com.cmtech.android.ble.common;


public enum PropertyType {
    PROPERTY_READ(0x01),
    PROPERTY_WRITE(0x02),
    PROPERTY_NOTIFY(0x04),
    PROPERTY_INDICATE(0x08),
    PROPERTY_INSTANTRUN(0x10); //即时命令属性，即不需要等待蓝牙响应，立即执行回调的命令

    private int propertyValue;

    PropertyType(int propertyValue) {
        this.propertyValue = propertyValue;
    }

    public int getPropertyValue() {
        return propertyValue;
    }
}
