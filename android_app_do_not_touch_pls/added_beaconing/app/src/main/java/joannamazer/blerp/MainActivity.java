package joannamazer.blerp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joannamazer.blerp.client.ClientActivity;
import joannamazer.blerp.databinding.ActivityMainBinding;
import joannamazer.blerp.server.ServerActivity;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ImageButton toggleTone;
    SensorManager sensorManager;
    Sensor proximity, accelerometer, magnetometer;
    TextView freq_name_TV, freq_val_TV, freq_unit_TV, lowerFrequency;
    TextView angle_name_TV, angle_val_TV, angle_unit_TV;
    Toolbar TB;
    ImageView pointerImage;
    DecimalFormat df, dff;
    int updateCount = 0;
    boolean refresh = false;
    float mCurrentDegree = 0f;
    boolean switchState = false;
    boolean mLastAccelerometerSet = false;
    boolean mLastMagnetometerSet = false;
    float[] mLastAccelerometer = new float[3];
    float[] mLastMagnetometer = new float[3];
    float[] mR = new float[9];
    float[] mOrientation = new float[3];
    AudioTrack audioTrack;
    int sampleRate=44100;
    int numSample=8820;
    double sample[]=new double[numSample*2];
    double freq1=400;
    double freq2=800;
    byte[] generatedSnd= new byte[4*numSample];
    Handler handler=new Handler();
    Thread thread;
//    boolean mScanning;
    public static int REQUEST_BLUETOOTH = 1;

    ListView listView;
    private ArrayAdapter aAdapter;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    private Map<String, BluetoothDevice> mScanResults;

    private boolean mConnected;
    private ScanCallback mScanCallback;
    private BluetoothGatt mGatt;

    private List<BluetoothDevice> mDevices;





    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.launchServerButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,
                ServerActivity.class)));
        binding.launchClientButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,
                ClientActivity.class)));


//        setContentView(R.layout.activity_main);
        lowerFrequency = findViewById(R.id.textView2);

        TB = findViewById(R.id.toolbar3);
        setSupportActionBar(TB);
        TB.setTitleTextColor(getResources().getColor(R.color.colorWhite));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pointerImage = findViewById(R.id.pointer);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        if(mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//                registerReceiver(mReceiver, filter);
            } else {Log.e("ERROR", "BLUETOOTH NOT ENABLED");}
        }


        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Enabled", Toast.LENGTH_LONG).show();
        } else {Toast.makeText(this, "BLE NOT SUPPORTED", Toast.LENGTH_LONG).show();}

        setupViews();

        thread=new Thread(new Runnable() {
            public void run(){
                try {genTone();}
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                handler.post(new Runnable(){
                    public void run()   {playSound();}
                });
            }
        });
    }


//    @SuppressLint("NewApi")
//    private void startScan() {
//        if (!hasPermissions() || mScanning) {
//            return;
//        }
//        List<ScanFilter> filters = new ArrayList<>();
//        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
//        filters.add(scanFilter);
//        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
//        mScanResults = new HashMap<>();
//        mScanCallback = new BtleScanCallback(mScanResults);
//        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
//        mScanning = true;
//        mHandler = new Handler();
//        mHandler.postDelayed(this.stopScan(), SCAN_PERIOD);
//    }

//    private void stopScan() {
//        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
//            mBluetoothLeScanner.stopScan(mScanCallback);
//            scanComplete();
//        }
//        mScanCallback = null;
//        mScanning = false;
//        mHandler = null;
//    }
//
//    @SuppressLint("NewApi")
//    private class BtleScanCallback extends ScanCallback {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            addScanResult(result);
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            for (ScanResult result : results) {
//                addScanResult(result);
//            }
//        }
//        @Override
//        public void onScanFailed(int errorCode) {
//            Log.e("ERROR", "BLE Scan Failed with code " + errorCode);
//        }
//        private void addScanResult(ScanResult result) {
//            stopScan();
//            BluetoothDevice bluetoothDevice = scanResult.getDevice();
//            connectDevice(bluetoothDevice);
//            BluetoothDevice device = result.getDevice();
//            String deviceAddress = device.getAddress();
//            mScanResults.put(deviceAddress, device);
//        }
//
//    }
//
//    @SuppressLint("NewApi")
//    private void connectDevice(BluetoothDevice device) {
//        GattClientCallback gattClientCallback = new GattClientCallback();
//        mGatt = device.connectGatt(this, false, gattClientCallback);
//    }

//    @SuppressLint("NewApi")
//    private class GattClientCallback extends BluetoothGattCallback {
//
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
//            if (status == BluetoothGatt.GATT_FAILURE) {
//                disconnectGattServer();
//                return;
//            } else if (status != BluetoothGatt.GATT_SUCCESS) {
//                disconnectGattServer();
//                return;
//            }
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                mConnected = true;
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                disconnectGattServer();
//            }
//        }
//
//        public void disconnectGattServer() {
//            mConnected = false;
//            if (mGatt != null) {
//                mGatt.disconnect();
//                mGatt.close();
//            }
//        }
//    }
//
//    private void scanComplete() {
//        if (mScanResults.isEmpty()) {
//            return;
//        }
//        for (deviceAddress : mScanResults.keySet()) {
//            Log.d("DATA", "Found device: " + deviceAddress);
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private boolean hasPermissions() {
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            requestBluetoothEnable();
//            return false;
//        } else if (!hasLocationPermissions()) {
//            requestLocationPermission();
//            return false;
//        }
//        return true;
//    }


//
//    private void requestBluetoothEnable() {
//        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        Log.d("DATA", "Requested user enables Bluetooth. Try starting the scan again.");
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private boolean hasLocationPermissions() {
//        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.M)
//    private void requestLocationPermission() {
//        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
//    }
//
//    public UUID convertFromInteger(int i) {
//        final long MSB = 0x0000000000001000L;
//        final long LSB = 0x800000805f9b34fbL;
//        long value = i & 0xFFFFFFFF;
//        return new UUID(MSB | (value << 32), LSB);
//    }

    public void setupViews() {

        toggleTone = this.findViewById(R.id.toggleButton);
        toggleTone.setImageResource(R.drawable.ic_play_arrow_black_24dp);

        freq_name_TV = this.findViewById(R.id.textView_1);
        freq_val_TV = this.findViewById(R.id.frequencyVal);
        freq_unit_TV = this.findViewById(R.id.textView11);

        angle_name_TV = this.findViewById(R.id.textView29);
        angle_val_TV = this.findViewById(R.id.textView30);
        angle_unit_TV = this.findViewById(R.id.degrees_unit);

        df = new DecimalFormat("000.0");
        df.setRoundingMode(RoundingMode.CEILING);
        dff = new DecimalFormat("00.00");
        dff.setRoundingMode(RoundingMode.CEILING);
        lowerFrequency.setText(Double.toString(freq1));
        freq_val_TV.setText(Double.toString(freq2));
    }

//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Log.i("BROADCAST_RECEIVER", "Device found: " + device.getName() + "; MAC " + device.getAddress());
//            }
//        }
//    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_frequency:
                launchPopUpWindow("Frequency Settings");
                return true;

            case R.id.action_settings:
                bluetoothPopUpWindow("Bluetooth Beacon Connect");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void launchPopUpWindow(String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setMessage(message);

        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);


        final EditText lowerFrequency = new EditText(MainActivity.this);
        lowerFrequency.setHint("Lower frequency limit [Hz]");
        lowerFrequency.setInputType(2);
        layout.addView(lowerFrequency);

        final EditText upperFrequency = new EditText(MainActivity.this);
        upperFrequency.setHint("Upper frequency limit [Hz]");
        upperFrequency.setInputType(2);
        layout.addView(upperFrequency);

        alertDialog.setView(layout);

        alertDialog.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        freq1 = Double.parseDouble(lowerFrequency.getText().toString());
                        freq2 = Double.parseDouble(upperFrequency.getText().toString());

                        dialog.dismiss();
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    public void bluetoothPopUpWindow(String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        String deviceAddress = mBluetoothAdapter.getAddress();
        String deviceName = mBluetoothAdapter.getName();
        String status = "Name: " + deviceName + "\nAddress: " + deviceAddress;
        alertDialog.setMessage(status);

//        mLeScanCallback.onLeScan();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();
        if(pairedDevices.size()>0) {
            for (BluetoothDevice device : pairedDevices) {
                String devicename = device.getName();
                String macAddress = device.getAddress();
                list.add("Name: " + devicename + "\nMAC Address: " + macAddress);
            }
            listView = new ListView(MainActivity.this);
            aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
            listView.setAdapter(aAdapter);
            listView.setBackgroundColor(getResources().getColor(R.color.colorDarkGrey));
        }
        alertDialog.setView(listView);
        alertDialog.show();
    }


    void genTone() throws IOException {

        double instantFrequency = 0;
        double numerator;
        double constant;

        for (int i = 0; i < numSample*2; i++) {
            if (i < numSample) {
                numerator = (double) (i) / (double) (numSample);
                constant = (numerator * (freq2 - freq1));
                instantFrequency = freq1 + constant;

                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / instantFrequency));
                Log.e("Current Value:", String.format("(freq: %f) (value %f)", instantFrequency, sample[i]));

            } else {
                numerator = (double) (i + 8820) / (double) (8820^2);
                constant = (numerator *(freq2 - freq1));
                instantFrequency = freq2 + (freq2 - constant);

                sample[i] = Math.sin(2 * Math.PI * (i-8820) / (sampleRate / instantFrequency));
                if ((i % 10) == 0) {
                    Log.e("Current Value:", String.format("(freq: %f) (value %f)", instantFrequency, sample[i]));
                }
            }
        }

        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    void playSound() {
        audioTrack= null;
        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length, AudioTrack.MODE_STATIC);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.setLoopPoints(0, 8820*2, -1);
            audioTrack.play();
        } catch (Exception e) {
            System.out.println("Exception occurred");
        }
    }

    @SuppressLint("NewApi")
    protected void onResume() {
        super.onResume();

//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivity(enableBtIntent);
//            finish();
//            return;
//        }
//
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            finish();
//            return;
//        }
//
//        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
//            finish();
//            return;
//        }
//        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
//        GattServerCallback gattServerCallback = new GattServerCallback();
//        mGattServer = mBluetoothManager.openGattServer(this, gattServerCallback);
//        setupServer();
//        startAdvertising();


        sensorManager.registerListener(this, proximity, 2 * 1000 * 1000);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }
//
//    @SuppressLint("NewApi")
//    private void startAdvertising() {
//        if (mBluetoothLeAdvertiser == null) {
//            return;
//        }
//        @SuppressLint("InlinedApi") AdvertiseSettings settings = new AdvertiseSettings.Builder()
//                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
//                .setConnectable(true)
//                .setTimeout(0)
//                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
//                .build();
//        ParcelUuid parcelUuid = new ParcelUuid(SERVICE_UUID);
//        AdvertiseData data = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .addServiceUuid(parcelUuid)
//                .build();
//        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
//
//    }
//
//    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
//        @Override
//        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//            Log.d("DATA", "Peripheral advertising started.");
//        }
//
//        @Override
//        public void onStartFailure(int errorCode) {
//            Log.d("DATA", "Peripheral advertising failed: " + errorCode);
//        }
//    };
//
//    @SuppressLint("NewApi")
//    private void setupServer() {
//        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//        BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
//                CHARACTERISTIC_UUID,
//                BluetoothGattCharacteristic.PROPERTY_WRITE,
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//        service.addCharacteristic(writeCharacteristic);
//        mGattServer.addService(service);
//    }

//    @SuppressLint("NewApi")
//    private class GattServerCallback extends BluetoothGattServerCallback {
//        @Override
//        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//            super.onConnectionStateChange(device, status, newState);
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                mConnected = true;
//                gatt.discoverServices();
//                mDevices.add(device);
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                mDevices.remove(device);
//            }
//        }
//    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, proximity);
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
//        stopAdvertising();
//        stopServer();
    }

//    private void stopServer() {
//        if (mGattServer != null) {
//            mGattServer.close();
//        }
//    }
//    private void stopAdvertising() {
//        if (mBluetoothLeAdvertiser != null) {
//            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
//        }
//    }
//
//    @SuppressLint("NewApi")
//    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//        super.onServicesDiscovered(gatt, status);
//        if (status != BluetoothGatt.GATT_SUCCESS) {
//            return;
//        }
//        BluetoothGattService service = gatt.getService(SERVICE_UUID);
//        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
//        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//        mInitialized = gatt.setCharacteristicNotification(characteristic, true);
//
//    }
//
//    @SuppressLint("NewApi")
//    public void onCharacteristicWriteRequest(BluetoothDevice device,
//                                             int requestId,
//                                             BluetoothGattCharacteristic characteristic,
//                                             boolean preparedWrite,
//                                             boolean responseNeeded,
//                                             int offset,
//                                             byte[] value) {
//        super.onCharacteristicWriteRequest(device,
//                requestId,
//                characteristic,
//                preparedWrite,
//                responseNeeded,
//                offset,
//                value);
//        if (characteristic.getUuid().equals(CHARACTERISTIC_UUID)) {
//            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
//            int length = value.length;
//            byte[] reversed = new byte[length];
//            for (int i = 0; i < length; i++) {
//                reversed[i] = value[length - (i + 1)];
//            }
//            characteristic.setValue(reversed);
//            for (BluetoothDevice device : mDevices) {
//                mGattServer.notifyCharacteristicChanged(device, characteristic, false);
//            }
//        }
//    }

//    @SuppressLint("NewApi")
//    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//        super.onCharacteristicChanged(gatt, characteristic);
//        byte[] messageBytes = characteristic.getValue();
//        String messageString = null;
//        try {
//            messageString = new String(bytes, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            Log.e("ERROR", "Unable to convert message bytes to string");
//        }
//        Log.d("DATA","Received message: " + messageString);
//    }
//
//    @SuppressLint("NewApi")
//    private void sendMessage() {
//        if (!mConnected || !mEchoInitialized) {
//            return;
//        }
//        BluetoothGattService service = gatt.getService(SERVICE_UUID);
//        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
//        String message = mBinding.messageEditText.getText().toString();
//        byte[] messageBytes = new byte[0];
//        try {
//            messageBytes = message.getBytes("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            Log.e("ERROR", "Failed to convert message string to byte array");
//        }
//        characteristic.setValue(messageBytes);
//        boolean success = mGatt.writeCharacteristic(characteristic);
//
//    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (updateCount == 9) {
            updateCount = 0;
            refresh = true;
        } else {
            refresh = false;
            updateCount++;
        }


        if (sensorEvent.sensor == accelerometer) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor == magnetometer) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }


        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegrees = (float)(Math.toDegrees(azimuthInRadians)+360)%360;


            RotateAnimation ra = new RotateAnimation(mCurrentDegree, -azimuthInDegrees, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);

            ra.setDuration(250);
            ra.setFillAfter(true);

            pointerImage.startAnimation(ra);
            mCurrentDegree = -azimuthInDegrees;

            if (refresh) {
//                gx_val_TV.setText(dff.format(mLastAccelerometer[0]));
//                gy_val_TV.setText(dff.format(mLastAccelerometer[1]));
//                gz_val_TV.setText(dff.format(mLastAccelerometer[2]));
//                bx_val_TV.setText(dff.format(mLastMagnetometer[0]));
//                by_val_TV.setText(dff.format(mLastMagnetometer[1]));
//                bz_val_TV.setText(dff.format(mLastMagnetometer[2]));
                angle_val_TV.setText(df.format(mCurrentDegree*-1));
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}


    public void toggleSwitchStatus(View view) {
        switchState = !switchState;
        if (switchState) {
            toggleTone.setImageResource(R.drawable.ic_stop_black_24dp);
            thread.start();
        } else {
            toggleTone.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            audioTrack.pause();

            thread=new Thread(new Runnable(){
                public void run(){
                    try {
                        genTone();
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run()   {
                            playSound();
                        }
                    });
                }
            });

        }
    }

}
