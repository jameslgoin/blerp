package joannamazer.blerp;

        import android.annotation.SuppressLint;
        import android.content.DialogInterface;
        import android.hardware.Sensor;
        import android.hardware.SensorEvent;
        import android.hardware.SensorEventListener;
        import android.hardware.SensorManager;
        import android.media.AudioFormat;
        import android.media.AudioManager;
        import android.media.AudioTrack;
        import android.os.Build;
        import android.support.design.widget.BottomNavigationView;
        import android.support.v7.app.AlertDialog;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.support.v7.widget.Toolbar;
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
        import java.math.RoundingMode;
        import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    AudioTrack audioTrack, tone;
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
    DecimalFormat df, dff, dfff;

    int updateCount = 0;
    boolean refresh = false;
    int duration = 1; // seconds
    int sampleRate = 8000;
    double freqOfTone = 440; // hz
    int durOfTone = 250;
    float mCurrentDegree = 0f;

    int numSamples = duration * sampleRate;

    byte gen_sound[] = new byte[2 * numSamples];
    double sample[] = new double[numSamples];

    boolean switchState = false;
    boolean mLastAccelerometerSet = false;
    boolean mLastMagnetometerSet = false;

    float[] mLastAccelerometer = new float[3];
    float[] mLastMagnetometer = new float[3];
    float[] mR = new float[9];
    float[] mOrientation = new float[3];





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

        setupViews();
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, gen_sound.length, AudioTrack.MODE_STATIC);
        generateTone();
        audioTrack.write(gen_sound, 0, gen_sound.length);
        freq_val_TV.setText(Double.toString(freqOfTone));

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
            case R.id.action_timing:
                launchPopUpWindow(getResources().getString(R.string.timing_dialog_message), 0);
                return true;

            case R.id.action_frequency:
                launchPopUpWindow(getResources().getString(R.string.frequency_dialog_message), 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void launchPopUpWindow(String message, int caseID) {
        final int CID = caseID;
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setMessage(message);

        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(2);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (CID == 0) {
                            durOfTone = Integer.parseInt(input.getText().toString());
                            Toast.makeText(getApplicationContext(), "Duration: " + durOfTone + " ms", Toast.LENGTH_SHORT).show();

                        } else {
                            freqOfTone = Double.parseDouble(input.getText().toString());
                            freq_val_TV.setText(Double.toString(freqOfTone));
                            Toast.makeText(getApplicationContext(), "Frequency: " + freqOfTone + " Hz", Toast.LENGTH_SHORT).show();
                        }
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
    public void onAccuracyChanged(Sensor sensor, int i) {
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
        dfff = new DecimalFormat("00.000");
        dfff.setRoundingMode(RoundingMode.CEILING);

    }


    private AudioTrack generateTone(double freqHz, int durationMs) {
        int phaseChange;
        int count = (int)(44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
        short[] samples = new short[count*2];
        for(int i = 0; i < count*2; i += 2){
            if (i < count) {
                phaseChange = 1;
            } else {
                phaseChange = -1;
            }

            short sample = (short)(Math.sin(2 * Math.PI * phaseChange * i / (44100.0 / freqHz)) * 0x7FFF);
            samples[i] = sample;
        }
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                count*2 * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
        track.write(samples, 0, count*2);
        return track;
    }

    public void toggleSwitchStatus(View view) {
        switchState = !switchState;
        if (switchState) {
            toggleTone.setImageResource(R.drawable.ic_stop_black_24dp);
            tone = generateTone(freqOfTone, durOfTone);
            playTone();

        } else {
            toggleTone.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            stopTone();
        }
    }

    void playTone() {
        int count = (int)(44100.0 * 2.0 * (durOfTone / 1000.0)) & ~1;
        tone.reloadStaticData();
        tone.setLoopPoints(0, count*2, -1);
        tone.play();
    }

    void stopTone() {
        tone.stop();
    }

    void generateTone() {

        for (int i = 0; i < numSamples; ++i) {
            if (i < (numSamples/4)) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
            } else if ((i >= (numSamples/2)) && (i < (3*numSamples/4))) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
            } else {
                sample[i] = 0;
            }
        }

        // convert to 16 bit pcm sound array (assume buffer is normalized)
        int idx = 0;
        for (double dVal : sample) {
            short val = (short) ((dVal * 32767));               // scale to maximum amplitude
            gen_sound[idx++] = (byte) (val & 0x00ff);           // in 16 bit wav PCM, first byte is the low order byte
            gen_sound[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    void playSound() {
        audioTrack.reloadStaticData();
        audioTrack.setLoopPoints(0, numSamples, -1);
        audioTrack.play();
    }

    void stopSound() {
        audioTrack.stop();
    }
}
