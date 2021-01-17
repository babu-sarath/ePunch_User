package com.scb.epunch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import com.scb.epunch.UtilsService.SharedPrefClass;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPassword extends AppCompatActivity {
    EditText email;
    ProgressBar progressBar;
    SharedPrefClass sharedPrefClass;
    RetryPolicy retryPolicy;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        email=findViewById(R.id.email);
        progressBar=findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
//        retryPolicy=new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        retryPolicy=new DefaultRetryPolicy(0, -1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        sharedPrefClass=new SharedPrefClass(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void goBack(View view) {
        onBackPressed();
    }

    public void forgotPassword(View view) {
        view.setEnabled(false);
        String emailStr=email.getText().toString();
        if(TextUtils.isEmpty(emailStr))
            email.setError("Fill in this field");
        else {
            email.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            String apiKey="https://epunchapp.herokuapp.com/reset/forgot/"+emailStr;
            JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET, apiKey, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("VOLLEY",response.toString());
                            try {
                                if(response.getBoolean("success")){
//                                    Toast.makeText(getApplicationContext(),response.getString("msg"),Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(ForgotPassword.this,MailSuccess.class));
                                    finish();
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
                            view.setEnabled(true);
                            String res=new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                            JSONObject object=new JSONObject(res);
                            Toast.makeText(getApplicationContext(),object.getString("msg"),Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.INVISIBLE);
                            email.setEnabled(true);
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
                    return headers;
                }
            };

            jsonObjectRequest.setRetryPolicy(retryPolicy);
            RequestQueue requestQueue = Volley.newRequestQueue(ForgotPassword.this);

            requestQueue.add(jsonObjectRequest);
        }
    }
}