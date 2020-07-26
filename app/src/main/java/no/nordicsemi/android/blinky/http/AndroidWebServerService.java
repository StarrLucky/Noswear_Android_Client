package no.nordicsemi.android.blinky.http;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

 public class AndroidWebServerService extends Service {
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
         } catch (IOException e) {
             e.printStackTrace();
         }
         return super.onStartCommand(intent, flags, startId);
     }
 }