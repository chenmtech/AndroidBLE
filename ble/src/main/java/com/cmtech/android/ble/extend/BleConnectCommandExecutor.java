package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.cmtech.android.ble.callback.BleScanCallback;
import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_FAILURE;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_SUCCESS;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CLOSED;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_DISCONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_SCANNING;

/**
  *
  * ClassName:      BleConnectCommandExecutor
  * Description:    设备连接相关命令及回调执行器
  * Author:         chenm
  * CreateDate:     2019-06-25 17:54
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 17:54
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleConnectCommandExecutor {
    private final BleDevice device; // 设备

    private volatile BleDeviceState connectState = CONNECT_DISCONNECT; // 设备连接状态

    private Handler mHandler = new Handler(Looper.getMainLooper()); // 主线程Handler

    private final BleScanCallback bleScanCallback = new BleScanCallback() {
        @Override
        public void onScanFinish(BluetoothLeDevice bluetoothLeDevice) {
            stopScan(device.getContext());

            if(bluetoothLeDevice != null) {
                BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    processScanResult(null);

                } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    processScanResult(bluetoothLeDevice);
                }
            } else {
                processScanResult(null);
            }
        }
    }; // 扫描回调

    private final IConnectCallback connectCallback = new IConnectCallback() {
        // 连接成功
        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    processConnectSuccess(deviceMirror);
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

    BleConnectCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException("BleConnectCommandExecutor.device不能为null");
        }

        this.device = device;

        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(device.getMacAddress()).build();

        bleScanCallback.setScanFilter(scanFilter);
    }

    // 开始扫描
    void startScan() {
        if(device.getState() == CONNECT_FAILURE || device.getState() == CONNECT_DISCONNECT) {
            bleScanCallback.startScan(device.getContext());

            device.setState(DEVICE_SCANNING);
        }
    }

    // 停止扫描
    void stopScan() {
        bleScanCallback.stopScan(device.getContext());

        device.setState(connectState);
    }

    // 断开连接
    void disconnect() {
        device.setState(DEVICE_DISCONNECTING);

        if(device.getDeviceMirror() != null) {
            device.getDeviceMirror().disconnect();
            //ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());
        }
    }


    // 处理扫描结果
    private void processScanResult(final BluetoothLeDevice bluetoothLeDevice) {

        if(device.getState() == DEVICE_SCANNING) {
            ViseLog.e("处理扫描结果: " + bluetoothLeDevice);

            if (bluetoothLeDevice != null) { // 扫描到设备，发起连接
                //ViseBle.getInstance().connect(device.getContext(), bluetoothLeDevice, connectCallback);
                connect(device.getContext(), bluetoothLeDevice, connectCallback);

                device.setState(DEVICE_CONNECTING);
            } else {
                device.stopScan();
            }
        }

    }

    private void connect(Context context, BluetoothLeDevice bluetoothLeDevice, IConnectCallback connectCallback) {
        if (bluetoothLeDevice == null || connectCallback == null) {
            ViseLog.e("This bluetoothLeDevice or connectCallback is null.");
            return;
        }
        DeviceMirror deviceMirror = new DeviceMirror(bluetoothLeDevice);

        deviceMirror.connect(context, connectCallback);
    }

    // 处理连接成功回调
    private void processConnectSuccess(DeviceMirror mirror) {
        // 防止重复连接成功
        if(device.getState() == CONNECT_SUCCESS) {
            ViseLog.e("再次连接成功!!!");

            return;
        }

        if(device.getState() == DEVICE_CLOSED) { // 设备已经关闭了，强行断开
            //ViseBle.getInstance().disconnect(mirror.getBluetoothLeDevice());
            mirror.disconnect();

            return;
        }

        if(device.getState() == DEVICE_SCANNING) {
            stopScan();
        }

        ViseLog.e("处理连接成功: " + mirror);

        device.setDeviceMirror(mirror);

        device.startGattExecutor();

        if(device.executeAfterConnectSuccess())
            setConnectState(CONNECT_SUCCESS);
        else {
            device.startDisconnection();
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("处理连接失败: " + bleException );

        device.stopGattExecutor();

        device.executeAfterConnectFailure();

        /*if(device.getDeviceMirror() != null) {
            device.getDeviceMirror().clear();
            //ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror());

            device.setDeviceMirror(null);
        }*/

        device.setDeviceMirror(null);

        setConnectState(CONNECT_FAILURE);

        if(bleException instanceof TimeoutException) {
            device.stopScan();
        }

    }

    // 处理连接断开
    private void processDisconnect() {
        if(connectState != CONNECT_DISCONNECT) {
            ViseLog.e("处理连接断开");

            device.stopGattExecutor();

            device.executeAfterDisconnect();

            /*if(device.getDeviceMirror() != null) {
                device.getDeviceMirror().clear();
                //ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror());

                device.setDeviceMirror(null);
            }*/

            device.setDeviceMirror(null);

            setConnectState(CONNECT_DISCONNECT);
        }
    }

    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;

        device.setState(connectState);
    }
}
