package com.scb.epunch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
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
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.scb.epunch.Adaptors.AttendanceAdaptor;
import com.scb.epunch.UtilsService.DateConverter;
import com.scb.epunch.UtilsService.SharedPrefClass;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Attendance extends AppCompatActivity {
    SharedPrefClass sharedPrefClass;
    DateConverter dateConverter;
    List<String> id=new ArrayList<>();
    List<String> entry=new ArrayList<>();
    List<String> date =new ArrayList<>();
    List<String> in_time =new ArrayList<>();
    List<String> out_time =new ArrayList<>();
    String[] spinnerData= {"ALL","ONTIME","LATE","HALF-DAY","ABSENT"};
    Spinner spinner;
    RetryPolicy retryPolicy;
    ListView listView;
    ProgressDialog progressDialog;
    MaterialDatePicker materialDatePicker;
    LottieAnimationView notFoundAnimation;
    TextView dateText;
    String startDate,endDate,prefEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        listView=findViewById(R.id.listView);
        dateText=findViewById(R.id.dateText);
        spinner=findViewById(R.id.spinner);
        notFoundAnimation=findViewById(R.id.notFoundAnimation);
        listView.setVisibility(View.GONE);
        notFoundAnimation.setVisibility(View.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item,spinnerData);
        spinner.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        sharedPrefClass=new SharedPrefClass(getApplicationContext());
        dateConverter=new DateConverter();
//        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        retryPolicy=new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        progressDialog=new ProgressDialog(Attendance.this,R.style.CustomAlertDialog);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing Request");

        //Date picker stuff
        CalendarConstraints.Builder calenderConstraintsBuilder=new CalendarConstraints.Builder();
        calenderConstraintsBuilder.setValidator(DateValidatorPointBackward.now());
        MaterialDatePicker.Builder<Pair<Long, Long>> builder= MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select date Range");
        builder.setCalendarConstraints(calenderConstraintsBuilder.build());
        materialDatePicker =builder.build();
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
            @Override public void onPositiveButtonClick(Pair<Long,Long> selection) {
                startDate = dateConverter.getDate(selection.first);
                endDate = dateConverter.getDate(selection.second);
                dateText.setText(String.format("%s to %s", startDate, endDate));
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        prefEmail=sharedPrefClass.getValue_string("email");
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(!prefs.contains("token")){
            startActivity(new Intent(Attendance.this, MainActivity.class));
            finish();
        }else {
            getInfo();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void getInfo() {
        String apiKey = "https://epunchapp.herokuapp.com/view/listAttendance/"+prefEmail;
        id.clear();date.clear();in_time.clear();out_time.clear();entry.clear();progressDialog.show();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        listView.setVisibility(View.VISIBLE);
                        JSONArray jsonArray = response.getJSONArray("data");
                        Log.d("VOLLEY", "success on if");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            id.add(jsonObject.getString("_id"));
                            date.add(jsonObject.getString("date"));
                            in_time.add(jsonObject.getString("in_time"));
                            out_time.add(jsonObject.getString("out_time"));
                            entry.add(jsonObject.getString("entry"));
                        }
                        AttendanceAdaptor attendanceAdaptor = new AttendanceAdaptor(id, entry, date, in_time, out_time, getApplicationContext());
                        attendanceAdaptor.notifyDataSetChanged();
                        listView.setAdapter(attendanceAdaptor);
                        Log.d("VOLLEY", "success on if");
                        progressDialog.dismiss();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (error instanceof ServerError && response != null) {
                    try {
                        notFoundAnimation.setVisibility(View.VISIBLE);
                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                        JSONObject object = new JSONObject(res);
                        Toast.makeText(getApplicationContext(), object.getString("msg"), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        Log.d("SERVER ERROR ", e.getMessage());
                    }
                }
            }
        }) {
            //Caching the json Data
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 15 * 60 * 1000; // in 15 minutes cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 12 hours this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

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

    public void dateFilter(View view) {
        materialDatePicker.show(getSupportFragmentManager(), "Date Picker");
    }

    public void goBack(View view) {
        onBackPressed();
    }

    public void search(View view) {
        String searchDate=dateText.getText().toString();
        if(searchDate.equals("From to To"))
            Toast.makeText(getApplicationContext(),"Please select a date range", Toast.LENGTH_LONG).show();
        else
            searchInfo();
    }

    private void searchInfo() {
        listView.setVisibility(View.GONE);
        String apiKey = "https://epunchapp.herokuapp.com/view/filterListAttendance";
        id.clear();date.clear();in_time.clear();out_time.clear();entry.clear();progressDialog.show();

        HashMap<String,String> data=new HashMap<>();
        data.put("email",prefEmail);
        data.put("startDate",startDate);
        data.put("endDate",endDate);
        data.put("entry",spinner.getSelectedItem().toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(data), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        listView.setVisibility(View.VISIBLE);
                        JSONArray jsonArray = response.getJSONArray("data");
                        Log.d("VOLLEY", "success on if");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            Log.d("VOLLEY", "inside loop");
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            id.add(jsonObject.getString("_id"));
                            date.add(jsonObject.getString("date"));
                            in_time.add(jsonObject.getString("in_time"));
                            out_time.add(jsonObject.getString("out_time"));
                            entry.add(jsonObject.getString("entry"));
                        }
                        AttendanceAdaptor attendanceAdaptor = new AttendanceAdaptor(id, entry, date, in_time, out_time, getApplicationContext());
                        attendanceAdaptor.notifyDataSetChanged();
                        listView.setAdapter(attendanceAdaptor);
                        Log.d("VOLLEY", "success on if");
                        progressDialog.dismiss();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if (error instanceof ServerError && response != null) {
                    try {
                        notFoundAnimation.setVisibility(View.VISIBLE);
                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                        JSONObject object = new JSONObject(res);
                        Toast.makeText(getApplicationContext(), object.getString("msg"), Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        Log.d("SERVER ERROR ", e.getMessage());
                    }
                }
            }
        }) {
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

    public void reset(View view) {
        notFoundAnimation.setVisibility(View.GONE);
        getInfo();
        dateText.setText("From to To");
        spinner.setSelection(0);
    }

    public void reload(View view) {
        notFoundAnimation.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);
        getInfo();
    }
}