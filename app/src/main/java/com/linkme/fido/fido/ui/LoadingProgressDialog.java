package com.linkme.fido.fido.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.linkme.fido.R;


public class LoadingProgressDialog extends Dialog {

	 public static LoadingProgressDialog show(Context context, CharSequence title,
                                              CharSequence message) {
	     return show(context, title, message, false); 
	 } 
	  
	 public static LoadingProgressDialog show(Context context, CharSequence title,
                                              CharSequence message, boolean indeterminate) {
	     return show(context, title, message, indeterminate, false, null); 
	 } 
	  
	 public static LoadingProgressDialog show(Context context, CharSequence title,
                                              CharSequence message, boolean indeterminate, boolean cancelable) {
	     return show(context, title, message, indeterminate, cancelable, null); 
	 } 

	 public static LoadingProgressDialog show(Context context, CharSequence title,
                                              CharSequence message, boolean indeterminate,
                                              boolean cancelable, OnCancelListener cancelListener) {
	     LoadingProgressDialog dialog = new LoadingProgressDialog(context); 
	     dialog.setTitle(title); 
	     dialog.setCancelable(cancelable); 
	     dialog.setOnCancelListener(cancelListener); 
	     /* add the ProgressBar to the dialog. */
	     ProgressBar progress = new ProgressBar(context);
	     progress.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.progress_bar_drawable));
	     int size = (int) Util.convertDpToPixel(60, context);
	     int sizeProgress = (int) Util.convertDpToPixel(49, context);
	     LinearLayout layout = new LinearLayout(context);
	     layout.setGravity(Gravity.CENTER);
	     //layout.setBackgroundResource(R.drawable.loading_icon_new);
	     layout.addView(progress, new LayoutParams(sizeProgress, sizeProgress));
	     
	     dialog.addContentView(layout, new LayoutParams(size, size));
	     
	     try {
		     if(context!=null)
		    	 dialog.show(); 
	     } catch(Exception e) {}
		 return dialog;
	 } 
	  
	 public LoadingProgressDialog(Context context) {
	     super(context, R.style.LoadingDialog); 
	 } 
}