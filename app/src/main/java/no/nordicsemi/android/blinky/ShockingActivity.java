package no.nordicsemi.android.blinky;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.IOException;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.http.AndroidWebServer;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;
import android.net.wifi.WifiManager;



public class ShockingActivity extends AppCompatActivity  {

    public static final String EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE";

    private AndroidWebServer androidWebServer = AndroidWebServer.getInstance();

    private BlinkyViewModel nrfModel;
    private Button ButtonOn;
    private Button ButtonOff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shocking);
        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        try {
            androidWebServer.start();
        } catch (IOException e) {
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
                 nrfModel.toggleLED(false);     // Только на включенном экране. Сделать по-другому (сервис?)
             }
            }
        });





        // Configure the view model
        final TextView textHttpInfo = findViewById(R.id.text_HttpInfo);
        final TextView nrfStatus = findViewById(R.id.text_status);

        textHttpInfo.setText(getIpAccess());

        ButtonOn = findViewById(R.id.ledOn_button);
        ButtonOff = findViewById(R.id.ledOff_button);



        ButtonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nrfModel.toggleLED(false);
                nrfStatus.setText("Shocking...");
            }
        });

        ButtonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrfModel.toggleLED(true);
                nrfStatus.setText("UnShocking...");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        androidWebServer.stop();
    }


    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":";
    }

}

