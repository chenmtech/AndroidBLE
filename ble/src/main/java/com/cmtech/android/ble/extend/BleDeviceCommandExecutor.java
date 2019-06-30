package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.HandlerThread;

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

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CLOSED;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CONNECTING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_FAILURE;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SCANNING;
import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_SUCCESS;

/**
  *
  * ClassName:      BleDeviceCommandExecutor
  * Description:    设备命令执行器
  * Author:         chenm
  * CreateDate:     2019-06-25 17:54
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-25 17:54
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleDeviceCommandExecutor {

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

    private Handler handler; // 执行命令的线程句柄

    private boolean quitHandlerWhenStoping; // 关闭时是否终止Handler

    private ScanCallback scanCallback; // 扫描回调

    private volatile boolean waitingResponse = false; // 是否在等待响应

    private int curReconnectTimes = 0; // 重连次数

    BleDeviceCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException();
        }

        this.device = device;
    }

    // 启动
    void start() {
        start(null);
    }

    void start(Handler handler) {
        if(handler == null) {
            HandlerThread handlerThread = new HandlerThread("MT_Device_Cmd");

            handlerThread.start();

            this.handler = new Handler(handlerThread.getLooper());

            quitHandlerWhenStoping = true;
        } else {
            this.handler = handler;

            quitHandlerWhenStoping = false;
        }

        ViseLog.e("devCmdExecutor starts.");
    }

    // 停止
    void stop() {
        if(handler == null) return;

        //disconnect();

        final CountDownLatch lock = new CountDownLatch(1);

        handler.post(new Runnable() {
            @Override
            public void run() {
                device.setConnectState(BleDeviceConnectState.CONNECT_CLOSED);

                lock.countDown();

            }
        });

        try {
            lock.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(quitHandlerWhenStoping) {
            handler.getLooper().quitSafely();
        }

        ViseLog.e("devCmdExecutor stops.");
    }

    // 开始扫描
    void startScan() {
        handler.post(new Runnable() {
            @Override
            public void run() {
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
                        Thread.sleep(1000);
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
    void disconnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ViseLog.e("disconnecting...");

                waitingResponse = true;

                if(device.getDeviceMirror() != null) {
                    ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());

                    ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror().getBluetoothLeDevice());
                }

                try {
                    Thread.sleep(500);
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

            List<DeviceMirror> deviceMirrors = ViseBle.getInstance().getDeviceMirrorPool().getDeviceMirrorList();

            ViseLog.e("Current DeviceMirror List: ");
            for(DeviceMirror deviceMirror : deviceMirrors) {
                ViseLog.e(deviceMirror);
            }

            if(device.getDeviceMirror().getUniqueSymbol().equals(mirror.getUniqueSymbol())) {
                ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(mirror);
            }

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
