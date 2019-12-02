package com.cmtech.android.ble.core;

import android.content.Context;

import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;

public abstract class AbstractDevice implements IDevice{
    protected final DeviceRegisterInfo registerInfo; // 注册信息
    protected volatile BleDeviceState state = CLOSED; // 实时状态
    private final List<OnDeviceListener> listeners; // 监听器列表
    private int battery = INVALID_BATTERY; // 电池电量
    protected IDeviceConnector connector;

    public AbstractDevice(DeviceRegisterInfo registerInfo) {
        if(registerInfo == null) {
            throw new NullPointerException("The register info is null.");
        }
        this.registerInfo = registerInfo;
        listeners = new LinkedList<>();
    }

    public final void setDeviceConnector(IDeviceConnector connector) {
        this.connector = connector;
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


    // 通知异常消息
    public void notifyExceptionMessage(int msgId) {
        for(OnDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onExceptionMsgNotified(this, msgId);
            }
        }
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
    public void callDisconnect(boolean stopAutoScan) {
        connector.callDisconnect(stopAutoScan);
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
    public boolean isStopped() {
        return connector.isStopped();
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
    public boolean isLocal() {
        return connector.isLocal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDevice that = (AbstractDevice) o;
        DeviceRegisterInfo thisInfo = getRegisterInfo();
        DeviceRegisterInfo thatInfo = that.getRegisterInfo();
        return thisInfo.equals(thatInfo);
    }

    @Override
    public int hashCode() {
        return (getRegisterInfo() != null) ? getRegisterInfo().hashCode() : 0;
    }
}
