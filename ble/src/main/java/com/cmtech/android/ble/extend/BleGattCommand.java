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

    private final BleGattElementOnline channel; // 执行命令的通道

    private final PropertyType propertyType;

    IBleDataCallback dataOpCallback; // 数据操作回调

    private final byte[] writtenData; // 如果是写操作，存放要写的数据；如果是notify或indicate操作，存放enable数据

    private final IBleDataCallback notifyOpCallback; // 如果是notify或indicate操作，存放notify或indicate的回调

    private final String elementString; // 命令操作的element的描述符

    private BleGattCommand(BleDevice device, BleGattElementOnline channel, PropertyType propertyType,
                           IBleDataCallback dataOpCallback,
                           byte[] writtenData, IBleDataCallback notifyOpCallback, String elementString) {
        this.device = device;

        this.channel = channel;

        this.propertyType = propertyType;

        this.dataOpCallback = dataOpCallback;

        this.writtenData = writtenData;

        this.notifyOpCallback = notifyOpCallback;

        this.elementString = elementString;
    }

    BleGattCommand(BleGattCommand gattCommand) {
        if(gattCommand == null)
            throw new NullPointerException();

        this.device = gattCommand.device;

        this.channel = gattCommand.channel;

        this.propertyType = gattCommand.propertyType;

        this.dataOpCallback = gattCommand.dataOpCallback;

        this.writtenData = gattCommand.writtenData;

        this.notifyOpCallback = gattCommand.notifyOpCallback;

        this.elementString = gattCommand.elementString;
    }

    /**
     * 执行命令。执行完一条命令不仅需要发送命令，还需要收到设备响应或者立即执行返回
     * @return 是否已经执行完命令，true-执行完 false-等待对方响应
     */
    boolean execute() throws InterruptedException{
        if(propertyType == PropertyType.PROPERTY_INSTANTRUN) {
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

        switch (propertyType) {
            case PROPERTY_READ:
                bleDeviceGatt.readData(channel, dataOpCallback);
                break;

            case PROPERTY_WRITE:
                bleDeviceGatt.writeData(channel, dataOpCallback, writtenData);
                break;

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                boolean isIndication = (propertyType == PropertyType.PROPERTY_INDICATE);

                if(writtenData[0] == 1) { // enable
                    bleDeviceGatt.enable(channel, dataOpCallback, notifyOpCallback, true, isIndication);
                } else { // disable
                    bleDeviceGatt.enable(channel, dataOpCallback, notifyOpCallback, false, isIndication);
                }
                break;

            default:
                break;
        }

        return false;
    }

    @Override
    public String toString() {
        switch (propertyType) {
            case PROPERTY_READ:
                return propertyType+" "+ elementString;

            case PROPERTY_WRITE:
                return propertyType + " " + elementString + " " + HexUtil.encodeHexStr(writtenData);

            case PROPERTY_NOTIFY:
            case PROPERTY_INDICATE:
                return propertyType + " " + elementString + " " + ((writtenData[0] != 0));

            case PROPERTY_INSTANTRUN:
                return propertyType + " " + elementString;

            default:
                return "";
        }
    }

    static class Builder {
        private BleGattElement element;

        private PropertyType propertyType;

        private BleDevice device;

        private byte[] data;

        private IBleDataCallback dataCallback;

        private IBleDataCallback notifyOpCallback;

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

        BleGattCommand build() {
            if(propertyType == PropertyType.PROPERTY_INSTANTRUN) {
                if(dataCallback == null) {
                    throw new NullPointerException("The dataOpCallback of instant commands is null. ");
                }

                return new BleGattCommand(null, null, propertyType, dataCallback,
                        null, null, "This is an instant command.");

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

                BleGattElementOnline.Builder builder = new BleGattElementOnline.Builder();

                BleGattElementOnline channel = builder.setBluetoothGatt(device.getBleDeviceGatt().getBluetoothGatt())
                        .setServiceUUID(element.getServiceUuid())
                        .setCharacteristicUUID(element.getCharacteristicUuid())
                        .setDescriptorUUID(element.getDescriptorUuid()).builder();

                return new BleGattCommand(device, channel, propertyType, dataCallback, data, notifyOpCallback, element.toString());
            }
        }
    }
}
