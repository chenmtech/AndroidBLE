package com.cmtech.android.ble.exception;

/**
 * ProjectName:    BtDeviceApp
 * Package:        com.cmtech.android.ble.exception
 * ClassName:      ScanException
 * Description:    扫描异常
 * Author:         作者名
 * CreateDate:     2019-10-22 06:20
 * UpdateUser:     更新者
 * UpdateDate:     2019-10-22 06:20
 * UpdateRemark:   更新说明
 * Version:        1.0
 */
public class ScanException extends BleException {
    private int scanErrCode;

    public ScanException(int scanErrCode) {
        super(BleExceptionCode.SCAN_ERR, "Scan Exception Occurred. The Error Code is " + scanErrCode);
        this.scanErrCode = scanErrCode;
    }

    public int getScanErrCode() {
        return scanErrCode;
    }
}
