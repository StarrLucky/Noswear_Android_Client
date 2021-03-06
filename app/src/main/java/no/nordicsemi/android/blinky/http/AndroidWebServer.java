package no.nordicsemi.android.blinky.http;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel;


public class AndroidWebServer extends NanoHTTPD  {

    private static final AndroidWebServer INSTANCE = new AndroidWebServer(8080);

    public static AndroidWebServer getInstance(){
        return INSTANCE;
    }

    private MutableLiveData<Integer> liveLedCommand = new MutableLiveData<>();


    public LiveData<Integer> getLedCommand()   {
        return  liveLedCommand;
    }

    private BlinkyViewModel nrfModel;


    private AndroidWebServer(int port)
    {
        super(port);
    }
    private AndroidWebServer(String hostname, int port)
    {

        super(hostname, port);
    }


    @Override
    public Response serve(IHTTPSession session)
    {
        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        String quer = session.getQueryParameterString();
       // if (quer.compareTo("shocking") > 0) {

        if (Objects.requireNonNull(parms.get("shocking")).compareTo("on") == 0)  {              // x.x.x.x/?shocking=on

            msg += "<p> Shocking! </p>";
            liveLedCommand.postValue(1);  // post value to livedata to read from activity thread
            //nrfModel.enableLedCommand();
            Log.i("AndroidWebServer", "We got command");
            return newFixedLengthResponse( msg + "</body></html>\n" );
        }
        else {
            msg += "<p> Wrong query: " + parms.get("shocking") +" </p>";

            return newFixedLengthResponse( msg + "</body></html>\n" );
        }

    }

}