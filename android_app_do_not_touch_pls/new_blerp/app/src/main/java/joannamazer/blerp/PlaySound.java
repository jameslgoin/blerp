package joannamazer.blerp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Created by joannamazer on 3/24/18.
 */


public class PlaySound {
    private final int sampleRate = 44100;
    private final int numSamples = sampleRate;
    private final double samples[] = new double[numSamples];
    private final byte generatedSnd[] = new byte[(numSamples*2)];

    final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT, numSamples,
            AudioTrack.MODE_STATIC);

    void genChirp() {

        int lowFreq = 12000;    // Hz
        int highFreq = 19000;   // Hz
        int duration = 200;     // ms
        int chirpSamples = (numSamples * (duration/1000));
        short[] mBuffer = new short[numSamples];

        int freqOffset = (highFreq - lowFreq) / duration; // frequency change every ms
        int chirpCounter = 0;

        int currentFreq = lowFreq;
        chirpSamples = 8820;
//        t0=t(1);
//        T=t1-t0;
//        k=(f1-f0)/T;
//        x=cos(2*pi*(k/2*t+f0).*t+phase);
//        Log.d("genchirp", "chirpSamples: " + Double.toString(chirpSamples));
//        Log.d("genchirp", "count: " + Double.toString(chirpCounter));


//        int T = chirpSamples - count;
//        double k = (highFreq - lowFreq) / T;
//
//        for (int i = 0; i < numSamples; i++) {
//            samples[i] = Math.cos(2.0 * Math.PI * (k/2.0 * i + lowFreq) * i);
//            mBuffer[i] = (short) (samples[i] * Short.MAX_VALUE);
//        }

        for (int i = 0; i < numSamples; i++) {

            if (chirpCounter == chirpSamples) {
                chirpCounter = 0;
            }

            double testSample = Math.sin(2 * Math.PI * i / (sampleRate/(currentFreq)));
//            Log.d("genchirp", "frequency: " + Double.toString(testSample));
            if (Math.floor(testSample) == 0) {
                currentFreq += freqOffset;
                samples[i] = Math.sin(2 * Math.PI * i / (sampleRate/currentFreq));
            } else {
                samples[i] = Math.sin(2 * Math.PI * i / (sampleRate/currentFreq));

            }
            mBuffer[i] = (short) (samples[i] * Short.MAX_VALUE);
        }

        audioTrack.write(mBuffer, 0, mBuffer.length);
        audioTrack.setLoopPoints(0, 8820, -1);
    }

//    void genTone(double freqOfTone, int durOfTone) {
//
//        double convertDur = 1000 / durOfTone;
//        short[] mBuffer = new short[numSamples];
//
//        for (int i = 0; i < (int)(numSamples/convertDur); ++i) {
//            samples[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
//            mBuffer[i] = (short) (samples[i]*Short.MAX_VALUE);
//        }
//        for (int i = (int)(numSamples/convertDur); i < numSamples; ++i) {
//            samples[i] = -1.0*Math.sin(2.0 * Math.PI * i / (sampleRate/freqOfTone));
//            mBuffer[i] = (short) (samples[i]*Short.MAX_VALUE);
//        }
//
//        audioTrack.write(mBuffer, 0, numSamples);
//        audioTrack.setLoopPoints(0, (int)(numSamples/(convertDur))*2, -1);
//    }


    void playSound(boolean on) {
        if (on) {
            audioTrack.play();
        }
        else {
            audioTrack.pause();
        }
    }
}


