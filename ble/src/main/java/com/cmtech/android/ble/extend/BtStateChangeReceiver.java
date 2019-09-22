package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.extend
 * ClassName:      BtStateChangeReceiver
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-09-22 13:19
 * UpdateUser:     更新者
 * UpdateDate:     2019-09-22 13:19
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public class BtStateChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    BleDeviceScanner.canUsed = true;

                    Toast.makeText(context, "蓝牙已打开。", Toast.LENGTH_SHORT).show();

                    break;

                case BluetoothAdapter.STATE_OFF:
                    BleDeviceScanner.canUsed = false;

                    break;

            }

        }
    }
}
