package com.cmtech.android.ble.extend;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;



public class BleGattElementOnline {
    private final BluetoothGattService service;
    private final BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;

    private final UUID serviceUUID;
    private final UUID characteristicUUID;
    private UUID descriptorUUID;

    private BleGattElementOnline(BluetoothGatt bluetoothGatt, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.descriptorUUID = descriptorUUID;

        if (serviceUUID != null && bluetoothGatt != null) {
            service = bluetoothGatt.getService(serviceUUID);
        } else
            service = null;

        if (service != null && characteristicUUID != null) {
            characteristic = service.getCharacteristic(characteristicUUID);
        } else
            characteristic = null;

        if (characteristic != null && descriptorUUID != null) {
            descriptor = characteristic.getDescriptor(descriptorUUID);
        }
    }

    public BleGattElementOnline(BluetoothGatt bluetoothGatt, BleGattElement element) {
        this(bluetoothGatt, element.getServiceUuid(), element.getCharacteristicUuid(), element.getDescriptorUuid());
    }

    public BluetoothGattService getService() {
        return service;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }

    public BleGattElementOnline setDescriptor(BluetoothGattDescriptor descriptor) {
        this.descriptor = descriptor;
        descriptorUUID = descriptor.getUuid();
        return this;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }

    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public UUID getDescriptorUUID() {
        return descriptorUUID;
    }

    public static class Builder {
        private BluetoothGatt bluetoothGatt;
        private UUID serviceUUID;
        private UUID characteristicUUID;
        private UUID descriptorUUID;

        public Builder() {
        }

        public Builder setBluetoothGatt(BluetoothGatt bluetoothGatt) {
            this.bluetoothGatt = bluetoothGatt;
            return this;
        }

        public Builder setCharacteristicUUID(UUID characteristicUUID) {
            this.characteristicUUID = characteristicUUID;
            return this;
        }

        public Builder setDescriptorUUID(UUID descriptorUUID) {
            this.descriptorUUID = descriptorUUID;
            return this;
        }

        public Builder setServiceUUID(UUID serviceUUID) {
            this.serviceUUID = serviceUUID;
            return this;
        }

        public BleGattElementOnline builder() {
            return new BleGattElementOnline(bluetoothGatt, serviceUUID, characteristicUUID, descriptorUUID);
        }
    }
}
