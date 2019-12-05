package com.cmtech.android.ble.core;

import com.vise.log.ViseLog;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.core
 * ClassName:      AbstractDeviceConnector
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-12-06 06:32
 * UpdateUser:     更新者
 * UpdateDate:     2019-12-06 06:32
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public abstract class AbstractDeviceConnector implements IDeviceConnector {
    protected final IDevice device; // 设备
    protected volatile BleDeviceState state = CLOSED; // 实时状态

    public AbstractDeviceConnector(IDevice device) {
        if(device == null) {
            throw new NullPointerException("The device is null");
        }
        this.device = device;
    }

    @Override
    public BleDeviceState getState() {
        return state;
    }

    @Override
    public void setState(BleDeviceState state) {
        if (this.state != state) {
            ViseLog.e("The state of device " + device.getAddress() + " is " + state);
            this.state = state;
            device.updateState();
        }
    }

    @Override
    public boolean isConnected() {
        return state == CONNECT;
    }

    @Override
    public boolean isDisconnected() {
        return state == FAILURE || state == DISCONNECT;
    }
}
