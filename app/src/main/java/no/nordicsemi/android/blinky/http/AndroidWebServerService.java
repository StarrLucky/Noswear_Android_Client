package no.nordicsemi.android.blinky.http;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;

public class AndroidWebServerService extends Service {
     private final static String TAG = "WebService";
     private AndroidWebServer androidWebServer = AndroidWebServer.getInstance();


     @Nullable
     @Override
     public IBinder onBind(Intent intent) {
         return null;
     }


     public void  OnCreate() {
         super.onCreate();
     }

     public void OnDestroy () {
         super.onDestroy();
     }

     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         try {
             androidWebServer.start();
             Log.i(TAG, "Web server Started");
         } catch (IOException e) {
             Log.e(TAG, "Couldn't start web service service");
             e.printStackTrace();
         }
         return super.onStartCommand(intent, flags, startId);
     }
 }