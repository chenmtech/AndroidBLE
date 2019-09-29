package com.cmtech.android.ble.core;


import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
  *
  * ClassName:      BleDeviceRegisterInfo
  * Description:    设备注册信息，字段信息将保存在数据库或Preference中
  * Author:         chenm
  * CreateDate:     2018-06-27 08:56
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-28 08:56
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class BleDeviceRegisterInfo implements Serializable{
    private final static long serialVersionUID = 1L;

    // 下面五个参数用于登记设备基本信息
    public static final String DEFAULT_DEVICE_NICKNAME = ""; // 缺省设备名
    public static final String DEFAULT_DEVICE_IMAGEPATH = ""; // 缺省设备图标路径名
    public static final boolean DEFAULT_DEVICE_AUTOCONNECT = true; // 设备打开时是否自动连接
    public static final int DEFAULT_DEVICE_RECONNECT_TIMES = 3; // 连接失败后的重连次数
    public static final boolean DEFAULT_WARN_WHEN_BLE_ERROR = true; // 缺省的蓝牙错误是否报警

    private String macAddress = ""; // 设备mac地址
    private String nickName = DEFAULT_DEVICE_NICKNAME; // 设备昵称
    private String uuidString = ""; // 设备广播Uuid16位字符串
    private String imagePath = DEFAULT_DEVICE_IMAGEPATH; // 设备图标路径名
    private boolean autoConnect = DEFAULT_DEVICE_AUTOCONNECT; // 设备打开后是否自动连接
    private int reconnectTimes = DEFAULT_DEVICE_RECONNECT_TIMES; // 连接断开后重连次数
    private boolean warnWhenBleError = DEFAULT_WARN_WHEN_BLE_ERROR; // 重连失败后是否报警

    public BleDeviceRegisterInfo() {
    }

    public BleDeviceRegisterInfo(String macAddress, String nickName, String uuidString, String imagePath,
                                 boolean autoConnect, int reconnectTimes, boolean warnWhenBleError) {
        this.macAddress = macAddress;
        this.nickName = nickName;
        this.uuidString = uuidString;
        this.imagePath = imagePath;
        this.autoConnect = autoConnect;
        this.reconnectTimes = reconnectTimes;
        this.warnWhenBleError = warnWhenBleError;
    }

    public String getMacAddress() {
        return macAddress;
    }
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public String getNickName() {
        return nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    public String getUuidString() {
        return uuidString;
    }
    public void setUuidString(String uuidString) {
        this.uuidString = uuidString;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public boolean autoConnect() {
        return autoConnect;
    }
    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }
    public int getReconnectTimes() {
        return reconnectTimes;
    }
    public void setReconnectTimes(int reconnectTimes) {
        this.reconnectTimes = reconnectTimes;
    }
    public boolean isWarnWhenBleError() {
        return warnWhenBleError;
    }
    public void setWarnWhenBleError(boolean warnWhenBleError) {
        this.warnWhenBleError = warnWhenBleError;
    }

    // 将设备基本信息保存到Pref
    public boolean saveToPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if((addressSet != null) && (addressSet.isEmpty() || !addressSet.contains(macAddress))) {
            addressSet.add(macAddress);
            editor.putStringSet("addressSet", addressSet);
        }

        editor.putString(macAddress+"_macAddress", macAddress);
        editor.putString(macAddress+"_nickName", nickName);
        editor.putString(macAddress+"_uuidString", uuidString);
        editor.putString(macAddress+"_imagePath", imagePath);
        editor.putBoolean(macAddress+"_autoConnect", autoConnect);
        editor.putInt(macAddress+"_reconnectTimes", reconnectTimes);
        editor.putBoolean(macAddress+"_warnWhenBleError", warnWhenBleError);
        return editor.commit();
    }

    // 从Pref中删除设备基本信息
    public boolean deleteFromPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if((addressSet != null) && !addressSet.isEmpty() && addressSet.contains(macAddress)) {
            addressSet.remove(macAddress);
            editor.putStringSet("addressSet", addressSet);
        }

        editor.remove(macAddress+"_macAddress");
        editor.remove(macAddress+"_nickName");
        editor.remove(macAddress+"_uuidString");
        editor.remove(macAddress+"_imagePath");
        editor.remove(macAddress+"_autoConnect");
        editor.remove(macAddress+"_reconnectTimes");
        editor.remove(macAddress+"_warnWhenBleError");
        return editor.commit();
    }

    // 从Pref创建所有的设备基本信息
    public static List<BleDeviceRegisterInfo> createAllFromPref(SharedPreferences pref) {
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if(addressSet == null || addressSet.isEmpty()) {
            return null;
        }

        // 转为数组排序
        String[] addressArr = addressSet.toArray(new String[0]);
        Arrays.sort(addressArr);
        List<BleDeviceRegisterInfo> infoList = new ArrayList<>();
        for(String macAddress : addressArr) {
            BleDeviceRegisterInfo basicInfo = createFromPref(pref, macAddress);
            if(basicInfo != null)
                infoList.add(basicInfo);
        }

        return infoList;
    }

    // 由Pref获取设备基本信息
    private static BleDeviceRegisterInfo createFromPref(SharedPreferences pref, String macAddress) {
        if(TextUtils.isEmpty(macAddress)) return null;

        String address = pref.getString(macAddress+"_macAddress", "");
        if("".equals(address)) return null;

        String nickName = pref.getString(macAddress+"_nickName", DEFAULT_DEVICE_NICKNAME);
        String uuidString = pref.getString(macAddress+"_uuidString", "");
        String imagePath = pref.getString(macAddress+"_imagePath", DEFAULT_DEVICE_IMAGEPATH);
        boolean autoConnect = pref.getBoolean(macAddress+"_autoConnect", DEFAULT_DEVICE_AUTOCONNECT);
        int reconnectTimes = pref.getInt(macAddress+"_reconnectTimes", DEFAULT_DEVICE_RECONNECT_TIMES);
        boolean warnWhenBleError = pref.getBoolean(macAddress+"_warnWhenBleError", DEFAULT_WARN_WHEN_BLE_ERROR);
        return new BleDeviceRegisterInfo(address, nickName, uuidString, imagePath, autoConnect, reconnectTimes, warnWhenBleError);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BleDeviceRegisterInfo that = (BleDeviceRegisterInfo) o;
        return macAddress.equalsIgnoreCase(that.macAddress);
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }
}
