package com.shwody.bledebugger.bean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

import java.util.Objects;

public class BleDevice {

    public BluetoothDevice device;

    public int rssi;

    public byte[] raw;

    public boolean isConnectable;


    public BleDevice(BluetoothDevice device, int rssi, byte[] raw) {
        this.device = device;
        this.rssi = rssi;
        this.raw = raw;
        isConnectable = true;
    }

    public BleDevice(BluetoothDevice device, int rssi, byte[] raw, boolean isConnectable) {
        this.device = device;
        this.rssi = rssi;
        this.raw = raw;
        this.isConnectable = isConnectable;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BluetoothDevice) {
            return this.device.getAddress().equals(((BluetoothDevice) o).getAddress());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.device.getAddress().hashCode();
    }
}
