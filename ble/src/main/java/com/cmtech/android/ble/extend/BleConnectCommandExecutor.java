package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;

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

import java.util.concurrent.locks.Lock;

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
  * Description:    设备连接相关命令执行器
  * Author:         chenm
  * CreateDate:     2019-06-25 17:54
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 17:54
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleConnectCommandExecutor {
    private final BleDevice device; // 设备

    private ScanCallback scanCallback; // 扫描回调，startScan时记录下来，stopScan时要用到

    private volatile BleDeviceState state = DEVICE_CLOSED; // 设备状态

    private volatile BleDeviceState connectState = CONNECT_DISCONNECT; // 设备连接状态

    private final Lock connLock; // 锁

    // 扫描回调类
    private class MyScanCallback implements IScanCallback {
        MyScanCallback() {
        }

        // 扫描到设备
        @Override
        public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
            connLock.lock();

            try{
                BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    processScanResult(false, null);

                } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    processScanResult(true, bluetoothLeDevice);
                }
            } finally {
                connLock.unlock();
            }

        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        // 扫描超时
        @Override
        public void onScanTimeout() {
            connLock.lock();

            try{
                processScanResult(false, null);
            } finally {
                connLock.unlock();
            }

        }

    }

    // 连接回调类
    private class MyConnectCallback implements IConnectCallback {
        MyConnectCallback() {
        }

        // 连接成功
        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            connLock.lock();

            try{
                processConnectSuccess(deviceMirror);
            } finally {
                connLock.unlock();
            }
        }

        // 连接失败
        @Override
        public void onConnectFailure(final BleException exception) {
            connLock.lock();

            try{
                processConnectFailure(exception);
            } finally {
                connLock.unlock();
            }

        }

        // 连接中断
        @Override
        public void onDisconnect(final boolean isActive) {
            connLock.lock();

            try{
                processDisconnect(isActive);
            } finally {
                connLock.unlock();
            }

        }
    }

    BleConnectCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException("BleConnectCommandExecutor.device不能为null");
        }

        this.device = device;

        connLock = device.getConnLock();
    }

    BleDeviceState getState() {
        return state;
    }

    void setState(BleDeviceState state) {
        if(this.state != state) {
            ViseLog.e("设备状态：" + state);

            this.state = state;

            device.updateState();
        }
    }

    // 开始扫描
    void startScan() {
        if(state == CONNECT_FAILURE || state == CONNECT_DISCONNECT) {
            scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(device.getMacAddress());

            scanCallback.setScan(true).scan();

            setState(DEVICE_SCANNING);
        }
    }

    // 停止扫描
    void stopScan() {
        if(scanCallback != null && scanCallback.isScanning()) {
            scanCallback.removeHandlerMsg();

            scanCallback.setScan(false).scan();

            ViseLog.e("stop scanning, please wait...");

            while(scanCallback.isScanning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        setState(connectState);
    }

    // 断开连接
    void disconnect() {
        setState(DEVICE_DISCONNECTING);

        if(device.getDeviceMirror() != null) {
            //ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());

            //ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror().getBluetoothLeDevice());
            device.getDeviceMirror().clear();
        }

        // 等待200ms
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        device.stopGattExecutor();

        device.executeAfterDisconnect();

        device.setDeviceMirror(null);

        setConnectState(CONNECT_DISCONNECT);
    }


    // 处理扫描结果
    private void processScanResult(boolean canConnect, final BluetoothLeDevice bluetoothLeDevice) {
        if (state != DEVICE_SCANNING) {
            return;
        }

        ViseLog.e("处理扫描结果: " + canConnect + '&' + bluetoothLeDevice);

        if (canConnect) { // 扫描到设备，发起连接
            MyConnectCallback connectCallback = new MyConnectCallback();

            ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);

            setState(DEVICE_CONNECTING);

        } else {
            setState(connectState);
        }
    }

    // 处理连接成功回调
    private void processConnectSuccess(DeviceMirror mirror) {
        // 防止重复连接成功
        if(state == CONNECT_SUCCESS) {
            ViseLog.e("再次连接成功!!!");

            return;
        }

        if(state == DEVICE_CLOSED) { // 设备已经关闭了，强行断开
            ViseBle.getInstance().disconnect(mirror.getBluetoothLeDevice());

            return;
        }

        if(state == DEVICE_SCANNING) {
            stopScan();
        }

        ViseLog.e("处理连接成功: " + mirror);

        device.setDeviceMirror(mirror);

        device.startGattExecutor();

        if(device.executeAfterConnectSuccess())
            setConnectState(CONNECT_SUCCESS);
        else {
            device.callDisconnect();
        }
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("处理连接失败: " + bleException );

        device.stopGattExecutor();

        device.executeAfterConnectFailure();

        if(device.getDeviceMirror() != null) {
            device.getDeviceMirror().clear();

            device.setDeviceMirror(null);
        }

        setConnectState(CONNECT_FAILURE);
    }

    // 处理连接断开
    private void processDisconnect(boolean isActive) {
        if(connectState != CONNECT_DISCONNECT) {
            ViseLog.e("处理连接断开: " + isActive);

            device.stopGattExecutor();

            device.executeAfterDisconnect();

            if(device.getDeviceMirror() != null) {
                device.getDeviceMirror().clear();

                device.setDeviceMirror(null);
            }

            setConnectState(CONNECT_DISCONNECT);
        }
    }

    private void setConnectState(BleDeviceState connectState) {
        this.connectState = connectState;

        setState(connectState);
    }
}
