package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.common.BleConfig;

/**
 * BleDeviceConfig: 进行一些viseBle包内部的配置
 * Created by bme on 2018/10/22.
 */

public class BleDeviceConfig {
    private BleDeviceConfig() {
    }

    // 配置扫描超时时间
    public static void setScanTimeout(int scanTimeout) {
        BleConfig.getInstance().setScanTimeout(scanTimeout);
    }

    // 配置连接超时时间
    public static void setConnectTimeout(int connectTimeout) {
        BleConfig.getInstance().setConnectTimeout(connectTimeout);
    }

    // 配置重连时间间隔
    public static void setReconnectInterval(int reconnectInterval) {
        BleConfig.getInstance().setConnectRetryInterval(reconnectInterval);
    }

    // 配置重连次数
    public static void setConnectRetryCount(int connectRetryCount) {
        BleConfig.getInstance().setConnectRetryCount(connectRetryCount);
    }

    // 配置数据操作重复次数
    public static void setOpDataRetryCount(int opDataRetryCount) {
        BleConfig.getInstance().setOperateRetryCount(opDataRetryCount);
    }

}
