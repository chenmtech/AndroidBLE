package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.callback.IBleScanCallback;
import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
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

    private volatile BleDeviceState connectState = CONNECT_DISCONNECT; // 设备连接状态

    private final BleDeviceScanner scanner = new BleDeviceScanner(); // 蓝牙扫描仪，完成开始扫描和停止扫描的功能

    private BleDeviceGatt bleDeviceGatt; // 设备Gatt，连接成功后赋值，完成连接以及数据通信等功能

    private int battery = -1; // 设备电池电量

    private final List<OnBleDeviceStateListener> stateListeners = new LinkedList<>(); // 设备状态监听器列表

    private final BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。设备连接成功后启动，设备连接失败或者断开时被停止

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private ExecutorService autoConnService; // 自动连接线程池

    private final IBleScanCallback bleScanCallback = new IBleScanCallback() {
        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            ViseLog.e("Found device: device address = " + bluetoothLeDevice.getAddress());

            BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                processScanResult(null);

            } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                processScanResult(bluetoothLeDevice);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            ViseLog.e("Scan Failed: errorCode = " + errorCode);

            processScanResult(null);
        }
    }; // 扫描回调

    // 处理扫描结果
    private void processScanResult(final BluetoothLeDevice bluetoothLeDevice) {

        ViseLog.e("处理扫描结果: " + bluetoothLeDevice);

        stopScanForever();

        if (bluetoothLeDevice != null) { // 扫描到设备，发起连接
            BleDeviceGatt bleDeviceGatt = new BleDeviceGatt(bluetoothLeDevice);

            bleDeviceGatt.connect(context, connectCallback);

            setState(DEVICE_CONNECTING);
        }
    }

    private final IBleConnectCallback connectCallback = new IBleConnectCallback() {
        // 连接成功
        @Override
        public void onConnectSuccess(final BleDeviceGatt bleDeviceGatt) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    processConnectSuccess(bleDeviceGatt);
                }
            });
        }

        // 连接失败
        @Override
        public void onConnectFailure(final BleException exception) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    processConnectFailure(exception);
                }
            });
        }

        // 连接中断
        @Override
        public void onDisconnect() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    processDisconnect();
                }
            });
        }
    }; // 连接回调

    // 处理连接成功回调
    private void processConnectSuccess(BleDeviceGatt bleDeviceGatt) {
        // 防止重复连接成功
        if(getState() == CONNECT_SUCCESS) {
            ViseLog.e("再次连接成功!!!");

            return;
        }

        if(getState() == DEVICE_CLOSED) { // 设备已经关闭了，强行断开
            bleDeviceGatt.clear();

            return;
        }

        if(getState() == DEVICE_SCANNING) {
            stopScanForever();
        }

        ViseLog.e("处理连接成功: " + bleDeviceGatt);

        this.bleDeviceGatt = bleDeviceGatt;

        startGattExecutor();

        if(executeAfterConnectSuccess())
            setConnectState(CONNECT_SUCCESS);
        else {
            startDisconnection();
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("处理连接失败: " + bleException );

        stopGattExecutor();

        executeAfterConnectFailure();

        bleDeviceGatt = null;

        setConnectState(CONNECT_FAILURE);

        if(bleException instanceof TimeoutException) {
            stopScanForever();
        }
    }

    // 处理连接断开
    private void processDisconnect() {
        if(connectState != CONNECT_DISCONNECT) {
            ViseLog.e("处理连接断开");

            stopGattExecutor();

            executeAfterDisconnect();

            bleDeviceGatt = null;

            setConnectState(CONNECT_DISCONNECT);
        }
    }

    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;

        setState(connectState);
    }

    public BleDevice(Context context, BleDeviceBasicInfo basicInfo) {
        this.context = context;

        this.basicInfo = basicInfo;

        gattCmdExecutor = new BleSerialGattCommandExecutor(this);

        bleDeviceGatt = null;

        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(getMacAddress()).build();

        scanner.setScanFilter(scanFilter);
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
                                scanner.startScan(getContext(), bleScanCallback);
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
            stopScanForever();
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
        setState(DEVICE_DISCONNECTING);

        if(getBleDeviceGatt() != null) {
            getBleDeviceGatt().disconnect();
        }
    }

    private void stopScanForever() {
        ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);

        mHandler.removeCallbacksAndMessages(null);

        scanner.stopScan(getContext()); // 设备处于扫描时，停止扫描
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

    protected final void read(BleGattElement element, IBleDataCallback dataCallback) {
        gattCmdExecutor.read(element, dataCallback);
    }

    protected final void write(BleGattElement element, byte[] data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    protected final void write(BleGattElement element, byte data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    protected final void notify(BleGattElement element, boolean enable, IBleDataCallback notifyOpCallback) {
        gattCmdExecutor.notify(element, enable, notifyOpCallback);
    }

    protected final void indicate(BleGattElement element, boolean enable, IBleDataCallback indicateOpCallback) {
        gattCmdExecutor.indicate(element, enable, indicateOpCallback);
    }

    protected final void runInstantly(IBleDataCallback dataCallback) {
        gattCmdExecutor.runInstantly(dataCallback);
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
