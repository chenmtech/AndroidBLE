package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.exception.BleException;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractDevice implements IDevice{
    private final DeviceRegisterInfo registerInfo; // device register information
    private int battery; // battery level
    private final List<OnDeviceListener> listeners; // device listeners
    protected final IConnector connector; // connector

    public AbstractDevice(DeviceRegisterInfo registerInfo) {
        if(registerInfo == null) {
            throw new NullPointerException("The register info is null.");
        }
        this.registerInfo = registerInfo;
        if(registerInfo.isLocal()) {
            connector = new BleConnector(this);
        } else {
            connector = new WebConnector(this);
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
    @Override
    public void setName(String name) {
        registerInfo.setName(name);
    }
    @Override
    public String getImagePath() {
        return registerInfo.getImagePath();
    }
    @Override
    public boolean isAutoConnect() {
        return registerInfo.isAutoConnect();
    }
    @Override
    public int getBattery() {
        return battery;
    }
    @Override
    public void setBattery(final int battery) {
        if(this.battery != battery) {
            this.battery = battery;
            for (final OnDeviceListener listener : listeners) {
                if (listener != null) {
                    listener.onBatteryUpdated(this);
                }
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
    public void handleException(BleException ex) {
        for(OnDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onExceptionNotified(this, ex);
            }
        }
    }

    @Override
    public void open(Context context) {
        connector.open(context);
    }
    @Override
    public void connect() {
        connector.connect();
    }
    @Override
    public void disconnect(boolean forever) {
        connector.disconnect(forever);
    }
    @Override
    public void close() {
        connector.close();
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
    public void switchState() {
        connector.switchState();
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
