package com.linkme.fido.activity;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;

import com.linkme.fido.R;
import com.linkme.fido.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
    }
}
