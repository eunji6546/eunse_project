public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "BroadcastReceiver:onReceive");

            final String action = intent.getAction();
            //mBluetoothLeService.initVib(true);
            if (ThinQBTSensorService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Log.e("############SEYEON", "CONNECT");
                mBluetoothLeService.initVib(true);




            } else if (ThinQBTSensorService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (ThinQBTSensorService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ThinQBTSensorService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.e("########SEYEON", "데이터 어벨러블하더라 시붐붐");
                displayData(intent.getStringExtra(ThinQBTSensorService.EXTRA_DATA));
                Log.e("######SEYEON", intent.getStringExtra(ThinQBTSensorService.EXTRA_DATA));
                mBluetoothLeService.initVib(true);
                //Log.e("############SEYEON", "DATA AVAILABLE");
                ThinQBTSensorService.on

                if (ThinQBTSensorService.ACTION_ACCEL_DATA.equals(action)){
                    //수평측정핪
                    //mBluetoothLeService.initAccel(true);


                }else if (ThinQBTSensorService.ACTION_VIB_DATA.equals(action)) {
                    Log.e("########SEYEON", "VIB DATA ACCEPT");
                    //진동 측정값
                    byte[] packet = intent.getByteArrayExtra(mBluetoothLeService.ACTION_VIB_DATA);
                    float vib_val = parseVibPacket(packet);
                    Log.e("#######", "VIB VAL : "+ Float.toString(vib_val));

                }
                else {
                    Log.e("###", "ELSE CASE : " + action);
                }
            }

        }
    };