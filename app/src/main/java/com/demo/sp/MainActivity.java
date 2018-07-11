package com.demo.sp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.demo.sp.sp.CSharedPreferences;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sp = CSharedPreferences.getSharedPreferences(this, "demo_sp", CSharedPreferences.MODE_MULTI_PROCESS);
        Log.i("ccc", "main sp : " + sp.getInt("value", -1));

        sp.registerOnSharedPreferenceChangeListener(this);
        Intent intent = new Intent(this, ProcessService.class);
        startService(intent);
        findViewById(R.id.text_view).postDelayed(new Runnable() {
            @Override
            public void run() {
                CSharedPreferences.getSharedPreferences(MainActivity.this, "demo_sp", CSharedPreferences.MODE_MULTI_PROCESS)
                        .edit().putInt("value", 5).apply();
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CSharedPreferences.getSharedPreferences(this, "demo_sp", CSharedPreferences.MODE_MULTI_PROCESS)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i("ccc", "main onSharedPreferenceChanged sp : " +
                sharedPreferences.getInt("value", -1) + "    key : " + sharedPreferences.getInt(key, -1));
    }
}
