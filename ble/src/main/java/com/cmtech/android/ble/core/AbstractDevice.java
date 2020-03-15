package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.OtherException;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;

import static com.cmtech.android.ble.core.DeviceState.CLOSED;
import static com.cmtech.android.ble.core.DeviceState.CONNECT;
import static com.cmtech.android.ble.core.DeviceState.CONNECTING;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECTING;
import static com.cmtech.android.ble.core.DeviceState.FAILURE;

public abstract class AbstractDevice implements IDevice{
    private Context context; // context
    private final DeviceRegisterInfo registerInfo; // device register information
    protected volatile DeviceState state = CLOSED; // state
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
    @Override
    public DeviceState getState() {
        return state;
    }
    @Override
    public void setState(DeviceState state) {
        if (this.state != state) {
            ViseLog.e(getAddress() + ": " + state);
            this.state = state;
            for(OnDeviceListener listener : listeners) {
                if(listener != null) {
                    listener.onStateUpdated(this);
                }
            }
        }
    }

    @Override
    public void open(Context context) {
        if (context == null) {
            throw new NullPointerException("The context is null.");
        }

        if (state != CLOSED) {
            ViseLog.e("The device is opened.");
            return;
        }

        ViseLog.e("Device.open()");
        this.context = context;

        connector.open(context);

        if (registerInfo.isAutoConnect()) {
            connect();
        } else {
            setState(DISCONNECT);
        }
    }
    @Override
    public void close() {
        connector.close();
        setState(DeviceState.CLOSED);
    }
    @Override
    public void connect() {
        setState(CONNECTING);
        connector.connect();
    }
    @Override
    public void disconnect(boolean forever) {
        setState(DISCONNECTING);
        connector.disconnect(forever);
    }


    // 切换状态
    @Override
    public void switchState() {
        ViseLog.e("Device.switchState()");
        if (state == FAILURE || state == DISCONNECT) {
            connect();
        } else if (state == CONNECT) {
            disconnect(true);
        } else { // 无效操作
            if(context != null)
                handleException(new OtherException(context.getString(R.string.invalid_operation)));
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
    public boolean onConnectSuccess() {
        setState(CONNECT);
        return true;
    }

    @Override
    public void onConnectFailure() {
        setState(FAILURE);
    }

    @Override
    public void onDisconnect() {
        setState(DISCONNECT);
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
