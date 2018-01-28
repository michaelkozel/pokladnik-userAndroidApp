package kozliksoft.pokladnau12;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_INTERNET = 1;
    String topicString = "";
    String getDataUrl = "/AndroidAppRequests/getEventsAndPaymentsJson.php";
    String getPokladnaValueURL = "/AndroidAppRequests/GetJson.php";
    List eventsAndPaymentsList;
    String webURL = "";
    TextView tv_udalosti_platby;
    TextView tv_pokladnaValue;
    SharedPreferences SP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        topicString = "pokladnaU12";
        tv_udalosti_platby = (TextView) findViewById(R.id.udalosti_platby);
        tv_pokladnaValue = (TextView) findViewById(R.id.pokladnaValue);
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        webURL = SP.getString("adress", "");
        if (webURL.isEmpty()) {
            Toast.makeText(this, "Nastavte prosím url webu", Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, MyPreferenceActivity.class);
            startActivity(i);
        }
        getDataUrl = webURL + getDataUrl;
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.INTERNET)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.INTERNET},
                        MY_PERMISSIONS_REQUEST_INTERNET);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        FirebaseMessaging.getInstance().subscribeToTopic(topicString);
        refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume", getDataUrl);
        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        webURL = SP.getString("adress", "");
        getDataUrl = webURL + "/AndroidAppRequests/getEventsAndPaymentsJson.php";
        getPokladnaValueURL = webURL + "/AndroidAppRequests/GetJson.php";
        refreshData();
    }

    public void refreshData() {
        Toast.makeText(MainActivity.this, "Refreshuji data", Toast.LENGTH_SHORT).show();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("view", "view");
        client.post(getDataUrl, params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(MainActivity.this, "Data nebyla načtena, zkontrolujte nastavení a připojení k internetu", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

                String JsonString = parseTextToJSON(responseString);
                Log.d("Mainactivity", JsonString);
                parseJSON(JsonString);
            }
        });

        getPokladnaValueURL = webURL + "/AndroidAppRequests/GetJson.php";
        client.post(getPokladnaValueURL, params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                String s = parseTextToJSON(responseString);
                Log.d("pokladna", responseString);
                parsePokladnaValueJSON(s);
            }
        });
    }

    private String parseTextToJSON(String s) {
        if (s.contains("{")) {
            int zacatek = s.indexOf("{");
            int konec = s.length();
            String text = s.substring(zacatek, konec);
            return text;
        }
        return "";
    }

    private void parseJSON(String parsedJSONString) {

        try {
            tv_udalosti_platby.setText("");
            JSONObject jsonObj = new JSONObject(parsedJSONString);
            JSONArray akcePlatby = jsonObj.getJSONArray("EventsDatesAndPayments");
            for (int i = 0; i < akcePlatby.length(); i++) {
                JSONObject c = akcePlatby.getJSONObject(i);
                String datum = c.getString("datum");
                String parseDatum[] = datum.split("-");
                String parsedDatum = parseDatum[2] + ". " + parseDatum[1] + ". " + parseDatum[0];
                String titulek = c.getString("titulek");
                String cena = c.getString("cena");
                pridejDoTextView(parsedDatum, titulek, cena);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void parsePokladnaValueJSON(String parsedJSONString) {

        try {
            JSONObject jsonObj = new JSONObject(parsedJSONString);
            JSONArray users = jsonObj.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject c = users.getJSONObject(i);
                if (c.getString("surname").equals("Pokladna") && c.getString("name").equals("Pokladna")) {
                    String pokladnaValue = c.getString("balance");
                    Log.d("value", pokladnaValue);
                    tv_pokladnaValue.setText("V pokladně aktuálně je: " + pokladnaValue + " Kč");
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void pridejDoTextView(String datum, String udalost, String cena) {
        tv_udalosti_platby.append("\n " + datum + " " + udalost + " " + cena + "\n\n");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent i = new Intent(this, MyPreferenceActivity.class);
                startActivity(i);
                return true;

            case R.id.action_load:
                refreshData();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }


    }

}
