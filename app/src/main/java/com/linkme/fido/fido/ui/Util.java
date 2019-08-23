package com.linkme.fido.fido.ui;

import android.content.Context;
import android.util.DisplayMetrics;

public class Util {
	
	/**
	 * Change dp to pixel
	 * @param dp
	 * @param context
	 * @return
	 */
	public static float convertDpToPixel(float dp, Context context){
	    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	    float px = dp * (metrics.densityDpi / 160f);
	    return px;
	}	
}
