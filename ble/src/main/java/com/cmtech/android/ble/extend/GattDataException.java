package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.exception.BleException;


/**
  *
  * ClassName:      GattDataException
  * Description:    Ble数据操作异常类
  * Author:         chenm
  * CreateDate:     2018-03-01 08:49
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-28 08:49
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class GattDataException extends BleException {
    GattDataException(BleException exception) {
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
