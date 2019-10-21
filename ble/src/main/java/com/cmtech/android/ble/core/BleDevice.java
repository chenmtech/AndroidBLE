package com.cmtech.android.ble.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

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
  * UpdateUser:     chenm
  * UpdateDate:     2019-10-21 07:02
  * UpdateRemark:   优化代码，
  * Version:        1.1
 */

public abstract class BleDevice {
    private static final int AUTO_CONNECT_INTERVAL = 10; // 自动连接间隔，单位：秒
    private static final int MIN_RSSI_WHEN_CONNECTED = -75; // 被连接时要求的最小RSSI
    private static final int MSG_REQUEST_CONNECT = 0; // 请求连接消息
    private static final int MSG_REQUEST_DISCONNECT = 1; // 请求断开消息
    public static final int INVALID_BATTERY = -1; // 无效电池电量
    // 设备监听器接口
    public interface OnBleDeviceListener {
        void onConnectStateUpdated(final BleDevice device); // 连接状态更新
        void onBleInnerErrorNotified(final BleDevice device); // BLE内部错误通知
        void onBatteryUpdated(final BleDevice device); // 电池电量更新
    }

    protected final Context context; // 上下文
    private final BleDeviceRegisterInfo registerInfo; // 注册信息
    private final List<OnBleDeviceListener> listeners; // 监听器列表
    private volatile BleDeviceState state = DEVICE_CLOSED; // 实时状态
    private BleDeviceState connectState = CONNECT_DISCONNECT; // 连接状态，只能是CONNECT_SUCCESS, CONNECT_FAILURE or CONNECT_DISCONNECT
    private int battery = INVALID_BATTERY; // 电池电量
    private BleDeviceDetailInfo detailInfo;// 详细信息，扫描到设备后赋值
    private BleGatt bleGatt; // Gatt，连接成功后赋值，完成连接状态改变处理以及数据通信功能
    private BleSerialGattCommandExecutor gattCmdExecutor; // Gatt命令执行器，在内部的一个单线程池中执行。连接成功后启动，连接失败或者断开时停止
    private ExecutorService autoConnService; // 自动连接服务
    // 绑定状态广播接收器
    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() == BluetoothDevice.BOND_BONDED && getMacAddress().equalsIgnoreCase(device.getAddress())) {
                    Toast.makeText(context, getMacAddress() + "绑定成功。", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    // 请求Handler
    private final Handler requestHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_REQUEST_CONNECT) {
                connect();
            } else if (msg.what == MSG_REQUEST_DISCONNECT) {
                disconnect();
            }
        }
    };
    // 扫描回调
    private final IBleScanCallback bleScanCallback = new IBleScanCallback() {
        @Override
        public void onDeviceFound(final BleDeviceDetailInfo bleDeviceDetailInfo) {
            ViseLog.e("The current rssi: " + bleDeviceDetailInfo.getRssi());

            if(bleDeviceDetailInfo.getRssi() >= MIN_RSSI_WHEN_CONNECTED)
                processFoundDevice(bleDeviceDetailInfo);
        }
        @Override
        public void onScanFailed(final int errorCode) {
            ViseLog.e("Scan failed with errorCode: = " + errorCode);

            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Toast.makeText(context, "正在扫描中，请等待。", Toast.LENGTH_LONG).show();
                    break;
                case SCAN_FAILED_BLE_CLOSED:
                    stopScan(true);
                    Toast.makeText(context, "蓝牙已关闭，请允许打开蓝牙", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    context.startActivity(intent);
                    break;
                case SCAN_FAILED_BLE_INNER_ERROR:
                    stopScan(true);
                    notifyBleInnerError();
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


    public BleDevice(Context context, BleDeviceRegisterInfo registerInfo) {
        this.context = context;
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
        return state == DEVICE_CLOSED;
    }
    private boolean isScanning() {
        return state == DEVICE_SCANNING;
    }
    protected boolean isConnected() {
        return state == CONNECT_SUCCESS;
    }
    private boolean isDisconnected() {
        return state == CONNECT_FAILURE || state == CONNECT_DISCONNECT;
    }
    // 是否在等待响应
    public boolean isWaitingResponse() {
        return state == DEVICE_SCANNING || state == DEVICE_CONNECTING || state == DEVICE_DISCONNECTING;
    }
    public boolean isStopped() {
        return isDisconnected() && (autoConnService == null || autoConnService.isTerminated());
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
    public int getBattery() {
        return battery;
    }
    protected void setBattery(final int battery) {
        if(this.battery != battery) {
            this.battery = battery;
            updateBattery();
        }
    }
    // 添加监听器
    public final void addListener(OnBleDeviceListener listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    // 删除监听器
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
    public void open() {
        if(!isClosed()) {
            throw new IllegalStateException("设备状态错误。");
        }

        ViseLog.e("BleDevice.open()");

        IntentFilter bondIntent = new IntentFilter();
        bondIntent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(bondStateReceiver, bondIntent);

        gattCmdExecutor = new BleSerialGattCommandExecutor(this);

        setState(CONNECT_DISCONNECT);
        if(registerInfo.autoConnect()) {
            requestConnect();
        }
    }

    // 切换设备状态
    public void switchState() {
        if(isClosed()) {
            throw new IllegalStateException("设备状态错误。");
        }

        ViseLog.e("BleDevice.switchState()");

        if(isDisconnected()) {
            requestConnect();
        } else if(isConnected()) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);
            requestDisconnect();
        } else if(isScanning()) {
            stopScan(true);
        } else { // CONNECTING or DISCONNECTING
            Toast.makeText(context, "请稍等...", Toast.LENGTH_SHORT).show();
        }
    }

    // 请求连接
    private void requestConnect() {
        if(autoConnService == null || autoConnService.isTerminated()) {
            ViseLog.e("BleDevice.requestConnect()");

            autoConnService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "MT_Auto_Connection");
                }
            });
            ((ScheduledExecutorService) autoConnService).scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if(isDisconnected()) {
                        requestHandler.sendEmptyMessage(MSG_REQUEST_CONNECT);
                    }
                }
            }, 0, AUTO_CONNECT_INTERVAL, TimeUnit.SECONDS);
        } else {
            Toast.makeText(context, "自动连接中，请稍等。", Toast.LENGTH_SHORT).show();
        }
    }

    // 请求断开
    public void requestDisconnect() {
        ViseLog.e("BleDevice.requestDisconnect()");

        requestHandler.removeCallbacksAndMessages(null);
        requestHandler.sendEmptyMessage(MSG_REQUEST_DISCONNECT);
    }

    // 关闭设备
    public void close() {
        if(!isStopped()) {
            throw new IllegalStateException("The device can't be closed.");
        }

        ViseLog.e("BleDevice.close()");

        ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);
        requestHandler.removeCallbacksAndMessages(null);
        setState(BleDeviceState.DEVICE_CLOSED);

        autoConnService = null;
        gattCmdExecutor = null;
        detailInfo = null;
        bleGatt = null;

        context.unregisterReceiver(bondStateReceiver);
    }

    private void connect() {
        if(isDisconnected()) {
            requestHandler.removeMessages(MSG_REQUEST_CONNECT);
            setState(DEVICE_SCANNING);
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(getMacAddress()).build();
            BleScanner.startScan(scanFilter, bleScanCallback);
        }
    }

    protected void disconnect() {
        if(bleGatt != null) {
            setState(DEVICE_DISCONNECTING);
            bleGatt.disconnect();
        }
        requestHandler.removeCallbacksAndMessages(null);
    }

    private void stopScan(boolean forever) {
        if(forever) {
            ExecutorUtil.shutdownNowAndAwaitTerminate(autoConnService);
        }
        BleScanner.stopScan(bleScanCallback); // 设备处于扫描时，停止扫描
        requestHandler.removeMessages(MSG_REQUEST_CONNECT);
        setState(connectState);
    }

    // 处理找到的设备
    private void processFoundDevice(final BleDeviceDetailInfo bleDeviceDetailInfo) {
        ViseLog.e("处理找到的设备: " + bleDeviceDetailInfo);

        BluetoothDevice bluetoothDevice = bleDeviceDetailInfo.getDevice();
        if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            stopScan(true);
            Toast.makeText(context, "设备未绑定，无法连接，请先绑定设备。", Toast.LENGTH_LONG).show();
            bleDeviceDetailInfo.getDevice().createBond();
        } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            stopScan(false);
            BleDevice.this.detailInfo = bleDeviceDetailInfo;
            new BleGatt().connect(context, bleDeviceDetailInfo.getDevice(), connectCallback);
            setState(DEVICE_CONNECTING);
        }
    }

    // 处理连接成功
    private void processConnectSuccess(BleGatt bleGatt) {
        // 防止重复连接成功
        if(isConnected()) {
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
            requestDisconnect();
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
                listener.onConnectStateUpdated(this);
            }
        }
    }

    // 通知Ble内部错误
    private void notifyBleInnerError() {
        if(registerInfo.isWarnWhenBleError()) {
            for(final OnBleDeviceListener listener : listeners) {
                if(listener != null) {
                    listener.onBleInnerErrorNotified(BleDevice.this);
                }
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
