package com.cmtech.android.ble;


public class BleConfig {
    public static final String CCC_UUID = "00002902-0000-1000-8000-00805f9b34fb"; // client characteristic config UUID
    public static final String BT_STANDARD_BASE_UUID = "0000XXXX-0000-1000-8000-00805F9B34FB"; // 蓝牙标准基础UUID
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000; // 缺省连接超时时间
    private static final int DEFAULT_DATA_OPERATE_TIMEOUT = 3000; // 缺省数据操作超时时间
    private static final int MIN_AUTO_SCAN_INTERVAL = 10; // 最小自动扫描间隔，单位：秒

    private static BleConfig instance;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT; //连接超时时间（毫秒）
    private int dataOperateTimeout = DEFAULT_DATA_OPERATE_TIMEOUT; //数据操作超时时间（毫秒）
    private int autoScanInterval = MIN_AUTO_SCAN_INTERVAL; // 自动扫描时间间隔，单位：秒

    private BleConfig() {
    }

    public static BleConfig getInstance() {
        if (instance == null) {
            synchronized (BleConfig.class) {
                if (instance == null) {
                    instance = new BleConfig();
                }
            }
        }
        return instance;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public BleConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getDataOperateTimeout() {
        return dataOperateTimeout;
    }

    public BleConfig setDataOperateTimeout(int dataOperateTimeout) {
        this.dataOperateTimeout = dataOperateTimeout;
        return this;
    }

    public int getAutoScanInterval() {
        return autoScanInterval;
    }

    public void setAutoScanInterval(int autoScanInterval) {
        this.autoScanInterval = (autoScanInterval < MIN_AUTO_SCAN_INTERVAL) ? MIN_AUTO_SCAN_INTERVAL : autoScanInterval;
    }
}
