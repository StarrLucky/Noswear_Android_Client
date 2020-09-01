package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.http.AndroidWebServer;
import no.nordicsemi.android.blinky.microsoft.speech.MicrophoneStream;
import no.nordicsemi.android.blinky.utils.ProfanityChecking;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

// from speech sdk

public class ShockingActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";
    private static final String TAG = "ShockingActivity";
    //private AndroidWebServer androidWebServer = AndroidWebServer.getInstance();
    // VIEW
    private BlinkyViewModel nrfModel;
    private TextView recognizedTextView;
    private TextView recognizedProfanityTextView;
    private Button recognizeContinuousButton;
    //SPEECH
    private static final String SpeechSubscriptionKey = "a274c21d23a248b98717bfceb4e27eac";
    private static final String SpeechRegion = "northeurope";
    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shocking);
        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceAddress = device.getAddress();
         //Intent webserviceIntent = new Intent(this, AndroidWebServerService.class);
         //startService(webserviceIntent);

        try {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(ShockingActivity.this, new String[]{RECORD_AUDIO, INTERNET, READ_EXTERNAL_STORAGE}, permissionRequestId);
        } catch (Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
            recognizedTextView.setText(getString(R.string.failed_recognition,ex.toString()));
        }


       /* try {
            androidWebServer.start();
            Log.i(TAG, "Web server Started");
        } catch (IOException e) {
            Log.e(TAG, "Exception found with starting web server" + e.toString());
            e.printStackTrace();
        }
        */

        nrfModel = ViewModelProviders.of(this).get(BlinkyViewModel.class);
        nrfModel.connect(device);
        LiveData<Integer> liveData = AndroidWebServer.getInstance().getLedCommand();
        liveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer value) {
                if (value == 1){
                    nrfModel.enableLedCommand();
                    Log.i(TAG, "Command from http");
                    //  nrfModel.toggleLED(false);     // Только на включенном экране. Сделать по-другому (сервис?)
                }
            }
        });

        // Configure the view model
        final TextView textHttpInfo = findViewById(R.id.text_HttpInfo);
        final TextView nrfStatus = findViewById(R.id.text_status);
        textHttpInfo.setText(getIpAccess());
        Button buttonOn = findViewById(R.id.ledOn_button);
        recognizedTextView = findViewById(R.id.recognizedText);
        recognizeContinuousButton = findViewById(R.id.button_RecognizeContinuous);
        recognizedProfanityTextView = findViewById(R.id.recognized_profatiny);

        buttonOn.setOnClickListener(view -> {
            nrfModel.enableLedCommand();
            nrfStatus.setText("Shocking...");
        });

        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }
        ProfanityChecking profanityChecking;
        profanityChecking = new ProfanityChecking(this);
        ProfanityChecking finalProfanityChecking = profanityChecking;
        recognizeContinuousButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "reco 3";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private AudioConfig audioInput = null;
            private String buttonText = "";

            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                disableButtons();
                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            ShockingActivity.this.runOnUiThread(() -> clickedButton.setText(buttonText));
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }
                    return;
                }

                try {
                    audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    reco = new SpeechRecognizer(speechConfig, "ru-RU", audioInput);
                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                    });
                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        if (s.length() > 0) {
                            if (finalProfanityChecking.checkProfanity(s) > 0)                       // checking for profanity language
                            {
                                addText(s, recognizedTextView);
                                setText(TextUtils.join(" ", finalProfanityChecking.getProfanityFound()).toUpperCase(), recognizedProfanityTextView);
                                Log.i(TAG, "Shocking command " + s);
                                nrfModel.enableLedCommand(); // ending command to shocking circuit

                            } else {
                                addText(s, recognizedTextView);
                                clearText(recognizedProfanityTextView);
                            }
                        }
                        finalProfanityChecking.clearProfanityFound();                               // clear list of words found in dictionary in recognized sentence
                    });

                    final Future<Void> task = reco.startContinuousRecognitionAsync();
                    setOnTaskCompletedListener(task, result -> {
                        continuousListeningStarted = true;
                        ShockingActivity.this.runOnUiThread(() -> {
                            buttonText = clickedButton.getText().toString();
                            clickedButton.setText("Stop");
                            clickedButton.setEnabled(true);
                        });
                    });
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    displayException(ex);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // androidWebServer.stop();
    }

    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":";
    }

    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private void disableButtons() {
        ShockingActivity.this.runOnUiThread(() -> recognizeContinuousButton.setEnabled(false));
    }

    private void enableButtons() {
        ShockingActivity.this.runOnUiThread(() -> recognizeContinuousButton.setEnabled(true));
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private String copyAssetToCacheAndGetFilePath(String filename) {
        File cacheFile = new File(getCacheDir() + "/" + filename);
        if (!cacheFile.exists()) {
            try {
                InputStream is = getAssets().open(filename);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                FileOutputStream fos = new FileOutputStream(cacheFile);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cacheFile.getPath();
    }

    private static ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }


    private void  clearText(TextView view) {
        view.setText("");
    }

    private void setText(final String s, TextView view) {
        clearText(view);
        view.setText(s);
    }

    private void addText(final String s, TextView view) {
      //  view.setText(view.getText().toString()  + s +"\n");
       // view.setText("\n" + s + "\n");
        view.setText(getString(R.string.text_ViewAddtext, s));
    }




}




