package com.cmtech.android.ble.callback.scan;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 设置扫描指定的单个设备，一般是设备名称和Mac地址
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/9/12 22:16.
 */
public class SingleFilterScanCallback extends ScanCallback {
    private AtomicBoolean hasFound = new AtomicBoolean(false);
    private String deviceName;//指定设备名称
    private String deviceMac;//指定设备Mac地址

    public SingleFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public ScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public ScanCallback setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
        return this;
    }
}
