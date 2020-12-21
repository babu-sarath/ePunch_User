package com.scb.epunch.UtilsService;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateConverter {

    public String getDate(Long value){
        DateFormat simple = new SimpleDateFormat("yyyy-MM-dd");
        Date result = new Date(value);
        return simple.format(result);
    }
}
