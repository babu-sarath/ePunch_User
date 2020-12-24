package com.scb.epunch;

import androidx.annotation.NonNull;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.android.material.textfield.TextInputLayout;
import com.scb.epunch.UtilsService.SharedPrefClass;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    EditText email,pwd;
    TextInputLayout text_input_layout_email,text_input_layout_password;
    String emailStr,pwdStr;
    SharedPrefClass sharedPrefClass;
    Button loginBtn;
    RetryPolicy retryPolicy;
    ProgressBar progressBar;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email=findViewById(R.id.email);
        pwd=findViewById(R.id.pwd);
        progressBar=findViewById(R.id.progressBar);
        loginBtn=findViewById(R.id.button);
        text_input_layout_email=findViewById(R.id.text_input_layout_email);
        text_input_layout_password=findViewById(R.id.text_input_layout_password);
        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        progressBar.setVisibility(View.INVISIBLE);
        requestApplicationPermissions();
        sharedPrefClass=new SharedPrefClass(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(prefs.contains("token")){
            //go to dash
            startActivity(new Intent(MainActivity.this, Home.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    //Permission check starts
    private void requestApplicationPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ALL){
            if(grantResults.length==0){
                Toast.makeText(getApplicationContext(),"You cannot use this application without the requested permissions",Toast.LENGTH_LONG).show();
                finishAffinity();
            }
        }
    }
    //Permission check ends

    public void login(View view) {
        emailStr=email.getText().toString();
        pwdStr=pwd.getText().toString();

        if(TextUtils.isEmpty(emailStr) || TextUtils.isEmpty(pwdStr)){
            if(TextUtils.isEmpty(emailStr))
                text_input_layout_email.setError("Enter your email");
            if(TextUtils.isEmpty(pwdStr))
                text_input_layout_password.setError("Enter your password");
        }
        else {
            progressBar.setVisibility(View.VISIBLE);
            email.setEnabled(false);
            pwd.setEnabled(false);
            loginBtn.setEnabled(false);

            HashMap<String, String> data=new HashMap<>();
            data.put("email",emailStr);
            data.put("password",pwdStr);

            String apiKey="https://epunchapp.herokuapp.com/auth/emplogin";

            JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(data),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(response.getBoolean("success")){
                                    sharedPrefClass.setValue_string("token",response.getString("token"));
                                    JSONObject jsonObject=response.getJSONObject("user");
                                    sharedPrefClass.setValue_string("username",jsonObject.getString("name"));
                                    sharedPrefClass.setValue_string("email",jsonObject.getString("email"));
                                    sharedPrefClass.setValue_string("grace",jsonObject.getString("grace"));
                                    sharedPrefClass.setValue_string("phone",jsonObject.getString("phone"));
                                    startActivity(new Intent(MainActivity.this, Home.class));
                                    finish();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressBar.setVisibility(View.INVISIBLE);
                    email.setEnabled(true);
                    pwd.setEnabled(true);
                    loginBtn.setEnabled(true);
                    NetworkResponse response=error.networkResponse;
                    if(error instanceof ServerError && response!=null){
                        try{
                            String res=new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                            JSONObject object=new JSONObject(res);
                            Toast.makeText(getApplicationContext(),object.getString("msg"),Toast.LENGTH_LONG).show();
                        }catch (Exception e){
                            Log.d("SERVER ERROR ",e.getMessage());
                        }
                    }
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String,String> headers=new HashMap<>();
                    headers.put("Content-Type","application/json");
                    return headers;
                }
            };

            //retry the stuff
            jsonObjectRequest.setRetryPolicy(retryPolicy);

            //adding the request
            RequestQueue requestQueue= Volley.newRequestQueue(this);
            requestQueue.add(jsonObjectRequest);
        }

    }

    public void forgotPassword(View view) {
        startActivity(new Intent(this,ForgotPassword.class));
    }

}