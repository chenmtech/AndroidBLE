package com.cmtech.android.ble.core;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.cmtech.android.ble.BleConfig;
import com.cmtech.android.ble.R;
import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.callback.IBleScanCallback;
import com.cmtech.android.ble.exception.BleException;
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

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_FAILURE;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT_SUCCESS;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.SCANNING;

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
  * UpdateUser:     chenm
  * UpdateDate:     2019-10-21 07:02
  * UpdateRemark:   优化代码，
  * Version:        1.1
 */

public abstract class BleDevice {
    private static final int MIN_RSSI_WHEN_CONNECTED = -75; // 被连接时要求的最小RSSI
    private static final int MSG_REQUEST_SCAN = 0; // 请求扫描消息
    private static final int MSG_REQUEST_DISCONNECT = 1; // 请求断开消息
    public static final int INVALID_BATTERY = -1; // 无效电池电量

    public static final int MSG_BLE_INNER_ERROR = R.string.scan_failed_ble_inner_error; // Ble内部错误通知
    public static final int MSG_BT_CLOSED = R.string.scan_failed_bt_closed; // 蓝牙关闭错误
    public static final int MSG_SCAN_ALREADY_STARTED = R.string.scan_failed_already_started;
    public static final int MSG_INVALID_OPERATION = R.string.invalid_operation;
    public static final int MSG_WAIT_SCAN = R.string.wait_scan_pls;
    public static final int MSG_BOND_DEVICE = R.string.device_unbond;

    // 设备监听器接口
    public interface OnBleDeviceListener {
        void onStateUpdated(final BleDevice device); // 设备状态更新
        void onExceptionMsgNotified(final BleDevice device, int msgId); // 异常消息通知
        void onBatteryUpdated(final BleDevice device); // 电池电量更新
    }

    private final BleDeviceRegisterInfo registerInfo; // 注册信息
    private final List<OnBleDeviceListener> listeners; // 监听器列表
    private volatile BleDeviceState state = CLOSED; // 实时状态
    private BleDeviceState connectState = CONNECT_DISCONNECT; // 连接状态，只能是CONNECT_SUCCESS, CONNECT_FAILURE or CONNECT_DISCONNECT
    private int battery = INVALID_BATTERY; // 电池电量
    private Context context; // 上下文，用于启动蓝牙连接。当调用open()打开设备时赋值
    private BleDeviceDetailInfo detailInfo;// 详细信息，扫描到设备后赋值
    private BleGatt bleGatt; // Gatt，连接成功后赋值，完成连接状态改变处理以及数据通信功能
    private BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。连接成功后启动，连接失败或者断开时停止
    private ExecutorService autoScanService; // 自动扫描服务

    // 请求处理Handler
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_REQUEST_SCAN) {
                scan();
            } else if (msg.what == MSG_REQUEST_DISCONNECT) {
                disconnect();
            }
        }
    };
    // 扫描回调
    private final IBleScanCallback bleScanCallback = new IBleScanCallback() {
        @Override
        public void onDeviceFound(final BleDeviceDetailInfo bleDeviceDetailInfo) {
            ViseLog.e("Device Found with the RSSI: " + bleDeviceDetailInfo.getRssi());

            if(bleDeviceDetailInfo.getRssi() >= MIN_RSSI_WHEN_CONNECTED)
                processFoundDevice(bleDeviceDetailInfo);
        }
        @Override
        public void onScanFailed(final int errorCode) {
            ViseLog.e("Scan failed with errorCode: = " + errorCode);

            switch (errorCode) {
                case CODE_ALREADY_STARTED:
                    notifyExceptionMessage(MSG_SCAN_ALREADY_STARTED);
                    break;
                case CODE_BLE_CLOSED:
                    stopScan(true);
                    notifyExceptionMessage(MSG_BT_CLOSED);
                    break;
                case CODE_BLE_INNER_ERROR:
                    stopScan(true);
                    if(registerInfo.isWarnBleInnerError()) {
                        notifyExceptionMessage(MSG_BLE_INNER_ERROR);
                    }
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
        // 连接断开
        @Override
        public void onDisconnect() {
            processDisconnect();
        }
    };


    public BleDevice(BleDeviceRegisterInfo registerInfo) {
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
    public String getMacAddress() {
        return registerInfo.getMacAddress();
    }
    public String getNickName() {
        return registerInfo.getNickName();
    }
    public String getUuidString() {
        return registerInfo.getUuidStr();
    }
    public String getImagePath() {
        return registerInfo.getImagePath();
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
        return state == CLOSED;
    }
    private boolean isScanning() {
        return state == SCANNING;
    }
    protected boolean isConnected() {
        return state == CONNECT_SUCCESS;
    }
    private boolean isDisconnected() {
        return state == CONNECT_FAILURE || state == CONNECT_DISCONNECT;
    }
    public boolean isWaitingResponse() {
        return state == SCANNING || state == CONNECTING || state == DISCONNECTING;
    }
    public boolean isStopped() {
        return isDisconnected() && ExecutorUtil.isDead(autoScanService);
    }
    private void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("The state of device " + getMacAddress() + " is " + state);
            this.state = state;
            updateState();
        }
    }
    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;
        setState(connectState);
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
    // 设备是否包含gatt elements
    protected boolean containGattElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if( element == null || element.retrieveGattObject(this) == null )
                return false;
        }
        return true;
    }
    // 设备是否包含gatt element
    protected boolean containGattElement(BleGattElement element) {
        return !( element == null || element.retrieveGattObject(this) == null );
    }

    // 打开设备
    public void open(Context context) {
        if(!isClosed()) {
            ViseLog.e("The device is opened.");
            return;
        }
        if(context == null) {
            throw new NullPointerException("The context is null.");
        }

        ViseLog.e("BleDevice.open()");
        this.context = context;
        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        setState(CONNECT_DISCONNECT);
        if(registerInfo.autoConnect()) {
            callAutoScan();
        }
    }

    // 切换状态
    public void switchState() {
        ViseLog.e("BleDevice.switchState()");
        if(isDisconnected()) {
            callAutoScan();
        } else if(isConnected()) {
            callDisconnect(true);
        } else if(isScanning()) {
            stopScan(true);
        } else { // 无效操作
            notifyExceptionMessage(MSG_INVALID_OPERATION);
        }
    }

    // 请求自动扫描
    public void callAutoScan() {
        if(isDisconnected()) {
            if (ExecutorUtil.isDead(autoScanService)) {
                ViseLog.e("BleDevice.callAutoScan()");

                autoScanService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable, "MT_Auto_Scan");
                    }
                });
                ((ScheduledExecutorService) autoScanService).scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        if (isDisconnected()) {
                            handler.sendEmptyMessage(MSG_REQUEST_SCAN);
                        }
                    }
                }, 0, BleConfig.getInstance().getAutoScanInterval(), TimeUnit.SECONDS);
            } else {
                notifyExceptionMessage(MSG_WAIT_SCAN);
            }
        }
    }

    // 请求断开
    public void callDisconnect(boolean stopAutoScan) {
        ViseLog.e("BleDevice.callDisconnect()");

        if(stopAutoScan) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        }
        handler.removeCallbacksAndMessages(null);
        handler.sendEmptyMessage(MSG_REQUEST_DISCONNECT);
    }

    // 停止扫描
    public void stopScan(boolean stopAutoScan) {
        if (stopAutoScan) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        }
        BleScanner.stopScan(bleScanCallback); // 设备处于扫描时，停止扫描
        handler.removeMessages(MSG_REQUEST_SCAN);
        setState(connectState);
    }

    // 关闭设备
    public void close() {
        if(!isStopped()) {
            ViseLog.e("The device can't be closed currently.");
            return;
        }

        ViseLog.e("BleDevice.close()");

        ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        handler.removeCallbacksAndMessages(null);
        setState(BleDeviceState.CLOSED);

        autoScanService = null;
        gattCmdExecutor = null;
        detailInfo = null;
        bleGatt = null;
        context = null;
    }

    private void scan() {
        if(isDisconnected()) {
            handler.removeMessages(MSG_REQUEST_SCAN);
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(getMacAddress()).build();
            BleScanner.startScan(scanFilter, bleScanCallback);
            setState(SCANNING);
        }
    }

    private void connect() {
        new BleGatt().connect(context, detailInfo.getDevice(), connectCallback);
        setState(CONNECTING);
    }

    protected void disconnect() {
        if(bleGatt != null) {
            setState(DISCONNECTING);
            bleGatt.disconnect();
        }
        handler.removeCallbacksAndMessages(null);
    }

    // 处理找到的设备
    private void processFoundDevice(final BleDeviceDetailInfo detailInfo) {
        ViseLog.e("Process found device: " + detailInfo);

        stopScan(false);
        BleDevice.this.detailInfo = detailInfo;
        if(context != null) {
            connect();
        }
    }

    // 处理连接成功
    private void processConnectSuccess(BleGatt bleGatt) {
        // 防止重复连接成功
        if(isConnected()) {
            ViseLog.e("Connect Success Again!!!");
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

        ViseLog.e("Process connect success: " + bleGatt);

        this.bleGatt = bleGatt;
        gattCmdExecutor.start();
        setConnectState(CONNECT_SUCCESS);
        if(!executeAfterConnectSuccess()) {
            callDisconnect(false);
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        if(state != CONNECT_FAILURE) {
            ViseLog.e("Process connect failure: " + bleException );

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(CONNECT_FAILURE);
            executeAfterConnectFailure();
        }
    }

    // 处理连接断开
    private void processDisconnect() {
        if(state != CONNECT_DISCONNECT) {
            ViseLog.e("Process disconnect.");

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(CONNECT_DISCONNECT);
            executeAfterDisconnect();
        }
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

    // 更新设备状态
    public final void updateState() {
        for(OnBleDeviceListener listener : listeners) {
            if(listener != null) {
                listener.onStateUpdated(this);
            }
        }
    }

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
