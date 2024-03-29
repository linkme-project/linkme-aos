package com.linkme.fido.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.linkme.fido.Constant;
import com.linkme.fido.LinkMeApp;
import com.linkme.fido.R;
import com.linkme.fido.databinding.ActivityMainBinding;
import com.linkme.fido.fido.PrefUtil;
import com.linkme.fido.fido.activity.FingerprintGoogleActivity;
import com.linkme.fido.fido.activity.FingerprintLegacyActivity;
import com.linkme.fido.fido.module.FingerprintUtility;
import com.linkme.fido.utils.JavascriptBridge;
import com.vp.fido.Constants;
import com.vp.fido.VPCManager;
import com.vp.fido.interfaces.VerifyObserver;
import com.vp.fido.publisher.VerifyResultPublisher;
import com.vp.fido.util.ErrorMsg;
import com.vp.fido.util.UtilitiesForAndroid;
import com.vp.fido.verify.VerifyResult;

import java.util.HashMap;

import etri.fido.rpclient.Operation;

/**
 * MainActivity
 * 최초 실행 스플래쉬 -> 로그인 -> 메인
 * 그 이후 스플래쉬 -> 메인
 * 메인에 서비스 별 탭 구분 (홈 투자 마이 설정 더보기)
 * 이것도 아직 없음...
 */

public class MainActivity extends BaseActivity implements VPCManager.VPCManagerCallback, VerifyObserver, ActivityCompat.OnRequestPermissionsResultCallback {

    private ActivityMainBinding binding;

    private final String TAG = MainActivity.class.getSimpleName();

    // FIDO 최초 설치 시 FIDO 서버에 등록되어 있던 기 정보들을 삭제하게 된다.
    // 서버통신에 시간이 걸리므로 사용자에게 프로그래스바를 표시해 준다.
    private ProgressDialog mProgressDialog;
    private final int INIT_FIDO = 1;						// FIDO 클라이언트 초기화
    private final int SHOW_PROGRESS = 2;					// 프로그래스바 표시
    private final int DISMISS_PROGRESS = 3;					// 프로그래스바 종료
    private final int REQ_PERMISSION_READ_PHONE_STATE_CHECK = 4;				// 폰 정보 사용 퍼미션 요청
    private final int REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK = 5;			// 저장소 사용 요청
    private final int RES_PERMISSION_CHECK_FINISH = 6;							// 퍼미션 체크 완료

    private final int REG = 0;
    private final int AUTH = 1;
    private final int DEREG = 2;

    // FIDO 생체인증 프로세스를 담당하는 VPClient 클라이언트 매니저
    private VPCManager mVPCManager;

    // 지문인증장치명
    public static final String FINGER_AAID = "2216#3101";

    public static MainActivity mMainActivity;
    private FingerprintUtility mFingerprintUtil;
    private PrefUtil mPrefUtil;

    // 안드로이드 6.0 이상에서 런타임시 폰 정보에 접근하기 위한 퍼미션 요청 코드
    private static final int REQUEST_READ_PHONE_STATE_PERMISSION = 255;

    private Handler wvHandler;
    private LinkMeApp App;

    public static MainActivity getMainActivity(){
        return mMainActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);
        if (App == null) App = (LinkMeApp) getApplication();

        if(wvHandler == null) {
            wvHandler = new Handler();
        }

        binding.wvMain.loadUrl(Constant.BASE_URL);

        mMainActivity = this;
        mFingerprintUtil = FingerprintUtility.getInstance();
        mPrefUtil = new PrefUtil(this);

        // 2017.01.17. yjson. 현재 액티비티를 FIDO 인증 결과 옵저버 리스트에 등록
        VerifyResultPublisher.getInstance().addUI(MainActivity.this);
//		VerifyResultPublisher.getInstance().addFIDO(this);
        initHandler.sendEmptyMessage(REQ_PERMISSION_READ_PHONE_STATE_CHECK);

        initSplash();
        initJsBridge();
    }

    @Override
    public void getRequestData() {
        Log.d(TAG, "getRequestData()");
    }

    @Override
    public void getResponseData(int reqCode, HashMap<String, String> resParams) {
        Log.d(TAG, "getResponseData() reqCode : " + reqCode);
        Log.d(TAG, "getResponseData() resParams : " + resParams.toString());
    }

    // FIDO 서버 초기화 종료 시 콜백 응답 수신
    @Override
    public void onFinishedReqInitASM() {
        Log.d(TAG, "onFinishedReqInitASM()");
        initHandler.sendEmptyMessage(DISMISS_PROGRESS);
    }


    // 로컬 사용자 인증이 이루어지면 그 결과를 콜백 형태로 받아온다.
    // 사용자가 로컬 인증동작을 취소하거나 로컬 인증의 성공 또는 실패 시 결과값이 넘어온다.
    @Override
    public void onVerifyResult(Intent resultIntent){
        Log.d(TAG, "onVerifyResult()");

        VerifyResult result = UtilitiesForAndroid.getVerifyResultVO(resultIntent);

        // 핸들러로 로컬 인증결과 메시지 전달
        resultHandler.sendMessage(resultHandler.obtainMessage(0, result));
    }

    Handler initHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            Log.i(TAG, "initHandler msg what : " + msg.what);

            switch(msg.what){

                case INIT_FIDO :

                    // VPClient를 관리하는 매니저 인스턴스 생성
                    mVPCManager = VPCManager.getInstance(MainActivity.this);

                    //주석 풀것
                    mVPCManager.getProperties("test");

					/*Logger.setLogable(true);
					RPLog.printable = true;
					PropertiesManager.getReqUrl();
					PropertiesManager.getResUrl();*/

                    // 인증장치 초기화 결과를 받아오기 위해서 콜백 메소드 등록
                    mVPCManager.setVPCManagerCallback(MainActivity.this);

                    Log.i(TAG, "mVPCManager.isFirstExe() : " + mVPCManager.isFirstExe());
                    Log.i(TAG, "mVPCManager.isFirstExeInitFinished() : " + mVPCManager.isFirstExeInitFinished());

                    // 최초 FIDO 초기화 실행 시 SQLite DB 정보 초기화
                    if(!mVPCManager.isFirstExeInitFinished()){
                        // 초기화 진행 중 프로그래스바 표시
                        initHandler.sendEmptyMessage(SHOW_PROGRESS);
                    }
                    break;

                case SHOW_PROGRESS :

                    if(mProgressDialog == null){
                        mProgressDialog = new ProgressDialog(MainActivity.this);
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        mProgressDialog.setMessage("FIDO 정보를 초기화 중입니다.");
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.show();
                    } else {
                        mProgressDialog.show();
                    }
                    break;

                case DISMISS_PROGRESS :
                    if(mProgressDialog != null && mProgressDialog.isShowing()){
                        mProgressDialog.dismiss();
                    }
                    break;

                // 2017.03.13. yjson. 빌드 버전에 따라 danger 퍼미션 요청 구분 처리함
                case REQ_PERMISSION_READ_PHONE_STATE_CHECK :
                    // 안드로이드 6.0 이상 단말에서 동적 퍼미션 요청
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if(checkReadPhoneStatePermission()){
                            initHandler.sendEmptyMessage(REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK);
                        }
                        // 안드로이드 6.0 미만 단말에서 퍼미션 체크 bypass
                    } else {
                        initHandler.sendEmptyMessage(REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK);
                    }

                    break;

                // 2017.03.13. yjson. 빌드 버전에 따라 danger 퍼미션 요청 구분 처리함
                case REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK :
                    // 안드로이드 6.0 이상 단말에서 동적 퍼미션 요청
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if(checkWriteExternalStoragePermission()){
                            initHandler.sendEmptyMessage(RES_PERMISSION_CHECK_FINISH);
                        }
                        // 안드로이드 6.0 미만 단말에서 퍼미션 체크 bypass
                    } else {
                        initHandler.sendEmptyMessage(RES_PERMISSION_CHECK_FINISH);
                    }

                    break;

                // 2017.03.13. yjson. 권한 체크 확인 후 초기화 수행
                case RES_PERMISSION_CHECK_FINISH :
                    initHandler.sendEmptyMessage(INIT_FIDO);
                    break;

                default :break;
            }
        }
    };

    // 로컬 인증 결과값을 전달받는 핸들러
    Handler resultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            VerifyResult result = (VerifyResult) msg.obj;

            // FIDO 인증 요청 프로세스 단계로 다음 중 하나임
            // 1 : 클라이언트 기본 설정 단계
            // 2 : FIDO 서버에 사용자 선택 동작(등록, 인증, 등록 해제)을 요청하는 단계
            // 3 : ASM으로 FIDO Client 요청을 전달하는 단계
            // 4 : FIDO 서버에 실행 결과값을 요청하는 단계
            int reqType = result.getRequestType();

            // reqType의 동작 결과코드
            // 1200 일 경우 정상처리, 그 외의 경우 에러
            int resultCode = result.getResult();

            // 서버에서 내려주는 세션값
            String postData = result.getPostData();

            // FIDO 인증 요청 동작값 구분
            // "Reg" : 등록
            // "Auth" : 인증
            // "Dereg" : 등록 해제
            String opCode = result.getOperationCode();

            Log.d(TAG, "mBroadCastReceiver reqType : " + reqType);
            Log.d(TAG, "mBroadCastReceiver resultCode : " + resultCode);
            Log.d(TAG, "mBroadCastReceiver opCode : " + opCode);

            String sResult = "";

            if(etri.fido.uaf.protocol.Operation.Reg.equals(opCode)){
                sResult = "등록";
            } else if(etri.fido.uaf.protocol.Operation.Auth.equals(opCode)){
                sResult = "인증";
            } else if(etri.fido.uaf.protocol.Operation.Dereg.equals(opCode)){
                sResult = "등록 해제";
            }

            if(resultCode == mVPCManager.SUCCESS){
                if(Operation.Reg.equals(opCode)){
                    mPrefUtil.putValue(Operation.Reg, true);
                } else if(Operation.Dereg.equals(opCode)){
                    mPrefUtil.putValue(Operation.Reg, false);
                } else {
                    // 2017.06.13. yjson. 인증 결과 데이터 추출 추가
                    sResult += "\n[[ 인증결과 postData : " + result.getPostData() + " ]]\n";
                }
                sResult += "FIDO 결과 성공!!";
//                showToastMsg(reqType, resultCode, sResult, opCode);
                App.showToast("지문인증 성공", Toast.LENGTH_SHORT);
            } else {
                sResult += "FIDO 결과 실패!!";
//                showToastMsg(reqType, resultCode, sResult, opCode);
                App.showToast("지문인증 실패", Toast.LENGTH_SHORT);
            }

            switch (opCode) {
                case etri.fido.uaf.protocol.Operation.Reg:
                    bridgeCallback(REG, resultCode == 1200? true : false);
                    break;
                case etri.fido.uaf.protocol.Operation.Auth:
                    bridgeCallback(AUTH, resultCode == 1200? true : false);
                    break;
                case etri.fido.uaf.protocol.Operation.Dereg:
                    bridgeCallback(DEREG, resultCode == 1200? true : false);
                    break;
            }
        }
    };

    // 안드로이드 6.0 이상 단말에서 FIDO 인증 사용을 위한 권한 설정 팝업 표시
    private boolean checkReadPhoneStatePermission(){
        // 폰 정보를 읽어오기 위한 퍼미션이 있는 경우
        if (ContextCompat.checkSelfPermission(mMainActivity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        // 폰 정보를 읽어오기 위한 퍼미션이 없는 경우 퍼미션 요청
        else {
            ActivityCompat.requestPermissions(mMainActivity, new String[] { Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE_PERMISSION);
            return false;
        }
    }


    // 안드로이드 6.0 이상 단말에서 FIDO 인증 사용을 위한 권한 설정 팝업 표시
    private boolean checkWriteExternalStoragePermission(){
        // 저장소 사용을 위한 퍼미션이 있는 경우
        if (ContextCompat.checkSelfPermission(mMainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        // 저장소 사용을 위한 위한 퍼미션이 없는 경우 퍼미션 요청
        else {
            ActivityCompat.requestPermissions(mMainActivity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK);
            return false;
        }
    }

    // 권한 설정
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult Permission granted");

            if(requestCode == REQUEST_READ_PHONE_STATE_PERMISSION) {
                initHandler.sendEmptyMessage(REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK);
            } else if(requestCode == REQ_PERMISSION_WRITE_EXTERNAL_STORAGE_CHECK) {
                initHandler.sendEmptyMessage(INIT_FIDO);
            }
        } else {
            Log.d(TAG, "onRequestPermissionsResult Permission Denied");
            Toast.makeText(mMainActivity, "지문인증을 사용하시려면 권한 설정이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume()");
        super.onResume();
    }


    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy()");
        super.onDestroy();

        // 2017.01.17. yjson. 현재 액티비티를 FIDO 인증 결과 옵저버 리스트에서 제거
        VerifyResultPublisher.getInstance().deleteUI(this);
    }

    // 지문등록 테스트 버튼을 선택한 경우
    public void onClickFingerTestReg() {
        Log.d(TAG, "onClickFingerTest reg");

        if (!isOnline()) {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show();
        } else if(isEnableFingerprint()) {
            if (!mPrefUtil.getValue(Operation.Reg, false)) {
                // 로컬 지문인증 동작 수행
                startFingerprintVerification();
                // FIDO 지문 등록 동작 수행
                mVPCManager.doOperation(Operation.Reg, FINGER_AAID, Constants.EXE_OPTION_AUTH);
            } else {
                Toast.makeText(this, "이미 등록 되었습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "" + "지문을 사용할 수 없는 기기입니다.", Toast.LENGTH_LONG).show();
        }
    }


    // 지문인증 테스트 버튼을 선택한 경우
    public void onClickFingerTestAuth() {
        Log.d(TAG, "onClickFingerTest auth");

        if (!isOnline()) {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show();
        } else if(isEnableFingerprint()) {
            if (mPrefUtil.getValue(Operation.Reg, false)) {
                // 로컬 지문인증 동작 수행
                startFingerprintVerification();
                // FIDO 지문 인증 동작 수행
                mVPCManager.doOperation(Operation.Auth, FINGER_AAID, Constants.EXE_OPTION_AUTH);
            } else {
                Toast.makeText(this, "등록 후 사용 가능합니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "" + "지문을 사용할 수 없는 기기입니다.", Toast.LENGTH_LONG).show();
        }
    }


    // 지문해제 테스트 버튼을 선택한 경우
    public void onClickFingerTestDereg() {
        Log.d(TAG, "onClickFingerTest dereg");

        if (!isOnline()) {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show();
        } else {
            // FIDO 지문 등록 해제 동작 수행
            mVPCManager.doOperation(Operation.Dereg, FINGER_AAID, Constants.EXE_OPTION_AUTH);
        }
    }

    // 로컬 지문인증 동작 수행
    private void startFingerprintVerification(){
        // 팬택이나 삼성폰을 사용하는 경우
        if("Legacy".equals(getFingerprintAPI())){
            Intent intent = new Intent(MainActivity.this, FingerprintLegacyActivity.class);
            startActivity(intent);
            // 구글 지문 API를 사용하는 경우
        } else if("Google".equals(getFingerprintAPI())){
            Intent intent = new Intent(MainActivity.this, FingerprintGoogleActivity.class);
            startActivity(intent);
            // 지문을 사용할 수 없는 기기인 경우
        } else if("None".equals(getFingerprintAPI())){
            Toast.makeText(this, "지문을 사용할 수 없는 기기입니다.", Toast.LENGTH_SHORT).show();
        }
    }


    // 2016.03.21. class VerifyException 발생으로 지문 API 사용방법에 따라 분기 처리, Fragment 분리함
    private String getFingerprintAPI(){
        // 삼성이나 팬텍의 지문 API를 사용 가능한 경우
        if(mFingerprintUtil.isEnableFingerprintLegacy(this)){
            Log.i(TAG, "isEnableFingerprint Legacy");
            return "Legacy";
        } else if(mFingerprintUtil.isEnableFingerprint_M(this)){
            Log.i(TAG, "isEnableFingerprint Google");
            return "Google";
        } else {
            Log.i(TAG, "isEnableFingerprint None");
            return "None";
        }
    }

    // 토스트 메시지 표시
    private void showToastMsg(int reqType, int resultCode, String sResult, String opCode){
        // FIDO 인증 결과 구분값 표시
        Toast.makeText(MainActivity.this, sResult + "\nreqType : " + reqType + "\n" + "opCode code : " + opCode +"\n" + "result code : " + resultCode + "\n" +
                ErrorMsg.getInstance().getErrorMsg(opCode, resultCode), Toast.LENGTH_SHORT).show();
    }


    // 네트워크 사용 가능한 상태인지 체크
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mMainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
        Log.d(TAG, "isOnline : " + isConnected);
        return isConnected;
    }

    private boolean isEnableFingerprint() {
        FingerprintUtility fu = new FingerprintUtility();

        // 지문을 지원하는 단말인지 여부 판별
        if (fu.isEnableFingerprint(this)) {
            // 지문이 등록되어 있는지 여부 판별
            if(fu.isExistFingerprint(this)){
                return true;
            } else {
                Toast.makeText(MainActivity.this, "" + "지문 등록 후 사용해 주세요.", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        Toast.makeText(MainActivity.this, "" + "지문을 사용할 수 없는 기기입니다.", Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * JavascriptInterface 추가 및 구현
     */
    private void initJsBridge() {
        binding.wvMain.addJavascriptInterface(new JavascriptBridge() {
            @JavascriptInterface
            @Override
            public void regFido() {
                wvHandler.post(() -> onClickFingerTestReg());
            }
            @JavascriptInterface
            @Override
            public void authFido() {
                wvHandler.post(() -> onClickFingerTestAuth());
            }
            @JavascriptInterface
            @Override
            public void deregFido() {
                wvHandler.post(() -> onClickFingerTestDereg());
            }
        }, "LinkMeApp");
    }

    private void bridgeCallback(int opType, boolean result) {
        binding.wvMain.loadUrl("javascript:window.App.methods.onFidoAuth(" + opType + "," + result + ")");
    }

    private void initSplash() {
        WebViewClient webview = new LinkMeClient();
        binding.wvMain.setWebViewClient(webview);
        binding.wvMain.setWebChromeClient(new WebChromeClient());
    }

    private void hideSplash() {
        binding.rlSplash.setVisibility(View.GONE);
    }

    class LinkMeClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideSplash();
        }

        @Override

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            hideSplash();

            switch(errorCode) {
                case ERROR_AUTHENTICATION: break;               // 서버에서 사용자 인증 실패
                case ERROR_BAD_URL: break;                           // 잘못된 URL
                case ERROR_CONNECT: break;                          // 서버로 연결 실패
                case ERROR_FAILED_SSL_HANDSHAKE: break;    // SSL handshake 수행 실패
                case ERROR_FILE: break;                                  // 일반 파일 오류
                case ERROR_FILE_NOT_FOUND: break;               // 파일을 찾을 수 없습니다
                case ERROR_HOST_LOOKUP: break;           // 서버 또는 프록시 호스트 이름 조회 실패
                case ERROR_IO: break;                              // 서버에서 읽거나 서버로 쓰기 실패
                case ERROR_PROXY_AUTHENTICATION: break;   // 프록시에서 사용자 인증 실패
                case ERROR_REDIRECT_LOOP: break;               // 너무 많은 리디렉션
                case ERROR_TIMEOUT: break;                          // 연결 시간 초과
                case ERROR_TOO_MANY_REQUESTS: break;     // 페이지 로드중 너무 많은 요청 발생
                case ERROR_UNKNOWN: break;                        // 일반 오류
                case ERROR_UNSUPPORTED_AUTH_SCHEME: break; // 지원되지 않는 인증 체계
                case ERROR_UNSUPPORTED_SCHEME: break;          // URI가 지원되지 않는 방식
                default: hideSplash(); break;
            }
        }
    }
}
