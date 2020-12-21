package com.scb.epunch.UtilsService;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefClass {
    private  static  final String USER_PREF="user_prefs_epunch";
    private SharedPreferences appShared;
    private SharedPreferences.Editor prefsEditor;

    public SharedPrefClass(Context context){
        appShared=context.getSharedPreferences(USER_PREF,Context.MODE_PRIVATE);
        this.prefsEditor=appShared.edit();
    }

    //int
    public int getValue_int(String key){
        return appShared.getInt(key,0);
    }

    public void setValue_int(String key, int value){
        prefsEditor.putInt(key,value).commit();
    }

    //string
    public String getValue_string(String key){
        return appShared.getString(key,"");
    }

    public void setValue_string(String key, String value){
        prefsEditor.putString(key,value).commit();
    }

    //boolean
    public boolean getValue_boolean(String key){
        return appShared.getBoolean(key,false);
    }

    public void setValue_boolean(String key, boolean value){
        prefsEditor.putBoolean(key,value).commit();
    }

    public void clear(){
        prefsEditor.clear().commit();
    }

    public void clear_specific_pref(String key){
        prefsEditor.remove(key).commit();
    }
}
