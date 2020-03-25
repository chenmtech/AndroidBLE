package com.cmtech.android.ble.core;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BleDeviceRegisterInfo extends DeviceRegisterInfo {
    private static final String ADDR_SET = "addrset";
    private static final String ADDRESS = "_address";
    private static final String UUID = "_uuid";
    private static final String NAME = "_name";
    private static final String ICON = "_icon";
    private static final String AUTOCONNECT = "_autoconnect";

    public BleDeviceRegisterInfo(String address, String uuidStr) {
        super(address, uuidStr);
    }

    private BleDeviceRegisterInfo(String address, String uuid, String name, String icon,
                               boolean autoConnect) {
        super(address, uuid, name, icon, autoConnect);
    }

    // 从Pref读取所有的设备注册信息
    public static List<DeviceRegisterInfo> readAllFromPref(SharedPreferences pref) {
        Set<String> addrSet = new HashSet<>();
        addrSet = pref.getStringSet(ADDR_SET, addrSet);
        if (addrSet == null || addrSet.isEmpty()) {
            return null;
        }
        // 转为数组排序
        String[] addrArr = addrSet.toArray(new String[0]);
        Arrays.sort(addrArr);
        List<DeviceRegisterInfo> infos = new ArrayList<>();
        for (String address : addrArr) {
            DeviceRegisterInfo info = readFromPref(pref, address);
            if (info != null)
                infos.add(info);
        }
        return infos;
    }

    // 由Pref读取设备注册信息
    private static DeviceRegisterInfo readFromPref(SharedPreferences pref, String address) {
        if (TextUtils.isEmpty(address)) return null;
        String addr = pref.getString(address + ADDRESS, "");
        if (TextUtils.isEmpty(addr)) return null;
        String uuid = pref.getString(address + UUID, "");
        String name = pref.getString(address + NAME, DEFAULT_DEVICE_NAME);
        String icon = pref.getString(address + ICON, DEFAULT_DEVICE_ICON);
        boolean autoConnect = pref.getBoolean(address + AUTOCONNECT, DEFAULT_DEVICE_AUTO_CONNECT);
        return new BleDeviceRegisterInfo(addr, uuid, name, icon, autoConnect);
    }

    // 将注册信息保存到Pref
    public boolean saveToPref(SharedPreferences pref) {
        if (TextUtils.isEmpty(address)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addrs = new HashSet<>();
        addrs = pref.getStringSet(ADDR_SET, addrs);
        if ((addrs != null) && (addrs.isEmpty() || !addrs.contains(address))) {
            addrs.add(address);
            editor.putStringSet(ADDR_SET, addrs);
        }
        editor.putString(address + ADDRESS, address);
        editor.putString(address + UUID, uuid);
        editor.putString(address + NAME, name);
        editor.putString(address + ICON, icon);
        editor.putBoolean(address + AUTOCONNECT, autoConnect);
        return editor.commit();
    }

    // 从Pref中删除注册信息
    public boolean deleteFromPref(SharedPreferences pref) {
        if (TextUtils.isEmpty(address)) return false;

        SharedPreferences.Editor editor = pref.edit();
        Set<String> addrs = pref.getStringSet(ADDR_SET, null);
        if (addrs != null && addrs.contains(address)) {
            addrs.remove(address);
            editor.putStringSet(ADDR_SET, addrs);
        }

        String[] strArr = new String[]{ADDRESS, UUID, NAME, ICON, AUTOCONNECT};
        for (String string : strArr) {
            editor.remove(address + string);
        }
        return editor.commit();
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
