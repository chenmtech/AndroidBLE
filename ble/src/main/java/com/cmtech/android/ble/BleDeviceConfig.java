package com.cmtech.android.ble;

import android.content.Context;

import com.cmtech.android.ble.common.BleConfig;
import com.cmtech.android.ble.extend.BleDeviceState;
import com.cmtech.android.ble.extend.BleDeviceType;

/**
 * BleDeviceConfig: 进行一些初始化和配置
 * Created by bme on 2018/10/22.
 */

public class BleDeviceConfig {

    private BleDeviceConfig() {

    }

    public static void initialize(Context context) {
        //ViseBle.getInstance().init(context);
    }

    // 配置扫描超时时间
    public static void setScanTimeout(int scanTimeout) {
        BleConfig.getInstance().setScanTimeout(scanTimeout);
    }

    // 配置连接超时时间
    public static void setConnectTimeout(int connectTimeout) {
        BleConfig.getInstance().setConnectTimeout(connectTimeout);
    }

    public static void addSupportedDeviceType(BleDeviceType deviceType) {
        BleDeviceType.addSupportedType(deviceType);
    }

    public static void setStateDescription(BleDeviceState connectState, String description) {
        connectState.setDescription(description);
    }

    public static void setStateIcon(BleDeviceState connectState, int icon) {
        connectState.setIcon(icon);
    }

}
