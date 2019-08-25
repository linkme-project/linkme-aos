package com.linkme.fido.utils;

import android.webkit.JavascriptInterface;

public abstract class JavascriptBridge {
    @JavascriptInterface
    public abstract void regFido();

    @JavascriptInterface
    public abstract void authFido();

    @JavascriptInterface
    public abstract void deregFido();
}
