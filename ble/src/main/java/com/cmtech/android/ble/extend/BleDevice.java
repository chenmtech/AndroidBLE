package com.cmtech.android.ble.extend;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SCANNING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SUCCESS;


/**
  *
  * ClassName:      BleDevice
  * Description:    表示低功耗蓝牙设备
  * Author:         chenm
  * CreateDate:     2018-02-19 07:02
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-05 07:02
  * UpdateRemark:   更新说明
  * Version:        1.0
 */


public abstract class BleDevice {
    public final static BleDeviceConnectState DEVICE_INIT_STATE = BleDeviceConnectState.CONNECT_CLOSED; // 初始连接状态

    private BleDeviceBasicInfo basicInfo; // 设备基本信息对象

    private BluetoothLeDevice bluetoothLeDevice = null; // 设备BluetoothLeDevice，当扫描到设备后会赋值

    private BleDeviceConnectState connectState = DEVICE_INIT_STATE; // 设备连接状态

    private final List<OnBleDeviceListener> deviceStateListeners = new LinkedList<>(); // 设备状态观察者列表

    private int battery = -1; // 设备电池电量

    private final BleSerialGattCommandExecutor gattCmdExecutor = new BleSerialGattCommandExecutor(this); // 串行Gatt命令执行器

    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程Handler

    private Handler deviceCmdHandle; // 设备连接相关命令线程Handler

    private final BleDeviceScanCommand scanCommand; // 扫描命令

    private final BleDeviceConnectCommand connectCommand; // 连接命令

    private final BleDeviceDisconnectCommand disconnectCommand; // 断开命令

    public BleDevice(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;

        scanCommand = new BleDeviceScanCommand(this);

        connectCommand = new BleDeviceConnectCommand(this);

        disconnectCommand = new BleDeviceDisconnectCommand(this);
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
                throw new NullPointerException("The device type can't be found out.");
            }

            int imageId = deviceType.getDefaultImage();

            return ContextCompat.getDrawable(context, imageId);
        } else {
            return new BitmapDrawable(context.getResources(), getImagePath());
        }
    }

    // 登记设备状态观察者
    public final void addConnectStateListener(OnBleDeviceListener listener) {
        if(!deviceStateListeners.contains(listener)) {
            deviceStateListeners.add(listener);
        }
    }

    // 删除设备状态观察者
    public final void removeConnectStateListener(OnBleDeviceListener listener) {
        deviceStateListeners.remove(listener);
    }

    DeviceMirror getDeviceMirror() {
        return ViseBle.getInstance().getDeviceMirror(bluetoothLeDevice);
    }

    BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    void setBluetoothLeDevice(BluetoothLeDevice bluetoothLeDevice) {
        this.bluetoothLeDevice = bluetoothLeDevice;
    }

    public boolean isClosed() {
        return connectState == BleDeviceConnectState.CONNECT_CLOSED;
    }

    public boolean isConnected() {
        return connectState == CONNECT_SUCCESS;
    }

    public boolean isWaitingResponse() {
        return connectState == CONNECT_SCANNING || connectState == CONNECT_CONNECTING;
    }

    BleDeviceConnectState getConnectState() {
        return connectState;
    }

    void setConnectState(BleDeviceConnectState connectState) {
        if(this.connectState != connectState) {
            ViseLog.e(connectState);

            this.connectState = connectState;

            updateConnectState();
        }
    }

    public String getConnectStateDescription() {
        return connectState.getDescription();
    }

    public int getConnectStateIcon() {
        return connectState.getIcon();
    }

    public int getBattery() {
        return battery;
    }

    protected void setBattery(final int battery) {
        this.battery = battery;

        for(final OnBleDeviceListener listener : deviceStateListeners) {
            if(listener != null) {
                listener.onBatteryUpdated(this);
            }
        }
    }

    // 打开设备
    public void open() {
        ViseLog.i(getMacAddress() + ": open()");

        if(!isClosed())
            return;

        HandlerThread handlerThread = new HandlerThread("MT_Conn_Device");

        handlerThread.start();

        deviceCmdHandle = new Handler(handlerThread.getLooper());

        if(basicInfo.autoConnect()) {
            startScan();
        }
    }

    // 关闭设备
    public void close() {
        ViseLog.i(getMacAddress() + ": close()");

        if(isClosed()) return;

        if(deviceCmdHandle == null) return;

        if (connectState == CONNECT_SCANNING)
            stopScan();

        disconnect();

        deviceCmdHandle.post(new Runnable() {
            @Override
            public void run() {
                setConnectState(BleDeviceConnectState.CONNECT_CLOSED);
            }
        });

        deviceCmdHandle.getLooper().quitSafely();
    }

    // 切换设备状态
    public final boolean switchState() {
        ViseLog.i("switchDeviceState");

        boolean switched = true;

        if(connectState == CONNECT_SUCCESS) {
            disconnect();

        } else if(connectState == CONNECT_SCANNING || connectState == CONNECT_CONNECTING) {
            switched = false;
        } else {
            startScan();
        }

        return switched;
    }

    protected void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    // 开始扫描
    protected void startScan() {
        scanCommand.execute(deviceCmdHandle);
    }

    // 停止扫描
    private void stopScan() {
        scanCommand.stop(deviceCmdHandle);
    }

    // 开始连接，有些资料说最好放到UI线程中执行连接
    void startConnect() {
        connectCommand.execute(deviceCmdHandle);
    }

    // 断开连接
    protected void disconnect() {
        disconnectCommand.execute(deviceCmdHandle);
    }

    /*
     * 抽象方法
     */
    protected abstract boolean executeAfterConnectSuccess(); // 连接成功后执行的操作

    protected abstract void executeAfterConnectFailure(); // 连接错误后执行的操作

    protected abstract void executeAfterDisconnect(); // 断开连接后执行的操作



    // 通知设备状态观察者
    public final void updateConnectState() {
        for(OnBleDeviceListener listener : deviceStateListeners) {
            if(listener != null) {
                listener.onConnectStateUpdated(this);
            }
        }
    }

    public final void notifyReconnectFailure() {
        if(basicInfo.isWarnAfterReconnectFailure()) {
            notifyReconnectFailure(true);
        }
    }

    // 通知设备状态观察者重连失败，是否报警
    public final void notifyReconnectFailure(final boolean isWarn) {
        for(final OnBleDeviceListener listener : deviceStateListeners) {
            if(listener != null) {
                listener.onReconnectFailureNotified(BleDevice.this, isWarn);
            }
        }
    }


    protected void startGattExecutor() {
        gattCmdExecutor.start();
    }

    protected void stopGattExecutor() {
        gattCmdExecutor.stop();
    }

    protected boolean isGattExecutorAlive() {
        return gattCmdExecutor.isAlive();
    }

    // 检测设备中是否包含Gatt Elements
    protected boolean checkElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if( element == null || element.retrieveGattObject(this) == null )
                return false;
        }

        return true;
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

    protected final void executeInstantly(IGattDataCallback gattDataCallback) {
        gattCmdExecutor.executeInstantly(gattDataCallback);
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
