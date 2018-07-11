package com.demo.sp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class NextActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, NextActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        SharedPreferences sp = getSharedPreferences("demo_sp", Context.MODE_PRIVATE);
        Log.i("ccc", "next sp : " + sp.hashCode());
    }
}
