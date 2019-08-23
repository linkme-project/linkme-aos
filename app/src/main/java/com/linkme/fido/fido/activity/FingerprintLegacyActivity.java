package com.linkme.fido.fido.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.linkme.fido.R;
import com.linkme.fido.fido.module.FingerprintUtility;
import com.linkme.fido.fido.module.IntentCode;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;
import com.vp.fido.Constants;
import com.vp.fido.VPCManager;
import com.vp.fido.publisher.VerifyResultPublisher;

import java.util.ArrayList;

import etri.fido.uaf.exception.ErrorCode;

/**
 * FIDO 지문 동작을 테스트 하기 위한 액티비티로, 인증 결과값을 콜백 형태로 받아와 화면에 결과를 표시한다.
 * @author yjson
 */
public class FingerprintLegacyActivity extends Activity implements Handler.Callback
{
	private final String TAG = FingerprintLegacyActivity.class.getSimpleName();

	// FIDO 생체인증 프로세스를 담당하는 VPClient 클라이언트 매니저
	private VPCManager mVPCManager;
	
	private Context mContext;
	
	private FingerprintUtility mFingerprintUtil;
	
	private final int A890K_SOFTWARE_VERSION_MIN = 2122; // for pantech (S0842122:RC2)
	private final int A880S_SOFTWARE_VERSION_MIN = 1124; // for pantech (S0221124:RC3)

	private final String ACTION_VERIFICATION = "btp.intent.action.verification";
	private final String ACTION_ENROLL = "android.intent.action.FingerScanSettings";
	private final String ACTION_CANCEL = "btp.intent.action.cancel";
	
	private final int NOT_INIT = -1;						// 초기화 값
	private final int API_PANTECH = 0;					// 팬텍 지문 API 사용
	private final int API_SAMSUNG = 1;					// 삼성 지문 API 사용
	private int mFingerprintDeviceVendor = NOT_INIT;		// 사용 가능한 단말 지문 API 저장
	
	private TextView mMessage;
	
	private final int START_ANIM = 1;							// 지문 인식 표시용 애니메이션 시작
	private final int STOP_ANIM = 0;							// 지문 인식 표시용 애니메이션 종료
	
	private ImageView img_touching;
	private AnimationDrawable mAnimListening;

	private Spass mSpass;
	private boolean isFeatureEnabled_fingerprint = false;
	private boolean needRetryIdentify;

	private SpassFingerprint mSpassFingerprint;
	private boolean onReadyIdentify = false;
	private boolean isFeatureEnabled_index = false;
	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_finger_legacy);
		
		mContext = this;
		
		mFingerprintUtil = FingerprintUtility.getInstance();
		
		// VPClient를 관리하는 매니저 인스턴스 생성
		mVPCManager = VPCManager.getInstance(FingerprintLegacyActivity.this);

		mMessage = (TextView) findViewById(R.id.message);

		img_touching = (ImageView) findViewById(R.id.img_touching);
		img_touching.setBackgroundResource(R.drawable.fingerprint_recognize_animation);
		mAnimListening = (AnimationDrawable) img_touching.getBackground();

		mSpass = new Spass();

		try {
			mSpass.initialize(FingerprintLegacyActivity.this);
		} catch (SsdkUnsupportedException e) {
			e.printStackTrace();
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}

		isFeatureEnabled_fingerprint = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

		if (isFeatureEnabled_fingerprint) {
			mSpassFingerprint = new SpassFingerprint(FingerprintLegacyActivity.this);
		}

		isFeatureEnabled_index = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_FINGER_INDEX);
		mHandler = new Handler(this);

		/*if(!FingerprintLegacyActivity.this.isFinishing()){

		} else {
			alertDialog = new AlertDialog.Builder(MainActivity.getMainActivity()).create();
		}*/

		mAlertDialog = new AlertDialog.Builder(mContext).create();

	}
	
	
	@Override
	protected void onResume()
	{
		Log.i(TAG, "onResume !!");
		
		super.onResume();
		startFingerService();
	}

	
	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause()");
		super.onPause();
		stopFingerService();
	}
	
	
	/**
	 * 팬텍용 소프트웨어 업데이트 메뉴로 이동
	 */
	private void moveSmartUpdateMenu()
	{
		Intent intent = new Intent();
		intent.setAction("com.pantech.app.apkmanager.appexe");
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		mContext.startActivity(intent);
	}
	
	
	@Override
	public void onBackPressed() {
		Log.i(TAG, "onBackPressed !!");
		
		mUIhandler.sendEmptyMessage(STOP_ANIM);

		// FIDO 클라이언트로 백버튼을 선택한 경우 사용자 중지임을 전달
		mVPCManager.onVerifyResult(mVPCManager.RESULT_CANCEL);
		// 2017.04.14. yjson. UI 컨트롤을 위한 결과값 전달
		VerifyResultPublisher.getInstance().setNewUIVerifyResult(Constants.BC_CODE_REQ_HTTP_PREPARE, ErrorCode.USER_CANCELLED, mVPCManager.getVPClient().getOPCode());

		super.onBackPressed();
	}


	/**
	 * 지문이 있는지 여부를 검사하고 유무에 따라 등록 또는 인증과정을 수행함
	 */
	private void startFingerService(){

		Log.d(TAG, "startFingerService called!!");
		Log.d(TAG, "mFingerprintDeviceVendor : " + mFingerprintDeviceVendor);

		switch(mFingerprintDeviceVendor){
			case NOT_INIT :
				// 팬텍폰이면서 지문을 사용할 수 있는 단말인지 여부를 판별
				if(mFingerprintUtil.isEnableFingerprint_P(mContext)){
					mFingerprintDeviceVendor = API_PANTECH;
					Log.d(TAG, "startFingerService() NOT_INIT isEnableFingerprint_P");
					checkExistFingerprint_P();
					return;
				}

				// 삼성폰이면서 지문을 사용할 수 있는지 여부를 판별
				if(mFingerprintUtil.isEnableFingerprint_S(mContext)){
					mFingerprintDeviceVendor = API_SAMSUNG;
					Log.d(TAG, "startFingerService() NOT_INIT isEnableFingerprint_S");
					checkExistFingerprint_S();
					return;
				}

			case API_PANTECH :
				Log.d(TAG, "startFingerService() isEnableFingerprint_P");
				checkExistFingerprint_P();
				break;

			case API_SAMSUNG :
				Log.d(TAG, "startFingerService() isEnableFingerprint_S");
				checkExistFingerprint_S();
				break;
		}
	}
	
	

	/**
	 * 삼성용 단말의 지문 등록 여부 처리
	 * <li/>지문이 있는 경우 검증 단계로 이동
	 * <li/>지문이 없는 경우 지문 등록 과정으로 이동
	 */
	private void checkExistFingerprint_S()
	{
		if(mFingerprintUtil.isExistFingerprint_S(this)) // 지문이 있으면
		{
			moveFingerprintVerify();
		}
		else
		// 지문이 없으면
		{
			showAlertDlgMoveFingerprintSetting();
		}
	}
	
	
	/**
	 * 팬텍용 단말의 지문 등록 여부 처리
	 * <li/>지문이 있는 경우 검증 단계로 이동
	 * <li/>지문이 없는 경우 지문 등록 과정으로 이동
	 */
	private void checkExistFingerprint_P() {

		Log.d(TAG, "mFingerprintUtil.isExistFingerprint_P(mFIDORegAndAuthActivity) : " + mFingerprintUtil.isExistFingerprint_P(mContext));

		if(mFingerprintUtil.isExistFingerprint_P(mContext)) // 지문이 있으면
		{
			if(!checkPantechSotfwareUpdate())
			{
				moveFingerprintVerify();
			}
		}
		else
		// 지문이 없으면
		{
			showAlertDlgMoveFingerprintSetting();
		}
	}
	
	
	private void moveFingerprintVerify()
	{
		Log.d(TAG, "moveFingerprintVerify() called!!");
		Log.d(TAG, "moveFingerprintVerify() mFingerprintDeviceVendor : " + mFingerprintDeviceVendor);

		switch(mFingerprintDeviceVendor){
			// 펜텍폰
			case API_PANTECH :
				Intent verifyIntent = new Intent(ACTION_VERIFICATION);
				verifyIntent.putExtra("callbackMessenger", new Messenger(fingerprintCallbackHandler));
				mContext.startService(verifyIntent);
				Log.d(TAG, "phantec fingerprint service start");
				break;

			// 삼성폰
			case API_SAMSUNG :
				startIdentify();
				Log.d(TAG, "samsung fingerprint service start");
				break;

			default: break;
		}
	}
	
	
	private boolean checkPantechSotfwareUpdate()
	{
		boolean softwareUpdate = false;

		if(mFingerprintUtil.isEnableFingerprint_P(this))
		{
			String modelName = Build.MODEL;
			if(modelName.equals("IM-A880S"))
			{
				if(getPantechSoftwareVersion() < A880S_SOFTWARE_VERSION_MIN)
				{
					softwareUpdate = true;
				}
			}
			else if(modelName.equals("IM-A890K"))
			{
				if(getPantechSoftwareVersion() < A890K_SOFTWARE_VERSION_MIN)
				{
					softwareUpdate = true;
				}
			}

			Log.i("fingerprint", "checkSotfwareUpdate = " + softwareUpdate);

			if(softwareUpdate)
			{
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
				alertDialog.setMessage(R.string.fingerprint_software_update_setting);
				alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				});
				alertDialog.setNegativeButton(R.string.app_exit, new AlertDialog.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				});
				alertDialog.setCancelable(false);
				alertDialog.setTitle(R.string.notice);
				alertDialog.show();
			}
		}

		return softwareUpdate;
	}
	
	
	private int getPantechSoftwareVersion()
	{
		String fullVersionName = "";
		String versionName = "";

		try
		{
			fullVersionName = SystemProperties.get("ro.product.baseband_ver");
			versionName = fullVersionName.substring(4, fullVersionName.length());
			Log.i("fingerprint", "pantech sw version=" + Integer.valueOf(versionName) + ", full version=" + fullVersionName);
			return Integer.valueOf(versionName);
		}
		catch(Exception e)
		{
			Toast.makeText(mContext, "pantech sw version check fail", Toast.LENGTH_LONG).show();
			return 0;
		}
	}

	
	private void stopFingerService() {
		
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		
		switch(mFingerprintDeviceVendor){
			case API_PANTECH :
				Log.d(TAG, "stopFingerService() isEnableFingerprint_P");
				stopFingerService_P();
//				cancelFingerService_P();
				break;

			case API_SAMSUNG :
				Log.d(TAG, "stopFingerService() isEnableFingerprint_S");
				stopFingerService_S();
				break;

			default: break;
		}
	}

	private void stopFingerService_P()
	{
		Intent verifyIntent = new Intent(ACTION_VERIFICATION);
		stopService(verifyIntent);
		Log.i("fingerprint", "stopFingerService()");
	}

	private void stopFingerService_S()
	{

		if (onReadyIdentify == true) {
			try {
				if (mSpassFingerprint != null) {
					mSpassFingerprint.cancelIdentify();
				}
				Log.i(TAG, "cancelIdentify is called");
			} catch (IllegalStateException ise) {
				Log.i(TAG, ise.getMessage());
			}
			onReadyIdentify = false;
			needRetryIdentify = false;
		} else {
			Log.i(TAG, "Please request Identify first");
		}

	}


	private void cancelFingerService_P()
	{
		Intent verifyIntent = new Intent(ACTION_CANCEL);
		stopService(verifyIntent);
		Log.i("fingerprint", "cancelFingerService()");
	}
	
	
	
	/**
	 * 팝업창 표시를 통해 지문 등록 설정 유도
	 * <li/> 마쉬멜로 이상 버전이나 팬택폰의 경우 알림창을 통해 지문 설정을 유도함.
	 * <li/> 삼성폰의 경우 지문 설정 페이지로 이동시킨 후 지문이 등록된 경우 인증과정을 수행함.
	 */
	private void showAlertDlgMoveFingerprintSetting(){
		final boolean enableS = mFingerprintUtil.isEnableFingerprint_S(mContext);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

		// 2016.06.28. yjson. 디바이스 종류 구분 추가(삼성, 팬텍폰이면서 M 이상의 버전이 경우)
		// 안드로이드 마쉬멜로 버전에서는 세팅 메뉴로 연결할 수 없으므로 사용자 Manual 표시
		if(!enableS){
			alertDialog.setMessage("등록된 지문이 없습니다. 설정 메뉴에서 지문을 등록한 후 다시 시도해 주세요.");
		} else {
			alertDialog.setMessage("등록된 지문이 없습니다. 설정 메뉴로 이동하시겠습니까?");
		}
		alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(enableS) {
					Log.d(TAG, "move to setting menu under legacy");

					if(mFingerprintDeviceVendor == API_SAMSUNG){

						mFingerprintUtil.getsPassManager().cancelIdentify();

						SpassFingerprint.RegisterListener mRegisterListener = new SpassFingerprint.RegisterListener()
						{

							@Override
							public void onFinished()
							{
								boolean hasRegisteredFinger = mFingerprintUtil.getsPassManager().hasRegisteredFinger();
								Log.d(TAG, "showAlertDlgMoveFingerprintSetting onFinished() hasRegisteredFinger : " + hasRegisteredFinger);

								if(hasRegisteredFinger)
								{
									Log.d(TAG, "hasRegisteredFinger || FIDOAuthManager.getInstance().isPaymentProcess()");
									moveFingerprintVerify();
								} else {
									// 지문 등록 후 사용 유도 메시지 표시와 함께 프로세스 종료
									Toast.makeText(mContext, "지문을 등록한 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
								}
							}
						};

						// 지문등록 페이지로 이동
						mFingerprintUtil.getsPassManager().moveRegisterFingerprint(mRegisterListener);
					}

				} else {
					finishActivity(mVPCManager.RESULT_CANCEL);
				}
			}
		});

		if(enableS) {
			alertDialog.setNegativeButton(R.string.btn_cancel, new AlertDialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finishActivity(mVPCManager.RESULT_CANCEL);
				}
			});
		}
		alertDialog.setCancelable(false);
		alertDialog.setTitle(R.string.notice);
		alertDialog.show();
	}

	private SpassFingerprint.IdentifyListener mIdentifyListener = new SpassFingerprint.IdentifyListener() {
			@Override
			public void onFinished(int eventStatus) {
				Log.e(TAG , "es = " + eventStatus);
				int FingerprintIndex = 0;
				String FingerprintGuideText = null;
				try {
					FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
				} catch (IllegalStateException ise) {
					Log.i(TAG,"exc = " + ise.getMessage());
				}
				if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
					Log.i(TAG,"onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);

//					stopFingerService_S();
					finishActivity(mVPCManager.RESULT_TRUE);

				} else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
					Log.i(TAG,"onFinished() : Password authentification Success");
//					stopFingerService_S();
					finishActivity(mVPCManager.RESULT_TRUE);
				} else if (eventStatus == SpassFingerprint.STATUS_OPERATION_DENIED) {
					Log.i(TAG,"onFinished() : Authentification is blocked because of fingerprint service internally.");



					if (!mAlertDialog.isShowing()) {
						mAlertDialog.setMessage("생체 인식 인증을 5회 시도했습니다.\n잠시 후 다시 시도해주세요.");
						mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "확인", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								stopFingerService();
								finishActivity(mVPCManager.RESULT_FALSE);
							}
						});

						mAlertDialog.setCancelable(false);
						mAlertDialog.setTitle(R.string.notice);
						mAlertDialog.show();
					}

				} else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED) {
					Log.i(TAG,"onFinished() : User cancel this identify.");
//					Toast.makeText(getApplicationContext(), "user cancel", Toast.LENGTH_SHORT).show();
//					finishActivity(mVPCManager.RESULT_FALSE);
				} else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
					Log.i(TAG,"onFinished() : The time for identify is finished.");
				} else if (eventStatus == SpassFingerprint.STATUS_QUALITY_FAILED) {
					Log.i(TAG,"onFinished() : Authentification Fail for identify.");
					needRetryIdentify = true;
//					FingerprintGuideText = mSpassFingerprint.getGuideForPoorQuality();
					FingerprintGuideText = "지문 전체가 인식되도록 손가락을 센서 중앙에 올리세요";
					Toast.makeText(mContext, FingerprintGuideText, Toast.LENGTH_SHORT).show();
				} else if (eventStatus == SpassFingerprint.STATUS_SENSOR_FAILED) {
					Log.e(TAG, "mSpassListener onFinished() : STATUS_SENSOR_FAILED");

					mAlertDialog.setMessage("지문 인식 센서에 오류가 발생했습니다.\n이 메시지가 반복될 경우 디바이스를 다시 시작하세요.");
					mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "확인", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							stopFingerService();
							finishActivity(mVPCManager.RESULT_FALSE);
						}
					});

					mAlertDialog.setCancelable(false);
					mAlertDialog.setTitle(R.string.notice);
					mAlertDialog.show();
				} else {
					Log.i(TAG,"onFinished() : Authentification Fail for identify");
					needRetryIdentify = true;
				}
				if (!needRetryIdentify) {
					resetIdentifyIndex();
				}
			}

			@Override
			public void onReady() {
				Log.i(TAG,"identify state is ready");
			}

			@Override
			public void onStarted() {
				Log.i(TAG,"User touched fingerprint sensor");
			}

			@Override
			public void onCompleted() {
				Log.e(TAG,"the identify is completed needRetryIdentify = " + needRetryIdentify );
				onReadyIdentify = false;
				if (needRetryIdentify) {
					needRetryIdentify = false;
					mHandler.sendEmptyMessageDelayed(MSG_AUTH, 100);
				}
			}
		};

	private Handler mHandler;
	private static final int MSG_AUTH = 1000;
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_AUTH:
				startIdentify();
				break;

		}
		return true;
	}

	private void startIdentify() {
		if (onReadyIdentify == false) {
			try {
				onReadyIdentify = true;
				if (mSpassFingerprint != null) {
					setIdentifyIndex();
					mSpassFingerprint.startIdentify(mIdentifyListener);
				}
				if (designatedFingers != null) {
					Log.i(TAG,"Please identify finger to verify you with " + designatedFingers.toString() + " finger");
				} else {
					Log.i(TAG,"Please identify finger to verify you");
				}
			} catch (SpassInvalidStateException ise) {
				onReadyIdentify = false;
				resetIdentifyIndex();
				if (ise.getType() == SpassInvalidStateException.STATUS_OPERATION_DENIED) {
					Log.i(TAG, "Exception: " + ise.getMessage());
					if (android.os.Build.VERSION.SDK_INT < 26) {
						Log.i(TAG , "android.os.Build.VERSION.SDK_INT < 26 = " +android.os.Build.VERSION.SDK_INT);
						mIdentifyListener.onFinished(SpassFingerprint.STATUS_OPERATION_DENIED);
					}

				}
			} catch (IllegalStateException e) {
				onReadyIdentify = false;
				resetIdentifyIndex();
				Log.i(TAG, "Exception: " + e);
			}
		} else {
			Log.i(TAG, "The previous request is remained. Please finished or cancel first");
		}
	}

	private void resetIdentifyIndex() {
		designatedFingers = null;
	}

	private ArrayList<Integer> designatedFingers = null;
	private void setIdentifyIndex() {
		if (isFeatureEnabled_index) {
			if (mSpassFingerprint != null && designatedFingers != null) {
				mSpassFingerprint.setIntendedFingerprintIndex(designatedFingers);
			}
		}
	}


	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	
	private final int PUBLISH_FINGER_PRESENT = 0x1000;
	private final int PUBLISH_FINGER_SCANNING = 0x1001;
	private final int PUBLISH_FINGER_SCANNED = 0x1002;
	private final int PUBLISH_PROCESS = 0x1003;
	private final int PUBLISH_VERIFY = 0x1004;

	private final int RESULT_SUCCESS = 0;
	
	
	/**
	 * 팬텍용 지문 인식 처리 핸들러
	 */

	private Handler fingerprintCallbackHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
				case PUBLISH_FINGER_PRESENT :
					Log.i("fingerprint", "callbackHandler PUBLISH_FINGER_PRESENT");
					break;
				case PUBLISH_FINGER_SCANNING :
					Log.i("fingerprint", "callbackHandler PUBLISH_FINGER_SCANNING");
					break;
				case PUBLISH_FINGER_SCANNED:
					Log.i("fingerprint", "callbackHandler PUBLISH_FINGER_SCANNED");
					mUIhandler.sendEmptyMessage(START_ANIM);
					break;
				case PUBLISH_PROCESS :
					Log.i("fingerprint", "callbackHandler PUBLISH_PROCESS");
					break;
				case PUBLISH_VERIFY :
					Log.i("fingerprint", "PUBLISH_VERIFY PUBLISH_VERIFY msg.arg1 = " + msg.arg1);
					mUIhandler.sendEmptyMessage(STOP_ANIM);
					
					if(msg.arg1 == RESULT_SUCCESS) // 지문일치
					{
						Log.i(TAG, "onAuthenticated()");
						stopFingerService_P();
						mUIhandler.sendEmptyMessage(STOP_ANIM);

						finishActivity(mVPCManager.RESULT_TRUE);
					}

					else
					// 지문일치하지 않을경우
					{
						Log.i(TAG, "onAuthenticationFailed()");
						mUIhandler.sendEmptyMessage(STOP_ANIM);
					}
					break;
			}
		}
	};

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult requestCode : " + requestCode);
		Log.i(TAG, "onActivityResult resultCode : " + resultCode);

		super.onActivityResult(requestCode, resultCode, data);

		// 팬텍폰 지문등록 결과 처리
		// 2016.03.25. yjson. 팬택폰 지문등록은 사용자가 설정메뉴에서 등록하도록 유도함
		// 등록과정에서 지문인식 오류 시 처리가 되지 않음(msg 값이 8로 넘어오는 경우 서비스 종료안됨)
		if (requestCode == IntentCode.INTENTCODE_FINGERPRINT_BTP_ENROL) {
			if (resultCode == Activity.RESULT_OK) {
				checkExistFingerprint_P();
			} else {
				// 지문 등록 후 사용 유도 메시지 표시와 함께 프로세스 종료
				Toast.makeText(mContext, "지문을 등록한 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	
	/**
	 * 2015.12.22. 사용자 인증 결과값 처리
	 * Activity 종료
	 * @param verifyResult
	 */
	public void finishActivity(int verifyResult) {

		Log.d(TAG, "finishActivity verifyResult : " + verifyResult);

		// 로컬 인증 결과가 성공인 경우 결과를 전달
		if (verifyResult == mVPCManager.RESULT_TRUE){
			mVPCManager.onVerifyResult(mVPCManager.RESULT_TRUE);
		// 로컬 인증 결과가 실패인 경우 결과를 전달
		} else if(verifyResult == mVPCManager.RESULT_FALSE){
			mVPCManager.onVerifyResult(mVPCManager.RESULT_FALSE);
		// 로컬 인증 취소인 경우 결과를 전달
		} else if(verifyResult == mVPCManager.RESULT_CANCEL){
			mVPCManager.onVerifyResult(mVPCManager.RESULT_CANCEL);
		}

		finish();
	}
	
	
	public void onScanStart()
	{
		mAnimListening.start();
	}

	public void onScanStop()
	{
		if (mAnimListening != null) {
			mAnimListening.stop();
		}
	}
	
	private Handler mUIhandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch(msg.what){
				case START_ANIM : onScanStart(); 
					break;
				case STOP_ANIM : onScanStop(); 
					break;
				default : break;
			}
		}
	};
}
