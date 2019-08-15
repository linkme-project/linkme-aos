package com.linkme.fido;

import android.app.Application;
import android.widget.Toast;

public class LinkMeApp extends Application {
    public static String userId;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void showToast(final String message, final int length) {
        Toast.makeText(this, message, length);
    }
}
