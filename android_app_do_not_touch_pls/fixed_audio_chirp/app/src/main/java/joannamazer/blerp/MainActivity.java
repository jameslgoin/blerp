package joannamazer.blerp;

        import android.annotation.SuppressLint;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.media.AudioFormat;
        import android.media.AudioManager;
        import android.media.AudioTrack;
        import android.os.Handler;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.support.v7.widget.Toolbar;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.animation.Animation;
        import android.view.animation.RotateAnimation;
        import android.widget.EditText;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.TextView;
        import android.widget.Toast;
        import android.bluetooth.*;
        import java.io.IOException;
        import java.math.RoundingMode;
        import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    ImageButton toggleTone;

    SensorManager sensorManager;
    Sensor proximity, accelerometer, magnetometer;

    TextView freq_name_TV, freq_val_TV, freq_unit_TV;
    TextView gx_name_TV, gx_val_TV, gx_unit_TV;
    TextView gy_name_TV, gy_val_TV, gy_unit_TV;
    TextView gz_name_TV, gz_val_TV, gz_unit_TV;
    TextView bx_name_TV, bx_val_TV, bx_unit_TV;
    TextView by_name_TV, by_val_TV, by_unit_TV;
    TextView bz_name_TV, bz_val_TV, bz_unit_TV;
    TextView angle_name_TV, angle_val_TV, angle_unit_TV;
    Toolbar TB;
    ImageView pointerImage;
    DecimalFormat df, dff;
    BluetoothAdapter bluetooth;

    int updateCount = 0;
    boolean refresh = false;

    double freqOfTone = 440; // hz
    int durOfTone = 250; // ms
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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TB = findViewById(R.id.toolbar3);
        setSupportActionBar(TB);
        TB.setTitleTextColor(getResources().getColor(R.color.colorWhite));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pointerImage = findViewById(R.id.pointer);
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        if(bluetooth != null) {
            // Continue with bluetooth setup.
            String status;
            if (bluetooth.isEnabled()) {
                String deviceAddress = bluetooth.getAddress();
                String deviceName = bluetooth.getName();
                status = deviceName + " : " + deviceAddress;
            }
            else
            {
                status = "BLUETOOTH NOT ENABLED";
            }
            Toast.makeText(this, status, Toast.LENGTH_LONG).show();
        }

        setupViews();

        thread=new Thread(new Runnable() {
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

    public void setupViews() {

        toggleTone = this.findViewById(R.id.toggleButton);
        toggleTone.setImageResource(R.drawable.ic_play_arrow_black_24dp);


        freq_name_TV = this.findViewById(R.id.textView_1);
        freq_val_TV = this.findViewById(R.id.frequencyVal);
        freq_unit_TV = this.findViewById(R.id.textView11);

        gx_name_TV = this.findViewById(R.id.textView_3);
        gx_val_TV = this.findViewById(R.id.gx_value);
        gx_unit_TV = this.findViewById(R.id.gx_unit);

        gy_name_TV = this.findViewById(R.id.textView_4);
        gy_val_TV = this.findViewById(R.id.gy_value);
        gy_unit_TV = this.findViewById(R.id.gy_unit);

        gz_name_TV = this.findViewById(R.id.textView_5);
        gz_val_TV = this.findViewById(R.id.gz_value);
        gz_unit_TV = this.findViewById(R.id.gz_unit);

        bx_name_TV = this.findViewById(R.id.textView_7);
        bx_val_TV = this.findViewById(R.id.bx_value);
        bx_unit_TV = this.findViewById(R.id.bx_unit);

        by_name_TV = this.findViewById(R.id.textView_8);
        by_val_TV = this.findViewById(R.id.by_value);
        by_unit_TV = this.findViewById(R.id.by_unit);

        bz_name_TV = this.findViewById(R.id.textView_9);
        bz_val_TV = this.findViewById(R.id.bz_value);
        bz_unit_TV = this.findViewById(R.id.bz_unit);

        angle_name_TV = this.findViewById(R.id.textView29);
        angle_val_TV = this.findViewById(R.id.textView30);
        angle_unit_TV = this.findViewById(R.id.degrees_unit);


        df = new DecimalFormat("000.0");
        df.setRoundingMode(RoundingMode.CEILING);
        dff = new DecimalFormat("00.00");
        dff.setRoundingMode(RoundingMode.CEILING);


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


            RotateAnimation ra = new RotateAnimation(mCurrentDegree, -azimuthInDegrees, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);

            ra.setDuration(250);
            ra.setFillAfter(true);

            pointerImage.startAnimation(ra);
            mCurrentDegree = -azimuthInDegrees;

            if (refresh) {
                gx_val_TV.setText(dff.format(mLastAccelerometer[0]));
                gy_val_TV.setText(dff.format(mLastAccelerometer[1]));
                gz_val_TV.setText(dff.format(mLastAccelerometer[2]));
                bx_val_TV.setText(dff.format(mLastMagnetometer[0]));
                by_val_TV.setText(dff.format(mLastMagnetometer[1]));
                bz_val_TV.setText(dff.format(mLastMagnetometer[2]));
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
