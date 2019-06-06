package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.exception.BleException;

/**
 * GattDataOpException: Ble数据操作异常
 * Created by bme on 2018/3/1.
 */

public class GattDataOpException extends BleException {
    public GattDataOpException(BleException exception) {
        super(exception.getCode(), exception.getDescription());
    }

    @Override
    public String toString() {
        return "GattDataOpException{" +
                "code=" + getCode() +
                ", description='" + getDescription() + '\'' +
                '}';
    }
}
