package com.cmtech.android.ble.exception.handler;

import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.ConnectException;
import com.cmtech.android.ble.exception.GattException;
import com.cmtech.android.ble.exception.InitiatedException;
import com.cmtech.android.ble.exception.OtherException;
import com.cmtech.android.ble.exception.ScanException;
import com.cmtech.android.ble.exception.TimeoutException;


public abstract class BleExceptionHandler {
    public BleExceptionHandler handleException(BleException exception) {
        if (exception != null) {
            if (exception instanceof ConnectException) {
                onConnectException((ConnectException) exception);
            } else if (exception instanceof GattException) {
                onGattException((GattException) exception);
            } else if (exception instanceof TimeoutException) {
                onTimeoutException((TimeoutException) exception);
            } else if (exception instanceof InitiatedException) {
                onInitiatedException((InitiatedException) exception);
            } else if (exception instanceof ScanException) {
                onScanException((ScanException) exception);
            } else {
                onOtherException((OtherException) exception);
            }
        }
        return this;
    }

    /**
     * connect failed
     */
    protected abstract void onConnectException(ConnectException e);

    /**
     * gatt error status
     */
    protected abstract void onGattException(GattException e);

    /**
     * operation timeout
     */
    protected abstract void onTimeoutException(TimeoutException e);

    /**
     * operation inititiated error
     */
    protected abstract void onInitiatedException(InitiatedException e);

    /**
     * operation scan error
     */
    protected abstract void onScanException(ScanException e);

    /**
     * other exceptions
     */
    protected abstract void onOtherException(OtherException e);
}
