package com.cmtech.android.ble.core;

import android.bluetooth.BluetoothAdapter;
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
import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * ClassName:      BleGatt
 * Description:    class for gatt operations
 * Author:         chenm
 * CreateDate:     2018-06-27 08:56
 * UpdateUser:     chenm
 * UpdateDate:     2019-06-28 08:56
 * UpdateRemark:   更新说明
 * Version:        1.0
 */

public class BleGatt {
    private static final int MSG_CONNECT = 1; // connection message
    private static final int MSG_DISCONNECT = 2; // disconnection message

    private BluetoothGatt bluetoothGatt; //底层蓝牙GATT
    private final Context context;
    private final String bleAddress; // ble connCallback address
    private final IBleConnectCallback connectCallback;//连接回调
    private volatile Pair<BleGattElement, IBleDataCallback> readElementCallback = null; // 读操作的Element和Callback对
    private volatile Pair<BleGattElement, IBleDataCallback> writeElementCallback = null; // 写操作的Element和Callback对
    private volatile Map<UUID, Pair<BleGattElement, IBleDataCallback>> notifyElementCallbackMap = new HashMap<>(); // Notify或Indicate操作的Element和Callback Map

    private final Lock connLock = new ReentrantLock();
    private boolean acting = false;

    // 回调Handler，除了onCharacteristicChanged回调在其本身的线程中执行外，其他所有回调处理都在此Handler中执行
    private final Handler callbackHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(acting) return;
            switch (msg.what) {
                case MSG_CONNECT:
                    BluetoothDevice bluetoothDevice = getBluetoothDevice(bleAddress);
                    if(bluetoothDevice != null) {
                        acting = true;
                        bluetoothDevice.connectGatt(context, false, coreGattCallback, BluetoothDevice.TRANSPORT_LE);
                    }
                    break;

                case MSG_DISCONNECT:
                    if(bluetoothGatt != null) {
                        acting = true;
                        bluetoothGatt.disconnect();
                    }
                    break;
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
            String newStateStr = (newState == 2) ? "CONNECTED" : "DISCONNECTED";
            ViseLog.i("onConnectionStateChange  status: " + status + " ,newState: " + newStateStr +
                    "  ,thread: " + Thread.currentThread());
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        // more connection success wrong
                        if (bluetoothGatt != null && bluetoothGatt != gatt) {
                            gatt.disconnect();
                            gatt.close();
                            return;
                        }

                        bluetoothGatt = gatt;
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        if (bluetoothGatt != null) {
                            bluetoothGatt.disconnect();
                            bluetoothGatt.close();
                            bluetoothGatt = null;
                        }

                        //more disconnection wrong
                        gatt.disconnect();
                        gatt.close();

                        if (status == GATT_SUCCESS) {
                            connectCallback.onDisconnect();
                        } else {
                            connectCallback.onConnectFailure(new ConnectException(gatt, status));
                        }
                        acting = false;
                    }
                }
            });
        }

        /**
         * 发现服务，主要用来获取设备支持的服务列表
         * @param gatt GATT
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status + "  ,thread: " + Thread.currentThread());
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    bluetoothGatt = gatt;

                    if (status == GATT_SUCCESS) {
                        connectCallback.onConnectSuccess(BleGatt.this);
                    } else {
                        if (bluetoothGatt != null) {
                            bluetoothGatt.disconnect();
                            bluetoothGatt.close();
                            bluetoothGatt = null;
                        }

                        connectCallback.onConnectFailure(new ConnectException(gatt, status));

                    }
                    acting = false;
                }
            });
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
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == GATT_SUCCESS) {
                        if (readElementCallback.second != null && readElementCallback.first.getCharacteristicUUID().equals(characteristic.getUuid()))
                            readElementCallback.second.onSuccess(characteristic.getValue(), readElementCallback.first);
                    } else {
                        ViseLog.e("readFailure " + new GattException(status));
                        if (readElementCallback != null && readElementCallback.second != null)
                            readElementCallback.second.onFailure(new GattException(status));
                    }
                }
            });
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

            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == GATT_SUCCESS) {
                        if (writeElementCallback.second != null && writeElementCallback.first.getCharacteristicUUID().equals(characteristic.getUuid()))
                            writeElementCallback.second.onSuccess(characteristic.getValue(), writeElementCallback.first);
                    } else {
                        ViseLog.e("writeFailure " + new GattException(status));
                        if (writeElementCallback.second != null)
                            writeElementCallback.second.onFailure(new GattException(status));
                    }
                }
            });
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
            for (Map.Entry<UUID, Pair<BleGattElement, IBleDataCallback>> notifyEntry : notifyElementCallbackMap.entrySet()) {
                UUID notifyKey = notifyEntry.getKey();
                Pair<BleGattElement, IBleDataCallback> notifyValue = notifyEntry.getValue();
                if (notifyValue != null && notifyValue.second != null && notifyKey.equals(characteristic.getUuid())) {
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
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == GATT_SUCCESS) {
                        if (readElementCallback.second != null && readElementCallback.first.getDescriptorUUID().equals(descriptor.getUuid()))
                            readElementCallback.second.onSuccess(descriptor.getValue(), readElementCallback.first);
                    } else {
                        ViseLog.e("readFailure " + new GattException(status));
                        if (readElementCallback != null && readElementCallback.second != null)
                            readElementCallback.second.onFailure(new GattException(status));
                    }
                }
            });
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
            callbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == GATT_SUCCESS) {
                        if (writeElementCallback.second != null && writeElementCallback.first.getDescriptorUUID().equals(descriptor.getUuid()))
                            writeElementCallback.second.onSuccess(descriptor.getValue(), writeElementCallback.first);
                    } else {
                        ViseLog.e("writeFailure " + new GattException(status));
                        if (writeElementCallback.second != null)
                            writeElementCallback.second.onFailure(new GattException(status));
                    }
                }
            });
        }

        /**
         * 阅读设备信号值
         * @param gatt GATT
         * @param rssi 设备当前信号
         * @param status 操作状态，成功还是失败
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi +
                    "  ,thread: " + Thread.currentThread());

        }
    };

    BleGatt(final Context context, final String bleAddress, final IBleConnectCallback connectCallback) {
        if (context == null || bleAddress == null || connectCallback == null) {
            throw new IllegalArgumentException("BleGatt params is null");
        }
        this.context = context;
        this.bleAddress = bleAddress;
        this.connectCallback = connectCallback;
    }

    // connect
    public void connect() {
        try{
            connLock.lock();
            callbackHandler.removeMessages(MSG_CONNECT);
            callbackHandler.removeMessages(MSG_DISCONNECT);
            callbackHandler.sendEmptyMessage(MSG_CONNECT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connLock.unlock();
        }

    }

    // disconnect
    public void disconnect(boolean forever) {
        try{
            connLock.lock();
            callbackHandler.removeMessages(MSG_CONNECT);
            callbackHandler.removeMessages(MSG_DISCONNECT);
            callbackHandler.sendEmptyMessage(MSG_DISCONNECT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connLock.unlock();
        }
    }

    // close
    public void close() {
        try{
            connLock.lock();
            ViseLog.e("BleGatt close.");
            callbackHandler.removeCallbacksAndMessages(null);
            acting = false;

            //refreshDeviceCache();
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }

            readElementCallback = null;
            writeElementCallback = null;
            notifyElementCallbackMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connLock.unlock();
        }
    }

    /**
     * 读取数据
     */
    public void readData(final BleGattElement gattElement, final IBleDataCallback dataCallback) {
        if (dataCallback == null || gattElement == null) {
            return;
        }

        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatt == null) return;
                boolean success = false;
                BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
                BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
                if (characteristic != null) {
                    if (descriptor != null) {
                        success = bluetoothGatt.readDescriptor(descriptor);
                    } else {
                        success = bluetoothGatt.readCharacteristic(characteristic);
                    }
                }

                if (success)
                    readElementCallback = Pair.create(gattElement, dataCallback);
            }
        });
    }

    /**
     * 写入数据
     *
     * @param data written data
     */
    public void writeData(final BleGattElement gattElement, final IBleDataCallback dataCallback, final byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("The data is null or length beyond 20 byte.");
            return;
        }
        if (dataCallback == null || gattElement == null) {
            return;
        }

        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatt == null) return;
                boolean success = false;
                BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
                BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
                if (characteristic != null) {
                    if (descriptor != null) {
                        descriptor.setValue(data);
                        success = bluetoothGatt.writeDescriptor(descriptor);
                    } else {
                        characteristic.setValue(data);
                        success = bluetoothGatt.writeCharacteristic(characteristic);
                    }
                }

                if (success)
                    writeElementCallback = Pair.create(gattElement, dataCallback);
            }
        });
    }

    /**
     * 设置使能
     *
     * @param enable       是否具备使能
     * @param isIndication 是否是指示器方式
     * @return isSuccess
     */
    public void enable(final BleGattElement gattElement, final IBleDataCallback dataCallback, final IBleDataCallback receiveCallback, final boolean enable, final boolean isIndication) {
        if (dataCallback == null || gattElement == null) {
            return;
        }

        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                if(bluetoothGatt == null) return;
                BluetoothGattCharacteristic characteristic = gattElement.getCharacteristic(bluetoothGatt);
                BluetoothGattDescriptor descriptor = gattElement.getDescriptor(bluetoothGatt);
                if (characteristic == null || descriptor == null) {
                    return;
                }
                boolean success = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                if (success) {
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

                if (success) {
                    writeElementCallback = Pair.create(gattElement, dataCallback);

                    if (enable) {
                        notifyElementCallbackMap.put(gattElement.getCharacteristicUUID(), Pair.create(gattElement, receiveCallback));
                    } else {
                        notifyElementCallbackMap.remove(gattElement.getCharacteristicUUID());
                    }
                }
            }
        });

    }

    /**
     * 获取蓝牙GATT
     *
     * @return 返回蓝牙GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    @Override
    public String toString() {
        return "BleGatt{" + bluetoothGatt.getDevice().getAddress() +
                ", " + bluetoothGatt.getDevice().getName() +
                '}';
    }

    private BluetoothDevice getBluetoothDevice(String address) {
        if(!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null) return null;
        return adapter.getRemoteDevice(address);
    }

}
