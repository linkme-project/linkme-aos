package com.linkme.fido.fido.module;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;

/**
 * 지문인식 UI 헬퍼 클래스
 */
public class FingerprintUiHelper extends FingerprintManagerCompat.AuthenticationCallback {

    private final String TAG = FingerprintUiHelper.class.getSimpleName();

    private Context mContext;
    private FingerprintManagerCompat mFingerprintManager;
    private CancellationSignal mCancellationSignal;
    private Callback mCallback;
    private boolean mSelfCancelled;

    public FingerprintUiHelper(Context context, FingerprintManagerCompat fingerprintManager, Callback callback) {
        mContext = context;
        mFingerprintManager = fingerprintManager;
        mCallback = callback;
    }

    /**
     * 지문 사용이 가능한지 여부를 리턴
     * <li/> 지문인식 하드웨어가 있고 등록된 지문이 있는 경우 true, 아닌 경우 false 리턴
     *
     * @return
     */
    public boolean isFingerprintAuthAvailable() {
        boolean result = false;

        try {
            if (mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints()) {
                result = true;
            }
        } catch (SecurityException e) {
            Toast.makeText(mContext, "지문 권한 설정 후 사용이 가능합니다.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, e.toString());
        }

        return result;
    }

    /**
     * 지문 인식 시작
     *
     * @param cryptoObject
     */
    public void startListening(FingerprintManagerCompat.CryptoObject cryptoObject) {

        if (!isFingerprintAuthAvailable()) {
            Log.d(TAG, "startListening isFingerprintAuthAvailable false");
            return;
        }

        Log.d(TAG, "startListening isFingerprintAuthAvailable true");

        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;

        try{
            mFingerprintManager.authenticate(cryptoObject, 0, mCancellationSignal, this, null);
        } catch (SecurityException e){
            Log.d(TAG, e.toString());
        }
    }

    /**
     * 지문인식 종료
     */
    public void stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {

    }


    /**
     * 지문인식 성공
     */
    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        mCallback.onAuthenticated();
    }

    /**
     * 지문인식 오류
     */
    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        Log.i(TAG, "onAuthenticationError errString : " + errString);
        if (!mSelfCancelled) {
            mCallback.onError();
        }
    }

    /**
     * 지문인식 실패
     */
    @Override
    public void onAuthenticationFailed() {
        mCallback.onAuthenticationFailed();
    }

    public interface Callback {
        void onAuthenticated();

        void onAuthenticationFailed();

        void onError();
    }
}
