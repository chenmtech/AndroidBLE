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
import android.util.Pair;

import com.cmtech.android.ble.callback.IBleConnectCallback;
import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.callback.IBleRssiCallback;
import com.cmtech.android.ble.common.BleConfig;
import com.cmtech.android.ble.common.BleConstant;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BleDeviceDetailInfo;
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
    private final BleDeviceDetailInfo deviceDetailInfo;//设备详细信息

    private BluetoothGatt bluetoothGatt;//蓝牙GATT

    private IBleRssiCallback rssiCallback;//获取信号值回调

    private IBleConnectCallback connectCallback;//连接回调

    private volatile Pair<IBleDataCallback, BleGattChannel> readPair = null;
    private volatile Pair<IBleDataCallback, BleGattChannel> writePair = null;
    private volatile Map<UUID, IBleDataCallback> notifyCallbackMap = new HashMap<>();

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
                handler.removeCallbacksAndMessages(null);

                if (connectCallback != null) {
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

            bluetoothGatt = gatt;

            if (status == 0) {
                ViseLog.i("onServicesDiscovered connectSuccess.");

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
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                readPair.first.onSuccess(characteristic.getValue(), readPair.second);
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
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                writePair.first.onSuccess(characteristic.getValue(), writePair.second);
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
            for (Map.Entry<UUID, IBleDataCallback> notifyEntry : notifyCallbackMap.entrySet()) {
                UUID notifyKey = notifyEntry.getKey();
                IBleDataCallback notifyValue = notifyEntry.getValue();

                if(notifyKey.equals(characteristic.getUuid())) {
                    notifyValue.onSuccess(characteristic.getValue(), null);
                    break;
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
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                readPair.first.onSuccess(descriptor.getValue(), readPair.second);
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
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                writePair.first.onSuccess(descriptor.getValue(), writePair.second);
            } else {
                writeFailure(new GattException(status));
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



    BleDeviceGatt(BleDeviceDetailInfo bleDeviceDetailInfo) {
        this.deviceDetailInfo = bleDeviceDetailInfo;
    }


    /**
     * 连接设备
     * @param context context
     * @param connectCallback connectCallback
     */
    public synchronized void connect(Context context, IBleConnectCallback connectCallback) {
        if(connectCallback == null) {
            throw new IllegalArgumentException("IBleConnectCallback is null");
        }

        this.connectCallback = connectCallback;

        handler.removeCallbacksAndMessages(null);

        handler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, BleConfig.getInstance().getConnectTimeout());

        if (deviceDetailInfo != null && deviceDetailInfo.getDevice() != null) {
            deviceDetailInfo.getDevice().connectGatt(context, false, coreGattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    /**
     * 写入数据
     *
     * @param data written data
     */
    public synchronized boolean writeData(IBleDataCallback bleDataCallback, BleGattChannel bleGattChannel, byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("this data is null or length beyond 20 byte.");
            return false;
        }

        if(bleDataCallback == null || bleGattChannel == null) {
            throw new IllegalArgumentException("The callback or channel is null.");
        }

        handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        if (bluetoothGatt != null && bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() != null) {
            bleGattChannel.getDescriptor().setValue(data);
            success = bluetoothGatt.writeDescriptor(bleGattChannel.getDescriptor());
        } else if (bluetoothGatt != null && bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() == null) {
            bleGattChannel.getCharacteristic().setValue(data);
            success = bluetoothGatt.writeCharacteristic(bleGattChannel.getCharacteristic());
        }

        if(success)
            writePair = Pair.create(bleDataCallback, bleGattChannel);

        return success;
    }

    /**
     * 读取数据
     */
    public synchronized boolean readData(IBleDataCallback bleDataCallback, BleGattChannel bleGattChannel) {
        if (bleDataCallback == null || bleGattChannel == null) {
            throw new IllegalArgumentException("The callback or channel is null.");
        }

        handler.removeMessages(MSG_READ_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;

        if (bluetoothGatt != null && bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() != null) {
            success = bluetoothGatt.readDescriptor(bleGattChannel.getDescriptor());
        } else if (bluetoothGatt != null && bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() == null) {
            success = bluetoothGatt.readCharacteristic(bleGattChannel.getCharacteristic());
        }

        if(success)
            readPair = new Pair<>(bleDataCallback, bleGattChannel);

        return success;
    }

    /**
     * 设置使能
     *
     * @param enable       是否具备使能
     * @param isIndication 是否是指示器方式
     * @return isSuccess
     */
    public synchronized boolean enable(IBleDataCallback bleDataCallback, BleGattChannel bleGattChannel, IBleDataCallback receiveCallback, boolean enable, boolean isIndication) {
        handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        if (bluetoothGatt != null && bleGattChannel.getCharacteristic() != null) {
            success = bluetoothGatt.setCharacteristicNotification(bleGattChannel.getCharacteristic(), enable);
        }
        BluetoothGattDescriptor bluetoothGattDescriptor = null;
        if (bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() != null) {
            bluetoothGattDescriptor = bleGattChannel.getDescriptor();
        } else if (bleGattChannel.getCharacteristic() != null && bleGattChannel.getDescriptor() == null) {
            if (bleGattChannel.getCharacteristic().getDescriptors() != null
                    && bleGattChannel.getCharacteristic().getDescriptors().size() == 1) {
                bluetoothGattDescriptor = bleGattChannel.getCharacteristic().getDescriptors().get(0);
            } else {
                bluetoothGattDescriptor = bleGattChannel.getCharacteristic()
                        .getDescriptor(UUID.fromString(BleConstant.CLIENT_CHARACTERISTIC_CONFIG));
            }
        }
        if (bluetoothGattDescriptor != null) {
            bleGattChannel.setDescriptor(bluetoothGattDescriptor);
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

        if(success) {
            writePair = Pair.create(bleDataCallback, bleGattChannel);

            if(enable) {
                notifyCallbackMap.put(bleGattChannel.getCharacteristicUUID(), receiveCallback);
            } else {
                notifyCallbackMap.remove(bleGattChannel.getCharacteristicUUID());
            }
        }
        return success;
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
     * @return deviceDetailInfo
     */
    public BleDeviceDetailInfo getDeviceDetailInfo() {
        return deviceDetailInfo;
    }


    /**
     * 刷新设备缓存
     *
     * @return 返回是否刷新成功
     */
    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (bluetoothGatt != null) {
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
        String uniqueSymbol = deviceDetailInfo.getAddress() + deviceDetailInfo.getName();

        return "BleDeviceGatt{" +
                "deviceDetailInfo=" + deviceDetailInfo +
                ", uniqueSymbol='" + uniqueSymbol + '\'' +
                '}';
    }

    /**
     * 清除设备资源，在不使用该设备时调用
     */
    public synchronized void clear() {
        ViseLog.i("BleDeviceGatt clear.");
        disconnect();
        refreshDeviceCache();
        close();

        writePair = null;
        readPair = null;
        notifyCallbackMap.clear();

        handler.removeCallbacksAndMessages(null);
    }

    /**
     * 连接失败处理
     *
     * @param bleException 回调异常
     */
    private void connectFailure(BleException bleException) {
        clear();

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
        handler.removeCallbacksAndMessages(null);
        writePair.first.onFailure(bleException);
        ViseLog.i("enableFailure " + bleException);
    }

    /**
     * 读取数据失败
     *
     * @param bleException exception
     */
    private void readFailure(BleException bleException) {
        handler.removeCallbacksAndMessages(null);
        readPair.first.onFailure(bleException);
        ViseLog.i("readFailure " + bleException);
    }

    /**
     * 写入数据失败
     *
     * @param bleException exception
     */
    private void writeFailure(BleException bleException) {
        handler.removeCallbacksAndMessages(null);
        writePair.first.onFailure(bleException);
        ViseLog.i("writeFailure " + bleException);
    }

}
