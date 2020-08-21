package no.nordicsemi.android.blinky;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.http.AndroidWebServer;
import no.nordicsemi.android.blinky.http.AndroidWebServerService;
import no.nordicsemi.android.blinky.microsoft.speech.MicrophoneStream;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;
import android.net.wifi.WifiManager;
import no.nordicsemi.android.blinky.utils.ProfanityChecking;
// from speech sdk
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

public class ShockingActivity extends AppCompatActivity  {

    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

    private static final String TAG = "ShockingActivity";

    private AndroidWebServer androidWebServer = AndroidWebServer.getInstance();

    private BlinkyViewModel nrfModel;
    private Button ButtonOn;
    private Button ButtonOff;

    private TextView recognizedTextView;
    private Button recognizeContinuousButton;

    private static final String SpeechSubscriptionKey = "yourkey";
    private static final String SpeechRegion = "westeurope";

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream()
    {
        if (microphoneStream != null)
        {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shocking);
        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

      //  Intent webserviceIntent = new Intent(this, AndroidWebServerService.class);
      //  startService(webserviceIntent);

        try {
            androidWebServer.start();
        } catch (IOException e)  {
            e.printStackTrace();
        }

        nrfModel = ViewModelProviders.of(this).get(BlinkyViewModel.class);
        nrfModel.connect(device);

        LiveData<Integer> liveData = AndroidWebServer.getInstance().getLedCommand();
        liveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer value) {
             if (value == 1)
             {
                 nrfModel.enableLedCommand();
               //  nrfModel.toggleLED(false);     // Только на включенном экране. Сделать по-другому (сервис?)
             }
            }
        });


        // Configure the view model
        final TextView textHttpInfo = findViewById(R.id.text_HttpInfo);
        final TextView nrfStatus = findViewById(R.id.text_status);
        textHttpInfo.setText(getIpAccess());
        ButtonOn = findViewById(R.id.ledOn_button);
        ButtonOff = findViewById(R.id.ledOff_button);

        ///////////////////////////////

        recognizedTextView = findViewById(R.id.recognizedText);
        recognizeContinuousButton = findViewById(R.id.button_RecognizeContinuous);

        try
        {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(ShockingActivity.this, new String[]{RECORD_AUDIO, INTERNET, READ_EXTERNAL_STORAGE}, permissionRequestId);
        }
        catch (Exception ex)
        {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
            recognizedTextView.setText("Could not initialize: " + ex.toString());
        }
        final SpeechConfig speechConfig;
        final KeywordRecognitionModel kwsModel;
        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return;
        }
        //////////////////////////////
        ProfanityChecking  profanityChecking = null;
        try {
           profanityChecking = new ProfanityChecking(this);
        } catch (FileNotFoundException e) {
            Log.e("ProfanityChecking", "could not init dictionary");
            e.printStackTrace();
        }

        ButtonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nrfModel.enableLedCommand();
                nrfStatus.setText("Shocking...");
            }
        });

        ButtonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrfModel.toggleLED(true);
                nrfStatus.setText("UnShocking...");
            } });

        ProfanityChecking finalProfanityChecking = profanityChecking;
        recognizeContinuousButton.setOnClickListener(new View.OnClickListener() {
            private static final String logTag = "reco 3";
            private boolean continuousListeningStarted = false;
            private SpeechRecognizer reco = null;
            private AudioConfig audioInput = null;
            private String buttonText = "";
            private ArrayList<String> content = new ArrayList<>();
//            private ProfanityChecking profanityCheck;
            @Override
            public void onClick(final View view) {
                final Button clickedButton = (Button) view;
                disableButtons();

                if (continuousListeningStarted) {
                    if (reco != null) {
                        final Future<Void> task = reco.stopContinuousRecognitionAsync();
                        setOnTaskCompletedListener(task, result -> {
                            Log.i(logTag, "Continuous recognition stopped.");
                            ShockingActivity.this.runOnUiThread(() -> {
                                clickedButton.setText(buttonText);
                            });
                            enableButtons();
                            continuousListeningStarted = false;
                        });
                    } else {
                        continuousListeningStarted = false;
                    }
                    return;
                }
                clearTextBox();

                try {
                    content.clear();
                    // audioInput = AudioConfig.fromDefaultMicrophoneInput();
                    audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                    reco = new SpeechRecognizer(speechConfig, "ru-RU", audioInput);

                    reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                    });

                    reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                        final String s = speechRecognitionResultEventArgs.getResult().getText();
                        if (s.length() > 1) {
                            if (finalProfanityChecking.checkProfanity(s)>0)
                            {
                                content.add("Found in:" + s );     // checking for profanity language
                                nrfModel.enableLedCommand();
                                Log.i(TAG,"Shocking command " + s);
                            }
                            else
                                {
                                    content.add(s);
                                }
                        }
                        setRecognizedText(TextUtils.join(" ", content));
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
        ShockingActivity.this.runOnUiThread(() -> {
            recognizeContinuousButton.setEnabled(false);
        });
    }

    private void enableButtons() {
      ShockingActivity.this.runOnUiThread(() -> {
            recognizeContinuousButton.setEnabled(true);
        });
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
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cacheFile.getPath();
    }

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private void clearTextBox() {
        AppendTextLine("", true);
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, true);
    }

    private void AppendTextLine(final String s, final Boolean erase) {
        ShockingActivity.this.runOnUiThread(() -> {
            if (erase) {
                recognizedTextView.setText(s);
            } else {
                String txt = recognizedTextView.getText().toString();
                recognizedTextView.setText(txt + System.lineSeparator() + s);
            }
        });
    }

}

