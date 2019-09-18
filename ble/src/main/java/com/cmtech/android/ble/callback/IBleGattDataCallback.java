package com.cmtech.android.ble.callback;

import com.cmtech.android.ble.extend.GattDataException;

/**
  *
  * ClassName:      IBleGattDataCallback
  * Description:    Gatt数据操作回调接口
  * Author:         chenm
  * CreateDate:     2019-06-28 08:48
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-28 08:48
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public interface IBleGattDataCallback {
    void onSuccess(byte[] data); // 数据操作成功

    void onFailure(GattDataException exception); // 数据操作失败
}
