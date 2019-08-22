package com.linkme.fido.fido.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassFingerprint.IdentifyListener;
import com.samsung.android.sdk.pass.SpassFingerprint.RegisterListener;

public class SPassFingerprintManager
{
	private Context context;
	private Spass mSpass;
	private boolean isSpassEnabled;
	private SpassFingerprint mSpassFingerprint;
	private boolean isStartIdentify = false;

	public SPassFingerprintManager(Context context)
	{
		this.context = context;

		try
		{
			mSpass = new Spass();
			mSpass.initialize(context);
			isSpassEnabled = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
		}
		catch (SsdkUnsupportedException e)
		{
			isSpassEnabled = false;
		}
		catch (UnsupportedOperationException e)
		{
			isSpassEnabled = false;
		}

		if(isSpassEnabled)
		{
			mSpassFingerprint = new SpassFingerprint(context);
		}
	}

	public boolean isSpassEnabled()
	{
		return isSpassEnabled;
	}

	public boolean hasRegisteredFinger()
	{
		if(mSpassFingerprint != null)
		{
			return mSpassFingerprint.hasRegisteredFinger();
		}
		else
		{
			return false;
		}
	}

	public void startIdentify(IdentifyListener listener)
	{
		if(mSpassFingerprint != null)
		{
			try
			{
				isStartIdentify = true;
				mSpassFingerprint.startIdentify(listener);
			}
			catch (IllegalStateException e) { }
		}
	}

	public void cancelIdentify()
	{
		if(mSpassFingerprint != null)
		{
			try
			{
				isStartIdentify = false;
				mSpassFingerprint.cancelIdentify();
			}
			catch (IllegalStateException e) { }
		}
	}

	public void moveRegisterFingerprint(RegisterListener mRegisterListener)
	{
		if(mSpassFingerprint != null)
		{
			mSpassFingerprint.registerFinger(context, mRegisterListener);
		}
	}

	private final String FINGERPRINT_USED_KEY = "spass_fingerprint_used";
	private final String FINGERPRINT_SELF_OFF_KEY = "spass_fingerprint_self_off";

	public void setFingerprintUsed(boolean used)
	{
		SharedPreferences pref = context.getSharedPreferences(FINGERPRINT_USED_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(FINGERPRINT_USED_KEY, used);
		editor.commit();
	}

	public boolean getFingerprintUsed()
	{
		SharedPreferences pref = context.getSharedPreferences(FINGERPRINT_USED_KEY, Context.MODE_PRIVATE);
		boolean result = pref.getBoolean(FINGERPRINT_USED_KEY, false);
		return result;
	}

	public void setFingerprintSelfOff(boolean isSelfOff)
	{
		SharedPreferences pref = context.getSharedPreferences(FINGERPRINT_SELF_OFF_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(FINGERPRINT_SELF_OFF_KEY, isSelfOff);
		editor.commit();
	}

	public boolean getFingerprintSelfOff()
	{
		SharedPreferences pref = context.getSharedPreferences(FINGERPRINT_SELF_OFF_KEY, Context.MODE_PRIVATE);
		boolean result = pref.getBoolean(FINGERPRINT_SELF_OFF_KEY, false);
		return result;
	}

	public boolean isStartIdentify()
	{
		return isStartIdentify;
	}

	public void setStartIdentify(boolean isStartIdentify)
	{
		this.isStartIdentify = isStartIdentify;
	}
}
