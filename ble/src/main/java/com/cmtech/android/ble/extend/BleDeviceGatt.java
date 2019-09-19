package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.callback.IBleRssiCallback;
import com.cmtech.android.ble.common.BleConfig;
import com.cmtech.android.ble.common.BleConstant;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.cmtech.android.ble.common.BleConstant.MSG_CONNECT_TIMEOUT;
import static com.cmtech.android.ble.common.BleConstant.MSG_READ_DATA_TIMEOUT;
import static com.cmtech.android.ble.common.BleConstant.MSG_RECEIVE_DATA_TIMEOUT;
import static com.cmtech.android.ble.common.BleConstant.MSG_WRITE_DATA_TIMEOUT;

public class BleDeviceGatt {
    private final Context context;

    private final BluetoothLeDevice bluetoothLeDevice;//设备基础信息

    private BluetoothGatt bluetoothGatt;//蓝牙GATT

    private IBleRssiCallback rssiCallback;//获取信号值回调

    private IBleConnectCallback connectCallback;//连接回调

    private boolean isIndication;//是否是指示器方式

    private boolean enable;//是否设置使能

    private volatile HashMap<String, BleGattChannel> writeInfoMap = new HashMap<>();//写入数据GATT信息集合
    private volatile HashMap<String, BleGattChannel> readInfoMap = new HashMap<>();//读取数据GATT信息集合
    private volatile HashMap<String, BleGattChannel> enableInfoMap = new HashMap<>();//设置使能GATT信息集合
    private volatile HashMap<String, IBleDataCallback> bleCallbackMap = new HashMap<>();//数据操作回调集合
    private volatile HashMap<String, IBleDataCallback> receiveCallbackMap = new HashMap<>();//数据接收回调集合

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                connectFailure(new TimeoutException());
            } else if (msg.what == MSG_WRITE_DATA_TIMEOUT) {
                writeFailure(new TimeoutException());
            } else if (msg.what == MSG_READ_DATA_TIMEOUT) {
                readFailure(new TimeoutException());
            } else if (msg.what == MSG_RECEIVE_DATA_TIMEOUT) {
                enableFailure(new TimeoutException());
            }
        }
    };

    /**
     * 蓝牙所有相关操作的核心回调类
     */
    private final BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        /**
         * 连接状态改变，主要用来分析设备的连接与断开
         * @param gatt GATT
         * @param status 改变前状态
         * @param newState 改变后状态
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            ViseLog.i("onConnectionStateChange  status: " + status + " ,newState: " + newState +
                    "  ,thread: " + Thread.currentThread());
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();

                bluetoothGatt = gatt;

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if(bluetoothGatt != null)
                    bluetoothGatt.disconnect();

                close();

                if (connectCallback != null) {
                    handler.removeCallbacksAndMessages(null);

                    clear();

                    if (status == GATT_SUCCESS) {
                        connectCallback.onDisconnect();
                    } else {
                        connectCallback.onConnectFailure(new ConnectException(gatt, status));
                    }
                }
            }
        }

        /**
         * 发现服务，主要用来获取设备支持的服务列表
         * @param gatt GATT
         * @param status 当前状态
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status + "  ,thread: " + Thread.currentThread());

            handler.removeMessages(MSG_CONNECT_TIMEOUT);

            if (status == 0) {
                ViseLog.i("onServicesDiscovered connectSuccess.");

                bluetoothGatt = gatt;

                if (connectCallback != null) {
                    connectCallback.onConnectSuccess(BleDeviceGatt.this);
                }
            } else {
                connectFailure(new ConnectException(gatt, status));
            }
        }

        /**
         * 读取特征值，主要用来读取该特征值包含的可读信息
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 当前状态
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicRead  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == GATT_SUCCESS) {
                handleSuccessData(readInfoMap, characteristic.getValue(), status, true);
            } else {
                readFailure(new GattException(status));
            }
        }

        /**
         * 写入特征值，主要用来发送数据到设备
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 当前状态
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, characteristic.getValue(), status, false);
            } else {
                writeFailure(new GattException(status));
            }
        }

        /**
         * 特征值改变，主要用来接收设备返回的数据信息
         * @param gatt GATT
         * @param characteristic 特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            for (Map.Entry<String, IBleDataCallback> receiveEntry : receiveCallbackMap.entrySet()) {
                String receiveKey = receiveEntry.getKey();
                IBleDataCallback receiveValue = receiveEntry.getValue();
                for (Map.Entry<String, BleGattChannel> gattInfoEntry : enableInfoMap.entrySet()) {
                    String bluetoothGattInfoKey = gattInfoEntry.getKey();
                    BleGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                    if (receiveKey.equals(bluetoothGattInfoKey)) {
                        receiveValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue, bluetoothLeDevice);
                    }
                }
            }
        }

        /**
         * 读取属性描述值，主要用来获取设备当前属性描述的值
         * @param gatt GATT
         * @param descriptor 属性描述
         * @param status 当前状态
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == GATT_SUCCESS) {
                handleSuccessData(readInfoMap, descriptor.getValue(), status, true);
            } else {
                readFailure(new GattException(status));
            }
        }

        /**
         * 写入属性描述值，主要用来根据当前属性描述值写入数据到设备
         * @param gatt GATT
         * @param descriptor 属性描述值
         * @param status 当前状态
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, descriptor.getValue(), status, false);
            } else {
                writeFailure(new GattException(status));
            }
            if (status == GATT_SUCCESS) {
                handleSuccessData(enableInfoMap, descriptor.getValue(), status, false);
            } else {
                enableFailure(new GattException(status));
            }
        }

        /**
         * 阅读设备信号值
         * @param gatt GATT
         * @param rssi 设备当前信号
         * @param status 当前状态
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi +
                    "  ,thread: " + Thread.currentThread());
            if (status == GATT_SUCCESS) {
                if (rssiCallback != null) {
                    rssiCallback.onSuccess(rssi);
                }
            } else {
                if (rssiCallback != null) {
                    rssiCallback.onFailure(new GattException(status));
                }
            }
        }
    };

    public BleDeviceGatt(Context context, BluetoothLeDevice bluetoothLeDevice) {
        this.context = context;

        this.bluetoothLeDevice = bluetoothLeDevice;
    }

    /**
     * 连接设备
     *
     * @param connectCallback connectCallback
     */
    public synchronized void connect(IBleConnectCallback connectCallback) {
        handler.removeCallbacksAndMessages(null);

        this.connectCallback = connectCallback;

        connect();
    }

    /**
     * 绑定一个具备读写或可通知能力的通道，设置需要操作数据的相关信息，包含：数据操作回调，数据操作类型，数据通道建立所需的UUID。
     *
     * @param bleCallback bleCallback
     * @param bleGattChannel gatt Channel to bind
     */
    public synchronized void bindChannel(IBleDataCallback bleCallback, BleGattChannel bleGattChannel) {
        if (bleCallback != null && bleGattChannel != null) {
            String key = bleGattChannel.getGattInfoKey();
            PropertyType propertyType = bleGattChannel.getPropertyType();
            if (!bleCallbackMap.containsKey(key)) {
                bleCallbackMap.put(key, bleCallback);
            }
            if (propertyType == PropertyType.PROPERTY_READ) {
                if (!readInfoMap.containsKey(key)) {
                    readInfoMap.put(key, bleGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_WRITE) {
                if (!writeInfoMap.containsKey(key)) {
                    writeInfoMap.put(key, bleGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bleGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_INDICATE) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bleGattChannel);
                }
            }
        }
    }

    /**
     * 解绑通道
     *
     * @param bleGattChannel gattChannel to unbind
     */
    public synchronized void unbindChannel(BleGattChannel bleGattChannel) {
        if (bleGattChannel != null) {
            String key = bleGattChannel.getGattInfoKey();
            if (bleCallbackMap.containsKey(key)) {
                bleCallbackMap.remove(key);
            }
            if (readInfoMap.containsKey(key)) {
                readInfoMap.remove(key);
            } else if (writeInfoMap.containsKey(key)) {
                writeInfoMap.remove(key);
            } else if (enableInfoMap.containsKey(key)) {
                enableInfoMap.remove(key);
            }
        }
    }

    /**
     * 写入数据
     *
     * @param data written data
     */
    public void writeData(byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("this data is null or length beyond 20 byte.");
            return;
        }
        if (!checkBluetoothGattInfo(writeInfoMap)) {
            return;
        }

        handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);

        write(data);
    }

    /**
     * 读取数据
     */
    public void readData() {
        if (!checkBluetoothGattInfo(readInfoMap)) {
            return;
        }

        handler.removeMessages(MSG_READ_DATA_TIMEOUT);

        read();
    }

    /**
     * 获取设备信号值
     *
     * @param rssiCallback rssiCallback
     */
    public void readRemoteRssi(IBleRssiCallback rssiCallback) {
        this.rssiCallback = rssiCallback;
        if (bluetoothGatt != null) {
            bluetoothGatt.readRemoteRssi();
        }
    }

    /**
     * 注册获取数据通知
     *
     * @param isIndication isIndication
     */
    public void registerNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }

        handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);

        enable = true;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 取消获取数据通知
     *
     * @param isIndication isIndication
     */
    public void unregisterNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }
        handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);

        enable = false;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 设置接收数据监听
     *
     * @param key             接收数据回调key，由serviceUUID+characteristicUUID+descriptorUUID组成
     * @param receiveCallback 接收数据回调
     */
    public void setNotifyListener(String key, IBleDataCallback receiveCallback) {
        receiveCallbackMap.put(key, receiveCallback);
    }

    /**
     * 获取蓝牙GATT
     *
     * @return 返回蓝牙GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 获取设备详细信息
     *
     * @return bluetoothLeDevice
     */
    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    /**
     * 移除数据操作回调
     *
     * @param key blecallback key
     */
    public synchronized void removeBleCallback(String key) {
        bleCallbackMap.remove(key);
    }

    /**
     * 移除接收数据回调
     *
     * @param key receive callback key
     */
    public synchronized void removeReceiveCallback(String key) {
        receiveCallbackMap.remove(key);
    }

    /**
     * 刷新设备缓存
     *
     * @return 返回是否刷新成功
     */
    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && bluetoothGatt != null) {
                final boolean success = (Boolean) refresh.invoke(bluetoothGatt);
                ViseLog.i("Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            ViseLog.e("An exception occured while refreshing device" + e);
        }
        return false;
    }

    /**
     * 主动断开设备连接
     */
    public synchronized void disconnect() {
        if (bluetoothGatt != null) {
            ViseLog.e("BluetoothGatt is disconnected");

            bluetoothGatt.disconnect();
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 关闭GATT
     */
    public synchronized void close() {
        if (bluetoothGatt != null) {
            ViseLog.e("BluetoothGatt is closed");

            bluetoothGatt.close();

            bluetoothGatt = null;
        }
    }

    @Override
    public String toString() {
        String uniqueSymbol = bluetoothLeDevice.getAddress() + bluetoothLeDevice.getName();

        return "BleDeviceGatt{" +
                "bluetoothLeDevice=" + bluetoothLeDevice +
                ", uniqueSymbol='" + uniqueSymbol + '\'' +
                '}';
    }

    /**
     * 清除设备资源，在不使用该设备时调用
     */
    public synchronized void clear() {
        ViseLog.i("deviceMirror clear.");
        disconnect();
        refreshDeviceCache();
        close();
        if (bleCallbackMap != null) {
            bleCallbackMap.clear();
        }
        if (receiveCallbackMap != null) {
            receiveCallbackMap.clear();
        }
        if (writeInfoMap != null) {
            writeInfoMap.clear();
        }
        if (readInfoMap != null) {
            readInfoMap.clear();
        }
        if (enableInfoMap != null) {
            enableInfoMap.clear();
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 检查BluetoothGattChannel集合是否有值
     *
     * @param bluetoothGattInfoHashMap hashMap
     * @return has gatt info
     */
    private boolean checkBluetoothGattInfo(HashMap<String, BleGattChannel> bluetoothGattInfoHashMap) {
        if (bluetoothGattInfoHashMap == null || bluetoothGattInfoHashMap.size() == 0) {
            ViseLog.e("this bluetoothGattInfo map is not value.");
            return false;
        }
        return true;
    }

    /**
     * 连接设备
     */
    private synchronized void connect() {
        handler.removeMessages(MSG_CONNECT_TIMEOUT);

        handler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, BleConfig.getInstance().getConnectTimeout());

        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            bluetoothLeDevice.getDevice().connectGatt(context, false, coreGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    /**
     * 设置使能
     *
     * @param enable       是否具备使能
     * @param isIndication 是否是指示器方式
     * @return isSuccess
     */
    private synchronized boolean enable(boolean enable, boolean isIndication) {
        handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        for (Map.Entry<String, BleGattChannel> entry : enableInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BleGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null) {
                success = bluetoothGatt.setCharacteristicNotification(bluetoothGattInfoValue.getCharacteristic(), enable);
            }
            BluetoothGattDescriptor bluetoothGattDescriptor = null;
            if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattDescriptor = bluetoothGattInfoValue.getDescriptor();
            } else if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                if (bluetoothGattInfoValue.getCharacteristic().getDescriptors() != null
                        && bluetoothGattInfoValue.getCharacteristic().getDescriptors().size() == 1) {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic().getDescriptors().get(0);
                } else {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic()
                            .getDescriptor(UUID.fromString(BleConstant.CLIENT_CHARACTERISTIC_CONFIG));
                }
            }
            if (bluetoothGattDescriptor != null) {
                bluetoothGattInfoValue.setDescriptor(bluetoothGattDescriptor);
                if (isIndication) {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                } else {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                }
                if (bluetoothGatt != null) {
                    bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                }
            }
        }
        return success;
    }

    /**
     * 读取数据
     *
     * @return isSuccess
     */
    private synchronized boolean read() {
        handler.removeMessages(MSG_READ_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        for (Map.Entry<String, BleGattChannel> entry : readInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BleGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                success = bluetoothGatt.readDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                success = bluetoothGatt.readCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 写入数据
     *
     * @param data written data
     * @return isSuccess
     */
    private synchronized boolean write(byte[] data) {
        handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        for (Map.Entry<String, BleGattChannel> entry : writeInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BleGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattInfoValue.getDescriptor().setValue(data);
                success = bluetoothGatt.writeDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                bluetoothGattInfoValue.getCharacteristic().setValue(data);
                success = bluetoothGatt.writeCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 连接失败处理
     *
     * @param bleException 回调异常
     */
    private void connectFailure(BleException bleException) {
        if(bluetoothGatt != null)
            bluetoothGatt.disconnect();

        close();

        if (connectCallback != null) {
            connectCallback.onConnectFailure(bleException);
        }

        ViseLog.i("connectFailure " + bleException);
    }

    /**
     * 使能失败
     *
     * @param bleException exception
     */
    private void enableFailure(BleException bleException) {
        handleFailureData(enableInfoMap, bleException, true);
        ViseLog.i("enableFailure " + bleException);
    }

    /**
     * 读取数据失败
     *
     * @param bleException exception
     */
    private void readFailure(BleException bleException) {
        handleFailureData(readInfoMap, bleException, true);
        ViseLog.i("readFailure " + bleException);
    }

    /**
     * 写入数据失败
     *
     * @param bleException exception
     */
    private void writeFailure(BleException bleException) {
        handleFailureData(writeInfoMap, bleException, true);
        ViseLog.i("writeFailure " + bleException);
    }

    /**
     * 处理数据发送成功
     *
     * @param bluetoothGattInfoHashMap hashMap
     * @param value                    待发送数据
     * @param status                   发送数据状态
     * @param isRemoveCall             是否需要移除回调
     */
    private synchronized void handleSuccessData(HashMap<String, BleGattChannel> bluetoothGattInfoHashMap, byte[] value, int status,
                                                boolean isRemoveCall) {
        handler.removeCallbacksAndMessages(null);

        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleDataCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleDataCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BleGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                BleGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onSuccess(value, bluetoothGattInfoValue, bluetoothLeDevice);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }

    /**
     * 处理数据发送失败
     *
     * @param bluetoothGattInfoHashMap  hashMap
     * @param bleException             回调异常
     * @param isRemoveCall             是否需要移除回调
     */
    private synchronized void handleFailureData(HashMap<String, BleGattChannel> bluetoothGattInfoHashMap, BleException bleException,
                                                boolean isRemoveCall) {
        handler.removeCallbacksAndMessages(null);

        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleDataCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleDataCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BleGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onFailure(bleException);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }
}
