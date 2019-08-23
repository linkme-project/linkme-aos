package com.linkme.fido.fido.activity;

import android.annotation.TargetApi;
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
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import com.linkme.fido.R;
import com.linkme.fido.fido.module.FingerprintUiHelper;
import com.linkme.fido.fido.module.FingerprintUtility;
import com.vp.fido.Constants;
import com.vp.fido.VPCManager;
import com.vp.fido.publisher.VerifyResultPublisher;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import etri.fido.uaf.exception.ErrorCode;

/**
 * FIDO 지문 동작을 테스트 하기 위한 액티비티로, 인증 결과값을 콜백 형태로 받아와 화면에 결과를 표시한다.
 * @author yjson
 */
public class FingerprintGoogleActivity extends Activity implements FingerprintUiHelper.Callback
{
	private final String TAG = FingerprintGoogleActivity.class.getSimpleName();

	private Context mContext;
	
	// FIDO 생체인증 프로세스를 담당하는 VPClient 클라이언트 매니저
	private VPCManager mVPCManager;
	
	// [[ 구글 지문 API에서 사용
	private FingerprintUtility mFingerprintUtil;
	private FingerprintUiHelper mFingerprintUiHelper;
	private FingerprintManagerCompat.CryptoObject mCryptoObject;
	// 구글 지문 API에서 사용 ]]

	private final int DEVICE_SETTING = 100;						// 단말에 등록된 지문이 없는 경우 등록 페이지로 이동하기 위한 플래그 값

	private static final int NOT_INIT = -1;						// 초기화 값
	private static final int API_GOOGLE = 2;					// 구글, LG 지문 API 사용
	private static int mFingerprintDeviceVendor = NOT_INIT;		// 사용 가능한 단말 지문 API 저장

	private final int START_ANIM = 1;							// 지문 인식 표시용 애니메이션 시작
	private final int STOP_ANIM = 0;							// 지문 인식 표시용 애니메이션 종료
	
	private ImageView img_touching;
	private AnimationDrawable mAnimListening;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_finger_legacy);
		
		mContext = this;
		
		// VPClient를 관리하는 매니저 인스턴스 생성
		mVPCManager = VPCManager.getInstance(FingerprintGoogleActivity.this);

		mFingerprintUtil = FingerprintUtility.getInstance();

		img_touching = (ImageView) findViewById(R.id.img_touching);
		img_touching.setBackgroundResource(R.drawable.fingerprint_recognize_animation);
		mAnimListening = (AnimationDrawable) img_touching.getBackground();
	}
	
	
	@Override
	protected void onResume()
	{
		Log.i(TAG, "onResume !!");
		
		super.onResume();
		startFingerService();
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


	private void showAlertDlgMoveFingerprintSetting(){
		final boolean isVerCodeOverM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
		// 안드로이드 마쉬멜로 버전에서는 세팅 메뉴로 연결할 수 없으므로 사용자 Manual 표시
		if(isVerCodeOverM){
			alertDialog.setMessage("등록된 지문이 없습니다. 설정 메뉴에서 지문을 등록한 후 다시 시도해 주세요.");
		} else {
			alertDialog.setMessage("등록된 지문이 없습니다. 설정 메뉴로 이동하시겠습니까?");
		}
		alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (!isVerCodeOverM) {
					Log.d(TAG, "move to setting menu under M");
					startActivityForResult(new Intent(Settings.ACTION_SETTINGS), DEVICE_SETTING);
				} else {
					Log.d(TAG, "move to setting menu in M");
					finishActivity(mVPCManager.RESULT_CANCEL);
				}
			}
		});
		if(!isVerCodeOverM) {
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


	/**
	 * 지문이 있는지 여부를 검사하고 유무에 따라 등록 또는 인증과정을 수행함
	 */
	private void startFingerService(){

		Log.d(TAG, "startFingerService called!!");
		Log.d(TAG, "mFingerprintDeviceVendor : " + mFingerprintDeviceVendor);

		if (mFingerprintDeviceVendor == NOT_INIT) {
			// 구글 지문 API 를 사용할 수 있는 단말인지 여부를 판별
			if (mFingerprintUtil.isEnableFingerprint_M(mContext)) {
				mFingerprintDeviceVendor = API_GOOGLE;
				Log.d(TAG, "startFingerService() isEnableFingerprint_M");
				checkExistFingerprint_M();
			}
		} else if (mFingerprintDeviceVendor == API_GOOGLE) {
			Log.d(TAG, "startFingerService() isEnableFingerprint_M");
			checkExistFingerprint_M();
		}
	}

	/**
	 * 구글 지문 API 사용 단말의 지문 등록 여부 처리
	 * <li/>지문이 있는 경우 검증 단계로 이동
	 * <li/>지문이 없는 경우 지문 등록 과정으로 이동
	 */
	private void checkExistFingerprint_M(){

		mFingerprintUiHelper = new FingerprintUiHelper(this, FingerprintManagerCompat.from(this), this);
//		mFingerprintUiHelper = new FingerprintUiHelper(mFIDORegAndAuthActivity, new FingerprintModule().providesFingerprintManager(mFIDORegAndAuthActivity), this);

		// 등록된 지문이 있는 경우
		if (mFingerprintUtil.isExistFingerprint_M()) {
			moveFingerprintVerify();
		// 등록된 지문이 없는 경우
		} else {
			// 지문등록 페이지로 유도 알림 팝업 표시
			showAlertDlgMoveFingerprintSetting();
		}
	}


	private void moveFingerprintVerify()
	{
		Log.d(TAG, "moveFingerprintVerify() called!!");

			// 구글 API 사용 폰(마쉬멜로 버전(23) 이상)
		if(mFingerprintDeviceVendor == API_GOOGLE) {
			createKey();
			// Set up the crypto object for later. The object will be authenticated by use of the fingerprint.
			if (initCipher()) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
					Log.d(TAG, "this android api can't support fingerprint");
					return;
				} else {
//					onScanStart();
					mCryptoObject = new FingerprintManagerCompat.CryptoObject(mFingerprintUtil.getCipher());
					mFingerprintUiHelper.startListening(mCryptoObject);
					Log.d(TAG, "mashmellow fingerprint service start");
					mUIhandler.sendEmptyMessage(START_ANIM);
				}
			}
		}
	}


	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause()");
		super.onPause();
		stopFingerService();
	}


	@Override
	public void onDestroy(){
		Log.i(TAG, "onDestroy()");
		super.onDestroy();
	}

	public void stopFingerService() {
		Log.d(TAG, "stopFingerService() isEnableFingerprint_M");
		stopFingerService_M();
	}


	private void stopFingerService_M(){
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		mFingerprintUiHelper.stopListening();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult requestCode : " + requestCode);

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == DEVICE_SETTING) {
			if (mFingerprintUtil.isExistFingerprint(mContext)) {
				moveFingerprintVerify();
			} else {
				// 지문 등록 후 사용 유도 메시지 표시와 함께 프로세스 종료
				Toast.makeText(mContext, "지문을 등록한 후 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
				finishActivity(mVPCManager.RESULT_CANCEL);
			}
		}
	}

	
	
	//=========================================================================================
	// [[ 구글 지문 API 사용을 위한 내용 추가
	//=========================================================================================
	@TargetApi(23)
	public void createKey() {
		// The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
		// for your flow. Use of keys is necessary if you need to know if the set of
		// enrolled fingerprints has changed.
		try {
			mFingerprintUtil.getKeyStore().load(null);
			// Set the alias of the entry in Android KeyStore where the key will appear
			// and the constrains (purposes) in the constructor of the Builder
			mFingerprintUtil.getKeyGenerator().init(new KeyGenParameterSpec.Builder(mFingerprintUtil.getKeyName(), KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
			.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
			// Require the user to authenticate with a fingerprint to authorize every use of the key
			.setUserAuthenticationRequired(true)
			.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
			.build());
			mFingerprintUtil.getKeyGenerator().generateKey();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
			//				throw new RuntimeException(e);
			Log.d(TAG, "Failed to createKey");
		}
	}


	/**
	 * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
	 * method.
	 *
	 * @return {@code true} if initialization is successful, {@code false} if the lock screen has
	 * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
	 * the key was generated.
	 */
	@TargetApi(23)
	private boolean initCipher() {
		try {
			mFingerprintUtil.getKeyStore().load(null);
			SecretKey key = (SecretKey) mFingerprintUtil.getKeyStore().getKey(mFingerprintUtil.getKeyName(), null);
			mFingerprintUtil.getCipher().init(Cipher.ENCRYPT_MODE, key);
			return true;
		} catch (KeyPermanentlyInvalidatedException e) {
			return false;
		} catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			//				throw new RuntimeException("Failed to init Cipher", e);
			Log.d(TAG, "Failed to init Cipher");
			return false;
		}
	}


	@Override
	public void onAuthenticated() {
		Log.i(TAG, "onAuthenticated()");
		stopFingerService();
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		finishActivity(mVPCManager.RESULT_TRUE);
	}

	@Override
	public void onAuthenticationFailed() {
		Log.i(TAG, "onAuthenticationFailed()");
//		stopFingerService();
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		// 2017.04.14. yjson. 재인증 수행을 위해 액티비티 유지하도록 아래 소스 주석 처리
//		finishActivity(mVPCManager.RESULT_FALSE);
	}

	/**
	 * 구글 지문 API 의 경우 5회 연속 시도시 에러가 발생하며, 재시도가 가능한 초기화에 시간이 걸림.
	 */
	@Override
	public void onError() {
		Log.i(TAG, "onError()");
		
		mUIhandler.sendEmptyMessage(STOP_ANIM);
		String msg = "지문인식 중 오류가 발생하였습니다. 나중에 다시 시도해 주세요.";
		showCancelDlg(msg);
	}


	private void showCancelDlg(String msg){
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
		alertDialogBuilder.setTitle("알림");
		alertDialogBuilder.setMessage(msg);
		alertDialogBuilder.setCancelable(false);

		alertDialogBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener(){
			// 확인 버튼 클릭시 설정
			public void onClick(DialogInterface dialog, int whichButton){
				stopFingerService();
				finishActivity(mVPCManager.RESULT_CANCEL);
				finish();
			}
		});
		alertDialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton){
				dialog.cancel();
			}
		});
		
		AlertDialog dlg = alertDialogBuilder.create();
		dlg.show();
	}


	//=========================================================================================
	// 구글 지문 API 사용을 위한 내용 추가 ]]
	//=========================================================================================


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
