package com.cmtech.android.ble.exception.handler;

import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.exception.InitiatedException;
import com.cmtech.android.ble.exception.OtherException;
import com.cmtech.android.ble.exception.ScanException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.vise.log.ViseLog;

public class DefaultBleExceptionHandler extends BleExceptionHandler {
    @Override
    protected void onConnectException(ConnectException e) {
        ViseLog.e(e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        ViseLog.e(e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        ViseLog.e(e.getDescription());
    }

    @Override
    protected void onInitiatedException(InitiatedException e) {
        ViseLog.e(e.getDescription());
    }

    @Override
    protected void onScanException(ScanException e) {
        ViseLog.e(e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        ViseLog.e(e.getDescription());
    }

}
