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
    public static final String DEFAULT_DEVICE_MAC_ADDRESS = ""; //缺省MAC地址
    public static final String DEFAULT_DEVICE_UUID_STR = ""; //缺省UUID串
    public static final String DEFAULT_DEVICE_NICK_NAME = ""; // 缺省设备名
    public static final String DEFAULT_DEVICE_IMAGE_PATH = ""; // 缺省设备图标路径名
    public static final boolean DEFAULT_DEVICE_AUTO_CONNECT = true; // 设备打开时是否自动连接
    public static final boolean DEFAULT_WARN_WHEN_BLE_ERROR = true; // 缺省的蓝牙错误是否报警

    private final String macAddress; // 设备mac地址
    private final String uuidStr; // 设备广播Uuid16位字符串
    private String nickName = DEFAULT_DEVICE_NICK_NAME; // 设备昵称
    private String imagePath = DEFAULT_DEVICE_IMAGE_PATH; // 设备图标路径名
    private boolean autoConnect = DEFAULT_DEVICE_AUTO_CONNECT; // 设备打开后是否自动连接
    private boolean warnWhenBleError = DEFAULT_WARN_WHEN_BLE_ERROR; // 重连失败后是否报警

    public BleDeviceRegisterInfo(String macAddress, String uuidStr) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
    }

    private BleDeviceRegisterInfo(String macAddress, String nickName, String uuidStr, String imagePath,
                                 boolean autoConnect, boolean warnWhenBleError) {
        this.macAddress = macAddress;
        this.uuidStr = uuidStr;
        this.nickName = nickName;
        this.imagePath = imagePath;
        this.autoConnect = autoConnect;
        this.warnWhenBleError = warnWhenBleError;
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
    public boolean isWarnWhenBleError() {
        return warnWhenBleError;
    }
    public void setWarnWhenBleError(boolean warnWhenBleError) {
        this.warnWhenBleError = warnWhenBleError;
    }
    public void update(BleDeviceRegisterInfo registerInfo) {
        if(macAddress.equalsIgnoreCase(registerInfo.macAddress) && uuidStr.equalsIgnoreCase(registerInfo.uuidStr)) {
            nickName = registerInfo.nickName;
            imagePath = registerInfo.imagePath;
            autoConnect = registerInfo.autoConnect;
            warnWhenBleError = registerInfo.warnWhenBleError;
        }
    }

    // 将注册信息保存到Pref
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
        editor.putString(macAddress+"_uuidStr", uuidStr);
        editor.putString(macAddress+"_nickName", nickName);
        editor.putString(macAddress+"_imagePath", imagePath);
        editor.putBoolean(macAddress+"_autoConnect", autoConnect);
        editor.putBoolean(macAddress+"_warnWhenBleError", warnWhenBleError);
        return editor.commit();
    }

    // 从Pref中删除注册信息
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
        editor.remove(macAddress+"_uuidStr");
        editor.remove(macAddress+"_nickName");
        editor.remove(macAddress+"_imagePath");
        editor.remove(macAddress+"_autoConnect");
        editor.remove(macAddress+"_warnWhenBleError");
        return editor.commit();
    }

    // 从Pref创建所有的设备注册信息对象
    public static List<BleDeviceRegisterInfo> createAllFromPref(SharedPreferences pref) {
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet("addressSet", addressSet);
        if(addressSet == null || addressSet.isEmpty()) {
            return null;
        }
        // 转为数组排序
        String[] addressArr = addressSet.toArray(new String[0]);
        Arrays.sort(addressArr);

        List<BleDeviceRegisterInfo> registerInfoList = new ArrayList<>();
        for(String macAddress : addressArr) {
            BleDeviceRegisterInfo registerInfo = createFromPref(pref, macAddress);
            if(registerInfo != null)
                registerInfoList.add(registerInfo);
        }

        return registerInfoList;
    }

    // 由Pref获取设备基本信息
    private static BleDeviceRegisterInfo createFromPref(SharedPreferences pref, String macAddress) {
        if(TextUtils.isEmpty(macAddress)) return null;

        String address = pref.getString(macAddress+"_macAddress", DEFAULT_DEVICE_MAC_ADDRESS);
        if(TextUtils.isEmpty(address)) return null;
        String uuidString = pref.getString(macAddress+"_uuidStr", DEFAULT_DEVICE_UUID_STR);
        String nickName = pref.getString(macAddress+"_nickName", DEFAULT_DEVICE_NICK_NAME);
        String imagePath = pref.getString(macAddress+"_imagePath", DEFAULT_DEVICE_IMAGE_PATH);
        boolean autoConnect = pref.getBoolean(macAddress+"_autoConnect", DEFAULT_DEVICE_AUTO_CONNECT);
        boolean warnWhenBleError = pref.getBoolean(macAddress+"_warnWhenBleError", DEFAULT_WARN_WHEN_BLE_ERROR);
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
