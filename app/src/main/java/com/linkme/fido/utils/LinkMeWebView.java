package com.linkme.fido.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class LinkMeWebView extends WebView {

    public LinkMeWebView(Context context) {
        super(context);
        init();
    }

    public LinkMeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        WebSettings settings = this.getSettings();
        settings.setJavaScriptEnabled(true);    //javscript
        settings.setJavaScriptCanOpenWindowsAutomatically(true);    //webview 내 팝업 허용
        settings.setDomStorageEnabled(true);    //dom
        settings.setSupportZoom(false);         //zoom
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);   //cache disable
        settings.setAppCacheEnabled(false); //local cache disable
        settings.setAllowFileAccess(true);  //for file upload
    }
}
