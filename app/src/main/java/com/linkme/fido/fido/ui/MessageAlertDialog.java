package com.linkme.fido.fido.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.linkme.fido.R;


public class MessageAlertDialog extends Dialog {

	private View parentView;
	private TextView mAlertMsg;

	Handler handler = new Handler();
	
	public MessageAlertDialog(Context context) {
		super(context, R.style.CustomDialog);

		parentView = LayoutInflater.from(getContext()).inflate(R.layout.common_message_alert_dialog, null);

		mAlertMsg = (TextView) parentView.findViewById(R.id.alertMessage);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
		lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lpWindow.dimAmount = 0.6f;
		getWindow().setAttributes(lpWindow);
		setContentView(parentView);
		
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				dismiss();
			}
		}, 1500);
	}

	public void setAlertText(CharSequence msg) {
		mAlertMsg.setText(msg);
	}

}
