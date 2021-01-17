package com.scb.epunch;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

public class ChangePassword extends AppCompatActivity {
    RetryPolicy retryPolicy;
    SharedPrefClass sharedPrefClass;
    EditText oldPwdEt,newPwdEt;
    ProgressBar progressBar;
    String prefEmail;
    TextInputLayout text_input_layout_old,text_input_layout_new;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        oldPwdEt=findViewById(R.id.oldPwd);
        newPwdEt=findViewById(R.id.newPwd);
        progressBar=findViewById(R.id.progressBar);
        text_input_layout_old=findViewById(R.id.text_input_layout_old);
        text_input_layout_new=findViewById(R.id.text_input_layout_new);
        progressBar.setVisibility(View.INVISIBLE);
        sharedPrefClass=new SharedPrefClass(this);
//        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        retryPolicy=new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        prefEmail=sharedPrefClass.getValue_string("email");
    }

    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(!prefs.contains("token")){
            startActivity(new Intent(ChangePassword.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void goBack(View view) {
        onBackPressed();
    }

    public void resetPassword(View view) {
        String oldpwd,newpwd;
        oldpwd=oldPwdEt.getText().toString();
        newpwd=newPwdEt.getText().toString();
        if(TextUtils.isEmpty(oldpwd) || TextUtils.isEmpty(newpwd)){
            if(TextUtils.isEmpty(oldpwd))
                text_input_layout_old.setError("Enter your your old password");
            if(TextUtils.isEmpty(newpwd))
                text_input_layout_new.setError("Enter you your new password");
        }
        else {
            if(oldpwd.equals(newpwd)){
                text_input_layout_old.setError("Enter different passwords");
                text_input_layout_new.setError("Enter different passwords");
            }
            else {
                view.setEnabled(false);
                sendPwdResetVolleyReq(oldpwd, newpwd,view);
            }
        }
    }

    private void sendPwdResetVolleyReq(String oldPwd, String newPwd, View view) {
        oldPwdEt.setEnabled(false);
        newPwdEt.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        HashMap<String, String> data=new HashMap<>();
        data.put("email",prefEmail);
        data.put("oldPwd",oldPwd);
        data.put("newPwd",newPwd);

        String apiKey="https://epunchapp.herokuapp.com/auth/passwordUpdate";

        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(data),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(getApplicationContext(),response.getString("msg"),Toast.LENGTH_LONG).show();
                            onBackPressed();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.INVISIBLE);
                oldPwdEt.setEnabled(true);
                newPwdEt.setEnabled(true);
                NetworkResponse response=error.networkResponse;
                if(error instanceof ServerError && response!=null){
                    try{
                        view.setEnabled(true);
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
}