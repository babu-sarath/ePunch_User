package com.scb.epunch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.Result;
import com.scb.epunch.UtilsService.SharedPrefClass;
import com.scb.epunch.UtilsService.VerifyLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

public class Home extends AppCompatActivity {
    CodeScannerView scannerView;
    TextView name, punchIn, punchOut,latitudeTV,longitudeTV,grace;
    SharedPrefClass sharedPrefClass;
    private CodeScanner mCodeScanner;
    DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest request;
    private LocationCallback locationCallback;
    boolean fusedLocationStatus;
    RetryPolicy retryPolicy;
    ProgressDialog progressDialog;
    String prefEmail;
    LinearLayout graceCard;
    private static final String officeLatA = "12.952";
    private static final String officeLongA = "77.638";
    private static final String officeLatB = "12.953";
    private static final String officeLongB = "77.639";
    VerifyLocation verifyLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sharedPrefClass=new SharedPrefClass(this);
        scannerView = findViewById(R.id.scanner_view);
        name = findViewById(R.id.name);
        punchIn = findViewById(R.id.punchIn);
        punchOut = findViewById(R.id.punchOut);
        latitudeTV = findViewById(R.id.latitudeTV);
        longitudeTV = findViewById(R.id.longitudeTV);
        graceCard = findViewById(R.id.graceCard);
        grace = findViewById(R.id.grace);

        verifyLocation=new VerifyLocation();
        mCodeScanner = new CodeScanner(this, scannerView);
        setScannerHeight();
        progressDialog=new ProgressDialog(Home.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Fetching location...");
        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        //initialize the fused location provider
        fusedLocationStatus=false;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        prefEmail=sharedPrefClass.getValue_string("email");
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(!prefs.contains("token")){
            startActivity(new Intent(Home.this, MainActivity.class));
            finish();
        }else {
//            if(verifyLocation.areThereMockPermissionApps(Home.this)){
//                AlertDialog.Builder builder=new AlertDialog.Builder(this);
//                builder.setCancelable(false)
//                        .setMessage("Cannot use this application when there is a mock location application running in the background!")
//                        .setTitle("Alert!")
//                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//                                finishAffinity();
//                            }
//                        });
//                AlertDialog alertDialog=builder.create();
//                alertDialog.show();
//            }
//            else {
                name.setText(sharedPrefClass.getValue_string("username"));
                getGrace();
                checkNewDay();
                getScanTimes();
                checkLocationEnabled();
//            }

        }

    }

    private void getGrace() {
        String apiKey="https://epunchapp.herokuapp.com/auth/grace/"+prefEmail;
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, apiKey, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")){
                                int graceInt=Integer.parseInt(response.getString("grace"));
                                setGrace(graceInt);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response=error.networkResponse;
                if(error instanceof ServerError && response!=null){
                    try{
                        String res=new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                        JSONObject object=new JSONObject(res);
                        Toast.makeText(getApplicationContext(),object.getString("msg"),Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }catch (Exception e){
                        Log.d("SERVER ERROR ",e.getMessage());
                    }
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorized", sharedPrefClass.getValue_string("token"));
                return headers;
            }
        };

        jsonObjectRequest.setRetryPolicy(retryPolicy);
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        requestQueue.add(jsonObjectRequest);

    }

    private void setGrace(int graceInt) {
        if(graceInt==0 || graceInt==1)
            graceCard.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.grace_red));
        else if(graceInt==2)
            graceCard.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.grace_orange));
        else
            graceCard.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.grace_green));
        grace.setText(String.valueOf(graceInt));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCodeScanner.releaseResources();
        if (fusedLocationStatus) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    private void setScannerHeight() {
        //code to set scanner-height based on phone height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int scannerHeight = (height * 50) / 100;
        scannerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, scannerHeight));
    }

    private void checkNewDay() {
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        String currentDateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        if(prefs.contains("dateRecord")){

            try {
                Date recordedDate=new SimpleDateFormat("dd/MM/yyyy").parse(sharedPrefClass.getValue_string("dateRecord"));
                Date currentDate=new SimpleDateFormat("dd/MM/yyyy").parse(currentDateStr);
                Calendar calOld=Calendar.getInstance();
                Calendar calNew=Calendar.getInstance();
                assert recordedDate != null;
                calOld.setTime(recordedDate);
                assert currentDate != null;
                calNew.setTime(currentDate);
                if(calNew.get(Calendar.MONTH) > calOld.get(Calendar.MONTH)){
                        //new month
                        refreshGrace();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if(!sharedPrefClass.getValue_string("dateRecord").equals(currentDateStr)){
                punchIn.setText("--:--");
                punchOut.setText("--:--");
                sharedPrefClass.setValue_string("dateRecord",currentDateStr);
            }
        }else
            sharedPrefClass.setValue_string("dateRecord",currentDateStr);   //if app open first time after install, this code runs only once in lifetime
    }

    private void refreshGrace() {
        String apiKey="https://epunchapp.herokuapp.com/auth/updateGrace/"+prefEmail;
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, apiKey, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")){
                                int graceInt=Integer.parseInt(response.getString("grace"));
                                setGrace(graceInt);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response=error.networkResponse;
                if(error instanceof ServerError && response!=null){
                    try{
                        String res=new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                        JSONObject object=new JSONObject(res);
                        Toast.makeText(getApplicationContext(),object.getString("msg"),Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }catch (Exception e){
                        Log.d("SERVER ERROR ",e.getMessage());
                    }
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorized", sharedPrefClass.getValue_string("token"));
                return headers;
            }
        };

        jsonObjectRequest.setRetryPolicy(retryPolicy);
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        requestQueue.add(jsonObjectRequest);
    }

    private void getScanTimes() {
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(prefs.contains("scanIn"))
            punchIn.setText(sharedPrefClass.getValue_string("scanIn"));
        if(prefs.contains("scanOut"))
            punchOut.setText(sharedPrefClass.getValue_string("scanOut"));
    }

    private void checkLocationEnabled() {
        request=new LocationRequest()
                .setFastestInterval(200)
                .setInterval(500)
                .setPriority(PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder locationBuilder=new LocationSettingsRequest.Builder().addLocationRequest(request);
        Task<LocationSettingsResponse> result=LocationServices.getSettingsClient(this).checkLocationSettings(locationBuilder.build());
        result.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize location
                // requests here.
                //get current location
                getCurrentLocation();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(e instanceof ResolvableApiException){
                    ResolvableApiException apiException=(ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(Home.this,1001);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null){
            if(resultCode==RESULT_OK){
                //get current location
                getCurrentLocation();
            }else if(resultCode==RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Cannot use application without location permission",Toast.LENGTH_LONG).show();
                finishAffinity();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        }else{
            progressDialog.show();
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LocationServices.getFusedLocationProviderClient(Home.this)
                            .removeLocationUpdates(this);
                    if (locationResult != null && locationResult.getLocations().size() > 0) {
                        int latestLocationIndex = locationResult.getLocations().size() - 1;
                        double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                        double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                        String lat = Double.toString(latitude);
                        String log = Double.toString(longitude);
                        latitudeTV.setText(lat);
                        longitudeTV.setText(log);
                        progressDialog.dismiss();
                    }
                }
            };
            fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            fusedLocationStatus=true;
            runScanner();
        }
    }

    private void runScanner(){
        mCodeScanner.startPreview();
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scanTime(result.getText());
                    }
                });
            }
        });

    }

    private void scanTime(String text) {
        if(text.equals("Imagic Punch-QR")){
            vibrateDevice();
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            if(checkScanLocation()) {
                String result=checkOfficeTimings(currentTime);
                if(result!=null){
                    sendVolleyInsertRequest();
                }
            }else {
                createAlert("You are not near office premises! Cannot Scan");
            }
        }else {
            Toast.makeText(getApplicationContext(),"Invalid QR code!",Toast.LENGTH_LONG).show();
            mCodeScanner.startPreview();
        }
    }

    private void vibrateDevice() {
        Vibrator vibrator=(Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            assert vibrator != null;
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        else {
            assert vibrator != null;
            vibrator.vibrate(100);
        }
    }

    private void createAlert(String msg) {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setMessage(msg)
                .setTitle("Alert!")
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mCodeScanner.startPreview();
                    }
                });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }

    private boolean checkScanLocation() {
        String lat,lon,approxLatitude,approxLongitude;
        lat=latitudeTV.getText().toString();
        lon=longitudeTV.getText().toString();
        approxLatitude=lat.substring(0,6);
        approxLongitude=lon.substring(0,6);
        return approxLatitude.equals(officeLatA) && approxLongitude.equals(officeLongA) || approxLatitude.equals(officeLatB) && approxLongitude.equals(officeLongB);
    }

    private String checkOfficeTimings(String currentTime) {
        Date morning, morningLimit, halfDayLimit, earlyMorning, scanTime;
        try {
            earlyMorning = formatter.parse("06:00:00");
            morning = formatter.parse("09:40:00");
            morningLimit = formatter.parse("11:00:00");
            halfDayLimit = formatter.parse("14:30:00");
            scanTime = formatter.parse(currentTime);

            if(punchIn.getText().toString().equals("--:--")){
                assert scanTime != null;
                //it is punchin
                if(scanTime.after(earlyMorning)&&scanTime.before(morning)){
                    //on time
                    punchIn.setText(currentTime);
                    sharedPrefClass.setValue_string("scanIn",currentTime);
                    return "ONTIME";
                }else  if(scanTime.after(morning)&&scanTime.before(morningLimit)){
                    //late
                    punchIn.setText(currentTime);
                    sharedPrefClass.setValue_string("scanIn",currentTime);
                    int graceInt=Integer.parseInt(grace.getText().toString());
                    if(graceInt==0)
                        setGrace(3);
                    else
                        setGrace(graceInt-1);
                    return "LATE";
                }else  if(scanTime.after(morningLimit)&&scanTime.before(halfDayLimit)){
                    //half day
                    punchIn.setText(currentTime);
                    sharedPrefClass.setValue_string("scanIn",currentTime);
                    return "HALF-DAY";
                }else
                    //abscent
                    return "ABSENT";

            }else {
                //it is punchout
                sharedPrefClass.setValue_string("scanOut",currentTime);
                punchOut.setText(currentTime);
                return "EXIT";
            }
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private void sendVolleyInsertRequest() {
        String apiKey="https://epunchapp.herokuapp.com/punches/create";
        HashMap<String,String> data=new HashMap<>();
        data.put("email",prefEmail);
        data.put("username",sharedPrefClass.getValue_string("username"));
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(data), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getBoolean("success")){
                        progressDialog.dismiss();
                        createAlert(response.getString("msg"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(),error.getMessage(),Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers=new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorized",sharedPrefClass.getValue_string("token"));
                return headers;
            }
        };
        //retry the stuff
        jsonObjectRequest.setRetryPolicy(retryPolicy);

        //adding the request
        RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(jsonObjectRequest);
    }

    public void showAttendance(View view) {
        startActivity(new Intent(Home.this,Attendance.class));
    }

    public void showSettings(View view) {
        startActivity(new Intent(Home.this,Settings.class));
    }
}