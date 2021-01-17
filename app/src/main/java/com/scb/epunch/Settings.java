package com.scb.epunch;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.scb.epunch.UtilsService.SharedPrefClass;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Settings extends AppCompatActivity {
    SharedPrefClass sharedPrefClass;
    TextView name,email,phone;
    String prefEmail;
    RetryPolicy retryPolicy;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPrefClass=new SharedPrefClass(this);
        name=findViewById(R.id.name);
        email=findViewById(R.id.email);
        phone=findViewById(R.id.phone);
//        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        retryPolicy=new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        prefEmail=sharedPrefClass.getValue_string("email");
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs=getSharedPreferences("user_prefs_epunch", Activity.MODE_PRIVATE);
        if(!prefs.contains("token")){
            startActivity(new Intent(Settings.this, MainActivity.class));
            finish();
        }
        name.setText(sharedPrefClass.getValue_string("username"));
        phone.setText(sharedPrefClass.getValue_string("phone"));
        email.setText(prefEmail);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void goBack(View view) {
        onBackPressed();
    }

    public void logout(View view) {
        sharedPrefClass.clear();
        startActivity(new Intent(Settings.this, MainActivity.class));
        finish();
    }

    public void changePwd(View view) {
        startActivity(new Intent(this,ChangePassword.class));
    }

    public void report(View view) {
        final View layout=getLayoutInflater().inflate(R.layout.send_report,null);
        EditText titleEt=layout.findViewById(R.id.title);
        EditText msgEt=layout.findViewById(R.id.msg);
        Button submit=layout.findViewById(R.id.submit);
        Button cancel=layout.findViewById(R.id.cancel);
        ProgressBar progressBar=layout.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setMessage("Any suggestions or bugs found? Feel free to send a report.")
                .setTitle("Send Report")
                .setView(layout);
        AlertDialog alertDialog=builder.create();
        alertDialog.show();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title,msg;
                title=titleEt.getText().toString();
                msg=msgEt.getText().toString();
                if(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(msg)){
                    sendReportVolleyReq(title,msg,alertDialog,progressBar,titleEt,msgEt,submit,cancel);
                }else {
                    if(TextUtils.isEmpty(title))
                        titleEt.setError("Do not leave this empty");
                    else
                        msgEt.setError("Do not leave this empty");
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    private void sendReportVolleyReq(String title, String msg, AlertDialog alertDialog, ProgressBar progressBar, EditText titleEt, EditText msgEt, Button submit, Button cancel) {
        titleEt.setEnabled(false);
        msgEt.setEnabled(false);
        submit.setEnabled(false);
        cancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        HashMap<String, String> data=new HashMap<>();
        data.put("email",prefEmail);
        data.put("title",title);
        data.put("message",msg);

        String apiKey="https://epunchapp.herokuapp.com/report";

        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(data),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Toast.makeText(getApplicationContext(),response.getString("msg"),Toast.LENGTH_LONG).show();
                            alertDialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.INVISIBLE);
                titleEt.setEnabled(true);
                msgEt.setEnabled(true);
                submit.setEnabled(true);
                cancel.setEnabled(true);
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

    public void editPhone(View view) {
        final EditText phoneNo=new EditText(this);
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setMessage("Update your phone number here.")
                .setTitle("Update")
                .setView(phoneNo);
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }
}