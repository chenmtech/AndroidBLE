package com.cmtech.android.ble.extend;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

import com.cmtech.android.ble.core.DeviceMirror;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_SUCCESS;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CLOSED;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_DISCONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_SCANNING;

/**
  *
  * ClassName:      BleDevice
  * Description:    低功耗蓝牙设备类
  * Author:         chenm
  * CreateDate:     2018-02-19 07:02
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-05 07:02
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public abstract class BleDevice {
    private BleDeviceBasicInfo basicInfo; // 设备基本信息

    private DeviceMirror deviceMirror = null; // 设备镜像，连接成功后赋值

    private int battery = -1; // 设备电池电量

    private final List<OnBleDeviceStateListener> stateListeners = new LinkedList<>(); // 设备状态监听器列表

    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler，连接命令都在主线程中执行

    private final BleConnectCommandExecutor connCmdExecutor; // 设备连接命令执行器，在mainHandler中执行命令

    private final BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。设备连接成功后被启动，设备连接失败或者断开时被停止


    public BleDevice(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;

        connCmdExecutor = new BleConnectCommandExecutor(this);

        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
    }

    public BleDeviceBasicInfo getBasicInfo() {
        return basicInfo;
    }

    public void setBasicInfo(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;
    }

    public String getMacAddress() {
        return basicInfo.getMacAddress();
    }

    public String getNickName() {
        return basicInfo.getNickName();
    }

    public String getUuidString() {
        return basicInfo.getUuidString();
    }

    int getReconnectTimes() {
        return basicInfo.getReconnectTimes();
    }

    public String getImagePath() {
        return basicInfo.getImagePath();
    }

    public Drawable getImageDrawable(Context context) {
        if(getImagePath().equals("")) {
            BleDeviceType deviceType = BleDeviceType.getFromUuid(getUuidString());

            if(deviceType == null) {
                throw new NullPointerException("The device type is not supported.");
            }

            return ContextCompat.getDrawable(context, deviceType.getDefaultImage());
        } else {
            return new BitmapDrawable(context.getResources(), getImagePath());
        }
    }

    DeviceMirror getDeviceMirror() {
        return deviceMirror;
    }

    void setDeviceMirror(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
    }

    public BleDeviceState getState() {
        return (connCmdExecutor == null) ? DEVICE_CLOSED : connCmdExecutor.getState();
    }

    public String getStateDescription() {
        return getState().getDescription();
    }

    public int getStateIcon() {
        return getState().getIcon();
    }

    public boolean isClosed() {
        return getState() == DEVICE_CLOSED;
    }

    protected boolean isConnected() {
        return getState() == CONNECT_SUCCESS;
    }

    private boolean isDisconnected() {
        return getState() == CONNECT_DISCONNECT;
    }

    public boolean isWaitingResponse() {
        return getState() == DEVICE_SCANNING || getState() == DEVICE_CONNECTING || getState() == DEVICE_DISCONNECTING;
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


    public void postWithMainHandler(Runnable runnable) {
        mainHandler.post(runnable);
    }

    // 打开设备
    public void open() {
        ViseLog.e("BleDevice.open()");

        if(isClosed() && basicInfo.autoConnect()) {
            connCmdExecutor.setState(CONNECT_DISCONNECT);

            connCmdExecutor.startScan(); // 设备关闭时，且允许自动连接，则开始扫描
        }
    }

    // 切换设备状态
    public void switchState() {
        ViseLog.e("BleDevice.switchState()");

        if(isConnected()) {
            disconnect(false); // 设备处于连接成功时，断开连接
        } else if(isDisconnected()) {
            connCmdExecutor.startScan(); // 设备处于连接断开时，开始扫描
        }
    }

    // 关闭设备
    public void close() {
        ViseLog.e("BleDevice.close()");

        if(getState() == CONNECT_DISCONNECT) {
            connCmdExecutor.setState(BleDeviceState.DEVICE_CLOSED);
        }

        /*switch(getState().getCode()) {
            case DEVICE_CLOSED_CODE:
            case CONNECT_FAILURE_CODE:
            case DEVICE_CONNECTING_CODE:
                return;

            case DEVICE_SCANNING_CODE:
                connCmdExecutor.stopScan();

                connCmdExecutor.setState(BleDeviceState.DEVICE_CLOSED);

                return;

            case CONNECT_DISCONNECT_CODE:
                connCmdExecutor.setState(BleDeviceState.DEVICE_CLOSED);
                return;

            case CONNECT_SUCCESS_CODE:
                disconnect(false);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(getState() != CONNECT_DISCONNECT);
                        connCmdExecutor.setState(BleDeviceState.DEVICE_CLOSED);
                    }
                }).start();

                return;

                default:

        }*/
    }

    // 断开连接
    public void disconnect(boolean isReconnect) {
        //mainHandler.removeCallbacksAndMessages(null);

        connCmdExecutor.disconnect(isReconnect);
    }

    protected abstract void executeAfterConnectSuccess(); // 连接成功后执行的操作

    protected abstract void executeAfterConnectFailure(); // 连接错误后执行的操作

    protected abstract void executeAfterDisconnect(); // 断开连接后执行的操作




    void startGattExecutor() {
        gattCmdExecutor.start();
    }

    void stopGattExecutor() {
        gattCmdExecutor.stop();
    }

    protected boolean isGattExecutorAlive() {
        return gattCmdExecutor.isAlive();
    }

    protected boolean containGattElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if( element == null || element.retrieveGattObject(this) == null )
                return false;
        }

        return true;
    }

    protected boolean containGattElement(BleGattElement element) {
        return !( element == null || element.retrieveGattObject(this) == null );
    }

    protected final void read(BleGattElement element, IGattDataCallback gattDataCallback) {
        gattCmdExecutor.read(element, gattDataCallback);
    }

    protected final void write(BleGattElement element, byte[] data, IGattDataCallback gattDataCallback) {
        gattCmdExecutor.write(element, data, gattDataCallback);
    }

    protected final void write(BleGattElement element, byte data, IGattDataCallback gattDataCallback) {
        gattCmdExecutor.write(element, data, gattDataCallback);
    }

    protected final void notify(BleGattElement element, boolean enable, IGattDataCallback notifyOpCallback) {
        gattCmdExecutor.notify(element, enable, notifyOpCallback);
    }

    protected final void indicate(BleGattElement element, boolean enable, IGattDataCallback indicateOpCallback) {
        gattCmdExecutor.indicate(element, enable, indicateOpCallback);
    }

    protected final void runInstantly(IGattDataCallback gattDataCallback) {
        gattCmdExecutor.runInstantly(gattDataCallback);
    }







    // 添加设备状态监听器
    public final void addDeviceStateListener(OnBleDeviceStateListener listener) {
        if(!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    // 删除设备状态监听器
    public final void removeDeviceStateListener(OnBleDeviceStateListener listener) {
        stateListeners.remove(listener);
    }

    // 更新设备状态
    public final void updateState() {
        for(OnBleDeviceStateListener listener : stateListeners) {
            if(listener != null) {
                listener.onConnectStateUpdated(this);
            }
        }
    }

    final void notifyReconnectFailure() {
        if(basicInfo.isWarnAfterReconnectFailure()) {
            notifyReconnectFailure(true);
        }
    }

    public final void cancelNotifyReconnectFailure() {
        notifyReconnectFailure(false);
    }

    // 通知重连失败，是否报警
    private void notifyReconnectFailure(final boolean isWarn) {
        for(final OnBleDeviceStateListener listener : stateListeners) {
            if(listener != null) {
                listener.onReconnectFailureNotified(BleDevice.this, isWarn);
            }
        }
    }

    private void updateBattery() {
        for (final OnBleDeviceStateListener listener : stateListeners) {
            if (listener != null) {
                listener.onBatteryUpdated(this);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BleDevice that = (BleDevice) o;

        BleDeviceBasicInfo thisInfo = getBasicInfo();

        BleDeviceBasicInfo thatInfo = that.getBasicInfo();

        return Objects.equals(thisInfo, thatInfo);
    }

    @Override
    public int hashCode() {
        return (getBasicInfo() != null) ? getBasicInfo().hashCode() : 0;
    }

}
