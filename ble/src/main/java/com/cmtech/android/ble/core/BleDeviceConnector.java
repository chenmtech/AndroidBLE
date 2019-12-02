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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.cmtech.android.ble.core.BleDeviceState.CLOSED;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.FAILURE;
import static com.cmtech.android.ble.core.BleDeviceState.CONNECT;
import static com.cmtech.android.ble.core.BleDeviceState.DISCONNECTING;
import static com.cmtech.android.ble.core.BleDeviceState.SCANNING;
import static com.cmtech.android.ble.core.IDevice.MSG_INVALID_OPERATION;

/**
  *
  * ClassName:      BleDeviceConnector
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

public class BleDeviceConnector implements IDeviceConnector{
    private static final int MIN_RSSI_WHEN_CONNECTED = -75; // 被连接时要求的最小RSSI
    private static final int MSG_REQUEST_SCAN = 0; // 请求扫描消息
    private static final int MSG_REQUEST_DISCONNECT = 1; // 请求断开消息

    public static final int MSG_BLE_INNER_ERROR = R.string.scan_failed_ble_inner_error; // Ble内部错误通知
    public static final int MSG_BT_CLOSED = R.string.scan_failed_bt_closed; // 蓝牙关闭错误
    public static final int MSG_SCAN_ALREADY_STARTED = R.string.scan_failed_already_started;
    public static final int MSG_WAIT_SCAN = R.string.wait_scan_pls;
    public static final int MSG_BOND_DEVICE = R.string.device_unbond;


    private final IDevice device; // 设备
    private volatile BleDeviceState state = CLOSED; // 实时状态
    private BleDeviceState connectState = DISCONNECT; // 连接状态，只能是CONNECT_SUCCESS, FAILURE or DISCONNECT
    private Context context; // 上下文，用于启动蓝牙连接。当调用open()打开设备时赋值
    private BleDeviceDetailInfo detailInfo;// 详细信息，扫描到设备后赋值
    private BleGatt bleGatt; // Gatt，连接成功后赋值，完成连接状态改变处理以及数据通信功能
    private BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。连接成功后启动，连接失败或者断开时停止
    private ExecutorService autoScanService; // 自动扫描服务

    // 请求处理Handler
    protected final Handler handler = new Handler(Looper.getMainLooper()) {
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
                    device.notifyExceptionMessage(MSG_SCAN_ALREADY_STARTED);
                    break;
                case CODE_BLE_CLOSED:
                    stopScan(true);
                    device.notifyExceptionMessage(MSG_BT_CLOSED);
                    break;
                case CODE_BLE_INNER_ERROR:
                    stopScan(true);
                    if(device.warnBleInnerError()) {
                        device.notifyExceptionMessage(MSG_BLE_INNER_ERROR);
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

    public BleDeviceConnector(IDevice device) {
        this.device = device;
    }

    public BleGatt getBleGatt() {
        return bleGatt;
    }

    @Override
    public BleDeviceState getState() {
        return state;
    }

    @Override
    public void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("The state of device " + device.getAddress() + " is " + state);
            this.state = state;
            device.updateState();
        }
    }

    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;
        setState(connectState);
    }

    // 设备是否包含gatt elements
    public boolean containGattElements(BleGattElement[] elements) {
        for(BleGattElement element : elements) {
            if( element == null || element.retrieveGattObject(this) == null )
                return false;
        }
        return true;
    }
    // 设备是否包含gatt element
    public boolean containGattElement(BleGattElement element) {
        return !( element == null || element.retrieveGattObject(this) == null );
    }

    // 打开设备
    @Override
    public void open(Context context) {
        if(state != CLOSED) {
            ViseLog.e("The device is opened.");
            return;
        }
        if(context == null) {
            throw new NullPointerException("The context is null.");
        }

        ViseLog.e("BleDeviceConnector.open()");
        this.context = context;
        gattCmdExecutor = new BleSerialGattCommandExecutor(this);
        setState(DISCONNECT);
        if(device.autoConnect()) {
            callAutoScan();
        }
    }

    // 切换状态
    @Override
    public void switchState() {
        ViseLog.e("BleDeviceConnector.switchState(): " + state);
        if(isDisconnected()) {
            callAutoScan();
        } else if(isConnected()) {
            forceDisconnect(true);
        } else if(isScanning()) {
            stopScan(true);
        } else { // 无效操作
            device.notifyExceptionMessage(MSG_INVALID_OPERATION);
        }
    }

    private boolean isScanning() {
        return state == SCANNING;
    }

    // 请求自动扫描
    private void callAutoScan() {
        if(isDisconnected()) {
            if (ExecutorUtil.isDead(autoScanService)) {
                ViseLog.e("BleDeviceConnector.callAutoScan()");

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
                device.notifyExceptionMessage(MSG_WAIT_SCAN);
            }
        }
    }

    // 强制断开
    @Override
    public void forceDisconnect(boolean forever) {
        ViseLog.e("BleDeviceConnector.forceDisconnect()");

        if(forever) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        }
        handler.removeCallbacksAndMessages(null);
        handler.sendEmptyMessage(MSG_REQUEST_DISCONNECT);
    }

    // 停止扫描
    private void stopScan(boolean stopAutoScan) {
        if (stopAutoScan) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        }
        BleScanner.stopScan(bleScanCallback); // 设备处于扫描时，停止扫描
        handler.removeMessages(MSG_REQUEST_SCAN);
        setState(connectState);
    }

    // 关闭设备
    @Override
    public void close() {
        if(!isDisconnectedForever()) {
            ViseLog.e("The device can't be closed currently.");
            return;
        }

        ViseLog.e("BleDeviceConnector.close()");

        ExecutorUtil.shutdownNowAndAwaitTerminate(autoScanService);
        handler.removeCallbacksAndMessages(null);
        setState(BleDeviceState.CLOSED);

        autoScanService = null;
        gattCmdExecutor = null;
        detailInfo = null;
        bleGatt = null;
        context = null;
    }

    @Override
    public void clear() {
        if(bleGatt != null) {
            bleGatt.clear();
        }
    }

    @Override
    public boolean isDisconnectedForever() {
        return isDisconnected() && ExecutorUtil.isDead(autoScanService);
    }

    @Override
    public boolean isConnected() {
        return state == CONNECT;
    }
    @Override
    public boolean isDisconnected() {
        return state == FAILURE || state == DISCONNECT;
    }

    private void disconnect() {
        if(bleGatt != null) {
            setState(DISCONNECTING);
            bleGatt.disconnect();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    private void scan() {
        if(isDisconnected()) {
            handler.removeMessages(MSG_REQUEST_SCAN);
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(device.getAddress()).build();
            BleScanner.startScan(scanFilter, bleScanCallback);
            setState(SCANNING);
        }
    }

    private void connect() {
        new BleGatt().connect(context, detailInfo.getDevice(), connectCallback);
        setState(CONNECTING);
    }

    // 处理找到的设备
    private void processFoundDevice(final BleDeviceDetailInfo detailInfo) {
        ViseLog.e("Process found device: " + detailInfo);

        stopScan(false);
        BleDeviceConnector.this.detailInfo = detailInfo;
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
        if(state == CLOSED) { // 设备已经关闭了，强行清除
            clear();
            return;
        }
        if(isScanning()) {
            stopScan(false);
        }

        ViseLog.e("Process connect success: " + bleGatt);

        this.bleGatt = bleGatt;
        gattCmdExecutor.start();
        setConnectState(CONNECT);

        if (!device.onConnectSuccess()) {
            forceDisconnect(false);
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        if(state != FAILURE) {
            ViseLog.e("Process connect failure: " + bleException );

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(FAILURE);
            device.onConnectFailure();
        }
    }

    // 处理连接断开
    private void processDisconnect() {
        if(state != DISCONNECT) {
            ViseLog.e("Process disconnect.");

            gattCmdExecutor.stop();
            bleGatt = null;
            setConnectState(DISCONNECT);
            device.onDisconnect();
        }
    }

    public boolean isGattExecutorAlive() {
        return gattCmdExecutor.isAlive();
    }

    public final void read(BleGattElement element, IBleDataCallback dataCallback) {
        gattCmdExecutor.read(element, dataCallback);
    }

    public final void write(BleGattElement element, byte[] data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    public final void write(BleGattElement element, byte data, IBleDataCallback dataCallback) {
        gattCmdExecutor.write(element, data, dataCallback);
    }

    public final void notify(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.notify(element, enable, receiveCallback);
    }

    public final void indicate(BleGattElement element, boolean enable, IBleDataCallback receiveCallback) {
        gattCmdExecutor.indicate(element, enable, receiveCallback);
    }

    public final void runInstantly(IBleDataCallback callback) {
        gattCmdExecutor.runInstantly(callback);
    }
}
