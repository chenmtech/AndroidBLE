package com.cmtech.android.ble.core;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BleDeviceRegisterInfo extends DeviceRegisterInfo {
    private static final String DEFAULT_DEVICE_MAC_ADDRESS = ""; //缺省MAC地址
    private static final String DEFAULT_DEVICE_UUID_STR = ""; //缺省UUID串
    private static final String ADDRESSSET = "addressset";
    private static final String MACADDRESS = "_macaddress";
    private static final String UUIDSTR = "_uuidstr";
    private static final String NAME = "_name";
    private static final String IMAGEPATH = "_imagepath";
    private static final String AUTOCONNECT = "_autoconnect";
    private static final String WARNBLEINNERERROR = "_warnbleinnererror";

    public BleDeviceRegisterInfo(String macAddress, String uuidStr) {
        super(macAddress, uuidStr);
    }

    private BleDeviceRegisterInfo(String macAddress, String uuidStr, String name, String imagePath,
                               boolean autoConnect, boolean warnWhenBleInnerError) {
        super(macAddress, uuidStr, name, imagePath, autoConnect, warnWhenBleInnerError);
    }

    // 从Pref读取所有的设备注册信息
    public static List<DeviceRegisterInfo> readAllFromPref(SharedPreferences pref) {
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet(ADDRESSSET, addressSet);
        if (addressSet == null || addressSet.isEmpty()) {
            return null;
        }
        // 转为数组排序
        String[] addresses = addressSet.toArray(new String[0]);
        Arrays.sort(addresses);
        List<DeviceRegisterInfo> registerInfoList = new ArrayList<>();
        for (String macAddress : addresses) {
            DeviceRegisterInfo registerInfo = readFromPref(pref, macAddress);
            if (registerInfo != null)
                registerInfoList.add(registerInfo);
        }
        return registerInfoList;
    }

    // 由Pref读取设备注册信息
    private static DeviceRegisterInfo readFromPref(SharedPreferences pref, String macAddress) {
        if (TextUtils.isEmpty(macAddress)) return null;
        String address = pref.getString(macAddress + MACADDRESS, DEFAULT_DEVICE_MAC_ADDRESS);
        if (TextUtils.isEmpty(address)) return null;
        String uuidString = pref.getString(macAddress + UUIDSTR, DEFAULT_DEVICE_UUID_STR);
        String nickName = pref.getString(macAddress + NAME, DEFAULT_DEVICE_NAME);
        String imagePath = pref.getString(macAddress + IMAGEPATH, DEFAULT_DEVICE_IMAGE_PATH);
        boolean autoConnect = pref.getBoolean(macAddress + AUTOCONNECT, DEFAULT_DEVICE_AUTO_CONNECT);
        boolean warnWhenBleError = pref.getBoolean(macAddress + WARNBLEINNERERROR, DEFAULT_WARN_WHEN_BLE_INNER_ERROR);
        return new BleDeviceRegisterInfo(address, uuidString, nickName, imagePath, autoConnect, warnWhenBleError);
    }

    // 将注册信息保存到Pref
    public boolean saveToPref(SharedPreferences pref) {
        if (TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = new HashSet<>();
        addressSet = pref.getStringSet(ADDRESSSET, addressSet);
        if ((addressSet != null) && (addressSet.isEmpty() || !addressSet.contains(macAddress))) {
            addressSet.add(macAddress);
            editor.putStringSet(ADDRESSSET, addressSet);
        }
        editor.putString(macAddress + MACADDRESS, macAddress);
        editor.putString(macAddress + UUIDSTR, uuidStr);
        editor.putString(macAddress + NAME, name);
        editor.putString(macAddress + IMAGEPATH, imagePath);
        editor.putBoolean(macAddress + AUTOCONNECT, autoConnect);
        editor.putBoolean(macAddress + WARNBLEINNERERROR, warnWhenBleInnerError);
        return editor.commit();
    }

    // 从Pref中删除注册信息
    public boolean deleteFromPref(SharedPreferences pref) {
        if (TextUtils.isEmpty(macAddress)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addressSet = pref.getStringSet(ADDRESSSET, null);
        if (addressSet != null && addressSet.contains(macAddress)) {
            addressSet.remove(macAddress);
            editor.putStringSet(ADDRESSSET, addressSet);
        }

        String[] strs = new String[]{MACADDRESS, UUIDSTR, NAME, IMAGEPATH, AUTOCONNECT, WARNBLEINNERERROR};
        for (String string : strs) {
            editor.remove(macAddress + string);
        }
        return editor.commit();
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
