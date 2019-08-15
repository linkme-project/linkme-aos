package com.linkme.fido.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.linkme.fido.R;
import com.linkme.fido.databinding.ActivityMainBinding;
import com.linkme.fido.network.APIService;
import com.linkme.fido.network.RetrofitAPI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity
 * 최초 실행 스플래쉬 -> 로그인 -> 메인
 * 그 이후 스플래쉬 -> 메인
 * 메인에 서비스 별 탭 구분 (홈 투자 마이 설정 더보기)
 * 이것도 아직 없음...
 */

public class MainActivity extends BaseActivity {

    private APIService mApiService;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mApiService = RetrofitAPI.getClient().create(APIService.class);

        apiTest();
    }


    private void apiTest() {
        Call<String> call = mApiService.getNotices();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_LONG);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "failure", Toast.LENGTH_LONG);
            }
        });
    }
}
