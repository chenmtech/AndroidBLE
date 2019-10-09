package com.cmtech.android.ble;

/**
 * @Description: 蓝牙通信相关配置
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/16 11:46
 */
public class BleConfig {
    public static final String CCC_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String BT_BASE_UUID = "0000XXXX-0000-1000-8000-00805F9B34FB"; // 蓝牙标准基础UUID

    private static final int DEFAULT_CONN_TIME = 30000;
    private static final int DEFAULT_OPERATE_TIME = 3000;

    private static BleConfig instance;
    private int connectTimeout = DEFAULT_CONN_TIME; //连接超时时间（毫秒）
    private int operateTimeout = DEFAULT_OPERATE_TIME; //数据操作超时时间（毫秒）

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
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * 设置数据操作超时时间
     *
     * @param operateTimeout 数据操作超时时间
     * @return 返回BleConfig
     */
    public BleConfig setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }
}
