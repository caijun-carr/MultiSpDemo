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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i("ccc", "server onSharedPreferenceChanged sp : " +
                sharedPreferences.getInt("value", -1) + "    key : " + sharedPreferences.getInt(key, -1));
    }
}
