package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.exception.BleException;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractDevice implements IDevice{
    private final DeviceRegisterInfo registerInfo; // 注册信息
    protected final IDeviceConnector connector; // 设备连接器
    private final List<OnDeviceListener> listeners; // 监听器列表
    private int battery; // 电池电量

    public AbstractDevice(DeviceRegisterInfo registerInfo) {
        if(registerInfo == null) {
            throw new NullPointerException("The register info is null.");
        }
        this.registerInfo = registerInfo;
        if(registerInfo.isLocal()) {
            connector = new BleDeviceConnector(this);
        } else {
            connector = new WebDeviceConnector(this);
        }
        listeners = new LinkedList<>();
        battery = INVALID_BATTERY;
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
    public boolean isLocal() {
        return registerInfo.isLocal();
    }
    @Override
    public final String getAddress() {
        return registerInfo.getMacAddress();
    }
    @Override
    public String getUuidString() {
        return registerInfo.getUuidStr();
    }
    @Override
    public String getName() {
        return registerInfo.getName();
    }
    public void setName(String name) {
        registerInfo.setName(name);
    }
    @Override
    public String getImagePath() {
        return registerInfo.getImagePath();
    }
    @Override
    public boolean autoConnect() {
        return registerInfo.autoConnect();
    }
    @Override
    public boolean isWarnWhenBleInnerError() {
        return registerInfo.isWarnWhenBleInnerError();
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
    // 更新电池电量
    private void updateBattery() {
        for (final OnDeviceListener listener : listeners) {
            if (listener != null) {
                listener.onBatteryUpdated(this);
            }
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
    @Override
    public void handleException(BleException ex) {
        for(OnDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onExceptionHandled(this, ex);
            }
        }
    }
    @Override
    public BleDeviceState getState() {
        return connector.getState();
    }
    @Override
    public void setState(BleDeviceState state) {
        connector.setState(state);
    }
    @Override
    public void open(Context context) {
        connector.open(context);
    }
    @Override
    public void switchState() {
        connector.switchState();
    }
    @Override
    public void forceDisconnect(boolean forever) {
        connector.forceDisconnect(forever);
    }
    @Override
    public void close() {
        connector.close();
    }
    @Override
    public void clear() {
        connector.clear();
    }
    @Override
    public boolean isDisconnectedForever() {
        return connector.isDisconnectedForever();
    }
    @Override
    public boolean isConnected() {
        return connector.isConnected();
    }
    @Override
    public boolean isDisconnected() {
        return connector.isDisconnected();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractDevice)) return false;
        AbstractDevice that = (AbstractDevice) o;
        return registerInfo.equals(that.registerInfo);
    }
    @Override
    public int hashCode() {
        return (registerInfo != null) ? registerInfo.hashCode() : 0;
    }
}
