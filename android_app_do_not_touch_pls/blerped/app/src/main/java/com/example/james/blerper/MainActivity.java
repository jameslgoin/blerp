package com.example.james.blerper;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.UUID;
import android.annotation.SuppressLint;

import android.content.DialogInterface;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.AudioFormat;
import android.media.AudioManager;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import android.widget.LinearLayout;

import java.math.RoundingMode;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    TextView myLabel;
    EditText myTextbox;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    ImageButton toggleTone;
    SensorManager sensorManager;
    Sensor proximity, accelerometer, magnetometer;
    TextView freq_name_TV, freq_val_TV, freq_unit_TV, smallFrequency;
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
    double freq1=18500;
    double freq2=19000;
    byte[] generatedSnd= new byte[4*numSample];
    Handler handler=new Handler();
    Thread thread;
    Button openButton, sendButton, closeButton;
    ListView listView;
    private ArrayAdapter aAdapter;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    public static int REQUEST_ENABLE_BT = 1;
    double compassAngle;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openButton = (Button) findViewById(R.id.open);
        sendButton = (Button) findViewById(R.id.send);
        closeButton = (Button) findViewById(R.id.close);

        smallFrequency = findViewById(R.id.textView2);

        TB = findViewById(R.id.toolbar3);
        setSupportActionBar(TB);
        TB.setTitleTextColor(getResources().getColor(R.color.colorWhite));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pointerImage = findViewById(R.id.pointer);

        setupViews();

        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    genTone();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                handler.post(new Runnable() {
                    public void run() {
                        playSound();
                    }
                });
            }
        });
    }

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
        smallFrequency.setText(Double.toString(freq1));
        freq_val_TV.setText(Double.toString(freq2));
    }


    //Open Button
    public void onOpenClick(View view) throws IOException {
        findBT();
        openBT();
    }

    //Send Button
    public void onSendClick(View view) throws IOException {
        sendData();
    }

    //Close button
    public void onCloseClick(View view) throws IOException {
        closeBT();
    }


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
//        bluetoothPopUpWindow("Bluetooth Beacon Connect");
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
                        smallFrequency.setText(Double.toString(freq1));
                        freq_val_TV.setText(Double.toString(freq2));
                        playSound();
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

//    public void bluetoothPopUpWindow(String message) {
//        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
//        LinearLayout layout = new LinearLayout(MainActivity.this);
//        layout.setOrientation(LinearLayout.VERTICAL);
//
//        String deviceAddress = mBluetoothAdapter.getAddress();
//        String deviceName = mBluetoothAdapter.getName();
//        String status = "Name: " + deviceName + "\nAddress: " + deviceAddress;
//        alertDialog.setMessage(status);
//
////        mLeScanCallback.onLeScan();
//
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//        ArrayList list = new ArrayList();
//        if(pairedDevices.size()>0) {
//            for (BluetoothDevice device : pairedDevices) {
//                String devicename = device.getName();
//                String macAddress = device.getAddress();
//                list.add("Name: " + devicename + "\nMAC Address: " + macAddress);
//            }
//            listView = new ListView(MainActivity.this);
//            aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
//            listView.setAdapter(aAdapter);
//            listView.setBackgroundColor(getResources().getColor(R.color.colorDarkGrey));
//        }
//        alertDialog.setView(listView);
//        alertDialog.show();
//    }



    public void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
//            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("raspberrypi")) {
                    mmDevice = device;
                    Log.d("findBT", "mmDevice: " + mmDevice);
                    break;
                }
            }
        }
//        myLabel.setText("Bluetooth Device Found");
        Log.d("find BT" , "BT device found");

    }

    public void openBT() throws IOException {
        UUID uuid = UUID.fromString("94F39D29-7D6D-437D-973B-FBA39E49D4EE"); //Standard SerialPortService ID
        Log.d("findBT", "uuid: " + uuid);
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();
        Log.d("openBT" , "BT opened");
//        myLabel.setText("Bluetooth Opened");
    }

    public void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
//        final String[] data4printing = {""};
        workerThread = new Thread(new Runnable() {
            public void run() {
                myLabel = (TextView) findViewById(R.id.label);

                while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
//                                    data4printing[0] = data;
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            myLabel.setText(data);
                                            compassAngle = Double.parseDouble(data);
                                        }
                                    });
                                    Log.d("thread", "DATA: " + data);
//                                    myLabel.setText(data);
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void sendData() throws IOException {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
//        myLabel.setText("Data Sent");
        Log.d("sendData" , "data sent");

    }

    public void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
//        myLabel.setText("Bluetooth Closed");
        Log.d("closedBT" , "BT closed");

    }

    void genTone() throws IOException {

        double instantFrequency = 0;
        double numerator;
        double constant;

        for (int i = 0; i < numSample; i++) {
            if (i < 8820) {
                numerator = (double) (i) / (double) (numSample);
                constant = (numerator * (freq2 - freq1));
                instantFrequency = freq1 + constant;

                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / instantFrequency));
                Log.e("Current Value:", String.format("(freq: %f) (value %f)", instantFrequency, sample[i]));

//            } else {
//                numerator = (double) (i + 8820) / (double) (8820^2);
//                constant = (numerator * (freq2 - freq1));
//                instantFrequency = freq2 + (freq2 - constant);
//
//                sample[i] = Math.sin(2 * Math.PI * (i-8820) / (sampleRate / instantFrequency));
//                if ((i % 10) == 0) {
//                    Log.e("Current Value:", String.format("(freq: %f) (value %f)", instantFrequency, sample[i]));
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
            audioTrack.setLoopPoints(0, 8820, -1);
            audioTrack.play();
        } catch (Exception e) {
            System.out.println("Exception occurred");
        }
    }


    @SuppressLint("NewApi")
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, proximity, 2 * 1000 * 1000);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }


    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, proximity);
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);

    }

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


//            RotateAnimation ra = new RotateAnimation(mCurrentDegree, -azimuthInDegrees, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
//
//            ra.setDuration(250);
//            ra.setFillAfter(true);

            mCurrentDegree = -azimuthInDegrees;

            if (refresh) {
                angle_val_TV.setText(df.format(compassAngle));

//                angle_val_TV.setText(df.format(mCurrentDegree*-1));
//                compassAngle = ((compassAngle + 180)%360);
            }

            RotateAnimation ra = new RotateAnimation( (float) compassAngle, -azimuthInDegrees, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
//
            ra.setDuration(250);
            ra.setFillAfter(true);
            pointerImage.startAnimation(ra);

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