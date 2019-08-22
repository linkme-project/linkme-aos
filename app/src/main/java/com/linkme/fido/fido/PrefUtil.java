package com.linkme.fido.fido;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by yjson on 2017-02-06.
 */

public class PrefUtil {
    private final String PREF_NAME = "liivmate_test_preference";
    private Context mContext;

    PrefUtil(Context context){
        this.mContext = context;
    }

    public void putValue(String key, String value){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putString(key, value);
        editor.commit();
    }

    public String getValue(String key, String defValue){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);

        try{
            return pref.getString(key, defValue);
        } catch (Exception e){
            return defValue;
        }
    }

    public void putValue(String key, boolean value){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(key, value);
        editor.commit();
    }

    public boolean getValue(String key, boolean defValue){
        SharedPreferences pref = mContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);

        try{
            return pref.getBoolean(key, defValue);
        } catch (Exception e){
            return defValue;
        }
    }
}
