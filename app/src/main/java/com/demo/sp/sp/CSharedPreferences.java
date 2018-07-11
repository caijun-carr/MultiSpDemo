package com.demo.sp.sp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.demo.sp.sp.SPConstant.NULL_STRING;
import static com.demo.sp.sp.SPConstant.SEPARATOR;
import static com.demo.sp.sp.SPConstant.TYPE_INT;

/**
 * 功能：
 * 作者： caijun
 * 时间： 2018/6/15  16 : 24
 */
public class CSharedPreferences implements SharedPreferences {

    public static final int MODE_MULTI_PROCESS = Context.MODE_MULTI_PROCESS;

    private volatile static CSharedPreferences sInstance;

    private Context mContext;
    private CContentObserver mObserver;
    private HashMap<SharedPreferences.OnSharedPreferenceChangeListener, Handler> mListeners = new HashMap<>();
    private HandlerThread mObserverThread;
    private volatile boolean mRegisteredContentObserver = false;

    private CSharedPreferences(Context context) {
        mContext = context.getApplicationContext();
    }

    public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
        if (mode != MODE_MULTI_PROCESS) {
            return context.getSharedPreferences(name, mode);
        }
        if (null == sInstance) {
            synchronized (CSharedPreferences.class) {
                if (null == sInstance) {
                    sInstance = new CSharedPreferences(context);
                }
            }
        }
        return sInstance;
    }

    @Override
    public Map<String, ?> getAll() {
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = Uri.parse(SPConstant.CONTENT_URI + SPConstant.SEPARATOR + SPConstant.TYPE_GET_ALL);
        Cursor cursor = cr.query(uri,null,null,null,null);
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(SPConstant.CURSOR_COLUMN_NAME);
            int typeIndex = cursor.getColumnIndex(SPConstant.CURSOR_COLUMN_TYPE);
            int valueIndex = cursor.getColumnIndex(SPConstant.CURSOR_COLUMN_VALUE);
            do {
                String key = cursor.getString(nameIndex);
                String type = cursor.getString(typeIndex);
                Object value = null;
                if (type.equalsIgnoreCase(SPConstant.TYPE_STRING)) {
                    value= cursor.getString(valueIndex);
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_BOOLEAN)) {
                    value= cursor.getString(valueIndex);
                } else if (type.equalsIgnoreCase(TYPE_INT)) {
                    value= cursor.getInt(valueIndex);
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_LONG)) {
                    value= cursor.getLong(valueIndex);
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_FLOAT)) {
                    value= cursor.getFloat(valueIndex);
                }
                resultMap.put(key,value);
            }
            while (cursor.moveToNext());
            cursor.close();
        }
        return resultMap;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        String rtn = getSingleValue(SPConstant.TYPE_STRING, key);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defValue;
        }
        return rtn;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        throw new UnsupportedOperationException("CSharedPreferences don't support getStringSet now!");
    }

    @Override
    public int getInt(String key, int defValue) {
        String rtn = getSingleValue(SPConstant.TYPE_INT, key);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defValue;
        }
        return Integer.parseInt(rtn);
    }

    @Override
    public long getLong(String key, long defValue) {
        String rtn = getSingleValue(SPConstant.TYPE_LONG, key);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defValue;
        }
        return Long.parseLong(rtn);
    }

    @Override
    public float getFloat(String key, float defValue) {
        String rtn = getSingleValue(SPConstant.TYPE_FLOAT, key);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defValue;
        }
        return Float.parseFloat(rtn);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        String rtn = getSingleValue(SPConstant.TYPE_BOOLEAN, key);
        if (rtn == null || rtn.equals(NULL_STRING)) {
            return defValue;
        }
        return Boolean.parseBoolean(rtn);
    }

    @Override
    public boolean contains(String key) {
        String rtn = getSingleValue(SPConstant.TYPE_CONTAIN, key);
        return !(rtn == null || rtn.equals(NULL_STRING)) && Boolean.parseBoolean(rtn);
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    @Override
    public synchronized void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (null == listener) {
            return;
        }
        if (!mListeners.containsKey(listener)) {
            Handler handler = null;
            final Looper looper = Looper.myLooper();
            if (looper != null) {
                handler = new Handler(looper);
            }
            //noinspection ConstantConditions
            mListeners.put(listener, handler);
        }
        Collection<OnSharedPreferenceChangeListener> listeners = mListeners.keySet();

        if (listeners.size() == 1) {
            mObserverThread = new HandlerThread("observer") {
                @Override
                protected void onLooperPrepared() {
                    super.onLooperPrepared();
                    mObserver = new CContentObserver(new Handler(getLooper()));

                    mContext.getContentResolver()
                            .registerContentObserver(Uri.parse(SPConstant.CONTENT_URI), true, mObserver);
                    mRegisteredContentObserver = true;
                }
            };
            mObserverThread.start();

            // wait synchronously until the mObserverThread registered the mObserver
            // cannot use Thread.join(); because the Looper of the HandlerThread runs forever until killed
            while (true) {
                if (mRegisteredContentObserver) {
                    mRegisteredContentObserver = false;
                    break;
                }
            }
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (null == listener) {
            return;
        }
        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            // cleanup
            mObserver = null;
            mObserverThread.quit();
            mObserverThread = null;
        }
    }

    private String getSingleValue(String type, String key) {
        ContentResolver cr = mContext.getContentResolver();
        Uri uri = Uri.parse(SPConstant.CONTENT_URI + SPConstant.SEPARATOR +
                type + SPConstant.SEPARATOR + key);
        return cr.getType(uri);
    }

    public final class EditorImpl implements Editor {
        private final Map<String, Object> mModified = new HashMap<String, Object>();
        private boolean mClear = false;

        public Editor putString(String key, String value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putStringSet(String key, Set<String> values) {
            synchronized (this) {
                throw new UnsupportedOperationException("CSharedPreferences don't support putStringSet now!");
            }
        }
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                mModified.put(key, this);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                mClear = true;
                return this;
            }
        }

        public void apply() {
            commit();
        }

        public boolean commit() {
            synchronized (this) {
                if (mClear) {
                    ContentResolver cr = mContext.getContentResolver();
                    Uri uri = Uri.parse(SPConstant.CONTENT_URI + SPConstant.SEPARATOR + SPConstant.TYPE_CLEAN);
                    cr.delete(uri,null,null);
                    mClear = false;
                }

                for (Map.Entry<String, Object> e : mModified.entrySet()) {
                    saveSingleValue(e.getKey(), e.getValue());
                }
            }
            return true;
        }

        private void saveSingleValue(String key, Object value) {
            String type = null;
            ContentValues cv = new ContentValues();
            if (value instanceof String) {
                type = SPConstant.TYPE_STRING;
                cv.put(SPConstant.VALUE, (String) value);
            } else if (value instanceof Integer) {
                type = SPConstant.TYPE_INT;
                cv.put(SPConstant.VALUE, (int) value);
            } else if (value instanceof Long) {
                type = SPConstant.TYPE_LONG;
                cv.put(SPConstant.VALUE, (long) value);
            } else if (value instanceof Float) {
                type = SPConstant.TYPE_FLOAT;
                cv.put(SPConstant.VALUE, (float) value);
            } else if (value instanceof Boolean) {
                type = SPConstant.TYPE_BOOLEAN;
                cv.put(SPConstant.VALUE, (boolean) value);
            }

            if (!TextUtils.isEmpty(type)) {
                ContentResolver cr = mContext.getContentResolver();
                Uri uri = Uri.parse(SPConstant.CONTENT_URI + SPConstant.SEPARATOR + type + SEPARATOR + key);
                cr.update(uri, cv, null, null);
            }
        }
    }

    private class CContentObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public CContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
            Log.i("ccc", "onChange");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.i("ccc", "onChange uri : " + uri.toString());
            if (null == mListeners || mListeners.size() <= 0) {
                return;
            }
            // clone to get around ConcurrentModificationException
            Set<Map.Entry<SharedPreferences.OnSharedPreferenceChangeListener, Handler>> entries
                    = new HashSet<>(mListeners.entrySet());

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            if (null == uri || uri.equals(Uri.parse(SPConstant.CONTENT_URI + SPConstant.SEPARATOR + SPConstant.TYPE_GET_ALL))) {
                resultMap.putAll(getAll());
            } else {
                try {
                    String[] path = uri.getPath().split(SPConstant.SEPARATOR);
                    String key = path[2];   
                    if (contains(key)) {
                        resultMap.put(key, null);
                    }
                } catch (Exception e) {

                }
                if (resultMap.size() > 0) {
                    for (final String key : resultMap.keySet()) {
                        for (Map.Entry<SharedPreferences.OnSharedPreferenceChangeListener, Handler> entry : entries) {
                            final SharedPreferences.OnSharedPreferenceChangeListener listener = entry.getKey();
                            final Handler handler = entry.getValue();
                            if (handler != null) {
                                // call the listener on the thread where the listener was registered
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onSharedPreferenceChanged(CSharedPreferences.this, key);
                                    }
                                });
                            } else {
                                listener.onSharedPreferenceChanged(CSharedPreferences.this, key);
                            }
                        }
                    }
                }
            }
            
        }

        public void addListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {

        }

        public void removeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {

        }
    }
}
