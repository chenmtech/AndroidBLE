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
    private static final String ADDRESSSET = "addressset";
    private static final String MACADDRESS = "_macaddress";
    private static final String UUIDSTR = "_uuidstr";
    private static final String NICKNAME = "_nickname";
    private static final String IMAGEPATH = "_imagepath";
    private static final String AUTOCONNECT = "_autoconnect";
    private static final String WARNBLEINNERERROR = "_warnbleinnererror";

    public static final String DEFAULT_DEVICE_MAC_ADDRESS = ""; //缺省MAC地址
    public static final String DEFAULT_DEVICE_UUID_STR = ""; //缺省UUID串
    public static final String DEFAULT_DEVICE_NICK_NAME = ""; // 缺省设备名
    public static final String DEFAULT_DEVICE_IMAGE_PATH = ""; // 缺省设备图标路径名
    public static final boolean DEFAULT_DEVICE_AUTO_CONNECT = true; // 设备打开时是否自动连接
    public static final boolean DEFAULT_WARN_BLE_INNER_ERROR = true; // 缺省的蓝牙内部错误是否报警

    private final String macAddress; // 设备mac地址
    private final String uuidStr; // 设备广播Uuid16位字符串
    private String nickName = DEFAULT_DEVICE_NICK_NAME; // 设备昵称
    private String imagePath = DEFAULT_DEVICE_IMAGE_PATH; // 设备图标完整路径
    private boolean autoConnect = DEFAULT_DEVICE_AUTO_CONNECT; // 设备打开后是否自动连接
    private boolean warnBleInnerError = DEFAULT_WARN_BLE_INNER_ERROR; // 蓝牙内部错误是否报警

    public BleDeviceRegisterInfo(String macAddress, String uuidStr) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
    }

    private BleDeviceRegisterInfo(String macAddress, String nickName, String uuidStr, String imagePath,
                                 boolean autoConnect, boolean warnBleInnerError) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
        this.nickName = nickName;
        this.imagePath = imagePath;
        this.autoConnect = autoConnect;
        this.warnBleInnerError = warnBleInnerError;
    }

    public String getMacAddress() {
        return macAddress;
    }
    public String getUuidStr() {
        return uuidStr;
    }
    public String getNickName() {
        return nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
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
    public boolean isWarnBleInnerError() {
        return warnBleInnerError;
    }
    public void setWarnBleInnerError(boolean warnBleInnerError) {
        this.warnBleInnerError = warnBleInnerError;
    }
    public void update(BleDeviceRegisterInfo registerInfo) {
        if(macAddress.equalsIgnoreCase(registerInfo.macAddress) && uuidStr.equalsIgnoreCase(registerInfo.uuidStr)) {
            nickName = registerInfo.nickName;
            imagePath = registerInfo.imagePath;
            autoConnect = registerInfo.autoConnect;
            warnBleInnerError = registerInfo.warnBleInnerError;
        }
    }

    // 将注册信息保存到Pref
    public boolean saveToPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet(ADDRESSSET, addressSet);
        if((addressSet != null) && (addressSet.isEmpty() || !addressSet.contains(macAddress))) {
            addressSet.add(macAddress);
            editor.putStringSet(ADDRESSSET, addressSet);
        }
        editor.putString(macAddress + MACADDRESS, macAddress);
        editor.putString(macAddress + UUIDSTR, uuidStr);
        editor.putString(macAddress + NICKNAME, nickName);
        editor.putString(macAddress + IMAGEPATH, imagePath);
        editor.putBoolean(macAddress + AUTOCONNECT, autoConnect);
        editor.putBoolean(macAddress + WARNBLEINNERERROR, warnBleInnerError);
        return editor.commit();
    }

    // 从Pref中删除注册信息
    public boolean deleteFromPref(SharedPreferences pref) {
        if(TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = pref.getStringSet(ADDRESSSET, null);
        if(addressSet != null && addressSet.contains(macAddress)) {
            addressSet.remove(macAddress);
            editor.putStringSet(ADDRESSSET, addressSet);
        }

        String[] strs = new String[]{MACADDRESS, UUIDSTR, NICKNAME, IMAGEPATH, AUTOCONNECT, WARNBLEINNERERROR};
        for(String string : strs) {
            editor.remove(macAddress + string);
        }
        return editor.commit();
    }

    // 从Pref读取所有的设备注册信息
    public static List<BleDeviceRegisterInfo> readAllFromPref(SharedPreferences pref) {
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet(ADDRESSSET, addressSet);
        if(addressSet == null || addressSet.isEmpty()) {
            return null;
        }
        // 转为数组排序
        String[] addresses = addressSet.toArray(new String[0]);
        Arrays.sort(addresses);
        List<BleDeviceRegisterInfo> registerInfoList = new ArrayList<>();
        for(String macAddress : addresses) {
            BleDeviceRegisterInfo registerInfo = readFromPref(pref, macAddress);
            if(registerInfo != null)
                registerInfoList.add(registerInfo);
        }
        return registerInfoList;
    }

    // 由Pref读取设备注册信息
    private static BleDeviceRegisterInfo readFromPref(SharedPreferences pref, String macAddress) {
        if(TextUtils.isEmpty(macAddress)) return null;
        String address = pref.getString(macAddress + MACADDRESS, DEFAULT_DEVICE_MAC_ADDRESS);
        if(TextUtils.isEmpty(address)) return null;
        String uuidString = pref.getString(macAddress + UUIDSTR, DEFAULT_DEVICE_UUID_STR);
        String nickName = pref.getString(macAddress + NICKNAME, DEFAULT_DEVICE_NICK_NAME);
        String imagePath = pref.getString(macAddress + IMAGEPATH, DEFAULT_DEVICE_IMAGE_PATH);
        boolean autoConnect = pref.getBoolean(macAddress + AUTOCONNECT, DEFAULT_DEVICE_AUTO_CONNECT);
        boolean warnWhenBleError = pref.getBoolean(macAddress + WARNBLEINNERERROR, DEFAULT_WARN_BLE_INNER_ERROR);
        return new BleDeviceRegisterInfo(address, nickName, uuidString, imagePath, autoConnect, warnWhenBleError);
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
