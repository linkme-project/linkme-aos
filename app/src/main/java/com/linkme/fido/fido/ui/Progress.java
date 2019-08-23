package com.linkme.fido.fido.ui;

import android.content.Context;
import android.content.DialogInterface;

public class Progress {
	public static LoadingProgressDialog progressDialog;

	public static boolean bLoading = false;


	/**
	 * Show loading progress bar
	 * @param mContext
	 */
	public static void showProgressBar(Context mContext) {
		try {
			if(progressDialog == null)
				progressDialog = LoadingProgressDialog.show(mContext, "", "", true, true, null);
			else if(progressDialog.isShowing()==false){
				//progressDialog.dismiss();
				progressDialog = LoadingProgressDialog.show(mContext, "", "", true, true, null);
			}

			bLoading = true;
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					bLoading = false;
				}
			});
		} catch(Exception e) {}
	}

	/**
	 * Close loading progressbar
	 */
	public static void closeProgressBar() {
		bLoading = false;
		try {
			if(progressDialog != null)
				progressDialog.dismiss();
		} catch(Exception e) {}
	}
}
