package com.cmtech.android.ble.extend;


import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  BleDeviceBasicInfo: 设备基本信息，字段信息将保存在数据库或Preference中
 *  Created by bme on 2018/6/27.
 */

public class BleDeviceBasicInfo implements Serializable{
    private final static long serialVersionUID = 1L;

    // 下面五个参数用于登记设备基本信息
    public static final String DEFAULT_DEVICE_NICKNAME = ""; // 缺省设备名

    public static final String DEFAULT_DEVICE_IMAGEPATH = ""; // 缺省设备图标路径名

    public static final boolean DEFAULT_DEVICE_AUTOCONNECT = true; // 设备打开时是否自动连接

    public static final int DEFAULT_DEVICE_RECONNECT_TIMES = 3; // 连接失败后的重连次数

    public static final boolean DEFAULT_WARN_AFTER_RECONNECT_FAILURE = true; // 重连失败后是否报警

    private String macAddress = ""; // 设备mac地址

    private String nickName = DEFAULT_DEVICE_NICKNAME; // 设备昵称

    private String uuidString = ""; // 设备广播Uuid16位字符串

    private String imagePath = DEFAULT_DEVICE_IMAGEPATH; // 设备图标路径名

    private boolean autoConnect = DEFAULT_DEVICE_AUTOCONNECT; // 设备打开后是否自动连接

    private int reconnectTimes = DEFAULT_DEVICE_RECONNECT_TIMES; // 连接断开后重连次数

    private boolean warnAfterReconnectFailure = DEFAULT_WARN_AFTER_RECONNECT_FAILURE; // 重连失败后是否报警

    public BleDeviceBasicInfo() {

    }

    public BleDeviceBasicInfo(String macAddress, String nickName, String uuidString, String imagePath,
                              boolean autoConnect, int reconnectTimes, boolean warnAfterReconnectFailure) {
        this.macAddress = macAddress;
        this.nickName = nickName;
        this.uuidString = uuidString;
        this.imagePath = imagePath;
        this.autoConnect = autoConnect;
        this.reconnectTimes = reconnectTimes;
        this.warnAfterReconnectFailure = warnAfterReconnectFailure;
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
    public boolean isWarnAfterReconnectFailure() {
        return warnAfterReconnectFailure;
    }
    public void setWarnAfterReconnectFailure(boolean warnAfterReconnectFailure) {
        this.warnAfterReconnectFailure = warnAfterReconnectFailure;
    }

    // 将设备基本信息保存到Pref
    public boolean saveToPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if(addressSet.isEmpty() || !addressSet.contains(macAddress)) {
            addressSet.add(macAddress);
            editor.putStringSet("addressSet", addressSet);
        }
        editor.putString(macAddress+"_macAddress", macAddress);
        editor.putString(macAddress+"_nickName", nickName);
        editor.putString(macAddress+"_uuidString", uuidString);
        editor.putString(macAddress+"_imagePath", imagePath);
        editor.putBoolean(macAddress+"_autoConnect", autoConnect);
        editor.putInt(macAddress+"_reconnectTimes", reconnectTimes);
        editor.putBoolean(macAddress+"_warnAfterReconnectFailure", warnAfterReconnectFailure);
        return editor.commit();
    }

    // 从Pref中删除设备基本信息
    public boolean deleteFromPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if(!addressSet.isEmpty() && addressSet.contains(macAddress)) {
            addressSet.remove(macAddress);
            editor.putStringSet("addressSet", addressSet);
        }
        editor.remove(macAddress+"_macAddress");
        editor.remove(macAddress+"_nickName");
        editor.remove(macAddress+"_uuidString");
        editor.remove(macAddress+"_imagePath");
        editor.remove(macAddress+"_autoConnect");
        editor.remove(macAddress+"_reconnectTimes");
        editor.remove(macAddress+"_warnAfterReconnectFailure");
        return editor.commit();
    }

    // 从Pref创建所有的设备基本信息
    public static List<BleDeviceBasicInfo> createAllFromPref(SharedPreferences pref) {
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if(addressSet.isEmpty()) {
            return null;
        }
        // 转为数组排序
        String[] addressArr = addressSet.toArray(new String[0]);
        Arrays.sort(addressArr);
        List<BleDeviceBasicInfo> infoList = new ArrayList<>();
        for(String macAddress : addressArr) {
            BleDeviceBasicInfo basicInfo = createFromPref(pref, macAddress);
            if(basicInfo != null)
                infoList.add(basicInfo);
        }
        return infoList;
    }

    // 由Pref创建设备基本信息
    private static BleDeviceBasicInfo createFromPref(SharedPreferences pref, String macAddress) {
        if(TextUtils.isEmpty(macAddress)) return null;

        String address = pref.getString(macAddress+"_macAddress", "");
        if("".equals(address)) return null;
        String nickName = pref.getString(macAddress+"_nickName", DEFAULT_DEVICE_NICKNAME);
        String uuidString = pref.getString(macAddress+"_uuidString", "");
        String imagePath = pref.getString(macAddress+"_imagePath", DEFAULT_DEVICE_IMAGEPATH);
        boolean autoConnect = pref.getBoolean(macAddress+"_autoConnect", DEFAULT_DEVICE_AUTOCONNECT);
        int reconnectTimes = pref.getInt(macAddress+"_reconnectTimes", DEFAULT_DEVICE_RECONNECT_TIMES);
        boolean warnAfterRecconnectFailure = pref.getBoolean(macAddress+"_warnAfterReconnectFailure", DEFAULT_WARN_AFTER_RECONNECT_FAILURE);
        return new BleDeviceBasicInfo(address, nickName, uuidString, imagePath, autoConnect, reconnectTimes, warnAfterRecconnectFailure);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleDeviceBasicInfo that = (BleDeviceBasicInfo) o;
        return macAddress.equalsIgnoreCase(that.macAddress);
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }
}
