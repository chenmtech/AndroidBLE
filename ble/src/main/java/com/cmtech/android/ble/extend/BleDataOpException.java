package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.exception.BleException;

/**
 * BleDataOpException: Ble数据操作异常
 * Created by bme on 2018/3/1.
 */

public class BleDataOpException extends BleException {
    public BleDataOpException(BleException exception) {
        super(exception.getCode(), exception.getDescription());
    }

    @Override
    public String toString() {
        return "BleDataOpException{" +
                "code=" + getCode() +
                ", description='" + getDescription() + '\'' +
                '}';
    }
}
