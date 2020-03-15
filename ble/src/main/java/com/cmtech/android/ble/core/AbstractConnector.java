package com.cmtech.android.ble.core;

import android.content.Context;

import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.DeviceState.CLOSED;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECT;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.core
 * ClassName:      AbstractConnector
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-12-06 06:32
 * UpdateUser:     更新者
 * UpdateDate:     2019-12-06 06:32
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public abstract class AbstractConnector implements IConnector {
    protected final IDevice device; // device
    protected Context context; // 上下文，用于启动蓝牙连接。当调用open()打开设备时赋值

    public AbstractConnector(IDevice device) {
        if(device == null) {
            throw new NullPointerException("The device is null");
        }
        this.device = device;
    }

    // 打开设备
    @Override
    public void open(Context context) {
        this.context = context;
    }

    @Override
    public void close() {
        context = null;
    }
}
