package com.cmtech.android.ble.core;

import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;
import static com.cmtech.android.ble.core.BleDeviceState.SCANNING;

public abstract class AbstractDevice implements IDevice{
    protected final DeviceRegisterInfo registerInfo; // 注册信息
    protected volatile BleDeviceState state = CLOSED; // 实时状态
    private final List<OnDeviceListener> listeners; // 监听器列表
    private int battery = INVALID_BATTERY; // 电池电量

    public interface MyCallback {
        boolean executeAfterConnectSuccess();
        void executeAfterConnectFailure();
        void executeAfterDisconnect();
    }

    protected MyCallback myCallback;

    public AbstractDevice(DeviceRegisterInfo registerInfo) {
        if(registerInfo == null) {
            throw new NullPointerException("The register info of BleDevice is null.");
        }
        this.registerInfo = registerInfo;
        listeners = new LinkedList<>();
    }

    @Override
    public void setCallback(MyCallback myCallback) {
        this.myCallback = myCallback;
    }

    @Override
    public DeviceRegisterInfo getRegisterInfo() {
        return registerInfo;
    }
    @Override
    public void updateRegisterInfo(DeviceRegisterInfo registerInfo) {
        this.registerInfo.update(registerInfo);
    }
    @Override
    public String getAddress() {
        return registerInfo.getMacAddress();
    }
    @Override
    public String getName() {
        return registerInfo.getName();
    }
    @Override
    public String getUuidString() {
        return registerInfo.getUuidStr();
    }
    @Override
    public String getImagePath() {
        return registerInfo.getImagePath();
    }
    @Override
    public BleDeviceState getState() {
        return state;
    }
    @Override
    public boolean isScanning() {
        return state == SCANNING;
    }
    @Override
    public boolean isConnected() {
        return state == CONNECT;
    }
    @Override
    public boolean isDisconnected() {
        return state == FAILURE || state == DISCONNECT;
    }
    @Override
    public void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("The state of device " + getAddress() + " is " + state);
            this.state = state;
            updateState();
        }
    }
    // 更新设备状态
    @Override
    public void updateState() {
        for(OnDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onStateUpdated(this);
            }
        }
    }
    @Override
    public int getBattery() {
        return battery;
    }
    @Override
    public void setBattery(final int battery) {
        if(this.battery != battery) {
            this.battery = battery;
            updateBattery();
        }
    }
    @Override
    public final void addListener(OnDeviceListener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    @Override
    public final void removeListener(OnDeviceListener listener) {
        listeners.remove(listener);
    }

    // 更新电池电量
    private void updateBattery() {
        for (final OnDeviceListener listener : listeners) {
            if (listener != null) {
                listener.onBatteryUpdated(this);
            }
        }
    }

    // 通知异常消息
    protected void notifyExceptionMessage(int msgId) {
        for(OnDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onExceptionMsgNotified(this, msgId);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BleDevice that = (BleDevice) o;
        DeviceRegisterInfo thisInfo = getRegisterInfo();
        DeviceRegisterInfo thatInfo = that.getRegisterInfo();
        return thisInfo.equals(thatInfo);
    }

    @Override
    public int hashCode() {
        return (getRegisterInfo() != null) ? getRegisterInfo().hashCode() : 0;
    }
}
