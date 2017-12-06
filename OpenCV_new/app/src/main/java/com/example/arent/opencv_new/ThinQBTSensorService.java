package com.example.arent.opencv_new;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.arent.opencv_new.BleAttributes;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by sk.lyu on 2017-01-20.
 */

public class ThinQBTSensorService extends Service {
    private final static String TAG = ThinQBTSensorService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mWasherGattService;
    private BluetoothGattCharacteristic mWasherTxCharacteristic;


    public static final int DISPLAY_BUTTON_EVENT = 25;
    public static final int RSSI_MAX = -65;

    public final static String ACTION_GATT_CONNECTED = "com.lge.washermeter.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.lge.washermeter.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.lge.washermeter.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_WASHER_SERVICES_DISCOVERED = "com.lge.washermeter.ACTION_WASHER_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.lge.washermeter.ACTION_DATA_AVAILABLE";
    public final static String ACTION_BATTERY_DATA = "com.lge.washermeter.ACTION_BATTERY_DATA";
    public final static String EXTRA_DATA = "com.lge.washermeter.EXTRA_DATA";
    public final static String BATTERY_DATA = "com.lge.washermeter.BATTERY_DATA";
    public final static String ACTION_BUTTON_EVENT = "com.lge.washermeter.ACTION_BUTTON_EVENT";
    public final static String ACTION_ACCEL_DATA = "com.lge.washermeter.ACTION_ACCEL_DATA";
    public final static String ACTION_VIB_DATA = "com.lge.washermeter.ACTION_VIB_DATA";
    public final static String ACTION_NOTI = "com.lge.washermeter.ACTION_NOTI";
    public final static String ACTION_TEMP_HUMID = "com.lge.washermeter.ACTION_TEMP_HUMID";
    public final static String ACTION_BUTTON_DATA = "com.lge.washermeter.ACTION_BUTTON_DATA";

    public static int rssi_value =0;
    NotificationManager mNotificationManager = null;
    private boolean mNotiEnable = true;
    private boolean mIsLoadLoggingData = false;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                boolean rssiStatus = mBluetoothGatt.readRemoteRssi();
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                broadcastUpdate(intentAction);
                mNotiEnable = true;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(BleAttributes.IMMEIDIATE_ALERT_SERVICE_UUID)) {
                        Log.i(TAG, "Immediate Alert service is found");
                    } else if (service.getUuid().equals(BleAttributes.LINKLOSS_SERVICE_UUID)) {
                        Log.i(TAG, "Linkloss service is found");
                    } else if (service.getUuid().equals(BleAttributes.BATTERY_SERVICE_UUID)) {
                        Log.i(TAG, "Battery service is found");
                        startCheckingBatteryState(service.getCharacteristic(BleAttributes.BATTERY_LEVEL_CHARACTERISTIC_UUID));
                    } else if (service.getUuid().equals(BleAttributes.WASHER_SERVICE_UUID)) {
                        setWasherService(gatt, service);
                    }
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.i(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG, "onChrRead by : " +characteristic.getUuid());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                // TODO : 여기서 만약 RX를 읽는 상황이면.. 모르겠다 ㅋㅋ
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic); // TODO : 필요한가 ??
            Log.e(TAG, "onCharacteristicChanged by : "+characteristic.getUuid());

            if (characteristic.getUuid().equals(BleAttributes.WASHER_TX_CHAR_UUID)) {
                Log.e(TAG, "IS TX :: Received Data :" + unsignedByteString(characteristic.getValue()));

                byte packet[] = characteristic.getValue();
                byte flag = packet[0];

                Log.e(TAG, "Flag is:" + flag);
                switch (flag) {
                    case BleAttributes.FLAG_TEMP_HUMID:     //온습도
                        Intent temp_humid_intent = new Intent(ACTION_TEMP_HUMID);
                        temp_humid_intent.putExtra(ACTION_TEMP_HUMID, packet);
                        sendBroadcast(temp_humid_intent);
                        break;
                    case BleAttributes.FLAG_DISPLACEMENT:   //진동계
                        Intent vib_intent = new Intent(ACTION_VIB_DATA);
                        vib_intent.putExtra(ACTION_VIB_DATA, packet);
                        sendBroadcast(vib_intent);
                        break;
                    case BleAttributes.FLAG_ACCEL_XYZ:      //수평계
                        Intent accel_intent = new Intent(ACTION_ACCEL_DATA);
                        accel_intent.putExtra(ACTION_ACCEL_DATA, packet);
                        sendBroadcast(accel_intent);
                        break;
                    case BleAttributes.FLAG_CLICK_BUTTON:   // 버튼
                        Intent btn_intent = new Intent(ACTION_BUTTON_EVENT);
                        btn_intent.putExtra(ACTION_BUTTON_DATA, packet);
                        sendBroadcast(btn_intent);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "onCharacteristiWrite()");
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.e(TAG, "onDescriptorWrite()");
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    private void startCheckingBatteryState(BluetoothGattCharacteristic btChar) {
        readCharacteristic(btChar);
    }

    private void setWasherService(BluetoothGatt gatt, BluetoothGattService service) {
        mWasherGattService = service;
        Log.i(TAG, "LG Preference Service is founded");
        mWasherTxCharacteristic = service.getCharacteristic(BleAttributes.WASHER_TX_CHAR_UUID);
        setCharacteristicNotification(mWasherTxCharacteristic, true);

        BluetoothGattDescriptor descriptor = mWasherTxCharacteristic.getDescriptor(BleAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    public void writeAccelCharacteristic(byte[] value) {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "mCurrentGattDevice == null");
        } else {
            BluetoothGattService mService = mBluetoothGatt.getService(BleAttributes.WASHER_SERVICE_UUID);
            if (mService != null && value != null) {
                BluetoothGattCharacteristic RxChar = mService.getCharacteristic(BleAttributes.WASHER_RX_CHAR_UUID);
                RxChar.setValue(value);
                // avoid simultaneous data sync
                boolean result = mBluetoothGatt.writeCharacteristic(RxChar);
                Log.e(TAG, "writeAccelCharacteristic - result: " + result + " write: " + Arrays.toString(value));
            } else {
                Log.e(TAG, "mCurrentGattDevice.getService(Defines.WASHER_SERVICE_UUID) == null");
            }
        }
    }

    private void broadcastUpdate(final String action) {
        Log.e(TAG, "broadcastUpdate()");
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        if (BleAttributes.BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            Intent intent = new Intent(ACTION_BATTERY_DATA);
            intent.putExtra(BATTERY_DATA, characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.i(TAG, "Battery read: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            sendBroadcast(intent);
            setWasherService(mBluetoothGatt, mWasherGattService);
        } else {
            Log.e(TAG, "broadcastUpdate value of "+characteristic.getUuid());

            Intent intent = new Intent(action);
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
            sendBroadcast(intent);
        }
    }

    public class LocalBinder extends Binder {
        public ThinQBTSensorService getService() {
            return ThinQBTSensorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return mBinder;
    }

    public void setLoadingLoggingData(boolean isLoadLoggingData) {
        mIsLoadLoggingData = isLoadLoggingData;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnBind()");
        // After using a given device, you should makeCheckResultXml sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        disconnect();
        new Handler().postDelayed(this::close, 500);
        return super.onUnbind(intent);
    }

    public final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "readCharacteristic");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        Log.i(TAG, "setCharacteristicNotification");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    private final BroadcastReceiver mNotiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ThinQBTSensorService.ACTION_NOTI.equals(action)) {
                Log.i("sk.lyu", "onReceive : ACTION_NOTI");
                if(mNotificationManager != null){
                    mNotiEnable = true;
                    mNotificationManager.cancel(1);
                    Log.w(TAG, "removeNoti - mNotiEnable = "+mNotiEnable);
                }
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ThinQBTSensorService.ACTION_NOTI);
        return intentFilter;
    }

    /**
     * 진동 세기 측정을 시작하거나 멈출 때 호출
     * @param on
     */
    public void initVib(boolean on) {
        byte[] packet = new byte[20];

        // init the WasherM
        packet[0] = BleAttributes.FLAG_STATE_CONFIG;
        packet[1] = 4;
        packet[2] = (byte)(on ? 1 : 0); // on/off
        writeAccelCharacteristic(packet);

        new Handler().postDelayed(() ->{
            Log.d(TAG,"Displacement Data : 0x01");
            packet[0] = BleAttributes.FLAG_GENERAL_CONFIG;
            packet[1] = 1;
            packet[2] = 1;
            writeAccelCharacteristic(packet);
        }, 500);
    }

    /**
     * 수평값 측정을 시작하거나 멈출 때 사용
     * @param on
     */
    public void initAccel(boolean on) {
        byte[] packet = new byte[20];

        // init the WasherM
        packet[0] = BleAttributes.FLAG_STATE_CONFIG;
        packet[1] = 4;
        packet[2] = (byte)(on ? 1 : 0); // on/off
        writeAccelCharacteristic(packet);

        new Handler().postDelayed(() ->{
            Log.d(TAG,"Accel XYZ Data : 0x03");
            packet[0] = BleAttributes.FLAG_GENERAL_CONFIG;
            packet[1] = 1;
            packet[2] = 3;
            writeAccelCharacteristic(packet);
        }, 500);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        this.registerReceiver(this.mNotiReceiver, makeIntentFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        // Do not forget to unregister the receiver!!!
        this.unregisterReceiver(this.mNotiReceiver);
    }

    private String unsignedByteString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int idx = 0 ; idx < data.length ; idx++) {

            builder.append( data[idx] & 0xFF );
            builder.append(", ");
        }
        return builder.toString();
    }
}
