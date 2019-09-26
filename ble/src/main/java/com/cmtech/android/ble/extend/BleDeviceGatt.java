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
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.cmtech.android.ble.common.BleConstant.MSG_CONNECT_TIMEOUT;
import static com.cmtech.android.ble.common.BleConstant.MSG_READ_DATA_TIMEOUT;
import static com.cmtech.android.ble.common.BleConstant.MSG_WRITE_DATA_TIMEOUT;

/**
 *
 * ClassName:      BleDeviceGatt
 * Description:    设备Gatt操作类，完成Gatt操作
 * Author:         chenm
 * CreateDate:     2018-06-27 08:56
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-28 08:56
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public class BleDeviceGatt {

    private BluetoothGatt bluetoothGatt; //底层蓝牙GATT
    private IBleRssiCallback rssiCallback; //获取信号值回调
    private IBleConnectCallback connectCallback;//连接回调

    private volatile Pair<BleGattElement, IBleDataCallback> readChannelCallback = null; // 读操作的Channel和Callback
    private volatile Pair<BleGattElement, IBleDataCallback> writeChannelCallback = null; // 写操作的Channel和Callback
    private volatile Map<UUID, Pair<BleGattElement, IBleDataCallback>> notifyChannelCallbackMap = new HashMap<>(); // Notify或Indicate操作的Channel和Callback Map

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                connectFailure(new TimeoutException());
            } else if (msg.what == MSG_WRITE_DATA_TIMEOUT) {
                writeFailure(new TimeoutException());
            } else if (msg.what == MSG_READ_DATA_TIMEOUT) {
                readFailure(new TimeoutException());
            }
        }
    };

    /**
     * 蓝牙所有Gatt操作的回调
     */
    private final BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {
        /**
         * 连接状态改变，主要用来分析设备的连接与断开
         * @param gatt GATT
         * @param status 操作状态，成功还是失败
         * @param newState 设备的当前状态
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            ViseLog.i("onConnectionStateChange  status: " + status + " ,newState: " + newState +
                    "  ,thread: " + Thread.currentThread());
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                bluetoothGatt = gatt;
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                //handler.removeCallbacksAndMessages(null);

                clear();
                if (connectCallback != null) {
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
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status + "  ,thread: " + Thread.currentThread());

            handler.removeMessages(MSG_CONNECT_TIMEOUT);
            bluetoothGatt = gatt;

            if (status == GATT_SUCCESS) {
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
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicRead  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                if(readChannelCallback.second != null && readChannelCallback.first.getCharacteristicUUID().equals(characteristic.getUuid()))
                    readChannelCallback.second.onSuccess(characteristic.getValue(), readChannelCallback.first);
            } else {
                readFailure(new GattException(status));
            }
        }

        /**
         * 写入特征值，主要用来发送数据到设备
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                if(writeChannelCallback.second != null && writeChannelCallback.first.getCharacteristicUUID().equals(characteristic.getUuid()))
                    writeChannelCallback.second.onSuccess(characteristic.getValue(), writeChannelCallback.first);
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
            for (Map.Entry<UUID, Pair<BleGattElement, IBleDataCallback>> notifyEntry : notifyChannelCallbackMap.entrySet()) {
                UUID notifyKey = notifyEntry.getKey();
                Pair<BleGattElement, IBleDataCallback> notifyValue = notifyEntry.getValue();
                if(notifyValue != null && notifyValue.second != null && notifyKey.equals(characteristic.getUuid())) {
                    notifyValue.second.onSuccess(characteristic.getValue(), notifyValue.first);
                    break;
                }
            }
        }

        /**
         * 读取属性描述值，主要用来获取设备当前属性描述的值
         * @param gatt GATT
         * @param descriptor 属性描述
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                if(readChannelCallback.second != null && readChannelCallback.first.getDescriptorUUID().equals(descriptor.getUuid()))
                    readChannelCallback.second.onSuccess(descriptor.getValue(), readChannelCallback.first);
            } else {
                readFailure(new GattException(status));
            }
        }

        /**
         * 写入属性描述值，主要用来根据当前属性描述值写入数据到设备
         * @param gatt GATT
         * @param descriptor 属性描述值
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);

            if (status == GATT_SUCCESS) {
                if(writeChannelCallback.second != null && writeChannelCallback.first.getDescriptorUUID().equals(descriptor.getUuid()))
                    writeChannelCallback.second.onSuccess(descriptor.getValue(), writeChannelCallback.first);
            } else {
                writeFailure(new GattException(status));
            }
        }

        /**
         * 阅读设备信号值
         * @param gatt GATT
         * @param rssi 设备当前信号
         * @param status 操作状态，成功还是失败
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



    BleDeviceGatt() {
    }


    /**
     * 连接设备
     * @param context context
     * @param connectCallback connectCallback
     */
    public synchronized void connect(Context context, BluetoothDevice device, IBleConnectCallback connectCallback) {
        if(connectCallback == null) {
            throw new IllegalArgumentException("IBleConnectCallback is null");
        }
        if(device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }

        this.connectCallback = connectCallback;
        handler.removeCallbacksAndMessages(null);
        handler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, BleConfig.getInstance().getConnectTimeout());
        device.connectGatt(context, false, coreGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    /**
     * 读取数据
     */
    public synchronized boolean readData(BleGattElement gattElement, IBleDataCallback dataCallback) {
        if (bluetoothGatt == null || dataCallback == null || gattElement == null) {
            return false;
        }

        //handler.removeMessages(MSG_READ_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
        BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
        if(characteristic != null) {
            if (descriptor != null) {
                success = bluetoothGatt.readDescriptor(descriptor);
            } else {
                success = bluetoothGatt.readCharacteristic(characteristic);
            }
        }

        if(success)
            readChannelCallback = Pair.create(gattElement, dataCallback);

        return success;
    }

    /**
     * 写入数据
     *
     * @param data written data
     */
    public synchronized boolean writeData(BleGattElement gattElement, IBleDataCallback dataCallback, byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("this data is null or length beyond 20 byte.");
            return false;
        }
        if(bluetoothGatt == null || dataCallback == null || gattElement == null) {
            return false;
        }

        //handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
        BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
        if(characteristic != null) {
            if (descriptor != null) {
                descriptor.setValue(data);
                success = bluetoothGatt.writeDescriptor(descriptor);
            } else {
                characteristic.setValue(data);
                success = bluetoothGatt.writeCharacteristic(characteristic);
            }
        }

        if(success)
            writeChannelCallback = Pair.create(gattElement, dataCallback);

        return success;
    }

    /**
     * 设置使能
     *
     * @param enable       是否具备使能
     * @param isIndication 是否是指示器方式
     * @return isSuccess
     */
    public synchronized boolean enable(BleGattElement gattElement, IBleDataCallback dataCallback, IBleDataCallback receiveCallback, boolean enable, boolean isIndication) {
        if(bluetoothGatt == null || dataCallback == null || gattElement == null) {
            return false;
        }

        //handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
        handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());

        boolean success = false;
        BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
        BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
        if(characteristic == null || descriptor == null) {
            return false;
        }
        success = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        if(success) {
            if (isIndication) {
                if (enable) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
            } else {
                if (enable) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
            }
            success = bluetoothGatt.writeDescriptor(descriptor);
        }

        if(success) {
            writeChannelCallback = Pair.create(gattElement, dataCallback);

            if(enable) {
                notifyChannelCallbackMap.put(gattElement.getCharacteristicUUID(), Pair.create(gattElement, receiveCallback));
            } else {
                notifyChannelCallbackMap.remove(gattElement.getCharacteristicUUID());
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
     * 主动断开设备连接
     */
    void disconnect() {
        if (bluetoothGatt != null) {
            ViseLog.e("BluetoothGatt is disconnected");

            bluetoothGatt.disconnect();
        }
        //handler.removeCallbacksAndMessages(null);
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
     * 关闭GATT
     */
    private void close() {
        if (bluetoothGatt != null) {
            ViseLog.e("BluetoothGatt is closed");
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    /**
     * 清除设备资源，在不使用该设备时调用
     */
    public synchronized void clear() {
        ViseLog.i("BleDeviceGatt clear.");
        disconnect();
        refreshDeviceCache();
        close();

        readChannelCallback = null;
        writeChannelCallback = null;
        notifyChannelCallbackMap.clear();

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public String toString() {
        return "BleDeviceGatt{" + bluetoothGatt.getDevice().getAddress() +
                ", " + bluetoothGatt.getDevice().getName() +
                '}';
    }

    /**
     * 连接失败处理
     *
     * @param bleException 回调异常
     */
    private void connectFailure(BleException bleException) {
        handler.removeMessages(MSG_CONNECT_TIMEOUT);
        clear();
        if (connectCallback != null) {
            connectCallback.onConnectFailure(bleException);
        }
        ViseLog.i("connectFailure " + bleException);
    }

    /**
     * 读取数据失败
     *
     * @param bleException exception
     */
    private void readFailure(BleException bleException) {
        handler.removeMessages(MSG_READ_DATA_TIMEOUT);
        if(readChannelCallback.second != null)
            readChannelCallback.second.onFailure(bleException);
        ViseLog.i("readFailure " + bleException);
    }

    /**
     * 写入数据失败
     *
     * @param bleException exception
     */
    private void writeFailure(BleException bleException) {
        handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
        if(writeChannelCallback.second != null)
            writeChannelCallback.second.onFailure(bleException);
        ViseLog.i("writeFailure " + bleException);
    }

}
