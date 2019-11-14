package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.R;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.SCANNING;

public abstract class AbstractDevice {
    public static final int INVALID_BATTERY = -1; // 无效电池电量
    public static final int MSG_INVALID_OPERATION = R.string.invalid_operation;

    // 设备监听器接口
    public interface OnBleDeviceListener {
        void onStateUpdated(final AbstractDevice device); // 设备状态更新
        void onExceptionMsgNotified(final AbstractDevice device, int msgId); // 异常消息通知
        void onBatteryUpdated(final AbstractDevice device); // 电池电量更新
    }

    protected final BleDeviceRegisterInfo registerInfo; // 注册信息
    protected volatile BleDeviceState state = CLOSED; // 实时状态
    private final List<OnBleDeviceListener> listeners; // 监听器列表
    private int battery = INVALID_BATTERY; // 电池电量


    public AbstractDevice(BleDeviceRegisterInfo registerInfo) {
        if(registerInfo == null) {
            throw new NullPointerException("The register info of BleDevice is null.");
        }
        this.registerInfo = registerInfo;
        listeners = new LinkedList<>();
    }

    public BleDeviceRegisterInfo getRegisterInfo() {
        return registerInfo;
    }
    public void updateRegisterInfo(BleDeviceRegisterInfo registerInfo) {
        this.registerInfo.update(registerInfo);
    }
    public String getAddress() {
        return registerInfo.getMacAddress();
    }
    public String getName() {
        return registerInfo.getNickName();
    }
    public String getUuidString() {
        return registerInfo.getUuidStr();
    }
    public String getImagePath() {
        return registerInfo.getImagePath();
    }
    public BleDeviceState getState() {
        return state;
    }
    protected boolean isScanning() {
        return state == SCANNING;
    }
    protected boolean isConnected() {
        return state == CONNECT;
    }
    protected boolean isDisconnected() {
        return state == FAILURE || state == DISCONNECT;
    }
    protected void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("The state of device " + getAddress() + " is " + state);
            this.state = state;
            updateState();
        }
    }
    // 更新设备状态
    public void updateState() {
        for(OnBleDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onStateUpdated(this);
            }
        }
    }
    public int getBattery() {
        return battery;
    }
    protected void setBattery(final int battery) {
        if(this.battery != battery) {
            this.battery = battery;
            updateBattery();
        }
    }
    public final void addListener(OnBleDeviceListener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    public final void removeListener(OnBleDeviceListener listener) {
        listeners.remove(listener);
    }

    public abstract void open(Context context); // 打开设备
    public abstract void switchState(); // 切换状态
    public abstract void callDisconnect(boolean stopAutoScan); // 请求断开
    public abstract boolean isStopped(); // 是否停止
    public abstract void close(); // 关闭设备
    public abstract void clear(); // 清除设备


    protected abstract boolean executeAfterConnectSuccess(); // 连接成功后执行的操作
    protected abstract void executeAfterConnectFailure(); // 连接错误后执行的操作
    protected abstract void executeAfterDisconnect(); // 断开连接后执行的操作

    // 更新电池电量
    private void updateBattery() {
        for (final OnBleDeviceListener listener : listeners) {
            if (listener != null) {
                listener.onBatteryUpdated(this);
            }
        }
    }

    // 通知异常消息
    protected void notifyExceptionMessage(int msgId) {
        for(OnBleDeviceListener listener : listeners) {
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
        BleDeviceRegisterInfo thisInfo = getRegisterInfo();
        BleDeviceRegisterInfo thatInfo = that.getRegisterInfo();
        return Objects.equals(thisInfo, thatInfo);
    }

    @Override
    public int hashCode() {
        return (getRegisterInfo() != null) ? getRegisterInfo().hashCode() : 0;
    }
}
