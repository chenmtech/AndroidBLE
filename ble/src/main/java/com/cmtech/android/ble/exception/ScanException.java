package com.cmtech.android.ble.exception;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.exception
 * ClassName:      ScanException
 * Description:    java类作用描述
 * Author:         作者名
 * CreateDate:     2019-12-05 04:31
 * UpdateUser:     更新者
 * UpdateDate:     2019-12-05 04:31
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public class ScanException extends BleException {
    public static final int SCAN_ERR_ALREADY_STARTED = 0;
    public static final int SCAN_ERR_BT_CLOSED = 1;
    public static final int SCAN_ERR_BLE_INNER_ERROR = 2;
    public static final int SCAN_ERR_WAIT_PLEASE = 3;

    private int scanError;

    public ScanException(int scanError, String description) {
        super(BleExceptionCode.SCAN_ERR, description);
        this.scanError = scanError;
    }

    public int getScanError() {
        return scanError;
    }

    @Override
    public String toString() {
        return "ScanException{" +
                "scanError=" + scanError +
                '}' + super.toString();
    }
}
