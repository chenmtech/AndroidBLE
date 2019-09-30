package com.cmtech.android.ble.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.callback.IBleScanCallback;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
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

import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_FAILURE;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_SUCCESS;
import static com.cmtech.android.ble.core.BleDeviceState.DEVICE_CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.DEVICE_CONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.DEVICE_DISCONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.DEVICE_SCANNING;

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
    private static final int CONNECT_INTERVAL_IN_SECOND = 10; // 自动连接间隔秒数

    private static final int MSG_START_CONNECT = 0; // 开始连接
    private static final int MSG_START_DISCONNECT = 1; // 开始断开

    public static final int MSG_CONNECT_TIMEOUT = 2; // 连接超时
    public static final int MSG_WRITE_DATA_TIMEOUT = 3; // 写数据超时
    public static final int MSG_READ_DATA_TIMEOUT = 4; // 读数据超时

    private final Context context;
    private volatile BleDeviceState state = DEVICE_CLOSED; // 设备实时状态
    private BleDeviceState connectState = CONNECT_DISCONNECT; // 设备连接状态，只能是CONNECT_SUCCESS, CONNECT_FAILURE or CONNECT_DISCONNECT
    private final BleDeviceRegisterInfo registerInfo; // 设备注册信息
    private BleDeviceDetailInfo deviceDetailInfo;//设备详细信息，扫描到设备后赋值
    private BleGatt bleGatt; // 设备Gatt，连接成功后赋值，完成连接以及数据通信等功能
    private final BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。设备连接成功后启动，设备连接失败或者断开时停止
    private final ScanFilter scanFilter; // 扫描过滤器
    private int battery = -1; // 设备电池电量
    private final List<OnBleDeviceStateListener> stateListeners; // 设备状态监听器列表
    private ExecutorService connService; // 定时连接服务
    // 动作Handler
    private final Handler actionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_START_CONNECT) {
                if(isDisconnect()) {
                    actionHandler.removeMessages(MSG_START_CONNECT);
                    setState(DEVICE_SCANNING);
                    BleDeviceScanner.startScan(scanFilter, bleScanCallback);
                }
            } else if (msg.what == MSG_START_DISCONNECT) {
                disconnect();
            }
        }
    };
    // gatt回调Handler
    private final Handler gattCallbackHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(bleGatt != null) {
                if (msg.what == MSG_CONNECT_TIMEOUT) {
                    bleGatt.connectFailure(new TimeoutException());
                } else if (msg.what == MSG_WRITE_DATA_TIMEOUT) {
                    bleGatt.writeFailure(new TimeoutException());
                } else if (msg.what == MSG_READ_DATA_TIMEOUT) {
                    bleGatt.readFailure(new TimeoutException());
                }
            }
        }
    };

    // 扫描回调
    private final IBleScanCallback bleScanCallback = new IBleScanCallback() {
        @Override
        public void onDeviceFound(final BleDeviceDetailInfo bleDeviceDetailInfo) {
            processFoundDevice(bleDeviceDetailInfo);
        }

        @Override
        public void onScanFailed(final int errorCode) {
            ViseLog.e("Scan failed with errorCode: = " + errorCode);

            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Toast.makeText(context, "扫描已开始", Toast.LENGTH_LONG).show();
                    break;
                case SCAN_FAILED_BLE_CLOSED:
                    stopScan(false);
                    Toast.makeText(context, "蓝牙已关闭。", Toast.LENGTH_LONG).show();
                    break;
                case SCAN_FAILED_BLE_ERROR:
                    stopScan(true);
                    notifyBleError();
                    break;
            }
        }
    };

    // 连接回调
    private final IBleConnectCallback connectCallback = new IBleConnectCallback() {
        // 连接成功
        @Override
        public void onConnectSuccess(final BleGatt bleGatt) {
            processConnectSuccess(bleGatt);
        }
        // 连接失败
        @Override
        public void onConnectFailure(final BleException exception) {
            processConnectFailure(exception);
        }
        // 连接中断
        @Override
        public void onDisconnect() {
            processDisconnect();
        }
    };


    public BleDevice(Context context, BleDeviceRegisterInfo registerInfo) {
        this.context = context;
        this.registerInfo = registerInfo;
        scanFilter = new ScanFilter.Builder().setDeviceAddress(getMacAddress()).build();
        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        stateListeners = new LinkedList<>();
    }

    public BleDeviceRegisterInfo getRegisterInfo() {
        return registerInfo;
    }

    public void updateRegisterInfo(BleDeviceRegisterInfo registerInfo) {
        this.registerInfo.setMacAddress(registerInfo.getMacAddress());
        this.registerInfo.setUuidString(registerInfo.getUuidString());
        this.registerInfo.setImagePath(registerInfo.getImagePath());
        this.registerInfo.setAutoConnect(registerInfo.autoConnect());
        this.registerInfo.setReconnectTimes(registerInfo.getReconnectTimes());
        this.registerInfo.setWarnWhenBleError(registerInfo.isWarnWhenBleError());
    }

    public String getMacAddress() {
        return registerInfo.getMacAddress();
    }
    public String getNickName() {
        return registerInfo.getNickName();
    }
    public String getUuidString() {
        return registerInfo.getUuidString();
    }
    public String getImagePath() {
        return registerInfo.getImagePath();
    }
    public Drawable getImageDrawable() {
        if(getImagePath().equals("")) {
            BleDeviceType deviceType = BleDeviceType.getFromUuid(getUuidString());
            if(deviceType == null) {
                throw new IllegalStateException("The device type is not supported.");
            }
            return ContextCompat.getDrawable(context, deviceType.getDefaultImage());
        } else {
            return new BitmapDrawable(context.getResources(), getImagePath());
        }
    }
    public BleGatt getBleGatt() {
        return bleGatt;
    }
    public String getStateDescription() {
        return state.getDescription();
    }
    public int getStateIcon() {
        return state.getIcon();
    }
    public boolean isClosed() {
        return state == DEVICE_CLOSED;
    }
    public boolean isScanning() {
        return state == DEVICE_SCANNING;
    }
    public boolean isConnect() {
        return state == CONNECT_SUCCESS;
    }
    public boolean isDisconnect() {
        return state == CONNECT_FAILURE || state == CONNECT_DISCONNECT;
    }
    public boolean isActing() {
        return state == DEVICE_SCANNING || state == DEVICE_CONNECTING || state == DEVICE_DISCONNECTING;
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
    // 设备是否包含elements
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
        if(!isClosed()) {
            throw new IllegalStateException("设备状态错误。");
        }

        ViseLog.e("BleDevice.open()");

        setState(CONNECT_DISCONNECT);
        if(registerInfo.autoConnect()) {
            startConnect();
        }
    }

    // 切换设备状态
    public void switchState() {
        ViseLog.e("BleDevice.switchState()");

        if(isClosed()) {
            throw new IllegalStateException("设备状态错误。");
        }

        if(isDisconnect()) {
            startConnect();
        } else if(isConnect()) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(connService);
            startDisconnect();
        } else if(isScanning()) {
            stopScan(true);
        } else { // CONNECTING or DISCONNECTING
            Toast.makeText(context, "请稍等...", Toast.LENGTH_SHORT).show();
        }
    }

    // 开始连接
    private void startConnect() {
        if(connService == null || connService.isTerminated()) {
            ViseLog.e("启动自动连接服务");

            connService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "MT_Auto_Connection");
                }
            });
            ((ScheduledExecutorService) connService).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if(isDisconnect()) {
                        actionHandler.sendEmptyMessage(MSG_START_CONNECT);
                    }
                }
            }, 0, CONNECT_INTERVAL_IN_SECOND, TimeUnit.SECONDS);
        } else {
            Toast.makeText(context, "自动连接中，请稍等。", Toast.LENGTH_SHORT).show();
        }
    }

    // 开始断开
    public void startDisconnect() {
        ViseLog.e("BleDevice.startDisconnect()");

        actionHandler.removeCallbacksAndMessages(null);
        actionHandler.sendEmptyMessage(MSG_START_DISCONNECT);
    }

    // 关闭设备
    public void close() {
        ViseLog.e("BleDevice.close()");

        if(isDisconnect()) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(connService);
            actionHandler.removeCallbacksAndMessages(null);
            setState(BleDeviceState.DEVICE_CLOSED);
        } else {
            Toast.makeText(context, "当前状态无法关闭设备。", Toast.LENGTH_SHORT).show();
        }
    }

    protected void disconnect() {
        if(bleGatt != null) {
            setState(DEVICE_DISCONNECTING);
            bleGatt.disconnect();
        }
        actionHandler.removeCallbacksAndMessages(null);
    }

    private void stopScan(boolean forever) {
        if(forever) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(connService);
        }

        BleDeviceScanner.stopScan(bleScanCallback); // 设备处于扫描时，停止扫描
        actionHandler.removeMessages(MSG_START_CONNECT);
        setState(connectState);
    }

    // 处理找到的设备
    private void processFoundDevice(final BleDeviceDetailInfo bleDeviceDetailInfo) {
        ViseLog.e("处理找到的设备: " + bleDeviceDetailInfo);

        BluetoothDevice bluetoothDevice = bleDeviceDetailInfo.getDevice();
        if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            stopScan(true);
            Toast.makeText(context, "设备未配对，请先配对。", Toast.LENGTH_LONG).show();
            bleDeviceDetailInfo.getDevice().createBond();
        } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            stopScan(false);
            BleDevice.this.deviceDetailInfo = bleDeviceDetailInfo;
            new BleGatt().connect(context, bleDeviceDetailInfo.getDevice(), connectCallback, gattCallbackHandler);
            setState(DEVICE_CONNECTING);
        }
    }

    // 处理连接成功
    private void processConnectSuccess(BleGatt bleGatt) {
        // 防止重复连接成功
        if(isConnect()) {
            ViseLog.e("再次连接成功!!!");
            return;
        }
        if(isClosed()) { // 设备已经关闭了，强行清除
            if(bleGatt != null)
                bleGatt.clear();
            return;
        }
        if(isScanning()) {
            stopScan(false);
        }

        ViseLog.e("处理连接成功: " + bleGatt);

        this.bleGatt = bleGatt;
        gattCmdExecutor.start();
        setConnectState(CONNECT_SUCCESS);
        if(!executeAfterConnectSuccess()) {
            startDisconnect();
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        if(state != CONNECT_FAILURE) {
            ViseLog.e("处理连接失败: " + bleException );

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(CONNECT_FAILURE);
            executeAfterConnectFailure();
        }
    }

    // 处理连接断开
    private void processDisconnect() {
        if(state != CONNECT_DISCONNECT) {
            ViseLog.e("处理连接断开");

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(CONNECT_DISCONNECT);
            executeAfterDisconnect();
        }
    }

    private void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("当前状态：" + state);

            this.state = state;
            updateState();
        }
    }

    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;
        setState(connectState);
    }

    protected abstract boolean executeAfterConnectSuccess(); // 连接成功后执行的操作
    protected abstract void executeAfterConnectFailure(); // 连接错误后执行的操作
    protected abstract void executeAfterDisconnect(); // 断开连接后执行的操作



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

    protected final void notify(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.notify(element, enable, receiveCallback);
    }

    protected final void indicate(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.indicate(element, enable, receiveCallback);
    }

    protected final void runInstantly(IBleDataCallback callback) {
        gattCmdExecutor.runInstantly(callback);
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

    private void notifyBleError() {
        if(registerInfo.isWarnWhenBleError()) {
            notifyBleError(true);
        }
    }

    // Ble错误，是否报警
    private void notifyBleError(final boolean isWarn) {
        for(final OnBleDeviceStateListener listener : stateListeners) {
            if(listener != null) {
                listener.onBleErrorNotified(BleDevice.this, isWarn);
            }
        }
    }

    public final void cancelNotifyBleError() {
        notifyBleError(false);
    }

    // 更新电池电量
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
        BleDeviceRegisterInfo thisInfo = getRegisterInfo();
        BleDeviceRegisterInfo thatInfo = that.getRegisterInfo();
        return Objects.equals(thisInfo, thatInfo);
    }

    @Override
    public int hashCode() {
        return (getRegisterInfo() != null) ? getRegisterInfo().hashCode() : 0;
    }

}
