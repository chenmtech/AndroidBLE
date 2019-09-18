package com.cmtech.android.ble.extend;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

import com.cmtech.android.ble.callback.IBleGattDataCallback;
import com.cmtech.android.ble.utils.ExecutorUtil;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_FAILURE;
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
    private final Context context;

    private BleDeviceBasicInfo basicInfo; // 设备基本信息

    private volatile BleDeviceState state = DEVICE_CLOSED; // 设备状态

    private BleDeviceGatt bleDeviceGatt; // 设备Gatt，连接成功后赋值

    private int battery = -1; // 设备电池电量

    private final List<OnBleDeviceStateListener> stateListeners = new LinkedList<>(); // 设备状态监听器列表

    private final BleConnectCommandExecutor connCmdExecutor; // 设备连接命令执行器，在主线程中执行连接命令和连接回调

    private final BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。设备连接成功后启动，设备连接失败或者断开时被停止

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private ExecutorService autoConnService; // 自动连接线程池

    public BleDevice(Context context, BleDeviceBasicInfo basicInfo) {
        this.context = context;

        this.basicInfo = basicInfo;

        connCmdExecutor = new BleConnectCommandExecutor(this);

        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        bleDeviceGatt = null;
    }

    public Context getContext() {
        return context;
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

    public BleDeviceGatt getBleDeviceGatt() {
        return bleDeviceGatt;
    }

    void setBleDeviceGatt(BleDeviceGatt bleDeviceGatt) {
        this.bleDeviceGatt = bleDeviceGatt;
    }

    public BleDeviceState getState() {
        return state;
    }

    void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("设备状态：" + state);

            this.state = state;

            updateState();
        }
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

    public boolean isFABRotated() {
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



    // 打开设备
    public void open() {
        ViseLog.e("BleDevice.open()");

        if(isClosed() && basicInfo.autoConnect()) {
            setState(CONNECT_DISCONNECT);

            startConnection();
        }
    }

    private void startConnection() {
        if(autoConnService == null || autoConnService.isTerminated()) {
            autoConnService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "MT_Auto_Connection");
                }
            });

            ((ScheduledExecutorService) autoConnService).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if(getState() == CONNECT_FAILURE || getState() == CONNECT_DISCONNECT) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                connCmdExecutor.startScan();
                            }
                        });
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);

            ViseLog.e("启动自动连接服务");
        }
    }

    // 切换设备状态
    public void switchState() {
        ViseLog.e("BleDevice.switchState()");

        if(getState() == CONNECT_DISCONNECT || getState() == CONNECT_FAILURE) {
            startConnection();

        } else if(getState() == CONNECT_SUCCESS) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);

            mHandler.removeCallbacksAndMessages(null);

            startDisconnection(); // 设备处于连接成功时，断开连接
        } else if(getState() == DEVICE_SCANNING) {
            stopScan();
        }
    }

    // 关闭设备
    public void close() {
        ViseLog.e("BleDevice.close()");

        if(getState() == CONNECT_DISCONNECT || getState() == CONNECT_FAILURE) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);

            mHandler.removeCallbacksAndMessages(null);

            setState(BleDeviceState.DEVICE_CLOSED);
        }

    }

    // 断开连接
    public void startDisconnection() {
        ViseLog.e("BleDevice.startDisconnection()");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        });

    }

    protected void disconnect() {
        connCmdExecutor.disconnect();
    }

    void stopScan() {
        ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);

        mHandler.removeCallbacksAndMessages(null);

        connCmdExecutor.stopScan(); // 设备处于扫描时，停止扫描
    }



    protected abstract boolean executeAfterConnectSuccess(); // 连接成功后执行的操作

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

    protected final void read(BleGattElement element, IBleGattDataCallback gattDataCallback) {
        gattCmdExecutor.read(element, gattDataCallback);
    }

    protected final void write(BleGattElement element, byte[] data, IBleGattDataCallback gattDataCallback) {
        gattCmdExecutor.write(element, data, gattDataCallback);
    }

    protected final void write(BleGattElement element, byte data, IBleGattDataCallback gattDataCallback) {
        gattCmdExecutor.write(element, data, gattDataCallback);
    }

    protected final void notify(BleGattElement element, boolean enable, IBleGattDataCallback notifyOpCallback) {
        gattCmdExecutor.notify(element, enable, notifyOpCallback);
    }

    protected final void indicate(BleGattElement element, boolean enable, IBleGattDataCallback indicateOpCallback) {
        gattCmdExecutor.indicate(element, enable, indicateOpCallback);
    }

    protected final void runInstantly(IBleGattDataCallback gattDataCallback) {
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
