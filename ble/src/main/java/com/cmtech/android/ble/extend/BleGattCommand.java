package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.utils.HexUtil;


/**
  *
  * ClassName:      BleGattCommand
  * Description:    表示一条Gatt操作命令
  * Author:         chenm
  * CreateDate:     2018-03-01 06:42
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-20 06:42
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleGattCommand{
    protected final DeviceMirror deviceMirror;          // 执行命令的设备镜像

    private final BluetoothGattChannel channel;       // 执行命令的通道

    private final IBleCallback dataOpCallback;        // 数据操作回调

    private final byte[] writtenData;                 // 如果是写操作，存放要写的数据；如果是notify或indicate操作，存放enable数据

    private final IBleCallback notifyOpCallback;      // 如果是notify或indicate操作，存放notify或indicate的回调

    private final String elementDescription; // 命令操作的element的描述符

    private final boolean isInstantCommand; // 是否是立刻执行的命令，即不需要通过蓝牙通信的命令

    private BleGattCommand(DeviceMirror deviceMirror, BluetoothGattChannel channel,
                           IBleCallback dataOpCallback,
                           byte[] writtenData, IBleCallback notifyOpCallback, String elementDescription, boolean isInstantCommand) {
        this.deviceMirror = deviceMirror;

        this.channel = channel;

        this.dataOpCallback = dataOpCallback;

        this.writtenData = writtenData;

        this.notifyOpCallback = notifyOpCallback;

        this.elementDescription = elementDescription;

        this.isInstantCommand = isInstantCommand;
    }

    BleGattCommand(BleGattCommand gattCommand) {
        this.deviceMirror = gattCommand.deviceMirror;

        this.channel = gattCommand.channel;

        this.dataOpCallback = gattCommand.dataOpCallback;

        this.writtenData = gattCommand.writtenData;

        this.notifyOpCallback = gattCommand.notifyOpCallback;

        this.elementDescription = gattCommand.elementDescription;

        this.isInstantCommand = gattCommand.isInstantCommand;
    }

    // 获取命令用的channel
    public BluetoothGattChannel getChannel() {
        return channel;
    }

    // 获取命令属性
    public PropertyType getPropertyType() {
        return channel.getPropertyType();
    }

    // 是否是instant命令
    boolean isInstantCommand() {
        return isInstantCommand;
    }

    // 执行命令
    boolean execute() throws InterruptedException{
        if(isInstantCommand) {
            if(dataOpCallback == null) {
                throw new NullPointerException();
            }

            dataOpCallback.onSuccess(null, null, null);

            return true;
        }

        if(deviceMirror == null || channel == null) return false;

        switch (channel.getPropertyType()) {
            case PROPERTY_READ:
                deviceMirror.bindChannel( dataOpCallback, channel);

                deviceMirror.readData();
                break;

            case PROPERTY_WRITE:
                deviceMirror.bindChannel( dataOpCallback, channel);

                deviceMirror.writeData(writtenData);
                break;

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                boolean isIndication = true;

                if(channel.getPropertyType() == PropertyType.PROPERTY_NOTIFY) {
                    isIndication = false;
                }

                deviceMirror.bindChannel( dataOpCallback, channel);

                if(writtenData[0] == 1) {
                    deviceMirror.registerNotify(isIndication);

                    deviceMirror.setNotifyListener(channel.getGattInfoKey(), notifyOpCallback);
                } else {
                    deviceMirror.unregisterNotify(isIndication);

                    deviceMirror.removeReceiveCallback(channel.getGattInfoKey());
                }
                break;

            default:
                break;
        }

        return false;
    }

    @Override
    public String toString() {
        if(isInstantCommand) return elementDescription;

        switch (channel.getPropertyType()) {
            case PROPERTY_READ:
                return channel.getPropertyType()+" "+elementDescription;

            case PROPERTY_WRITE:
                return channel.getPropertyType() + " " + elementDescription + " " + HexUtil.encodeHexStr(writtenData);

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                return channel.getPropertyType() + " " + elementDescription + " " + ((writtenData[0] != 0));

                default:
                    return "";
        }
    }

    // 获取Gatt信息key
    String getGattInfoKey() {
        if(isInstantCommand) return "";

        return channel.getGattInfoKey();
    }

    static class Builder {
        private BleGattElement element;
        private PropertyType propertyType;
        private DeviceMirror deviceMirror;
        private byte[] data;
        private IBleCallback dataCallback;
        private IBleCallback notifyOpCallback;
        private boolean isInstantCommand = false;

        Builder() {
        }

        Builder setDeviceMirror(DeviceMirror deviceMirror) {
            this.deviceMirror = deviceMirror;
            return this;
        }

        Builder setBluetoothElement(BleGattElement element) {
            this.element = element;
            return this;
        }

        Builder setPropertyType(PropertyType propertyType) {
            this.propertyType = propertyType;
            return this;
        }

        Builder setDataCallback(IBleCallback dataCallback) {
            this.dataCallback = dataCallback;
            return this;
        }

        Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        Builder setNotifyOpCallback(IBleCallback notifyOpCallback) {
            this.notifyOpCallback = notifyOpCallback;
            return this;
        }

        Builder setInstantCommand(boolean isInstantCommand) {
            this.isInstantCommand = isInstantCommand;
            return this;
        }

        BleGattCommand build() {
            if(isInstantCommand) {
                if(dataCallback != null) {
                    return new BleGattCommand(null, null, dataCallback,
                            null, null, "an instant cmd.", true);
                } else {
                    return null;
                }
            } else {

                if (deviceMirror == null || element == null || dataCallback == null) return null;

                if (propertyType == PropertyType.PROPERTY_WRITE
                        || propertyType == PropertyType.PROPERTY_NOTIFY
                        || propertyType == PropertyType.PROPERTY_INDICATE) {
                    if (data == null || data.length == 0) return null;
                }

                if (propertyType == PropertyType.PROPERTY_NOTIFY
                        || propertyType == PropertyType.PROPERTY_INDICATE) {
                    if (data[0] == 1 && notifyOpCallback == null) return null;
                }

                BluetoothGattChannel.Builder builder = new BluetoothGattChannel.Builder();

                BluetoothGattChannel channel = builder.setBluetoothGatt(deviceMirror.getBluetoothGatt())
                        .setPropertyType(propertyType)
                        .setServiceUUID(element.getServiceUuid())
                        .setCharacteristicUUID(element.getCharacteristicUuid())
                        .setDescriptorUUID(element.getDescriptorUuid()).builder();

                return new BleGattCommand(deviceMirror, channel, dataCallback, data, notifyOpCallback, element.toString(), false);
            }
        }
    }
}
