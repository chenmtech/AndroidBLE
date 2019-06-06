package com.cmtech.android.ble.extend;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

/**
 * BleDeviceFragment：设备的Fragment
 * Created by bme on 2018/2/27.
 */

public abstract class BleDeviceFragment extends Fragment{
    private static final String TAG = "BleDeviceFragment";

    private IBleDeviceFragmentActivity activity; //包含BleDeviceFragment的Activity，必须要实现IBleDeviceFragmentActivity接口

    private BleDevice device; // 设备



    public BleDeviceFragment() {
    }

    public static BleDeviceFragment create(String macAddress, Class<? extends BleDeviceFragment> fragClass) {
        BleDeviceFragment fragment = null;
        try {
            fragment = fragClass.newInstance();
            bundleMacAddress(fragment, macAddress);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fragment;
    }

    // 将设备的mac地址添加到Fragment的Argument中
    public static void bundleMacAddress(BleDeviceFragment fragment, String macAddress) {
        Bundle bundle = new Bundle();
        bundle.putString("device_mac", macAddress);
        fragment.setArguments(bundle);
    }

    public BleDevice getDevice() {
        return device;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof IBleDeviceFragmentActivity) {
            activity = (IBleDeviceFragmentActivity) context;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 用macAddress获取BleDevice
        Bundle bundle = getArguments();

        if(bundle == null) throw new IllegalStateException();

        String deviceMac = bundle.getString("device_mac");

        device = activity.findDevice(deviceMac);

        if(device == null) throw new IllegalArgumentException();

        // 更新连接状态
        updateState();

        // 注册设备状态观察者
        device.registerConnectStateListener(activity);

        device.updateConnectState();

        // 打开设备
        device.open();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 移除activity设备状态观察者
        //device.removeConnectStateListener(activity);
    }

    // 切换设备状态，根据设备的当前状态实现状态切换
    public void switchDeviceState() {
        if(!device.switchState()) {
            Toast.makeText(getActivity(), "正在等待连接响应，请稍等。", Toast.LENGTH_SHORT).show();
        }
    }

    // 更新状态
    public void updateState() {
        // isAdded()用来判断Fragment是否与Activity关联，如果关联了，才能更新状态信息
        if(device != null && isAdded()) {

        }
    }

    public abstract void openConfigActivity();

    public void close() {
        if(activity != null) {
            activity.closeFragment(this);
        }
    }
    //////////////////////////////////////////////////////////////////////////

}
