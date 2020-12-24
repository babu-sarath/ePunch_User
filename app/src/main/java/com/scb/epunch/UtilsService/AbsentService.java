package com.scb.epunch.UtilsService;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.scb.epunch.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AbsentService extends Worker {
    SharedPrefClass sharedPrefClass;
    public AbsentService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Date currentTimeDate,limit;
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("HH:mm:ss");
        try {
            currentTimeDate=simpleDateFormat.parse(currentTime);
            limit=simpleDateFormat.parse("15:00:00");
            if(currentTimeDate.after(limit)){
                Log.d("Work Task","Did job");
//                sendVolleyRequest();
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("com.scb.epunch", "com.scb.epunch", NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(channel);
                }
                NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "com.scb.epunch")
                        .setContentTitle("ePunch")
                        .setContentText("Seems like you have forgot to punch in today! Please mark yourself absent")
                        .setSmallIcon(R.mipmap.ic_launcher);
                notificationManager.notify(1897, notification.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success();
    }

    private void sendVolleyRequest() {
        String email=sharedPrefClass.getValue_string("email");
        String apiKey="https://epunchapp.herokuapp.com/punches/markAbsent/"+email;
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("com.scb.epunch", "com.scb.epunch", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, apiKey, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")){
                                NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "com.scb.epunch")
                                        .setContentTitle("ePunch")
                                        .setContentText(response.getString("msg"))
                                        .setSmallIcon(R.mipmap.ic_launcher);
                                notificationManager.notify(1897, notification.build());
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
                        Log.d("ABSENTSERVICE ERROR",object.getString("msg"));
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

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        requestQueue.add(jsonObjectRequest);
    }
}
