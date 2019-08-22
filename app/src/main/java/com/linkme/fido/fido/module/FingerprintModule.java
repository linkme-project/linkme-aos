package com.linkme.fido.fido.module;

import android.app.KeyguardManager;
import android.content.Context;
import android.security.keystore.KeyProperties;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class FingerprintModule {

    /**
     * 지문인식 매니저 리턴
     * @param context
     * @return
     */
    public FingerprintManagerCompat providesFingerprintManager(Context context){
//        return (FingerprintManagerCompat) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return FingerprintManagerCompat.from(context);
    }

    /**
     * 키가드 매니저 리턴
     * @param context
     * @return
     */
    public KeyguardManager providesKeyguardManager(Context context){
        return (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    }


    /**
     * 안드로이드 키스토어 리턴
     * @return
     */
    public KeyStore provicesKeystore(){
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
//            throw new RuntimeException("Failed to get an instance of KeyStore", e);
            throw new IllegalArgumentException("Failed to get an instance of KeyStore");
        }
    }

    /**
     * 키 제너레이터 리턴
     * @return
     */
    public KeyGenerator providesKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
//            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
            throw new IllegalArgumentException("Failed to get an instance of KeyGenerator");
        }
    }

    /**
     * 암호화 객체 리턴
     * @param keyStore
     * @return
     */
    public Cipher providesCipher(KeyStore keyStore) {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            throw new RuntimeException("Failed to get an instance of Cipher", e);
            throw new IllegalArgumentException("Failed to get an instance of Cipher");
        }
    }
}
