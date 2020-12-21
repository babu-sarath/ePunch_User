package com.scb.epunch.Adaptors;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.scb.epunch.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AttendanceAdaptor extends BaseAdapter {
    List<String> id=new ArrayList<>();
    List<String> entry=new ArrayList<>();
    List<String> date =new ArrayList<>();
    List<String> in_time =new ArrayList<>();
    List<String> out_time =new ArrayList<>();
    Integer[] icon={R.drawable.ic_present,R.drawable.ic_late,R.drawable.ic_half,R.drawable.ic_absent};
    LayoutInflater inflater;
    private Context context;

    public AttendanceAdaptor(List<String> id, List<String> entry, List<String> date, List<String> in_time, List<String> out_time, Context context) {
        this.id = id;
        this.entry = entry;
        this.date = date;
        this.in_time = in_time;
        this.out_time = out_time;
        this.context = context;
        inflater=(LayoutInflater.from(context));
    }

    @Override
    public int getCount() {
        return id.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView=inflater.inflate(R.layout.list_attendance,null);
        TextView idTv=convertView.findViewById(R.id.id);
        TextView dateTv=convertView.findViewById(R.id.date);
        TextView checkinTv=convertView.findViewById(R.id.checkin);
        TextView checkoutTv=convertView.findViewById(R.id.checkout);
        TextView type=convertView.findViewById(R.id.type);
        ImageView imageIv=convertView.findViewById(R.id.image);
        idTv.setText(id.get(position));
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
        try {
            Date dateObj = inputFormat.parse(date.get(position));
            String formattedDate = outputFormat.format(dateObj);
            dateTv.setText(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        checkinTv.setText(in_time.get(position));
        checkoutTv.setText(out_time.get(position));
        switch(entry.get(position).toUpperCase()){
            case "ONTIME":imageIv.setImageResource(icon[0]);
            break;
            case "LATE":imageIv.setImageResource(icon[1]);
            break;
            case "HALF-DAY":imageIv.setImageResource(icon[2]);
            break;
            case "ABSENT":imageIv.setImageResource(icon[3]);
            break;
        }
        type.setText(entry.get(position).toUpperCase());
        return convertView;
    }
}
