package com.demo.sp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.demo.sp.sp.CSharedPreferences;

public class ProcessService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{
    public ProcessService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("ccc", "server onCreate");
        CSharedPreferences.getSharedPreferences(this, "demo_sp", Context.MODE_MULTI_PROCESS)
                .registerOnSharedPreferenceChangeListener(this);
        Log.i("ccc", "server onCreate" + CSharedPreferences.getSharedPreferences(this, "demo_sp", Context.MODE_MULTI_PROCESS).getAll().toString());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int value = super.onStartCommand(intent, flags, startId);
        Log.i("ccc", "onStartCommand intent : " + intent.toString());
        if (null != intent && null != intent.getStringExtra("key") && intent.getStringExtra("key").equalsIgnoreCase("add")) {
            SharedPreferences sp = CSharedPreferences.getSharedPreferences(this, "demo_sp", Context.MODE_MULTI_PROCESS);
            sp.edit().putInt("value", sp.getInt("value", -1) + 1).apply();
        }
        return value;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i("ccc", "server onSharedPreferenceChanged sp : " +
                sharedPreferences.getInt("value", -1) + "    key : " + sharedPreferences.getInt(key, -1));
    }
}
