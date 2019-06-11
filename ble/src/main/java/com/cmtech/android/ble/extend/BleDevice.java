package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;

import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.callback.scan.ScanCallback;
import com.cmtech.android.ble.callback.scan.SingleFilterScanCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
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


public abstract class BleDevice implements Handler.Callback {
    private final static BleDeviceConnectState DEVICE_INIT_STATE = BleDeviceConnectState.CONNECT_CLOSED;

    private class MyScanCallback implements IScanCallback {

        MyScanCallback() {

        }

        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                processScanResult(false, null);

            } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                processScanResult(true, bluetoothLeDevice);

            }
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        @Override
        public void onScanTimeout() {
            processScanResult(false, null);
        }

    }

    private class MyConnectCallback implements IConnectCallback {
        MyConnectCallback() {
        }

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    processConnectSuccess(deviceMirror);
                }
            });
        }
        @Override
        public void onConnectFailure(final BleException exception) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    processConnectFailure(exception);
                }
            });
        }
        @Override
        public void onDisconnect(final boolean isActive) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    processDisconnect(isActive);
                }
            });
        }
    }

    private BleDeviceBasicInfo basicInfo; // 设备基本信息对象

    private BluetoothLeDevice bluetoothLeDevice = null; // 设备BluetoothLeDevice，当扫描到设备后会赋值

    private DeviceMirror deviceMirror = null; // 设备镜像，连接到设备后会赋值

    // 扫描回调适配器，将IScanCallback适配为BluetoothAdapter.LeScanCallback，每次新的扫描必须创建新的实例
    private ScanCallback scanCallback;

    private BleDeviceConnectState connectState = DEVICE_INIT_STATE; // 设备连接状态，初始化为关闭状态

    private final List<OnBleDeviceListener> deviceStateListeners = new LinkedList<>(); // 设备状态观察者列表

    // 主线程Handler，包括连接相关的处理和Gatt消息的处理，都在主线程中执行
    private final Handler mainHandler = new Handler(Looper.getMainLooper(), this);

    private int battery = -1; // 设备电池电量

    private int curReconnectTimes = 0; // 当前重连次数

    private boolean closing = false; // 标记设备是否正在关闭

    private final BleGattCommandExecutor gattCmdExecutor = new BleGattCommandExecutor(this); // Gatt命令执行者


    public BleDevice(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;
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
            int imageId = BleDeviceType.getFromUuid(getUuidString()).getDefaultImage();

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
        return deviceMirror;
    }

    public boolean isClosed() {
        return connectState == BleDeviceConnectState.CONNECT_CLOSED;
    }

    public boolean isConnected() {
        return connectState == CONNECT_SUCCESS;
    }

    public boolean isWaitingResponse() {
        return (connectState == CONNECT_SCANNING) || (connectState == CONNECT_CONNECTING);
    }

    public String getConnectStateDescription() {
        return connectState.getDescription();
    }

    public int getConnectStateIcon() {
        return connectState.getIcon();
    }

    private void setConnectState(BleDeviceConnectState connectState) {
        if(this.connectState != connectState) {
            ViseLog.e(connectState + " in " + Thread.currentThread());

            this.connectState = connectState;

            updateConnectState();
        }
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(final int battery) {
        this.battery = battery;

        for(final OnBleDeviceListener listener : deviceStateListeners) {
            if(listener != null) {
                listener.onBatteryUpdated(BleDevice.this);
            }
        }
    }

    // 打开设备
    public void open() {
        ViseLog.i(getMacAddress() + ": open()");

        if(!isClosed())
            return;

        closing = false;

        if(basicInfo.autoConnect()) {
            startScan();
        }
    }

    // 关闭设备
    public void close() {
        ViseLog.i(getMacAddress() + ": close()");

        removeCallbacksAndMessages();

        closing = true;

        if(connectState == CONNECT_SCANNING)
            stopScan();

        disconnect();

        setConnectState(BleDeviceConnectState.CONNECT_CLOSED);
    }

    // 切换设备状态
    public final boolean switchState() {
        ViseLog.i("switchDeviceState");

        boolean switched = true;

        if(connectState == CONNECT_SUCCESS) {
            removeCallbacksAndMessages();

            disconnect();

        } else if(connectState == CONNECT_SCANNING || connectState == CONNECT_CONNECTING) {
            switched = false;

        } else {
            removeCallbacksAndMessages();

            curReconnectTimes = 0;

            startScan();
        }

        return switched;
    }

    // 发送Gatt消息
    protected final void sendMessage(int what, Object obj) {
        Message.obtain(mainHandler, what, obj).sendToTarget();
    }

    protected void removeCallbacksAndMessages() {
        mainHandler.removeCallbacksAndMessages(null);
    }

    protected void post(Runnable runnable) {
        mainHandler.post(runnable);
    }

    protected void startGattExecutor() {
        gattCmdExecutor.start();
    }

    protected void stopGattExecutor() {
        gattCmdExecutor.stop();
    }

    protected boolean checkElements(BleGattElement[] elements) {
        return gattCmdExecutor.checkElements(elements);
    }

    protected boolean isGattExecutorAlive() {
        return gattCmdExecutor.isAlive();
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

    protected final void instExecute(IGattDataCallback gattDataCallback) {
        gattCmdExecutor.instExecute(gattDataCallback);
    }

    // 开始扫描
    private void startScan() {
        ViseLog.e("startScan in " + Thread.currentThread());

        scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(basicInfo.getMacAddress());

        scanCallback.setScan(true).scan();

        setConnectState(CONNECT_SCANNING);
    }


    // 开始连接，有些资料说最好放到UI线程中执行连接
    private void startConnect() {
        ViseLog.e("startConnect in " + Thread.currentThread());

        MyConnectCallback connectCallback = new MyConnectCallback();

        ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);

        setConnectState(CONNECT_CONNECTING);
    }

    // 断开连接
    protected void disconnect() {
        ViseLog.e("disconnect in " + Thread.currentThread());

        executeAfterDisconnect();

        removeCallbacksAndMessages();

        if(deviceMirror != null)
            deviceMirror.disconnect();
    }

    // 通知设备状态观察者
    public final void updateConnectState() {
        for(OnBleDeviceListener listener : deviceStateListeners) {
            if(listener != null) {
                listener.onConnectStateUpdated(this);
            }
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

    // 处理扫描结果
    private void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.e("ProcessScanResult: " + canConnect + " in " + Thread.currentThread());

        if(closing) {
            return;
        }

        if(canConnect) {
            this.bluetoothLeDevice = bluetoothLeDevice;

            startConnect(); // 扫描成功，启动连接
        } else {
            removeCallbacksAndMessages();

            reconnect();
        }
    }

    // 处理连接成功回调
    private void processConnectSuccess(DeviceMirror mirror) {
        ViseLog.e("processConnectSuccess in " + Thread.currentThread());

        if (closing) {
            return;
        }

        deviceMirror = mirror;

        if(isConnected()) {
            deviceMirror.clear();
        } else {

            removeCallbacksAndMessages();

            // 设备执行连接后处理，如果出错则断开
            if (!executeAfterConnectSuccess()) {
                disconnect();
                return;
            }
        }

        curReconnectTimes = 0;

        setConnectState(CONNECT_SUCCESS);
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("processConnectFailure in " + Thread.currentThread() + " with " +bleException );

        if (!closing) {
            // 仍然有可能会连续执行两次下面语句
            removeCallbacksAndMessages();

            executeAfterConnectFailure();

            //bluetoothLeDevice = null;

            setConnectState(BleDeviceConnectState.CONNECT_FAILURE);

            reconnect();
        }
    }

    // 处理连接断开
    private void processDisconnect(boolean isActive) {
        ViseLog.e("processDisconnect: " + isActive+ " in " + Thread.currentThread() );

        if (!closing) {
            if (!isActive) {
                removeCallbacksAndMessages();

                executeAfterDisconnect();
            }

            setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);
        }
    }

    // 重新连接
    private void reconnect() {
        int canReconnectTimes = basicInfo.getReconnectTimes();

        if(canReconnectTimes != -1 && curReconnectTimes >= canReconnectTimes) {
            if(basicInfo.isWarnAfterReconnectFailure()) {
                notifyReconnectFailure(true); // 重连失败后通知报警
            }

            setConnectState(BleDeviceConnectState.CONNECT_FAILURE);
        } else {
            if(curReconnectTimes < canReconnectTimes) {
                curReconnectTimes++;
            }

            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!isConnected())
                        startScan();
                }
            }, 500);
        }
    }

    /*
     * 抽象方法
     */
    protected abstract boolean executeAfterConnectSuccess(); // 连接成功后执行的操作

    protected abstract void executeAfterConnectFailure(); // 连接错误后执行的操作

    protected abstract void executeAfterDisconnect(); // 断开连接后执行的操作

    protected abstract void processMessage(Message msg); // 处理消息函数

    /**
     * 私有方法
     */
    // 创建工作线程，输出Handler
    /*private Handler createWorkHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);

        thread.start();

        return new Handler(thread.getLooper(), this);
    }*/

    @Override
    public boolean handleMessage(Message message) {
        processMessage(message);

        return false;
    }

    // 停止扫描
    private void stopScan() {
        removeCallbacksAndMessages();

        ViseLog.i("stopScan in " + Thread.currentThread());

        if(scanCallback != null)
            scanCallback.setScan(false).scan();
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
