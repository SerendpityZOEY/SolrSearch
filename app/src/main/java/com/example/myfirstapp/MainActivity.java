package com.example.myfirstapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    SearchView searchView;
    private ProgressDialog pDialog;
    ListView lv;
    public ListAdapter adapter;

    private DatabaseReference mDatabase;
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final String Tag_Email = "c_email_sni";
    private static final String Tag_Repo = "repo_sni";
    JSONArray dataJ = null;

    final List<String> email = new ArrayList<String>();
    final List<String> repo = new ArrayList<String>();
    List<String> res = new ArrayList<String>();
    String en = null;
    String q;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        lv = (ListView) findViewById(R.id.listView);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // User properties
        final String profession = "Student";
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance( this );
        analytics.setUserProperty( "profession", profession );

        mDatabase = FirebaseDatabase.getInstance().getReference();

        searchView = (SearchView)findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query){
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText){
                q = newText;
                if(q != ""){
                    try{
                        en = URLEncoder.encode(q, "UTF-8");
                    }catch(UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                    String url = "http://192.12.242.139:8983/solr/fixr_delta/select?q=*%3A*&fq="+en+"&wt=json&indent=true";
                    mDatabase.child("user_trace").push().setValue(q);
                    new LoadData(url, q).execute();

                    //Log search event
                    Bundle bundle = new Bundle();
                    bundle.putString(Param.SEARCH_TERM, q);
                    mFirebaseAnalytics.logEvent(Event.SEARCH, bundle);

                }else{
                    String url = "http://192.12.242.139:8983/solr/fixr_delta/select?q=*%3A*&fq=&wt=json&indent=true";
                    new LoadData(url, "*:*").execute();
                }
                return false;
            }
        });
        String url = "http://192.12.242.139:8983/solr/fixr_delta/select?q=*%3A*&fq=&wt=json&indent=true";
        new LoadData(url, "*:*").execute();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                String item = ((TextView)view).getText().toString();

                //parse item
                Map<String, String> commitObj = new HashMap<String, String>();

                String[] tokens = item.split("\n");
                for(String str:tokens){
                    int ind = str.indexOf(":");
                    commitObj.put(str.substring(0,ind),str.substring(ind+1));
                }
                mDatabase.child("item_click").push().setValue(commitObj);
                //Select Content
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, commitObj.get("Email"));
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, commitObj.get("Repo"));

                // Log.d("email & repo: ","> "+commitObj.get("Email"));
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "commit");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                Toast.makeText(getBaseContext(), item, Toast.LENGTH_LONG).show();

            }
        });

    }

    private class LoadData extends AsyncTask<Void, Void, Void> {
        String url;
        String q;

        LoadData(String url, String q) {
            this.url = url;
            this.q = q;
        }

        @Override
        protected Void doInBackground(Void... params) {

            ServiceHandler sh = new ServiceHandler();
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
            String emailadd="";
            String repo_sni="";
            email.clear();
            repo.clear();
            res.clear();
            Log.d("Response: ","> "+jsonStr);
            if(jsonStr!=null){
                try{
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject jsonObjresponse = jsonObj.getJSONObject("response");
                    dataJ = jsonObjresponse.getJSONArray("docs");
                    for(int i=0;i<dataJ.length();i++){
                        JSONObject json_data = dataJ.getJSONObject(i);
                        emailadd = json_data.getString(Tag_Email).substring(2, json_data.getString(Tag_Email).length()-2);
                        repo_sni = json_data.getString(Tag_Repo).substring(2, json_data.getString(Tag_Repo).length()-2);
                        email.add(emailadd);
                        repo.add(repo_sni);
                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
                for(int i=0;i<email.size();i++){
                    res.add("\n"+"<b>Email:</b>"+email.get(i)+"<br>"+"<b>Repo:</b>"+repo.get(i)+"\n");
                }
                mDatabase.child("doInBackground").push().setValue(res);
                // [START custom_event]
                Bundle Firebaseparams = new Bundle();
                Firebaseparams.putString("commit_email", emailadd);
                Firebaseparams.putString("commit_repo", repo_sni);
                mFirebaseAnalytics.logEvent("doInBackground", Firebaseparams);
                // [END custom_event]

            }else {
                Log.e("ServiceHandler", "Response is Null");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDatabase.child("onPostExecute").push().setValue("Test");

            adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, res){
                @Override
                public String getItem(int position){
                    return Html.fromHtml(res.get(position)).toString();
                }
            };
            lv.setAdapter(adapter);
        }
    }
}
