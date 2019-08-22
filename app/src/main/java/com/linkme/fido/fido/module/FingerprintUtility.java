package com.linkme.fido.fido.module;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.vp.fido.util.Utilities;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/**
 * 2016.03.07. yjson 신규 생성
 * 지문 관련 유틸리티 클래스
 */
public class FingerprintUtility {

    private final String TAG = FingerprintUtility.class.getSimpleName();

    private static FingerprintUtility fingerprintUtility = null;

    /** Alias for our key in the Android Key Store */
    private final String KEY_NAME = "kvp.jjy.MispAndroid320.fingerprint";

    private FingerprintModule mFingerprintModule;
    private KeyguardManager mKeyguardManager;
    private FingerprintManagerCompat mFingerprintManager;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher mCipher;

    public static FingerprintUtility getInstance() {
        if (fingerprintUtility == null) {
            fingerprintUtility = new FingerprintUtility();
        }
        return fingerprintUtility;
    }
    
    private SPassFingerprintManager sPassManager;


    //=========================================================================================
    // [[ 지문 사용 가능 여부 판별을 위한 내용 추가
    //=========================================================================================

    private final String FINGERPRINT_LIBRARY = "com.pantech.device.fingerprint";
    private final String CHECK_FINGERPRINT = "fingerscan_enroll";

    /**
     * 단말이 지문을 사용할 수 있는 조건인지 여부를 체크
     * @param context
     * @return
     */
    public boolean isEnableFingerprint(Context context)
    {
        return isEnableFingerprintLegacy(context) || isEnableFingerprint_M(context);
    }

    // 삼성이나 팬텍의 지문 API를 사용 가능한 경우 true 리턴
    public boolean isEnableFingerprintLegacy(Context context){
        if(isEnableFingerprint_S(context)){
            return true;
        }

        if(isEnableFingerprint_P(context)){
            return true;
        }

        return false;
    }


    /**
     * 팬택폰 단말의 경우 지문이 사용 가능한지 여부를 체크
     * @param context
     * @return
     */
    public boolean isEnableFingerprint_P(Context context)
    {
        boolean hasFingerprintLibrary = false;
        hasFingerprintLibrary = hasSystemSharedLibraryInstalled(context, FINGERPRINT_LIBRARY) || getPantechPhone();

        Log.i(TAG, "isEnableFingerprint_P() hasFingerprintLibrary : " + hasFingerprintLibrary);
        return hasFingerprintLibrary;
    }


    /**
     * 삼성폰 단말의 경우 지문이 사용 가능한지 여부를 체크
     * @param context
     * @return
     */
    public boolean isEnableFingerprint_S(Context context)
    {
        boolean isSpassEnabled = false;
		
        try
        {
            Spass mSpass = new Spass();
            mSpass.initialize(context);
            isSpassEnabled = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
        }
        catch(SsdkUnsupportedException e)
        {
            isSpassEnabled = false;
        }
        catch(UnsupportedOperationException e)
        {
            isSpassEnabled = false;
        }

        Log.i(TAG, "isEnableFingerprint_S() isSpassEnabled : " + isSpassEnabled);

        return isSpassEnabled;
    }


    /**
     * 구글 지문 API 사용이 가능한지 여부를 체크
     * @param context
     * @return
     */
    public boolean isEnableFingerprint_M(Context context){

        boolean enable = true;

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            enable = false;
        } else {
            try {
                mFingerprintModule = new FingerprintModule();
                mKeyguardManager = mFingerprintModule.providesKeyguardManager(context);
                mFingerprintManager = mFingerprintModule.providesFingerprintManager(context);
                mKeyStore = mFingerprintModule.provicesKeystore();
                mKeyGenerator = mFingerprintModule.providesKeyGenerator();
                mCipher = mFingerprintModule.providesCipher(mKeyStore);

                if (!mKeyguardManager.isKeyguardSecure()) {
                    // Show a message that the user hasn't set up a fingerprint or lock screen.
//            Toast.makeText(this, "Secure lock screen hasn't set up.\n" + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "the user hasn't set up a fingerprint or lock screen");
                    enable = false;
                }

                if(!mFingerprintManager.isHardwareDetected()){
                    enable = false;
                }

            } catch (Exception e){
                Log.d(TAG, "isEnableFingerprint_M exception : " + e.toString());
                return false;
            }
        }

        Log.i(TAG, "isEnableFingerprint_M() : " + enable);

        return enable;
    }


    /**
     * 팬택폰 라이브러리가 설치되어 있는지 여부를 검사
     * @param context
     * @param libraryName
     * @return
     */
    private boolean hasSystemSharedLibraryInstalled(Context context, String libraryName)
    {
        boolean hasLibraryInstalled = false;
        try
        {
            if(!TextUtils.isEmpty(libraryName))
            {
                String[] installedLibraries = context.getPackageManager().getSystemSharedLibraryNames();
                if(installedLibraries != null)
                {
                    for(String s : installedLibraries)
                    {
                        if(libraryName.equals(s))
                        {
                            hasLibraryInstalled = true;
                            break;
                        }
                    }
                }
            }

            return hasLibraryInstalled;
        }
        catch(Exception e)
        {
            Utilities.ExceptionTrace(e);
            Toast.makeText(context, "fingerprint library check fail", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    /**
     * 팬택폰 단말의 모델명을 체크
     * @return
     */
    private boolean getPantechPhone()
    {
        String modelName = Build.MODEL;
        Log.i("fingerprint", "modelName=" + modelName);

        if(modelName.equals("IM-A880S") || modelName.equals("IM-A890K"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }



    /**
     * 지문 존재 여부를 검사
     * @return
     */
    public boolean isExistFingerprint(Context context)
    {
        if(isExistFingerprint_S(context)){
            return true;
        }

        if(isExistFingerprint_P(context)){
            return true;
        }

        if(isExistFingerprint_M()){
            return true;
        }

        return false;
    }

    public  boolean isExistFingerprint_P(Context context)
    {
        int checkFingerprint = Settings.Secure.getInt(context.getContentResolver(), CHECK_FINGERPRINT, 0);

        if(checkFingerprint == 1) // 지문이 있으면
        {
            return true;
        }
        Log.i(TAG, "isExistFingerprint_P() checkFingerprint : " + checkFingerprint);

        return false;
    }

    public boolean isExistFingerprint_S(Context context)
    {
        boolean hasRegisteredFinger = false;

        try {
        	sPassManager = new SPassFingerprintManager(context);
        	hasRegisteredFinger = sPassManager.hasRegisteredFinger();
            Log.i(TAG, "isExistFingerprint_S() hasRegisteredFinger : " + hasRegisteredFinger);
        } catch (NullPointerException e){
            Log.i(TAG, "exception : " + e.toString());
        }

        return hasRegisteredFinger;
    }


    /**
     * 구글 지문 사용 가능 여부 판별을 위한 내용 추가
     * @return
     */
    public boolean isExistFingerprint_M(){
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        } else {
            try {
                //noinspection ResourceType
                if (!mFingerprintManager.hasEnrolledFingerprints()) {
                    Log.d(TAG, "no fingerprints are registered");
                    return false;
                }
            } catch (NullPointerException e){
                // 구글 지문 API를 사용하지 않는 경우의 호출 예외처리
                Log.d(TAG, "mFingerprintManager is null");
                return false;
            }
        }

        return true;
    }

    //=========================================================================================
    // 지문 사용 가능 여부 판별을 위한 내용 추가 ]]
    //=========================================================================================


    public String getKeyName(){
        return KEY_NAME;
    }

    public KeyStore getKeyStore(){
        return mKeyStore;
    }

    public KeyGenerator getKeyGenerator(){
        return mKeyGenerator;
    }

    public Cipher getCipher(){
        return mCipher;
    }
    
    public SPassFingerprintManager getsPassManager(){
    	return sPassManager;
    }
}
