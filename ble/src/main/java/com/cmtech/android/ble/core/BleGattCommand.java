package com.cmtech.android.ble.core;

import com.cmtech.android.ble.callback.IBleDataCallback;
import com.cmtech.android.ble.utils.HexUtil;


/**
  *
  * ClassName:      BleGattCommand
  * Description:    表示Gatt命令，包含Gatt命令执行所需的全部信息
  * Author:         chenm
  * CreateDate:     2018-03-01 06:42
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-20 06:42
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

class BleGattCommand{
    final BleDevice device; // 执行命令的设备
    private final BleGattElement element; // 命令操作的element
    private final BleGattCmdType bleGattCmdType; // 命令类型
    IBleDataCallback dataCallback; // 数据操作回调
    private final byte[] writtenData; // 待写数据。如果是写操作，存放要写的数据；如果是notify或indicate操作，存放enable值
    private final IBleDataCallback receiveCallback; // 如果是notify或indicate操作，存放notify或indicate的回调
    private final String description; // 命令描述符

    private BleGattCommand(BleDevice device, BleGattElement element, BleGattCmdType bleGattCmdType,
                           IBleDataCallback dataCallback,
                           byte[] writtenData, IBleDataCallback receiveCallback, String description) {
        this.device = device;
        this.element = element;
        this.bleGattCmdType = bleGattCmdType;
        this.dataCallback = dataCallback;
        this.writtenData = writtenData;
        this.receiveCallback = receiveCallback;
        this.description = description;
    }

    BleGattCommand(BleGattCommand gattCommand) {
        if(gattCommand == null)
            throw new IllegalArgumentException("BleGattCommand is null.");

        this.device = gattCommand.device;
        this.element = gattCommand.element;
        this.bleGattCmdType = gattCommand.bleGattCmdType;
        this.dataCallback = gattCommand.dataCallback;
        this.writtenData = gattCommand.writtenData;
        this.receiveCallback = gattCommand.receiveCallback;
        this.description = gattCommand.description;
    }

    /**
     * 执行命令。执行完一条命令不仅需要发送命令，还需要收到设备响应或者立即执行返回
     * @return 是否已经执行完命令，true-执行完 false-等待对方响应
     */
    boolean execute() throws InterruptedException{
        if(bleGattCmdType == BleGattCmdType.GATT_CMD_INSTANTRUN) {
            if(dataCallback == null) {
                throw new IllegalStateException("The dataCallback of instant commands is null. ");
            }

            dataCallback.onSuccess(null, null);
            return true;
        }
        if(device == null || device.getBleGatt() == null || element == null) {
            throw new IllegalStateException("The gatt or element of the non-instant commands is null.");
        }

        BleGatt bleGatt = device.getBleGatt();

        switch (bleGattCmdType) {
            case GATT_CMD_READ:
                bleGatt.readData(element, dataCallback);
                break;

            case GATT_CMD_WRITE:
                bleGatt.writeData(element, dataCallback, writtenData);
                break;

            case GATT_CMD_NOTIFY:
            case GATT_CMD_INDICATE:
                bleGatt.enable(element, dataCallback, receiveCallback, (writtenData[0] == 1), (bleGattCmdType == BleGattCmdType.GATT_CMD_INDICATE));
                break;

            default:
                break;
        }

        return false;
    }

    @Override
    public String toString() {
        return description;
    }

    static class Builder {
        private BleDevice device;
        private BleGattElement element;
        private BleGattCmdType bleGattCmdType;
        private byte[] data;
        private IBleDataCallback dataCallback;
        private IBleDataCallback receiveCallback;

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

        Builder setBleGattCmdType(BleGattCmdType bleGattCmdType) {
            this.bleGattCmdType = bleGattCmdType;
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

        Builder setReceiveCallback(IBleDataCallback receiveCallback) {
            this.receiveCallback = receiveCallback;
            return this;
        }

        BleGattCommand build() {
            if(bleGattCmdType == BleGattCmdType.GATT_CMD_INSTANTRUN) {
                if(dataCallback == null) {
                    throw new NullPointerException("The dataCallback of instant commands is null. ");
                }

                return new BleGattCommand(null, null, bleGattCmdType, dataCallback,
                        null, null, "<" + bleGattCmdType + ">");
            }
            if(device == null || device.getBleGatt() == null || element == null) {
                throw new NullPointerException("The device mirror or element of the non-instant commands is null.");
            }
            if (bleGattCmdType == BleGattCmdType.GATT_CMD_WRITE
                    || bleGattCmdType == BleGattCmdType.GATT_CMD_NOTIFY
                    || bleGattCmdType == BleGattCmdType.GATT_CMD_INDICATE) {
                if (data == null || data.length == 0) {
                    throw new NullPointerException("The data of the write, notify or indicate commands is null");
                }
            }
            if (bleGattCmdType == BleGattCmdType.GATT_CMD_NOTIFY
                    || bleGattCmdType == BleGattCmdType.GATT_CMD_INDICATE) {
                if (data[0] == 1 && receiveCallback == null) {
                    throw new NullPointerException("The receive callback of the 'enable' notify or indicate commands is null");
                }
            }

            String description = "<" + bleGattCmdType + " " + element.toString();
            if(bleGattCmdType == BleGattCmdType.GATT_CMD_WRITE) {
                description += HexUtil.encodeHexStr(data);
            } else if(bleGattCmdType == BleGattCmdType.GATT_CMD_INDICATE || bleGattCmdType == BleGattCmdType.GATT_CMD_NOTIFY) {
                description += ((data[0] == 1) ? "enable" : "disable");
            }
            description += ">";

            return new BleGattCommand(device, element, bleGattCmdType, dataCallback, data, receiveCallback, description);
        }
    }
}