package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

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

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_FAILURE;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SCANNING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SUCCESS;

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
            processConnectSuccess(deviceMirror);
        }

        @Override
        public void onConnectFailure(final BleException exception) {
            processConnectFailure(exception);
        }

        @Override
        public void onDisconnect(final boolean isActive) {
            processDisconnect(isActive);
        }
    }

    private final BleDevice device; // 设备

    private final Handler handler = new Handler(Looper.getMainLooper()); // 执行命令的Handler

    private ScanCallback scanCallback; // 扫描回调，startScan时记录下来是因为stopScan时还要用到

    private volatile boolean waitingResponse = false; // 是否在等待响应

    private int curReconnectTimes = 0; // 重连次数

    BleConnectCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException();
        }

        this.device = device;
    }

    // 开始扫描
    void startScan() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacksAndMessages(null);

                ViseLog.e("start scanning...");

                scanCallback = new SingleFilterScanCallback(new MyScanCallback()).setDeviceMac(device.getMacAddress());

                scanCallback.setScan(true).scan();

                device.setConnectState(CONNECT_SCANNING);

                incrementReconnectTimes();

                waitingResponse = true;
            }
        });

    }

    // 停止扫描
    void stopScan() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(scanCallback != null && scanCallback.isScanning()) {
                    waitingResponse = true;

                    scanCallback.removeHandlerMsg();

                    scanCallback.setScan(false).scan();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                waitingResponse = false;
            }
        });

    }

    // 开始连接
    private void startConnect(final BluetoothLeDevice bluetoothLeDevice) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ViseLog.e("start connecting...");

                MyConnectCallback connectCallback = new MyConnectCallback();

                ViseBle.getInstance().connect(bluetoothLeDevice, connectCallback);

                device.setConnectState(CONNECT_CONNECTING);

                waitingResponse = true;
            }
        });

    }

    // 断开连接
    void disconnect(final boolean isReconnect) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ViseLog.e("BleDevice disconnect()");

                waitingResponse = true;

                if(device.getDeviceMirror() != null) {
                    ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());

                    ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror().getBluetoothLeDevice());
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(device.getConnectState() != BleDeviceConnectState.CONNECT_DISCONNECT) {
                    device.executeAfterDisconnect();

                    device.stopGattExecutor();

                    device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);
                }

                clearReconnectTimes();

                waitingResponse = false;

                if(isReconnect) {
                    startScan();
                }
            }
        });

    }

    private synchronized void clearReconnectTimes() {
        curReconnectTimes = 0;
    }

    private synchronized void incrementReconnectTimes() {
        curReconnectTimes++;
    }

    private synchronized void reconnect() {
        int totalReconnectTimes = device.getReconnectTimes();

        if(totalReconnectTimes != -1 && curReconnectTimes >= totalReconnectTimes) {
            device.notifyReconnectFailure();

            device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

            curReconnectTimes = 0;
        } else {
            device.startScan();

            ViseLog.i("reconnect times: " + curReconnectTimes);
        }
    }

    // 处理扫描结果
    private synchronized void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
        ViseLog.e("processScanResult: " + canConnect + '-' + bluetoothLeDevice);

        if (canConnect) {
            startConnect(bluetoothLeDevice); // 连接
        } else {
            device.setConnectState(CONNECT_FAILURE);

            reconnect(); // 重连
        }

        waitingResponse = false;
    }

    // 处理连接成功回调
    private synchronized void processConnectSuccess(DeviceMirror mirror) {
        ViseLog.e("processConnectSuccess: " + mirror);

        // 防止重复连接成功
        if(device.getConnectState() == CONNECT_SUCCESS) {
            ViseLog.e("device connected again!!!!!!");

            return;
        }

        device.setDeviceMirror(mirror);

        device.setConnectState(CONNECT_SUCCESS);

        clearReconnectTimes();

        waitingResponse = false;

        device.startGattExecutor();

        device.executeAfterConnectSuccess();
    }

    // 处理连接错误
    private synchronized void processConnectFailure(final BleException bleException) {
        ViseLog.e("processConnectFailure: " + bleException );

        device.setConnectState(BleDeviceConnectState.CONNECT_FAILURE);

        waitingResponse = false;

        device.executeAfterConnectFailure();

        device.stopGattExecutor();

        device.setDeviceMirror(null);

        reconnect();
    }

    // 处理连接断开
    private synchronized void processDisconnect(boolean isActive) {
        ViseLog.e("processDisconnect: " + isActive);

        device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

        waitingResponse = false;

        device.executeAfterDisconnect();

        device.stopGattExecutor();

        device.setDeviceMirror(null);
    }
}
