package com.cmtech.android.ble;

/**
 * @Description: 蓝牙通信相关配置
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/16 11:46
 */
public class BleConfig {
    public static final String CCC_UUID = "00002902-0000-1000-8000-00805f9b34fb"; // client characteristic config UUID
    public static final String BT_BASE_UUID = "0000XXXX-0000-1000-8000-00805F9B34FB"; // 蓝牙标准基础UUID
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000; // 缺省连接超时时间
    private static final int DEFAULT_DATA_OPERATE_TIMEOUT = 3000; // 缺省数据操作超时时间

    private static BleConfig instance;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT; //连接超时时间（毫秒）
    private int dataOperateTimeout = DEFAULT_DATA_OPERATE_TIMEOUT; //数据操作超时时间（毫秒）

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

    /**
     * 获取连接超时时间
     *
     * @return 返回连接超时时间
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置连接超时时间
     *
     * @param connectTimeout 连接超时时间
     * @return 返回BleConfig
     */
    public BleConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 获取数据操作超时时间
     *
     * @return 返回数据操作超时时间
     */
    public int getDataOperateTimeout() {
        return dataOperateTimeout;
    }

    /**
     * 设置数据操作超时时间
     *
     * @param dataOperateTimeout 数据操作超时时间
     * @return 返回BleConfig
     */
    public BleConfig setDataOperateTimeout(int dataOperateTimeout) {
        this.dataOperateTimeout = dataOperateTimeout;
        return this;
    }
}
