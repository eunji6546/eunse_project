package com.example.arent.opencv_new;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by sk.lyu on 2017-01-20.
 */

public class BleAttributes {
    //DESCRIPTOR
    public static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //PROXIMITY SERVICE
    public static final UUID IMMEIDIATE_ALERT_SERVICE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID LINKLOSS_SERVICE_UUID = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

    //BATTERY SERVICE
    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    //LGE PREFERNCE SERVICE
    public static final UUID WASHER_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID WASHER_RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); //property : WRITE, WRITE_NO_RESPONS
    public static final UUID WASHER_TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); //property : NOTIFY

    //BleFlags
    public static final byte FLAG_CLICK_BUTTON = (byte) 110;
    public static final byte FLAG_DISPLACEMENT = (byte) 104;
    public static final byte FLAG_ACCEL_XYZ = (byte) 152;
    public static final byte FLAG_GENERAL_CONFIG = (byte) 157;
    public static final byte FLAG_STATE_CONFIG = (byte) 158;
    public static final byte FLAG_TEMP_HUMID = (byte) 107;

}
