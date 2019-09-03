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

import static com.cmtech.android.ble.extend.BleDeviceConnectState.CONNECT_CLOSED;
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
    // 扫描回调类
    private class MyScanCallback implements IScanCallback {
        MyScanCallback() {
        }

        @Override
        public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
            BluetoothDevice bluetoothDevice = bluetoothLeDevice.getDevice();

            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                device.postWithMainHandler(new Runnable() {
                    @Override
                    public void run() {
                        processScanResult(false, null);
                    }
                });

            } else if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                device.postWithMainHandler(new Runnable() {
                    @Override
                    public void run() {
                        processScanResult(true, bluetoothLeDevice);
                    }
                });
            }
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {

        }

        @Override
        public void onScanTimeout() {
            device.postWithMainHandler(new Runnable() {
                @Override
                public void run() {
                    processScanResult(false, null);
                }
            });
        }

    }

    // 连接回调类
    private class MyConnectCallback implements IConnectCallback {
        MyConnectCallback() {
        }

        @Override
        public void onConnectSuccess(final DeviceMirror deviceMirror) {
            device.postWithMainHandler(new Runnable() {
                @Override
                public void run() {
                    processConnectSuccess(deviceMirror);
                }
            });
        }

        @Override
        public void onConnectFailure(final BleException exception) {
            device.postWithMainHandler(new Runnable() {
                @Override
                public void run() {
                    processConnectFailure(exception);
                }
            });
        }

        @Override
        public void onDisconnect(final boolean isActive) {
            device.postWithMainHandler(new Runnable() {
                @Override
                public void run() {
                    processDisconnect(isActive);
                }
            });
        }
    }

    private final BleDevice device; // 设备

    private ScanCallback scanCallback; // 扫描回调，startScan时记录下来是因为stopScan时还要用到

    private volatile boolean waitingResponse = false; // 是否在等待响应

    private int curReconnectTimes = 0; // 重连次数

    BleConnectCommandExecutor(BleDevice device) {
        if(device == null) {
            throw new NullPointerException("BleConnectCommandExecutor：device不能为null");
        }

        this.device = device;
    }

    // 开始扫描
    void startScan() {
        device.postWithMainHandler(new Runnable() {
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
        device.postWithMainHandler(new Runnable() {
            @Override
            public void run() {
                if(scanCallback != null && scanCallback.isScanning()) {
                    ViseLog.e("stop scanning...");

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
        device.postWithMainHandler(new Runnable() {
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
        device.postWithMainHandler(new Runnable() {
            @Override
            public void run() {
                ViseLog.e("disconnecting...");

                waitingResponse = true;

                if(device.getDeviceMirror() != null) {
                    ViseBle.getInstance().getDeviceMirrorPool().disconnect(device.getDeviceMirror().getBluetoothLeDevice());

                    ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(device.getDeviceMirror().getBluetoothLeDevice());
                }

                device.postDelayedWithMainHandler(new Runnable() {
                    @Override
                    public void run() {
                        if(device.getConnectState() != BleDeviceConnectState.CONNECT_DISCONNECT && device.getConnectState() != CONNECT_CLOSED) {

                            device.stopGattExecutor();

                            device.executeAfterDisconnect();

                            device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);
                        }

                        clearReconnectTimes();

                        waitingResponse = false;

                        if(isReconnect) {
                            startScan();
                        }
                    }
                }, 500);
            }
        });
    }

    private synchronized void clearReconnectTimes() {
        curReconnectTimes = 0;
    }

    private synchronized void incrementReconnectTimes() {
        curReconnectTimes++;
    }

    private void reconnect() {
        int totalReconnectTimes = device.getReconnectTimes();

        if(totalReconnectTimes != -1 && curReconnectTimes >= totalReconnectTimes) {
            device.notifyReconnectFailure();

            device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

            curReconnectTimes = 0;
        } else {
            startScan();

            ViseLog.i("reconnect times: " + curReconnectTimes);
        }
    }

    // 处理扫描结果
    private void processScanResult(boolean canConnect, BluetoothLeDevice bluetoothLeDevice) {
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
    private void processConnectSuccess(DeviceMirror mirror) {
        ViseLog.e("processConnectSuccess: " + mirror);

        // 防止重复连接成功
        if(device.isConnected()) {
            ViseLog.e("device connected again!!!!!!");

            return;
        }

        if(device.isClosed()) {
            ViseBle.getInstance().disconnect(mirror.getBluetoothLeDevice());
            return;
        }

        device.setDeviceMirror(mirror);

        clearReconnectTimes();

        waitingResponse = false;

        device.startGattExecutor();

        device.executeAfterConnectSuccess();

        device.setConnectState(CONNECT_SUCCESS);
    }

    // 处理连接错误
    private void processConnectFailure(final BleException bleException) {
        ViseLog.e("processConnectFailure: " + bleException );

        waitingResponse = false;

        device.stopGattExecutor();

        device.executeAfterConnectFailure();

        device.setDeviceMirror(null);

        device.setConnectState(BleDeviceConnectState.CONNECT_FAILURE);

        if(bleException instanceof TimeoutException) {
            curReconnectTimes = device.getReconnectTimes();
        }

        reconnect();
    }

    // 处理连接断开
    private void processDisconnect(boolean isActive) {
        ViseLog.e("processDisconnect: " + isActive);

        waitingResponse = false;

        device.stopGattExecutor();

        device.executeAfterDisconnect();

        device.setDeviceMirror(null);

        device.setConnectState(BleDeviceConnectState.CONNECT_DISCONNECT);

    }
}
