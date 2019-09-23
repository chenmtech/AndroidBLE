package com.cmtech.android.ble.extend;

import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.utils.HexUtil;


/**
  *
  * ClassName:      BleGattCommand
  * Description:    表示一条Gatt命令
  * Author:         chenm
  * CreateDate:     2018-03-01 06:42
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-20 06:42
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleGattCommand{
    final BleDevice device; // 执行命令的设备

    private final BleGattChannel channel; // 执行命令的通道

    IBleDataCallback dataOpCallback; // 数据操作回调

    private final byte[] writtenData; // 如果是写操作，存放要写的数据；如果是notify或indicate操作，存放enable数据

    private final IBleDataCallback notifyOpCallback; // 如果是notify或indicate操作，存放notify或indicate的回调

    private final String elementString; // 命令操作的element的描述符

    private final boolean isInstantCommand; // 是否是即刻命令，即不需要等待蓝牙响应的命令

    private BleGattCommand(BleDevice device, BleGattChannel channel,
                           IBleDataCallback dataOpCallback,
                           byte[] writtenData, IBleDataCallback notifyOpCallback, String elementString, boolean isInstantCommand) {
        this.device = device;

        this.channel = channel;

        this.dataOpCallback = dataOpCallback;

        this.writtenData = writtenData;

        this.notifyOpCallback = notifyOpCallback;

        this.elementString = elementString;

        this.isInstantCommand = isInstantCommand;
    }

    BleGattCommand(BleGattCommand gattCommand) {
        if(gattCommand == null)
            throw new NullPointerException();

        this.device = gattCommand.device;

        this.channel = gattCommand.channel;

        this.dataOpCallback = gattCommand.dataOpCallback;

        this.writtenData = gattCommand.writtenData;

        this.notifyOpCallback = gattCommand.notifyOpCallback;

        this.elementString = gattCommand.elementString;

        this.isInstantCommand = gattCommand.isInstantCommand;
    }

    /**
     * 执行命令。执行完一条命令不仅需要发送命令，还需要收到设备响应或者立即执行返回
     * @return 是否已经执行完命令，true-执行完 false-等待对方响应
     */
    boolean execute() throws InterruptedException{
        if(isInstantCommand) {
            if(dataOpCallback == null) {
                throw new NullPointerException("The dataOpCallback of instant commands is null. ");
            }

            dataOpCallback.onSuccess(null, null);

            return true;
        }

        if(device == null || device.getBleDeviceGatt() == null || channel == null) {
            throw new NullPointerException("The gatt or channel of the non-instant commands is null.");
        }

        BleDeviceGatt bleDeviceGatt = device.getBleDeviceGatt();

        switch (channel.getPropertyType()) {
            case PROPERTY_READ:
                bleDeviceGatt.bindChannel( dataOpCallback, channel);

                bleDeviceGatt.readData();
                break;

            case PROPERTY_WRITE:
                bleDeviceGatt.bindChannel( dataOpCallback, channel);

                bleDeviceGatt.writeData(writtenData);
                break;

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                boolean isIndication = (channel.getPropertyType() == PropertyType.PROPERTY_INDICATE);

                bleDeviceGatt.bindChannel( dataOpCallback, channel);

                if(writtenData[0] == 1) {
                    bleDeviceGatt.registerNotify(isIndication);

                    bleDeviceGatt.setNotifyListener(channel.getGattInfoKey(), notifyOpCallback);
                } else {
                    bleDeviceGatt.unregisterNotify(isIndication);

                    bleDeviceGatt.removeReceiveCallback(channel.getGattInfoKey());
                }
                break;

            default:
                break;
        }

        return false;
    }

    @Override
    public String toString() {
        if(isInstantCommand) return elementString;

        switch (channel.getPropertyType()) {
            case PROPERTY_READ:
                return channel.getPropertyType()+" "+ elementString;

            case PROPERTY_WRITE:
                return channel.getPropertyType() + " " + elementString + " " + HexUtil.encodeHexStr(writtenData);

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                return channel.getPropertyType() + " " + elementString + " " + ((writtenData[0] != 0));

            default:
                return "";
        }
    }

    void removeBleCallback() {
        if(isInstantCommand) return;

        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(device != null && device.getBleDeviceGatt() != null) {
            device.getBleDeviceGatt().removeBleCallback(channel.getGattInfoKey());
        }
    }

    static class Builder {
        private BleGattElement element;

        private PropertyType propertyType;

        private BleDevice device;

        private byte[] data;

        private IBleDataCallback dataCallback;

        private IBleDataCallback notifyOpCallback;

        private boolean isInstantCommand = false;

        Builder() {
        }

        Builder setDevice(BleDevice device) {
            this.device = device;

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

        Builder setDataCallback(IBleDataCallback dataCallback) {
            this.dataCallback = dataCallback;

            return this;
        }

        Builder setData(byte[] data) {
            this.data = data;

            return this;
        }

        Builder setNotifyOpCallback(IBleDataCallback notifyOpCallback) {
            this.notifyOpCallback = notifyOpCallback;

            return this;
        }

        Builder setInstantCommand(boolean isInstantCommand) {
            this.isInstantCommand = isInstantCommand;

            return this;
        }

        BleGattCommand build() {
            if(isInstantCommand) {
                if(dataCallback == null) {
                    throw new NullPointerException("The dataOpCallback of instant commands is null. ");
                }

                return new BleGattCommand(null, null, dataCallback,
                        null, null, "This is an instant command.", true);

            } else {

                if(device == null || device.getBleDeviceGatt() == null || element == null) {
                    throw new NullPointerException("The device mirror or element of the non-instant commands is null.");
                }

                if (propertyType == PropertyType.PROPERTY_WRITE
                        || propertyType == PropertyType.PROPERTY_NOTIFY
                        || propertyType == PropertyType.PROPERTY_INDICATE) {
                    if (data == null || data.length == 0) {
                        throw new NullPointerException("The data of the write, notify or indicate commands is null");
                    };
                }

                if (propertyType == PropertyType.PROPERTY_NOTIFY
                        || propertyType == PropertyType.PROPERTY_INDICATE) {
                    if (data[0] == 1 && notifyOpCallback == null) {
                        throw new NullPointerException("The callback of the 'enable' notify or indicate commands is null");
                    }
                }

                BleGattChannel.Builder builder = new BleGattChannel.Builder();

                BleGattChannel channel = builder.setBluetoothGatt(device.getBleDeviceGatt().getBluetoothGatt())
                        .setPropertyType(propertyType)
                        .setServiceUUID(element.getServiceUuid())
                        .setCharacteristicUUID(element.getCharacteristicUuid())
                        .setDescriptorUUID(element.getDescriptorUuid()).builder();

                return new BleGattCommand(device, channel, dataCallback, data, notifyOpCallback, element.toString(), false);
            }
        }
    }
}
