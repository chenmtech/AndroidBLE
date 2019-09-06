package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;

import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.callback.scan.ScanCallback;
import com.cmtech.android.ble.callback.scan.SingleFilterScanCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.ViseBle;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CLOSED;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_DISCONNECT;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_DISCONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_FAILURE;
import static com.cmtech.android.ble.extend.BleDeviceState.DEVICE_SCANNING;
import static com.cmtech.android.ble.extend.BleDeviceState.CONNECT_SUCCESS;

/**
  *
  * ClassName:      BleConnectCommandExecutor
  * Description:    设备连接相关命令执行器
  * Author:         chenm
  * CreateDate:     2019-06-25 17:54
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 17:54
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleConnectCommandExecutor {
    private volatile BleDeviceState state = DEVICE_CLOSED; // 设备状态

    private volatile BleDeviceState connectState = CONNECT_DISCONNECT; // 设备连接状态

    private Lock opLock = new ReentrantLock();

    // 扫描回调类
    private class MyScanCallback implements IScanCallback {
        MyScanCallback() {
        }

        @Override
        public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
            opLock.lock();

            try{
                BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    processScanResult(false, null);

                } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    processScanResult(true, bluetoothLeDevice);
                }
            } finally {
                opLock.unlock();
            }

        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        @Override
        public void onScanTimeout() {
            opLock.lock();

            try{
                processScanResult(false, null);
            } finally {
                opLock.unlock();
            }

        }

    }

    // 连接回调类
    private class MyConnectCallback implements IConnectCallback {
        MyConnectCallback() {
        }

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            opLock.lock();

            try{
                processConnectSuccess(deviceMirror);
            } finally {
                opLock.unlock();
            }

        }

        @Override
        public void onConnectFailure(final BleException exception) {
            opLock.lock();

            try{
                processConnectFailure(exception);
            } finally {
                opLock.unlock();
            }

        }

        @Override
        public void onDisconnect(final boolean isActive) {
            opLock.lock();

            try{
                processDisconnect(isActive);
            } finally {
                opLock.unlock();
            }

        }
    }

    private final BleDevice device; // 设备

    private ScanCallback scanCallback; // 扫描回调，startScan时记录下来是因为stopScan时还要用到

    private int curReconnectTimes = 0; // 重连次数


    BleConnectCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException("BleConnectCommandExecutor.device不能为null");
        }

        this.device = device;
    }

    BleDeviceState getState() {
        return state;
    }

    void setState(BleDeviceState state) {
        opLock.lock();

        try {
            if(this.state != state) {
                ViseLog.e("设备状态：" + state);

                this.state = state;

                device.updateState();
            }
        } finally {
            opLock.unlock();
        }

    }

    // 开始扫描
    void startScan() {

        device.postWithMainHandler(new Runnable() {
            @Override
            public void run() {
                opLock.lock();

                try {
                    if(state != CONNECT_FAILURE && state != CONNECT_DISCONNECT)
                        return;

                    scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(device.getMacAddress());

                    scanCallback.setScan(true).scan();

                    setState(DEVICE_SCANNING);

                    curReconnectTimes++;
                } finally {
                    opLock.unlock();
                }

            }
        });
    }

    // 停止扫描
    void stopScan() {
        device.postWithMainHandler(new Runnable() {
            @Override
            public void run() {
                if(scanCallback != null && scanCallback.isScanning()) {

                    scanCallback.removeHandlerMsg();

                    scanCallback.setScan(false).scan();

                    ViseLog.e("stop scanning, please wait...");

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                setState(connectState);
            }
        });
    }

    // 断开连接
    // isReconnect: 断开后是否重连
    void disconnect(final boolean isReconnect) {
        device.postWithMainHandler(new Runnable() {
            @Override
            public void run() {
                opLock.lock();

                try {
                    setState(DEVICE_DISCONNECTING);

                    if(device.getDeviceMirror() != null) {
                        ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());

                        ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror().getBluetoothLeDevice());
                    }
                } finally {
                    opLock.unlock();
                }

                // 等待200ms
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                opLock.lock();

                try {
                    // 再检查是否断开
                    if(connectState != BleDeviceState.CONNECT_DISCONNECT) {

                        device.stopGattExecutor();

                        device.executeAfterDisconnect();

                        device.setDeviceMirror(null);

                        setConnectState(CONNECT_DISCONNECT);
                    }

                    curReconnectTimes = 0;

                    if(isReconnect) {
                        reconnect();
                    }
                } finally {
                    opLock.unlock();
                }

            }
        });
    }


    // 处理扫描结果
    private void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
        if (state != DEVICE_SCANNING) {
            return;
        }

        ViseLog.e("处理扫描结果: " + canConnect + '&' + bluetoothLeDevice);

        if (canConnect) {
            MyConnectCallback connectCallback = new MyConnectCallback();

            ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);

            setState(DEVICE_CONNECTING);
        } else {
            setState(connectState);

            reconnect(); // 重连
        }

    }

    private void reconnect() {
        int canReconnectTimes = device.getReconnectTimes();

        if(canReconnectTimes != -1 && curReconnectTimes >= canReconnectTimes) {
            device.notifyReconnectFailure();

            setConnectState(CONNECT_DISCONNECT);

            curReconnectTimes = 0;
        } else {
            startScan();

            ViseLog.i("重连次数: " + curReconnectTimes);
        }
    }

    // 处理连接成功回调
    private void processConnectSuccess(DeviceMirror mirror) {
        ViseLog.e("处理连接成功: " + mirror);

        // 防止重复连接成功
        if(state == CONNECT_SUCCESS) {
            ViseLog.e("device connected again!!!!!!");

            return;
        }

        if(state == DEVICE_CLOSED) {
            ViseBle.getInstance().disconnect(mirror.getBluetoothLeDevice());
            return;
        }

        device.setDeviceMirror(mirror);

        curReconnectTimes = 0;

        device.startGattExecutor();

        device.executeAfterConnectSuccess();

        setConnectState(CONNECT_SUCCESS);
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("处理连接失败: " + bleException );

        device.stopGattExecutor();

        device.executeAfterConnectFailure();

        device.setDeviceMirror(null);

        setConnectState(CONNECT_FAILURE);

        if(bleException instanceof TimeoutException) {
            device.notifyReconnectFailure();

            setConnectState(CONNECT_DISCONNECT);

            curReconnectTimes = 0;
        } else {
            reconnect();
        }
    }

    // 处理连接断开
    private void processDisconnect(boolean isActive) {
        ViseLog.e("处理连接断开: " + isActive);

        device.stopGattExecutor();

        device.executeAfterDisconnect();

        device.setDeviceMirror(null);

        setConnectState(CONNECT_DISCONNECT);
    }

    private void setConnectState(BleDeviceState connectState) {
        opLock.lock();

        try{
            this.connectState = connectState;

            setState(connectState);
        } finally {
            opLock.unlock();
        }

    }
}
