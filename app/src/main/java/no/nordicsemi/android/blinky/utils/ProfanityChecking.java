package no.nordicsemi.android.blinky.utils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.blinky.R;

public class ProfanityChecking {
    private static final String TAG = "ProfanityChecking";
    private static List<String> profanityDictionary = new ArrayList<>();
    private static List<String> profanityFound = new ArrayList<>();

    public ProfanityChecking(Activity activity) {
       setProfanityDictionary(activity);
    }

    public void clearProfanityFound()
    {
        profanityFound.clear();
    }

    public List<String> getProfanityFound()
    {
        return profanityFound;
    }

    private static void setProfanityDictionary(Activity activity) {
        String str;
        Resources res = activity.getResources();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.profanity_dic)))) {    // opening RAW txt dictionary RESOURCE (RAW/profanity_dic.txt)
            while ((str = reader.readLine()) != null) {
                profanityDictionary.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int checkProfanity(String str) {
        int badWordCounter = 0;
        Log.i(TAG, "input string: "+ str + "str length:" + str.length());
        str = str.replaceAll("\\.", "");  // removing punctuation marks
        str = str.replaceAll("\\?", "");  // removing punctuation marks
        str = str.replaceAll("\\!", "");  // removing punctuation marks
        str = str.replaceAll("\\,", "");  // removing punctuation marks
        String[] splitWords = str.toLowerCase().split("\\s+");  //works
       if (splitWords.length > 1) {
           for (String splitWord : splitWords) {
               if (profanityDictionary.contains(splitWord)) {
                   badWordCounter++;
                   profanityFound.add(splitWord.toLowerCase());
               }
           }
        } else if (splitWords.length == 1) {
            if (profanityDictionary.contains(splitWords[0].toLowerCase())) {
                badWordCounter++;
                profanityFound.add(splitWords[0].toLowerCase());
            }
       }
        return badWordCounter;

    }

}
