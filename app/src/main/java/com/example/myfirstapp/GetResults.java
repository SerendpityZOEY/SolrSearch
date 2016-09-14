package com.example.myfirstapp;

import android.os.AsyncTask;
import android.widget.TextView;

/**
 * Created by yue on 8/25/16.
 */
public class GetResults extends AsyncTask<String, Integer, String> {
    private final TextView t;

    GetResults(TextView t){
        this.t = t;
    }
    @Override
    protected String doInBackground(String... params) {
        //make rest request here
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String result = "alksdjflkasdjflkaser";
        return result;
    }
    @Override
    protected void onPostExecute(String result) {
        t.setText(result);
    }

}
