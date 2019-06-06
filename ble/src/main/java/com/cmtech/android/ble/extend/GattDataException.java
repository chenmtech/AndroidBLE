package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.exception.BleException;

/**
 * GattDataException: Ble数据操作异常
 * Created by bme on 2018/3/1.
 */

public class GattDataException extends BleException {
    public GattDataException(BleException exception) {
        super(exception.getCode(), exception.getDescription());
    }

    @Override
    public String toString() {
        return "GattDataException{" +
                "code=" + getCode() +
                ", description='" + getDescription() + '\'' +
                '}';
    }
}
