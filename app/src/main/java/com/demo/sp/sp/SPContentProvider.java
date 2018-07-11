package com.demo.sp.sp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 功能：
 * 作者： caijun
 * 时间： 2018/6/15  16 : 25
 */
public class SPContentProvider extends ContentProvider {

    private static final String SHARED_PREFRENCES_NAME = "shared_prefrences_name";

    private static UriMatcher sURIMatcher;


    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sURIMatcher.addURI(SPConstant.AUTHORITY,
                SPConstant.CONTENT_URI,
                0);
    }

    @Override
    public boolean onCreate() {

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] path= uri.getPath().split(SPConstant.SEPARATOR);
        String type=path[1];
        if (type.equals(SPConstant.TYPE_GET_ALL)) {
            Map<String, ?> all = getSp(getContext()).getAll();
            if (all != null) {
                MatrixCursor cursor = new MatrixCursor(new String[] {SPConstant.CURSOR_COLUMN_NAME,
                        SPConstant.CURSOR_COLUMN_TYPE, SPConstant.CURSOR_COLUMN_VALUE});
                Set<String> keySet = all.keySet();
                for (String key : keySet) {
                    Object[] rows = new Object[3];
                    rows[0] = key;
                    rows[2] = all.get(key);
                    if (rows[2] instanceof Boolean) {
                        rows[1] = SPConstant.TYPE_BOOLEAN;
                    }else if (rows[2] instanceof String) {
                        rows[1] = SPConstant.TYPE_STRING;
                    }else if (rows[2] instanceof Integer) {
                        rows[1] = SPConstant.TYPE_INT;
                    }else if (rows[2] instanceof Long) {
                        rows[1] = SPConstant.TYPE_LONG;
                    }else if (rows[2] instanceof Float) {
                        rows[1] = SPConstant.TYPE_FLOAT;
                    }
                    cursor.addRow(rows);
                }
                return cursor;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        // 用这个来取数值
        String[] path = uri.getPath().split(SPConstant.SEPARATOR);
        String type = path[1];
        String key = path[2];
        if (type.equals(SPConstant.TYPE_CONTAIN)) {
            return String.valueOf(null != getSp(getContext()) && getSp(getContext()).contains(key));
        }
        return getStringValue(getContext(), key, type);
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String[] path = uri.getPath().split(SPConstant.SEPARATOR);
        String type = path[1];
        String key = path[2];
        Object obj = (Object) values.get(SPConstant.VALUE);
        if (obj != null) {
            try {
                if (type.equalsIgnoreCase(SPConstant.TYPE_STRING)) {
                    getSp(getContext()).edit().putString(key, String.valueOf(obj)).apply();
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_BOOLEAN)) {
                    getSp(getContext()).edit().putBoolean(key, (boolean) (obj)).apply();
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_INT)) {
                    getSp(getContext()).edit().putInt(key, (int) (obj)).apply();
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_LONG)) {
                    getSp(getContext()).edit().putLong(key, (long) (obj)).apply();
                } else if (type.equalsIgnoreCase(SPConstant.TYPE_FLOAT)) {
                    getSp(getContext()).edit().putFloat(key, (float) (obj)).apply();
                }
                getContext().getContentResolver().notifyChange(uri, null);
            } catch (ClassCastException e) {

            }
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String[] path = uri.getPath().split(SPConstant.SEPARATOR);
        String type = path[1];
        if (type.equals(SPConstant.TYPE_CLEAN)) {
            getSp(getContext()).edit().clear().apply();
            getContext().getContentResolver().notifyChange(uri, null);
            return 0;
        }
        String key = path[2];
        if (getSp(getContext()).contains(key)) {
            getSp(getContext()).edit().remove(key).apply();
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        insert(uri,values);
        return 0;
    }

    private SharedPreferences getSp(Context context) {
        return context.getSharedPreferences(SHARED_PREFRENCES_NAME, Context.MODE_PRIVATE);
    }

    private String getStringValue(Context context, String name, String type) {
        SharedPreferences sp = getSp(context);
        if (!sp.contains(name)) {
            return null;
        } else {
            if (type.equalsIgnoreCase(SPConstant.TYPE_STRING)) {
                return sp.getString(name, null);
            } else if (type.equalsIgnoreCase(SPConstant.TYPE_BOOLEAN)) {
                return String.valueOf(sp.getBoolean(name, false));
            } else if (type.equalsIgnoreCase(SPConstant.TYPE_INT)) {
                return String.valueOf(sp.getInt(name, 0));
            } else if (type.equalsIgnoreCase(SPConstant.TYPE_LONG)) {
                return String.valueOf(sp.getLong(name, 0L));
            } else if (type.equalsIgnoreCase(SPConstant.TYPE_FLOAT)) {
                return String.valueOf(sp.getFloat(name, 0f));
            }
            return null;
        }
    }
}
