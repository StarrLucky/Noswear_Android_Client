package no.nordicsemi.android.blinky.microsoft.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
        import android.media.AudioRecord;
        import android.media.MediaRecorder;
        import android.util.Log;

        import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;
        import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat;





/**
 * MicrophoneStream exposes the Android Microphone as an PullAudioInputStreamCallback
 * to be consumed by the Speech SDK.
 * It configures the microphone with 16 kHz sample rate, 16 bit samples, mono (single-channel).
 */
public class MicrophoneStream extends PullAudioInputStreamCallback {
    private final static int SAMPLE_RATE = 16000;
    private final AudioStreamFormat format;
    private volatile AudioRecord recorder;

    public MicrophoneStream() {
        this.format = AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, (short)16, (short)1);
        this.initMic();
    }

    public AudioStreamFormat getFormat() {
        return this.format;
    }

    @Override
    public int read(byte[] bytes) {
        final AudioRecord recorder = this.recorder;
        if (recorder == null) {
            // The stream was closed while the SDK was still draining it.
            return 0;
        }
        long ret = recorder.read(bytes, 0, bytes.length);
        return (int)ret;
    }

    @Override
    public void close() {
        // The SDK closes the stream when the AudioConfig is disposed and the activity closes it
        // again when it releases the recognizer, so this has to tolerate being called twice.
        final AudioRecord recorder = this.recorder;
        if (recorder == null) {
            return;
        }
        this.recorder = null;
        try {
            recorder.stop();
        } catch (IllegalStateException ex) {
            Log.w("MicrophoneStream", "could not stop the recorder, " + ex.toString());
        }
        recorder.release();
    }

    private void initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        AudioFormat af = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build();
        this.recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(af)
                .build();

        this.recorder.startRecording();
    }
}
