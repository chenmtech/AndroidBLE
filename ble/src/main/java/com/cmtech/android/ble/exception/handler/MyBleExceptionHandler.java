package com.cmtech.android.ble.exception.handler;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.cmtech.android.ble.exception.ScanException;
import com.vise.log.ViseLog;

import static com.cmtech.android.ble.callback.IBleScanCallback.SCAN_FAILED_ALREADY_STARTED;
import static com.cmtech.android.ble.callback.IBleScanCallback.SCAN_FAILED_BLE_CLOSED;
import static com.cmtech.android.ble.callback.IBleScanCallback.SCAN_FAILED_BLE_INNER_ERROR;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.exception.handler
 * ClassName:      MyBleExceptionHandler
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-10-22 06:50
 * UpdateUser:     更新者
 * UpdateDate:     2019-10-22 06:50
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public class MyBleExceptionHandler extends DefaultBleExceptionHandler {
    private Context context;

    public MyBleExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    protected void onScanException(ScanException e) {
        super.onScanException(e);

        int code = e.getScanErrCode();

        switch (code) {
            case SCAN_FAILED_ALREADY_STARTED:
                Toast.makeText(context, "已在扫描中，请等待。", Toast.LENGTH_LONG).show();
                break;
            case SCAN_FAILED_BLE_CLOSED:
                Toast.makeText(context, "系统蓝牙已关闭，请允许打开蓝牙。", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(intent);
                break;
            case SCAN_FAILED_BLE_INNER_ERROR:
                Toast.makeText(context, "系统蓝牙内部错误，请重启蓝牙。", Toast.LENGTH_LONG).show();
                break;
        }
    }
}
