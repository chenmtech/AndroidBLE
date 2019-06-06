package com.cmtech.android.ble.extend;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * BleDeviceManager: 所有设备的管理器
 * Created by bme on 2018/12/08.
 */

public class BleDeviceManager {
    private Context context;

    private List<BleDevice> deviceList = new ArrayList<>();

    public BleDeviceManager(Context context) {
        this.context = context;
    }

    // 创建并添加一个设备
    public BleDevice createAndAddDevice(BleDeviceBasicInfo basicInfo) {
        BleDevice device = findDevice(basicInfo);
        if(device != null) return null;

        // 创建设备
        device = createDevice(basicInfo);
        if(device == null) return null;

        // 将设备添加到设备列表
        deviceList.add(device);
        // 按地址排序
        Collections.sort(deviceList, new Comparator<BleDevice>() {
            @Override
            public int compare(BleDevice o1, BleDevice o2) {
                return o1.getMacAddress().compareTo(o2.getMacAddress());
            }
        });
        return device;
    }

    // 删除一个设备
    public void deleteDevice(BleDevice device) {
        deviceList.remove(device);
    }

    // 获取设备清单
    public List<BleDevice> getDeviceList() {
        return deviceList;
    }

    // 获取所有设备的Mac列表
    public List<String> getDeviceMacList() {
        List<String> deviceMacList = new ArrayList<>();
        for(BleDevice device : deviceList) {
            deviceMacList.add(device.getMacAddress());
        }
        return deviceMacList;
    }

    // 根据设备基本信息创建设备
    private BleDevice createDevice(BleDeviceBasicInfo basicInfo) {
        // 获取相应的抽象工厂
        AbstractBleDeviceFactory factory = AbstractBleDeviceFactory.getBLEDeviceFactory(basicInfo);
        return (factory == null) ? null : factory.createDevice(context);
    }

    // 用基本信息寻找设备
    public BleDevice findDevice(BleDeviceBasicInfo basicInfo) {
        if(basicInfo == null) return null;
        return findDevice(basicInfo.getMacAddress());
    }

    // 用mac地址寻找设备
    public BleDevice findDevice(String macAddress) {
        if(TextUtils.isEmpty(macAddress)) return null;

        BleDevice found = null;
        for(BleDevice device : deviceList) {
            if(device.getMacAddress().equalsIgnoreCase(macAddress)) {
                found = device;
                break;
            }
        }
        return found;
    }

    // 有设备打开了
    public boolean hasDeviceOpened() {
        boolean open = false;
        for(BleDevice device : deviceList) {
            if(!device.isClosed()) {
                open = true;
                break;
            }
        }
        return open;
    }
}
