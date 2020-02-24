package io.turntotech.android.digitalleashparent;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    //Declare variables
    private FusedLocationProviderClient mFusedLocationClient;

    TextView txtViewParentLoc;
    Button btnSave;
    ListView listViewChild;
    SharedPreferences sharedPrefs;

    //linear layout
    TextView txtViewParentLat;
    TextView txtViewParentLong;
    Button btnUpdate;

    //linear layout3
    TextView txtViewParentUserName;
    EditText editTextParentUserName;

    //linear layout2
    TextView txtViewRadius;
    EditText editTextRadius;

    //linear layout4
    EditText editTextAddChild;
    Button btnAdd;

    List<String> childrenUserNames;
    ArrayAdapter arrayAdapter;

    double distanceInMiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
        readFromSharedPref();
        updateLastLocation();
    }

    private void readFromSharedPref() {

        sharedPrefs = getSharedPreferences("parentInfo", MODE_PRIVATE);

        String parentUsername = sharedPrefs.getString("Parent Username", "");
        String parentRadius = sharedPrefs.getString("Radius", "");
        Set<String> children = sharedPrefs.getStringSet("Child Username", new HashSet<String>());

        editTextParentUserName.setText(parentUsername);
        editTextRadius.setText(parentRadius);

        childrenUserNames.addAll(children);
        arrayAdapter.notifyDataSetChanged();
    }

    private void setup() {

        txtViewParentLoc = (TextView) findViewById(R.id.txtViewParentLoc);
        txtViewParentLat = (TextView) findViewById(R.id.txtViewParentLat);
        txtViewParentLong = (TextView) findViewById(R.id.txtViewParentLong);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);
        txtViewParentUserName = (TextView) findViewById(R.id.txtViewParentUserName);
        editTextParentUserName = (EditText) findViewById(R.id.editTextParentUserName);
        txtViewRadius = (TextView) findViewById(R.id.txtViewRadius);
        editTextRadius = (EditText) findViewById(R.id.editTextRadius);
        btnSave = (Button) findViewById(R.id.btnSave);
        editTextAddChild = (EditText) findViewById(R.id.editTextAddChild);
        btnAdd = (Button) findViewById(R.id.btnAdd);


        childrenUserNames = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, childrenUserNames);


        listViewChild = (ListView) findViewById(R.id.listViewChild);
        listViewChild.setAdapter(arrayAdapter);


        //Select and deleting child from list using long button press
        listViewChild.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                childrenUserNames.remove(position);
                arrayAdapter.notifyDataSetChanged();
                Toast.makeText(getBaseContext(), "Child Removed", Toast.LENGTH_LONG).show();

                return true;

            }
        });

        //Select child to retrieve location information
        listViewChild.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                GetServerData getServerData = new GetServerData();

                String child_username = childrenUserNames.get(position);

                getServerData.execute(child_username);
            }
        });
    }


    //Get current location
    public void updateLastLocation() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Need Location Permission", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.

                        if (location != null) {
                            // Logic to handle location object

                            location.getLatitude();
                            location.getLongitude();

                            txtViewParentLat.setText(Double.toString(location.getLatitude()));
                            txtViewParentLong.setText(Double.toString(location.getLongitude()));
                        }
                    }
                });
    }

    //"Update" button click
    public void onUpdate(View view) {
        Toast.makeText(this, "Your Current Location", Toast.LENGTH_LONG).show();
        updateLastLocation();
    }

    //"Save" button click
    public void onSave(View view) {

        String parentUserName = editTextParentUserName.getText().toString();
        String radius = editTextRadius.getText().toString();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("Parent Username", parentUserName);
        editor.putString("Radius", radius);

        Set<String> children = new HashSet<>();
//        for(int i=0;i<childrenUserNames.size();i++){
//            String childName = childrenUserNames.get(i);
//        }
        for (String childName : childrenUserNames) { // foreach loop
            children.add(childName);
        }

        editor.putStringSet("Child Username", children);
        editor.commit();

        //hide keyboard
        hideKeyboard(getBaseContext(), view);
        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }

    //"Add Child" button click
    public void onAdd(View view) {

        //add elements to List
        childrenUserNames.add(editTextAddChild.getText().toString());
        arrayAdapter.notifyDataSetChanged();

        Toast.makeText(this, "Child Added", Toast.LENGTH_LONG).show();
        editTextAddChild.setText("");

        //hide keyboard
        hideKeyboard(getBaseContext(), view);
    }


    //Get JSON data from server
    public void getJsonData(String child_username) throws IOException, JSONException {

        String parent_username = "" + editTextParentUserName.getText();
        String urlString = "https://turntotech.firebaseio.com/digitalleash/users/" + parent_username + "/" + child_username + ".json";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /*milliseconds*/);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        int responseCode = conn.getResponseCode();

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader buffered = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;
        StringBuffer response = new StringBuffer();

        while ((output = buffered.readLine()) != null) {
            response.append(output);
        }
        conn.disconnect();

        //print result
        System.out.println(response.toString());

        parseAndPrintJson(response.toString());
    }

    //Parse and print JSON data
    public void parseAndPrintJson(String response) throws JSONException {

//        try {
        JSONObject jsonObject = new JSONObject(response);

        StringBuilder stringBuild = new StringBuilder();

        double latitude = jsonObject.getDouble("latitude");
        double longitude = jsonObject.getDouble("longitude");
        double timestamp = jsonObject.getDouble("timestamp");

        stringBuild.append("\nlatitude: " + latitude);
        stringBuild.append("  longitude: " + longitude);
        stringBuild.append(" timestamp: " + timestamp);

        System.out.println(latitude + longitude + timestamp);

        //Parent Latitude and Longitude text to string
        String parLat = txtViewParentLat.getText().toString();
        String parLong = txtViewParentLong.getText().toString();

        //Parent Latitude and Longitude String to double
        double parentLat = Double.parseDouble(parLat);
        double parentLong = Double.parseDouble(parLong);

        //Child to parent distance
        Location myLocation = new Location("");
        myLocation.setLatitude(parentLat);
        myLocation.setLongitude(parentLong);

        Location targetLocation = new Location("");
        targetLocation.setLatitude(latitude);
        targetLocation.setLongitude(longitude);

        double distanceInMeters = targetLocation.distanceTo(myLocation);
        distanceInMiles = distanceInMeters * 0.000621;

        System.out.println(distanceInMiles);
    }

    //Check if child is within radius of parent then display correct activity screen
    public void childParentRange(double distanceInMiles) {

        String rad = (editTextRadius.getText().toString());
        double range = Double.parseDouble(rad);

        if (distanceInMiles <= range) {
            Intent withinRange = new Intent(MainActivity.this, ActivityGreen.class);
            startActivity(withinRange);

        } else {
            Intent outOfRange = new Intent(MainActivity.this, ActivityRed.class);
            startActivity(outOfRange);
        }
    }

    public static void hideKeyboard(Context context, View view){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public class GetServerData extends AsyncTask<String, String, Boolean> {

        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(String... child_username) {

            try {
                getJsonData(child_username[0]);
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {

            // do something UI
            if (result) {
                childParentRange(distanceInMiles);
            } else {
                Toast.makeText(MainActivity.this.getBaseContext(), "Error in parsing", Toast.LENGTH_LONG).show();
            }
        }
    }
}



