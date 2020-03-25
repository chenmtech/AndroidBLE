package com.cmtech.android.ble.core;

import android.content.Context;

import com.cmtech.android.ble.BleConfig;
import com.vise.log.ViseLog;

import java.util.Timer;
import java.util.TimerTask;

import static com.cmtech.android.ble.core.DeviceState.CLOSED;
import static com.cmtech.android.ble.core.DeviceState.CONNECTING;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECT;
import static com.cmtech.android.ble.core.DeviceState.DISCONNECTING;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.core
 * ClassName:      AbstractConnector
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-12-06 06:32
 * UpdateUser:     更新者
 * UpdateDate:     2019-12-06 06:32
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public abstract class AbstractConnector implements IConnector {
    protected final String address; // address
    protected final IConnectorCallback connCallback; // connCallback
    protected Context context; // context, set in open function
    protected volatile DeviceState state = CLOSED; // state
    private Timer reconnTimer = new Timer(); // reconnection timer
    private boolean canReconn = false; // Can be reconnected?
    private final long reconnTime; // reconnection times

    public AbstractConnector(String address, IConnectorCallback connCallback) {
        this(address, connCallback, BleConfig.getReconnInterval());
    }

    public AbstractConnector(String address, IConnectorCallback connCallback, long reconnTime) {
        if(connCallback == null) {
            throw new NullPointerException("The connCallback is null");
        }
        this.address = address;
        this.connCallback = connCallback;
        this.reconnTime = reconnTime;
    }

    // 打开设备
    @Override
    public boolean open(Context context) {
        if (state != CLOSED) {
            return false;
        }

        ViseLog.e("Connector.open()");
        this.context = context;
        setState(DISCONNECT);
        return true;
    }

    @Override
    public void close() {
        context = null;
        setState(CLOSED);
        reconnTimer.cancel();
        canReconn = false;
    }

    @Override
    public void connect() {
        setState(CONNECTING);
        reconnTimer.cancel();
        canReconn = true;
    }

    @Override
    public void disconnect(boolean forever) {
        setState(DISCONNECTING);
        reconnTimer.cancel();
        this.canReconn = !forever;
    }

    @Override
    public DeviceState getState() {
        return state;
    }

    protected void setState(DeviceState state) {
        if (this.state != state) {
            ViseLog.e(address + ": " + state);
            this.state = state;
            connCallback.onConnectorStateUpdated();
        }
    }

    protected void reconnect() {
        if(canReconn && reconnTime > 0) {
            reconnTimer.cancel();
            reconnTimer = new Timer();
            reconnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    connect();
                }
            }, reconnTime);
        }
    }
}
