package joannamazer.blerp;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

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

    void genTone(double freqOfTone, int durOfTone) {
        // fill out the array
//        durOfTone = durOfTone * 2;
        double convertDur = 1000 / durOfTone;
        short[] mBuffer = new short[numSamples];

        for (int i = 0; i < (int)(numSamples/convertDur); ++i) {
            samples[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
            mBuffer[i] = (short) (samples[i]*Short.MAX_VALUE);
        }
        for (int i = (int)(numSamples/convertDur); i < numSamples; ++i) {
            samples[i] = -1.0*Math.sin(2.0 * Math.PI * i / (sampleRate/freqOfTone));
            mBuffer[i] = (short) (samples[i]*Short.MAX_VALUE);
        }

        audioTrack.write(mBuffer, 0, numSamples);
        audioTrack.setLoopPoints(0, (int)(numSamples/(convertDur))*2, -1);
    }


    void playSound(boolean on) {
        if (on) {

            audioTrack.play();
        }
        else {
            audioTrack.pause();
        }
    }
}